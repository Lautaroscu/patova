import csv
import io
import logging

log = logging.getLogger(__name__)

from fastapi import APIRouter, Depends, HTTPException, UploadFile, File, status
from fastapi.responses import StreamingResponse
from sqlalchemy import func, literal_column, select
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.ext.asyncio import AsyncSession

from patova.api.deps import verify_admin_key
from patova.db.session import get_session
from patova.models.enums import NumberSource, NumberStatus
from patova.models.phone_number import PhoneNumber
from patova.schemas.reports import DeleteBlockRequest, ManualSeedRequest
from patova.services.phone_normalization import normalize_to_e164
from patova.services.spam_engine import SpamEngineService

router = APIRouter()


@router.post("/seed-range", status_code=status.HTTP_200_OK)
async def manual_admin_seed(
    payload: ManualSeedRequest,
    db: AsyncSession = Depends(get_session),
    _api_key: str = Depends(verify_admin_key),
):
    """
    Endpoint administrativo expuesto para tu panel UX/UI.
    Permite forzar manualmente la inserción y expansión de un número semilla.
    """
    normalized = normalize_to_e164(payload.seed_number)
    if not normalized:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Formato de número inválido."
        )

    phone_int = int(normalized.lstrip("+"))
    start_base, _ = SpamEngineService.get_block_bounds(phone_int)

    try:
        # 1. Expandir el bloque de 1000 números
        processed_data = SpamEngineService.expand_batch([phone_int])

        # 2. Persistir masivamente usando la sesión asíncrona
        insert_values = []
        for num, is_pred in processed_data:
            insert_values.append({
                "phone_number": num,
                "is_predicted": is_pred,
                "status": NumberStatus.SPAM,
                "spam_score": 90,
                "source": NumberSource.SEED,
                "import_count": 0 if is_pred else 1,
            })

        stmt = pg_insert(PhoneNumber).values(insert_values)
        stmt = stmt.on_conflict_do_update(
            index_elements=["phone_number"],
            set_={
                "is_predicted": stmt.excluded.is_predicted,
                "status": stmt.excluded.status,
                "spam_score": stmt.excluded.spam_score,
                "import_count": PhoneNumber.import_count + stmt.excluded.import_count,
                "updated_at": func.now(),
            }
        )
        stmt = stmt.returning(literal_column("xmax"))
        res = await db.execute(stmt)
        rows = res.fetchall()
        await db.commit()

        # Contar cuántas filas se insertaron como nuevas
        nuevos_bloqueados = sum(1 for row in rows if row[0] == 0)

        return {
            "status": "success",
            "seed_procesado": phone_int,
            "bloque_base_calculado": start_base,
            "registros_bd_impactados": nuevos_bloqueados,
        }
    except Exception as e:
        await db.rollback()
        log.exception("Error en /admin/seed-range")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error interno al procesar el rango semilla."
        )


@router.post("/prune", status_code=status.HTTP_200_OK)
async def manual_admin_prune(
    db: AsyncSession = Depends(get_session),
    _api_key: str = Depends(verify_admin_key),
):
    """
    Endpoint administrativo para gatillar manualmente la purga de registros predictivos
    antiguos y sin hits de llamadas atajadas.
    """
    try:
        purgados = await SpamEngineService.prune_predicted_numbers(db)
        return {
            "status": "success",
            "registros_purgados": purgados
        }
    except Exception as e:
        log.exception("Error en /admin/prune")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error interno al purgar registros."
        )


@router.get("/export", summary="Exportar números spam a CSV")
async def export_spam_numbers(
    db: AsyncSession = Depends(get_session),
    _api_key: str = Depends(verify_admin_key),
):
    """
    Exporta todos los números marcados como SPAM a un archivo CSV descargable.
    Incluye: número E.164, score, estado, fuente, es_predictivo, reportes.
    """
    stmt = (
        select(PhoneNumber)
        .where(PhoneNumber.status == NumberStatus.SPAM)
        .order_by(PhoneNumber.spam_score.desc())
    )
    result = await db.execute(stmt)
    numbers = result.scalars().all()

    output = io.StringIO()
    writer = csv.writer(output)
    writer.writerow(["numero_e164", "spam_score", "estado", "fuente", "es_predictivo", "cantidad_reportes"])
    for p in numbers:
        writer.writerow([
            p.number_e164,
            p.spam_score,
            p.status.value if p.status else "UNVERIFIED",
            p.source.value if p.source else "SEED",
            "1" if p.is_predicted else "0",
            p.report_count or 0,
        ])

    output.seek(0)
    return StreamingResponse(
        iter([output.getvalue()]),
        media_type="text/csv",
        headers={"Content-Disposition": "attachment; filename=patova_spam_export.csv"},
    )


