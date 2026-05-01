
from numguard.db.redis import get_redis_client


async def test_redis_ping():
    client = get_redis_client()
    result = await client.ping()
    assert result is True
    await client.aclose()
