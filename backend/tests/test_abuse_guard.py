from unittest.mock import AsyncMock

import pytest

from numguard.services.abuse_guard import (
    _hash_device,
    _hash_number,
    check_dedupe,
    check_report_quota,
    record_report,
)

pytestmark = pytest.mark.unit


class TestCheckReportQuota:
    @pytest.mark.asyncio
    async def test_under_limit_allowed(self):
        mock_redis = AsyncMock()
        mock_redis.get.return_value = b"2"
        result = await check_report_quota(mock_redis, "device-1")
        assert result is True

    @pytest.mark.asyncio
    async def test_at_limit_rejected(self):
        mock_redis = AsyncMock()
        mock_redis.get.return_value = b"3"
        result = await check_report_quota(mock_redis, "device-2")
        assert result is False

    @pytest.mark.asyncio
    async def test_no_reports_allowed(self):
        mock_redis = AsyncMock()
        mock_redis.get.return_value = None
        result = await check_report_quota(mock_redis, "device-3")
        assert result is True


class TestCheckDedupe:
    @pytest.mark.asyncio
    async def test_not_duplicate(self):
        mock_redis = AsyncMock()
        mock_redis.get.return_value = None
        result = await check_dedupe(mock_redis, "device-1", "+541112345678")
        assert result is True

    @pytest.mark.asyncio
    async def test_is_duplicate(self):
        mock_redis = AsyncMock()
        mock_redis.get.return_value = b"1"
        result = await check_dedupe(mock_redis, "device-1", "+541112345678")
        assert result is False


class TestRecordReport:
    @pytest.mark.asyncio
    async def test_records_limit_and_dedupe(self):
        mock_redis = AsyncMock()
        await record_report(mock_redis, "device-1", "+541112345678")
        assert mock_redis.incr.call_count == 1
        assert mock_redis.setex.call_count == 1
        assert mock_redis.expire.call_count == 1


class TestPrivacyHashes:
    def test_device_id_hashed(self):
        h = _hash_device("my-device-id")
        assert len(h) == 64
        assert "my-device-id" not in h

    def test_number_hashed(self):
        h = _hash_number("+541112345678")
        assert len(h) == 64
        assert "+54" not in h
