import hashlib

import redis.asyncio as redis
import structlog
from fastapi.security import APIKeyHeader

from numguard.core.config import get_settings

logger = structlog.get_logger(__name__)

API_KEY_HEADER = APIKeyHeader(name="X-NumGuard-Key", auto_error=False)

_RATE_LIMIT_IP_PREFIX = "numguard:rate-limit-ip"
_RATE_LIMIT_TOKEN_PREFIX = "numguard:rate-limit-token"


def _hash_token(token: str) -> str:
    return hashlib.sha256(token.encode()).hexdigest()


async def check_ip_rate_limit(
    redis_client: redis.Redis,
    ip: str,
    limit: int | None = None,
    window_sec: int | None = None,
) -> bool:
    settings = get_settings()
    limit = limit or settings.rate_limit_per_ip
    window_sec = window_sec or settings.rate_limit_redis_window_sec
    key = f"{_RATE_LIMIT_IP_PREFIX}:{ip}"

    try:
        current = int(await redis_client.get(key) or 0)
        if current >= limit:
            logger.warning("rate_limit_ip_exceeded", ip=ip, current=current, limit=limit)
            return False
        await redis_client.incr(key)
        await redis_client.expire(key, window_sec)
        return True
    except Exception:
        logger.exception("rate_limit_ip_redis_failed", ip=ip)
        return True


async def check_token_rate_limit(
    redis_client: redis.Redis,
    token: str,
    limit: int | None = None,
    window_sec: int | None = None,
) -> bool:
    settings = get_settings()
    limit = limit or settings.rate_limit_per_ip
    window_sec = window_sec or settings.rate_limit_redis_window_sec
    key = f"{_RATE_LIMIT_TOKEN_PREFIX}:{_hash_token(token)}"

    try:
        current = int(await redis_client.get(key) or 0)
        if current >= limit:
            logger.warning(
                "rate_limit_token_exceeded",
                token_hash=_hash_token(token)[:8],
                current=current,
                limit=limit,
            )
            return False
        await redis_client.incr(key)
        await redis_client.expire(key, window_sec)
        return True
    except Exception:
        logger.exception("rate_limit_token_redis_failed")
        return True
