import hashlib
import json
from typing import Any

import redis.asyncio as redis
import structlog

from patova.core.config import get_settings

logger = structlog.get_logger(__name__)

SPAM_REPUTATION_PREFIX = "patova:spam-reputation"
REPORT_COUNT_PREFIX = "patova:report-events"


def _reputation_key(number_e164: str) -> str:
    hashed = hashlib.sha256(number_e164.encode()).hexdigest()
    return f"{SPAM_REPUTATION_PREFIX}:{hashed}"


def _report_events_key(number_e164: str) -> str:
    hashed = hashlib.sha256(number_e164.encode()).hexdigest()
    return f"{REPORT_COUNT_PREFIX}:{hashed}"


class SpamReputationCache:
    def __init__(self, redis_client: redis.Redis | None = None) -> None:
        self._redis = redis_client
        self._settings = get_settings()

    async def _redis_available(self) -> bool:
        r = self._redis
        if r is None:
            return False
        try:
            return bool(await r.ping())  # type: ignore[misc]
        except Exception:
            logger.warning("redis_unavailable_falling_back_to_db")
            return False

    async def get(self, number_e164: str) -> dict | None:
        r = self._redis
        if not await self._redis_available():
            return None
        try:
            key = _reputation_key(number_e164)
            raw = await r.get(key)  # type: ignore[union-attr]
            if raw is None:
                return None
            return json.loads(raw)
        except Exception:
            logger.exception("redis_get_failed", number_e164=number_e164)
            return None

    async def set(self, number_e164: str, data: dict[str, Any]) -> None:
        r = self._redis
        if not await self._redis_available():
            return
        try:
            key = _reputation_key(number_e164)
            ttl = self._settings.spam_reputation_cache_ttl
            await r.setex(key, ttl, json.dumps(data))  # type: ignore[union-attr]
        except Exception:
            logger.exception("redis_set_failed", number_e164=number_e164)

    async def invalidate(self, number_e164: str) -> None:
        r = self._redis
        if not await self._redis_available():
            return
        try:
            key = _reputation_key(number_e164)
            await r.delete(key)  # type: ignore[union-attr]
        except Exception:
            logger.exception("redis_invalidate_failed", number_e164=number_e164)

    async def record_report_event(self, number_e164: str) -> bool:
        should_invalidate = False
        r = self._redis
        if not await self._redis_available():
            return False
        try:
            key = _report_events_key(number_e164)
            count = await r.incr(key)  # type: ignore[union-attr]
            await r.expire(key, 86400)  # type: ignore[union-attr]
            threshold = self._settings.cache_invalidation_report_threshold
            if count >= threshold:
                await self.invalidate(number_e164)
                await r.delete(key)  # type: ignore[union-attr]
                should_invalidate = True
                logger.info(
                    "cache_invalidated_by_report_threshold",
                    number_e164=number_e164,
                    report_count=count,
                )
        except Exception:
            logger.exception("redis_report_event_failed", number_e164=number_e164)
        return should_invalidate