@router.post("/import-csv", status_code=status.HTTP_200_OK, summary="Importar semillas desde CSV")
async def import_spam_csv(
    file: UploadFile = File(...),
    db: AsyncSession = Depends(get_session),
    _api_key: str = Depends(verify_admin_key),
):
    """
    Importa un CSV de números semilla para seed masivo.
    El CSV debe tener una columna 'numero_e164' (o ser una lista de números, uno por línea).
    Cada número se normaliza y se expande en un bloque de 1000.
    Límite: 500 semillas por importación.
    """
    contents = await file.read()
    text = contents.decode("utf-8", errors="ignore")

    seeds_raw = []
    try:
        reader = csv.DictReader(io.StringIO(text))
        for row in reader:
            val = row.get("numero_e164") or row.get("number") or row.get("numero") or ""
            if val.strip():
                seeds_raw.append(val.strip())
    except Exception:
        # Fallback: una columna sin header, un número por línea
        for line in text.splitlines():
            line = line.strip()
            if line and not line.startswith("#"):
                seeds_raw.append(line)

    if not seeds_raw:
        raise HTTPException(status_code=400, detail="El CSV no contiene números válidos.")

    if len(seeds_raw) > 500:
        raise HTTPException(status_code=400, detail="Máximo 500 semillas por importación.")

    # Normalizar y convertir a int
    phone_ints = []
    errores = []
    for raw in seeds_raw:
        normalized = normalize_to_e164(raw)
        if normalized:
            phone_ints.append(int(normalized.lstrip("+")))
        else:
            errores.append(raw)

    if not phone_ints:
        raise HTTPException(status_code=400, detail=f"Ningún número válido encontrado. Errores: {errores[:10]}")

    try:
        processed_data = SpamEngineService.expand_batch(phone_ints)

        insert_values = [
            {
                "phone_number": num,
                "is_predicted": is_pred,
                "status": NumberStatus.SPAM,
                "spam_score": 90,
                "source": NumberSource.SEED,
                "import_count": 0 if is_pred else 1,
            }
            for num, is_pred in processed_data
        ]

        # Chunking para no superar el límite de 65.535 parámetros de PostgreSQL
        CHUNK_SIZE = 500
        nuevos = 0
        for i in range(0, len(insert_values), CHUNK_SIZE):
            chunk = insert_values[i : i + CHUNK_SIZE]
            stmt = pg_insert(PhoneNumber).values(chunk)
            stmt = stmt.on_conflict_do_update(
                index_elements=["phone_number"],
                set_={
                    "is_predicted": stmt.excluded.is_predicted,
                    "status": stmt.excluded.status,
                    "spam_score": stmt.excluded.spam_score,
                    "import_count": PhoneNumber.import_count + stmt.excluded.import_count,
                    "updated_at": func.now(),
                }
            )
            stmt = stmt.returning(literal_column("xmax"))
            res = await db.execute(stmt)
            rows = res.fetchall()
            nuevos += sum(1 for row in rows if row[0] == 0)

        await db.commit()

        return {
            "status": "success",
            "semillas_procesadas": len(phone_ints),
            "registros_expandidos": len(processed_data),
            "registros_nuevos_en_bd": nuevos,
            "errores_de_formato": errores,
        }
    except Exception as e:
        await db.rollback()
        log.exception("Error en /admin/import-csv")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error interno al importar semillas desde el CSV."
        )


@router.post("/delete-block", status_code=status.HTTP_200_OK)
async def delete_admin_block(
    payload: DeleteBlockRequest,
    db: AsyncSession = Depends(get_session),
    _api_key: str = Depends(verify_admin_key),
):
    """
    Endpoint administrativo que permite eliminar de la base de datos un número semilla
    y todos los registros predictivos asociados a su bloque de 1.000.
    """
    normalized = normalize_to_e164(payload.phone_number)
    if not normalized:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Formato de número inválido."
        )

    phone_int = int(normalized.lstrip("+"))

    try:
        deleted_count, start_range, end_range = await SpamEngineService.delete_block(db, phone_int)

        # Disparar la invalidación de caché en segundo plano
        from patova.workers.tasks import invalidate_validate_cache_block_task
        invalidate_validate_cache_block_task.delay(start_range, end_range)

        return {
            "status": "success",
            "phone_procesado": phone_int,
            "bloque_inicio": start_range,
            "bloque_fin": end_range,
            "registros_eliminados": deleted_count,
        }
    except Exception as e:
        log.exception("Error en /admin/delete-block")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Error interno al eliminar el bloque."
        )


