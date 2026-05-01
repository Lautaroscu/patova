import json
from unittest.mock import AsyncMock

import pytest

from numguard.services.cache_service import (
    TTL_ALLOW,
    TTL_BLOCK,
    TTL_INVALID_PREFIX,
    TTL_SUSPECT,
    TTL_UNKNOWN,
    _cache_key,
    get_cached_result,
    set_cached_result,
)


class TestCacheKey:
    def test_key_does_not_contain_raw_number(self):
        key = _cache_key("+541112345678")
        assert "+54" not in key
        assert "1112345678" not in key
        assert key.startswith("numguard:validate:")

    def test_key_is_deterministic(self):
        assert _cache_key("+541112345678") == _cache_key("+541112345678")

    def test_different_numbers_produce_different_keys(self):
        assert _cache_key("+541112345678") != _cache_key("+541199999999")

    def test_cache_key_prefix(self):
        key = _cache_key("+541112345678")
        assert key.count("numguard:validate:") == 1


class TestGetCachedResult:
    @pytest.mark.asyncio
    async def test_returns_none_on_miss(self):
        mock_redis = AsyncMock()
        mock_redis.get.return_value = None
        result = await get_cached_result(mock_redis, "+541112345678")
        assert result is None

    @pytest.mark.asyncio
    async def test_returns_parsed_dict_on_hit(self):
        mock_redis = AsyncMock()
        mock_redis.get.return_value = json.dumps({"verdict": "BLOCK", "spam_score": 90})
        result = await get_cached_result(mock_redis, "+541112345678")
        assert result == {"verdict": "BLOCK", "spam_score": 90}


class TestSetCachedResult:
    @pytest.mark.asyncio
    async def test_sets_with_correct_ttl_for_block(self):
        mock_redis = AsyncMock()
        await set_cached_result(
            mock_redis, "+541112345678", {"verdict": "BLOCK"}, "BLOCK"
        )
        mock_redis.setex.assert_called_once()
        args = mock_redis.setex.call_args
        assert args[0][1] == TTL_BLOCK

    @pytest.mark.asyncio
    async def test_sets_with_correct_ttl_for_allow(self):
        mock_redis = AsyncMock()
        await set_cached_result(
            mock_redis, "+541112345678", {"verdict": "ALLOW"}, "ALLOW"
        )
        args = mock_redis.setex.call_args
        assert args[0][1] == TTL_ALLOW

    @pytest.mark.asyncio
    async def test_sets_with_correct_ttl_for_suspect(self):
        mock_redis = AsyncMock()
        await set_cached_result(
            mock_redis, "+541112345678", {"verdict": "SUSPECT"}, "SUSPECT"
        )
        args = mock_redis.setex.call_args
        assert args[0][1] == TTL_SUSPECT

    @pytest.mark.asyncio
    async def test_sets_with_correct_ttl_for_unknown(self):
        mock_redis = AsyncMock()
        await set_cached_result(
            mock_redis, "+541112345678", {"verdict": "UNKNOWN"}, "UNKNOWN"
        )
        args = mock_redis.setex.call_args
        assert args[0][1] == TTL_UNKNOWN

    @pytest.mark.asyncio
    async def test_sets_with_correct_ttl_for_invalid_prefix(self):
        mock_redis = AsyncMock()
        await set_cached_result(
            mock_redis, "+541112345678", {"verdict": "INVALID_PREFIX"}, "INVALID_PREFIX"
        )
        args = mock_redis.setex.call_args
        assert args[0][1] == TTL_INVALID_PREFIX

    @pytest.mark.asyncio
    async def test_cache_key_uses_sha256_hash(self):
        mock_redis = AsyncMock()
        await set_cached_result(
            mock_redis, "+541112345678", {"verdict": "BLOCK"}, "BLOCK"
        )
        key = mock_redis.setex.call_args[0][0]
        assert "numguard:validate:" in key
        assert "+54" not in key
        assert "1112345678" not in key
