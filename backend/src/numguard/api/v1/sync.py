from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from numguard.api.deps import verify_api_key
from numguard.db.session import get_session
from numguard.schemas.sync import SyncRequest, SyncResponse
from numguard.services.sync import sync_behavior

router = APIRouter()


@router.post("/behavior/sync", response_model=SyncResponse)
async def behavior_sync(
    body: SyncRequest,
    session: AsyncSession = Depends(get_session),
    _api_key: str = Depends(verify_api_key),
):
    return await sync_behavior(
        session=session,
        user_id=body.user_id,
        client_last_sync_timestamp=body.client_last_sync_timestamp,
        local_changes=body.local_changes,
    )
