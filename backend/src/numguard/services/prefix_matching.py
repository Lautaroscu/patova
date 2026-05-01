from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from numguard.models.area_prefix import AreaPrefix


async def find_prefix_id(
    session: AsyncSession, prefix_candidate: str
) -> str | None:
    stmt = select(AreaPrefix.id).where(AreaPrefix.prefix == prefix_candidate)
    result = await session.execute(stmt)
    row = result.scalar_one_or_none()
    return str(row) if row else None
