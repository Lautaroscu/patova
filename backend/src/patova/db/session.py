from collections.abc import AsyncGenerator

from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from sqlalchemy.pool import NullPool

from patova.core.config import get_settings

_engine = None
_async_sessionmaker: async_sessionmaker[AsyncSession] | None = None


def _get_async_sessionmaker() -> async_sessionmaker[AsyncSession]:
    global _engine, _async_sessionmaker
    if _engine is None:
        settings = get_settings()
        _engine = create_async_engine(
            settings.database_url, echo=False, poolclass=NullPool
        )
        _async_sessionmaker = async_sessionmaker(_engine, expire_on_commit=False)
    return _async_sessionmaker


async def get_session() -> AsyncGenerator[AsyncSession, None]:
    sessionmaker = _get_async_sessionmaker()
    async with sessionmaker() as session:
        yield session
