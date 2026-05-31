import hashlib
from datetime import UTC, datetime

import redis.asyncio as redis

from patova.core.config import get_settings

_REPORT_LIMIT_PREFIX = "patova:report-limit"
_REPORT_DEDUPE_PREFIX = "patova:report-dedupe"


def _today_key() -> str:
    return datetime.now(UTC).strftime("%Y-%m-%d")


def _hash_device(device_id: str) -> str:
    return hashlib.sha256(device_id.encode()).hexdigest()


def _hash_number(number_e164: str) -> str:
    return hashlib.sha256(number_e164.encode()).hexdigest()


async def check_report_quota(client: redis.Redis, device_id: str) -> bool:
    settings = get_settings()
    limit = settings.max_reports_per_device_per_day
    key = f"{_REPORT_LIMIT_PREFIX}:{_today_key()}:{_hash_device(device_id)}"
    current = await client.get(key)
    if current is None:
        return True
    return int(current) < limit


async def check_dedupe(client: redis.Redis, device_id: str, number_e164: str) -> bool:
    device_h = _hash_device(device_id)
    number_h = _hash_number(number_e164)
    key = f"{_REPORT_DEDUPE_PREFIX}:{_today_key()}:{device_h}:{number_h}"
    exists = await client.get(key)
    return exists is None


async def record_report(client: redis.Redis, device_id: str, number_e164: str) -> None:
    device_h = _hash_device(device_id)
    number_h = _hash_number(number_e164)
    limit_key = f"{_REPORT_LIMIT_PREFIX}:{_today_key()}:{device_h}"
    dedupe_key = f"{_REPORT_DEDUPE_PREFIX}:{_today_key()}:{device_h}:{number_h}"

    await client.incr(limit_key)
    await client.expire(limit_key, 86400)

    await client.setex(dedupe_key, 86400, "1")
