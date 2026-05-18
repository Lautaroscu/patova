from unittest.mock import AsyncMock

import pytest

from numguard.core.cache import SpamReputationCache

pytestmark = pytest.mark.unit


class TestSpamReputationCache:
    @pytest.mark.asyncio
    async def test_get_returns_data_when_redis_available(self):
        mock_redis = AsyncMock()
        mock_redis.ping.return_value = True
        mock_redis.get.return_value = '{"verdict": "BLOCK", "score": 90}'

        cache = SpamReputationCache(redis_client=mock_redis)
        result = await cache.get("+541112345678")

        assert result is not None
        assert result["verdict"] == "BLOCK"
        assert result["score"] == 90

    @pytest.mark.asyncio
    async def test_get_returns_none_on_miss(self):
        mock_redis = AsyncMock()
        mock_redis.ping.return_value = True
        mock_redis.get.return_value = None

        cache = SpamReputationCache(redis_client=mock_redis)
        result = await cache.get("+541112345678")

        assert result is None

    @pytest.mark.asyncio
    async def test_get_returns_none_when_redis_unavailable(self):
        mock_redis = AsyncMock()
        mock_redis.ping.side_effect = ConnectionError("Redis down")

        cache = SpamReputationCache(redis_client=mock_redis)
        result = await cache.get("+541112345678")

        assert result is None

    @pytest.mark.asyncio
    async def test_set_does_not_raise_when_redis_unavailable(self):
        mock_redis = AsyncMock()
        mock_redis.ping.side_effect = ConnectionError("Redis down")

        cache = SpamReputationCache(redis_client=mock_redis)
        await cache.set("+541112345678", {"verdict": "BLOCK"})

        mock_redis.setex.assert_not_called()

    @pytest.mark.asyncio
    async def test_invalidate_does_not_raise_when_redis_unavailable(self):
        mock_redis = AsyncMock()
        mock_redis.ping.side_effect = ConnectionError("Redis down")

        cache = SpamReputationCache(redis_client=mock_redis)
        await cache.invalidate("+541112345678")

        mock_redis.delete.assert_not_called()

    @pytest.mark.asyncio
    async def test_record_report_event_without_invalidation(self):
        mock_redis = AsyncMock()
        mock_redis.ping.return_value = True
        mock_redis.incr.return_value = 2

        cache = SpamReputationCache(redis_client=mock_redis)
        should_invalidate = await cache.record_report_event("+541112345678")

        assert should_invalidate is False
        mock_redis.incr.assert_called_once()
        mock_redis.expire.assert_called_once()

    @pytest.mark.asyncio
    async def test_record_report_event_triggers_invalidation_at_threshold(self):
        mock_redis = AsyncMock()
        mock_redis.ping.return_value = True
        mock_redis.incr.return_value = 5

        cache = SpamReputationCache(redis_client=mock_redis)
        should_invalidate = await cache.record_report_event("+541112345678")

        assert should_invalidate is True
        mock_redis.delete.assert_called()

    @pytest.mark.asyncio
    async def test_record_report_event_handles_redis_failure(self):
        mock_redis = AsyncMock()
        mock_redis.ping.return_value = True
        mock_redis.incr.side_effect = ConnectionError("Redis error")

        cache = SpamReputationCache(redis_client=mock_redis)
        should_invalidate = await cache.record_report_event("+541112345678")

        assert should_invalidate is False

    @pytest.mark.asyncio
    async def test_set_uses_configured_ttl(self):
        mock_redis = AsyncMock()
        mock_redis.ping.return_value = True

        cache = SpamReputationCache(redis_client=mock_redis)
        await cache.set("+541112345678", {"verdict": "BLOCK", "score": 90})

        mock_redis.setex.assert_called_once()
        args = mock_redis.setex.call_args[0]
        assert args[1] == 3600

    @pytest.mark.asyncio
    async def test_redis_available_returns_false_when_ping_raises(self):
        mock_redis = AsyncMock()
        mock_redis.ping.side_effect = OSError("Connection refused")

        cache = SpamReputationCache(redis_client=mock_redis)
        available = await cache._redis_available()

        assert available is False

    @pytest.mark.asyncio
    async def test_get_handles_redis_get_exception(self):
        mock_redis = AsyncMock()
        mock_redis.ping.return_value = True
        mock_redis.get.side_effect = RuntimeError("Unexpected redis error")

        cache = SpamReputationCache(redis_client=mock_redis)
        result = await cache.get("+541112345678")

        assert result is None

    @pytest.mark.asyncio
    async def test_none_redis_client_returns_false_available(self):
        cache = SpamReputationCache(redis_client=None)
        available = await cache._redis_available()

        assert available is False


