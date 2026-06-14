import gzip
import io
from datetime import datetime

from fastapi import APIRouter, Depends
from fastapi.responses import StreamingResponse
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from patova.db.session import get_session
from patova.models.enums import NumberStatus
from patova.models.phone_number import PhoneNumber
from patova.schemas.reports import CallKitDeltaResponse
from patova.services.spam_engine import SpamEngineService

router = APIRouter()


@router.get("/callkit")
async def download_callkit_list(db: AsyncSession = Depends(get_session)):
    """
    Hace streaming asíncrono y comprime en Gzip línea por línea (NDJSON comprimido)
    para que iOS lo procese eficientemente sin reventar la RAM del backend.
    """
    async def gzip_stream_generator():
        bio = io.BytesIO()
        # Inicializamos un compresor de ráfaga en memoria
        # Usamos context manager para asegurar que el trailer se escriba al final al cerrarse.
        with gzip.GzipFile(fileobj=bio, mode='wb', compresslevel=6) as gzip_file:
            # Consumimos el generador asíncrono server-side de la Fase 2
            async for phone_number in SpamEngineService.fetch_sorted_callkit_list(db):
                line = f"{phone_number}\n".encode('utf-8')
                gzip_file.write(line)
                gzip_file.flush()

                # Yield los bytes comprimidos disponibles en bio
                chunk = bio.getvalue()
                if chunk:
                    yield chunk
                    bio.seek(0)
                    bio.truncate(0)

        # Retornamos los bytes de cierre que se generan al salir del context manager
        chunk = bio.getvalue()
        if chunk:
            yield chunk

    return StreamingResponse(
        gzip_stream_generator(),
        media_type="application/gzip",
        headers={"Content-Disposition": "attachment; filename=callkit_blacklist.gz"}
    )


@router.get("/callkit/delta", response_model=CallKitDeltaResponse)
async def download_callkit_delta(
    since: datetime | None = None,
    db: AsyncSession = Depends(get_session),
):
    """
    Retorna los cambios (agregados y eliminados) desde la fecha 'since'.
    Si 'since' no se proporciona, retorna todos los números SPAM actuales como 'added'.
    """
    if since is None:
        stmt = select(PhoneNumber.phone_number).where(
            PhoneNumber.status == NumberStatus.SPAM
        )
        res = await db.execute(stmt)
        added_list = list(res.scalars().all())
        return CallKitDeltaResponse(added=added_list, removed=[])

    # 1. Obtener agregados: creados después de 'since' y con estado SPAM
    added_stmt = select(PhoneNumber.phone_number).where(
        PhoneNumber.status == NumberStatus.SPAM,
        PhoneNumber.created_at > since
    )
    added_res = await db.execute(added_stmt)
    added_list = list(added_res.scalars().all())

    # 2. Obtener removidos: modificados después de 'since', creados antes o igual a 'since'
    # y cuyo estado actual no es SPAM.
    removed_stmt = select(PhoneNumber.phone_number).where(
        PhoneNumber.status != NumberStatus.SPAM,
        PhoneNumber.updated_at > since,
        PhoneNumber.created_at <= since
    )
    removed_res = await db.execute(removed_stmt)
    removed_list = list(removed_res.scalars().all())

    return CallKitDeltaResponse(added=added_list, removed=removed_list)

