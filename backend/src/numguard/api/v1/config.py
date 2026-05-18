from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from numguard.api.deps import verify_api_key
from numguard.db.session import get_session
from numguard.models.device_config import DeviceConfig
from numguard.schemas.device_config import DeviceConfigRequest, DeviceConfigResponse

router = APIRouter()

@router.get("/{device_id}", response_model=DeviceConfigResponse)
async def get_config(
    device_id: str,
    session: AsyncSession = Depends(get_session),
    _api_key: str = Depends(verify_api_key),
):
    stmt = select(DeviceConfig).where(DeviceConfig.device_id == device_id)
    result = await session.execute(stmt)
    config = result.scalar_one_or_none()
    
    if not config:
        return DeviceConfigResponse(device_id=device_id)
        
    return DeviceConfigResponse(
        device_id=config.device_id,
        block_non_contacts=config.block_non_contacts,
        allowed_prefixes=config.allowed_prefixes,
        blocked_prefixes=config.blocked_prefixes,
    )

@router.put("/{device_id}", response_model=DeviceConfigResponse)
async def update_config(
    device_id: str,
    body: DeviceConfigRequest,
    session: AsyncSession = Depends(get_session),
    _api_key: str = Depends(verify_api_key),
):
    stmt = select(DeviceConfig).where(DeviceConfig.device_id == device_id)
    result = await session.execute(stmt)
    config = result.scalar_one_or_none()
    
    if not config:
        config = DeviceConfig(device_id=device_id)
        session.add(config)
        
    config.block_non_contacts = body.block_non_contacts
    config.allowed_prefixes = body.allowed_prefixes
    config.blocked_prefixes = body.blocked_prefixes
    
    await session.commit()
    
    return DeviceConfigResponse(
        device_id=config.device_id,
        block_non_contacts=config.block_non_contacts,
        allowed_prefixes=config.allowed_prefixes,
        blocked_prefixes=config.blocked_prefixes,
    )
