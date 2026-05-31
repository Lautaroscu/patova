import redis.asyncio as redis

from patova.core.config import get_settings


def get_redis_client() -> redis.Redis:
    settings = get_settings()
    return redis.Redis.from_url(str(settings.redis_url), decode_responses=True)
