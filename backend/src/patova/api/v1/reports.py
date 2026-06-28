import hashlib
import logging

log = logging.getLogger(__name__)

from fastapi import APIRouter, Depends, HTTPException, Request, status
from sqlalchemy import func, literal_column
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.ext.asyncio import AsyncSession


from patova.api.deps import verify_api_key
from patova.core.metrics import reports_total
from patova.db.redis import get_redis_client
from patova.db.session import get_session
from patova.models.enums import NumberSource, NumberStatus
from patova.models.phone_number import PhoneNumber
from patova.schemas.reports import AndroidReportBatch, ReportRequest, ReportResponse
from patova.services.abuse_guard import check_dedupe, check_report_quota, record_report
from patova.services.phone_normalization import normalize_to_e164
from patova.services.report_service import process_report
from patova.services.rate_limit import get_limiter
from patova.services.spam_engine import SpamEngineService
from patova.workers.tasks import invalidate_validate_cache_task, recalculate_spam_score

router = APIRouter()
limiter = get_limiter()


def _hash_ip(request: Request) -> str | None:
    forwarded = request.headers.get("X-Forwarded-For")
    if forwarded:
        ip = forwarded.split(",")[0].strip()
    else:
        ip = request.client.host if request.client else None
    if not ip:
        return None
    import hashlib

    return hashlib.sha256(ip.encode()).hexdigest()


@router.post("/report", response_model=ReportResponse)
async def report(
    request: Request,
    body: ReportRequest,
    session: AsyncSession = Depends(get_session),
    _api_key: str = Depends(verify_api_key),
):
    redis_client = get_redis_client()

    if not await check_report_quota(redis_client, body.device_id):
        raise HTTPException(
            status_code=429,
            detail="Daily report limit reached for this device",
        )

    if not await check_dedupe(redis_client, body.device_id, body.number):
        raise HTTPException(
            status_code=429,
            detail=(
                "Duplicate report: same device already reported "
                "this number in the last 24 hours"
            ),
        )

    ip_hash = _hash_ip(request)
    try:
        response = await process_report(session, body, ip_hash)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

    await record_report(redis_client, body.device_id, body.number)
    reports_total.inc()

    recalculate_spam_score.delay(str(response.number_e164))
    invalidate_validate_cache_task.delay(response.number_e164)

    return response


@router.post("/reports/batch", status_code=status.HTTP_201_CREATED)
@limiter.limit("5/minute")
async def report_spam_batch(
    request: Request,
    payload: AndroidReportBatch,
    db: AsyncSession = Depends(get_session),
    _api_key: str = Depends(verify_api_key),
):
    """
    Endpoint utilizado por los clientes Android para sincronizar periódicamente
    los números bloqueados localmente. Dispara la expansión automática por rangos.
    """
    if not payload.numbers:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="La lista de números no puede estar vacía."
        )

    if len(payload.numbers) > 50:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="El lote supera el límite máximo de 50 números por solicitud."
        )

    # 1. Normalizar cada número usando normalize_to_e164
    normalized_numbers = []
    for num in payload.numbers:
        norm = normalize_to_e164(str(num))
        if norm:
            normalized_numbers.append(int(norm.lstrip("+")))
        else:
            log.warning("Número de lote inválido omitido: %s", num)

    if not normalized_numbers:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Ninguno de los números provistos en el lote es válido."
        )

    try:
        # 2. Ejecutar la deduplicación y expansión en memoria
        processed_data = SpamEngineService.expand_batch(normalized_numbers)

        # 2. Construir valores a insertar
        insert_values = [
            {
                "phone_number": num,
                "is_predicted": is_pred,
                "status": NumberStatus.SPAM,
                "spam_score": 90,
                "source": NumberSource.CROWDSOURCE,
            }
            for num, is_pred in processed_data
        ]

        # 3. Persistir en chunks de 500 filas para no superar el límite de
        #    65.535 parámetros de PostgreSQL (cada fila tiene ~10 columnas).
        CHUNK_SIZE = 500
        nuevos_bloqueados = 0
        for i in range(0, len(insert_values), CHUNK_SIZE):
            chunk = insert_values[i : i + CHUNK_SIZE]
            stmt = pg_insert(PhoneNumber).values(chunk)
            stmt = stmt.on_conflict_do_update(
                index_elements=["phone_number"],
                set_={
                    "is_predicted": stmt.excluded.is_predicted,
                    "status": stmt.excluded.status,
                    "spam_score": stmt.excluded.spam_score,
                    "updated_at": func.now(),
                }
            )
            stmt = stmt.returning(literal_column("xmax"))
            res = await db.execute(stmt)
            rows = res.fetchall()
            nuevos_bloqueados += sum(1 for row in rows if row[0] == 0)

        await db.commit()

        return {
            "status": "success",
            "numeros_nuevos_guardados": nuevos_bloqueados
        }
    except Exception as e:
        await db.rollback()
        log.exception("Error en /reports/batch")
        raise HTTPException(status_code=500, detail="Error interno al procesar el lote. Intente más tarde.")


@router.post("/report/public", response_model=ReportResponse)
@limiter.limit("5/minute")
async def report_public(
    request: Request,
    body: ReportRequest,
    session: AsyncSession = Depends(get_session),
):
    redis_client = get_redis_client()

    # Si el device_id viene del cliente web, le anexamos el hash de su IP y lo hasheamos a SHA-256
    # (64 caracteres) para que quepa en el campo reporter_device_id (VARCHAR(64)) de la base de datos.
    ip_hash = _hash_ip(request) or "unknown-ip"
    combined = f"web-{body.device_id}-{ip_hash}"
    body.device_id = hashlib.sha256(combined.encode()).hexdigest()


    if not await check_report_quota(redis_client, body.device_id):
        raise HTTPException(
            status_code=429,
            detail="Se ha alcanzado el límite diario de reportes para este dispositivo.",
        )

    if not await check_dedupe(redis_client, body.device_id, body.number):
        raise HTTPException(
            status_code=429,
            detail=(
                "Reporte duplicado: este dispositivo ya reportó "
                "este número en las últimas 24 horas."
            ),
        )

    try:
        response = await process_report(session, body, ip_hash)
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))

    await record_report(redis_client, body.device_id, body.number)
    reports_total.inc()

    recalculate_spam_score.delay(str(response.number_e164))
    invalidate_validate_cache_task.delay(response.number_e164)

    return response

