from fastapi import APIRouter, Depends, Request
from sqlalchemy.ext.asyncio import AsyncSession

from patova.api.deps import verify_api_key
from patova.db.session import get_session
from patova.services.stats_service import get_stats

router = APIRouter()


@router.get("/stats")
async def stats(
    request: Request,
    session: AsyncSession = Depends(get_session),
    _api_key: str = Depends(verify_api_key),
):
    return await get_stats(session)
