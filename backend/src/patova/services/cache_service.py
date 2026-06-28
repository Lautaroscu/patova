import hashlib
import json

import redis.asyncio as redis

from patova.core.config import get_settings

CACHE_PREFIX = "patova:validate"

TTL_BLOCK = 3600
TTL_ALLOW = 86400
TTL_SUSPECT = 900
TTL_UNKNOWN = 300
TTL_INVALID_PREFIX = 86400

_VERDICT_TTL = {
    "BLOCK": TTL_BLOCK,
    "ALLOW": TTL_ALLOW,
    "SUSPECT": TTL_SUSPECT,
    "UNKNOWN": TTL_UNKNOWN,
    "INVALID_PREFIX": TTL_INVALID_PREFIX,
}


def _cache_key(number_e164: str) -> str:
    hashed = hashlib.sha256(number_e164.encode()).hexdigest()
    return f"{CACHE_PREFIX}:{hashed}"


async def get_cached_result(client: redis.Redis, number_e164: str) -> dict | None:
    settings = get_settings()
    if not settings.validate_cache_enabled:
        return None
    key = _cache_key(number_e164)
    raw = await client.get(key)
    if raw is None:
        return None
    return json.loads(raw)


async def set_cached_result(
    client: redis.Redis, number_e164: str, result: dict, verdict: str
) -> None:
    settings = get_settings()
    if not settings.validate_cache_enabled:
        return
    key = _cache_key(number_e164)
    ttl = _VERDICT_TTL.get(verdict, TTL_UNKNOWN)
    await client.setex(key, ttl, json.dumps(result))


async def invalidate_validate_cache(client: redis.Redis, number_e164: str) -> None:
    settings = get_settings()
    if not settings.validate_cache_enabled:
        return
    key = _cache_key(number_e164)
    await client.delete(key)


async def invalidate_validate_cache_range(
    client: redis.Redis, start_number: int, end_number: int
) -> None:
    settings = get_settings()
    if not settings.validate_cache_enabled:
        return
    pipeline = client.pipeline()
    for num in range(start_number, end_number + 1):
        key = _cache_key(f"+{num}")
        pipeline.delete(key)
    await pipeline.execute()