class TestSpamReputationCacheIntegration:
    @pytest.mark.asyncio
    async def test_full_lifecycle_get_set_invalidate(self):
        mock_redis = AsyncMock()
        mock_redis.ping.return_value = True
        mock_redis.get.return_value = '{"verdict": "SUSPECT", "score": 45}'

        cache = SpamReputationCache(redis_client=mock_redis)

        await cache.set("+541112345678", {"verdict": "SUSPECT", "score": 45})
        assert mock_redis.setex.called

        result = await cache.get("+541112345678")
        assert result == {"verdict": "SUSPECT", "score": 45}

        await cache.invalidate("+541112345678")
        assert mock_redis.delete.called


class TestCacheResilienceEndToEnd:
    @pytest.mark.asyncio
    async def test_redis_down_does_not_break_operation(self):
        mock_redis = AsyncMock()
        mock_redis.ping.side_effect = ConnectionError("Redis is down")
        mock_redis.get.side_effect = ConnectionError("Redis is down")
        mock_redis.setex.side_effect = ConnectionError("Redis is down")
        mock_redis.delete.side_effect = ConnectionError("Redis is down")
        mock_redis.incr.side_effect = ConnectionError("Redis is down")

        cache = SpamReputationCache(redis_client=mock_redis)

        get_result = await cache.get("+541112345678")
        assert get_result is None

        await cache.set("+541112345678", {"verdict": "BLOCK"})

        await cache.invalidate("+541112345678")

        should_invalidate = await cache.record_report_event("+541112345678")
        assert should_invalidate is False


class TestRateLimitSecurity:
    @pytest.mark.asyncio
    async def test_ip_rate_limit_allows_first_request(self):
        from numguard.core.security import check_ip_rate_limit

        mock_redis = AsyncMock()
        mock_redis.get.return_value = 0

        result = await check_ip_rate_limit(mock_redis, "192.168.1.1")
        assert result is True
        mock_redis.incr.assert_called_once()
        mock_redis.expire.assert_called_once()

    @pytest.mark.asyncio
    async def test_ip_rate_limit_blocks_exceeded(self):
        from numguard.core.security import check_ip_rate_limit

        mock_redis = AsyncMock()
        mock_redis.get.return_value = 60

        result = await check_ip_rate_limit(mock_redis, "192.168.1.1")
        assert result is False
        mock_redis.incr.assert_not_called()

    @pytest.mark.asyncio
    async def test_ip_rate_limit_handles_redis_failure(self):
        from numguard.core.security import check_ip_rate_limit

        mock_redis = AsyncMock()
        mock_redis.get.side_effect = ConnectionError("Redis error")

        result = await check_ip_rate_limit(mock_redis, "192.168.1.1")
        assert result is True

    @pytest.mark.asyncio
    async def test_token_rate_limit_allows_first_request(self):
        from numguard.core.security import check_token_rate_limit

        mock_redis = AsyncMock()
        mock_redis.get.return_value = 0

        result = await check_token_rate_limit(mock_redis, "test-token-123")
        assert result is True
        mock_redis.incr.assert_called_once()

    @pytest.mark.asyncio
    async def test_token_rate_limit_blocks_exceeded(self):
        from numguard.core.security import check_token_rate_limit

        mock_redis = AsyncMock()
        mock_redis.get.return_value = 1000

        result = await check_token_rate_limit(mock_redis, "test-token-123")
        assert result is False

    @pytest.mark.asyncio
    async def test_token_rate_limit_handles_redis_failure(self):
        from numguard.core.security import check_token_rate_limit

        mock_redis = AsyncMock()
        mock_redis.get.side_effect = ConnectionError("Redis error")

        result = await check_token_rate_limit(mock_redis, "test-token-123")
        assert result is True


class TestExceptionHandlingMiddleware:
    def test_404_returns_json(self, client):
        response = client.get("/this-path-does-not-exist")
        assert response.status_code == 404
        json_data = response.json()
        assert "detail" in json_data
