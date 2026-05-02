import time

import pytest
from httpx import ASGITransport, AsyncClient

from numguard.core.config import get_settings
from numguard.db.redis import get_redis_client
from numguard.main import create_app

pytestmark = pytest.mark.integration


@pytest.fixture(scope="module")
def _app():
    return create_app()


@pytest.fixture(scope="module")
async def client(_app):
    transport = ASGITransport(app=_app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        yield ac


def _headers():
    return {
        "X-NumGuard-Key": get_settings().numguard_api_key,
        "Content-Type": "application/json",
    }


def _unique_device(prefix: str) -> str:
    return f"{prefix}-{int(time.time() * 1000)}"


@pytest.fixture(autouse=True)
async def _flush_abuse_keys():
    redis_client = get_redis_client()
    keys = await redis_client.keys("numguard:report-limit:*")
    if keys:
        await redis_client.delete(*keys)
    keys = await redis_client.keys("numguard:report-dedupe:*")
    if keys:
        await redis_client.delete(*keys)


class TestReportEndpoint:
    async def test_report_requires_api_key(self, client: AsyncClient):
        response = await client.post(
            "/v1/report",
            json={
                "number": "+541112345678",
                "device_id": _unique_device("test-report"),
            },
        )
        assert response.status_code == 401

    async def test_report_new_number_creates_phone(self, client: AsyncClient):
        response = await client.post(
            "/v1/report",
            json={
                "number": "+543411234550",
                "device_id": _unique_device("dev-new"),
                "report_type": "SPAM_CALL",
            },
            headers=_headers(),
        )
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "accepted"
        assert data["number_e164"] == "+543411234550"
        assert data["report_count"] >= 1
        assert "new_spam_score" in data

    async def test_report_existing_number_increments_count(self, client: AsyncClient):
        response = await client.post(
            "/v1/report",
            json={
                "number": "+541199999999",
                "device_id": _unique_device("dev-incr"),
                "report_type": "SCAM",
            },
            headers=_headers(),
        )
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "accepted"
        assert data["number_e164"] == "+541199999999"
        assert data["report_count"] >= 1

    async def test_duplicate_report_same_device_rejected(self, client: AsyncClient):
        device = _unique_device("dev-dup")
        body = {
            "number": "+542214567890",
            "device_id": device,
            "report_type": "SPAM_CALL",
        }
        await client.post("/v1/report", json=body, headers=_headers())
        response = await client.post("/v1/report", json=body, headers=_headers())
        assert response.status_code == 429

    async def test_report_invalidates_validate_cache(self, client: AsyncClient):
        suffix = str(int(time.time() * 1000))[-6:]
        number = f"+54341{suffix}"
        await client.post(
            "/v1/report",
            json={
                "number": number,
                "device_id": _unique_device("dev-cache-inv"),
                "report_type": "SPAM_CALL",
            },
            headers=_headers(),
        )
        response = await client.post(
            "/v1/validate",
            json={
                "number": number,
                "device_id": "cache-check-dev",
            },
            headers=_headers(),
        )
        data = response.json()
        assert data["cached"] is False

    async def test_scoring_follows_weights(self, client: AsyncClient):
        suffix = str(int(time.time() * 1000))[-6:]
        number = f"+54341{suffix}"
        for i in range(3):
            resp = await client.post(
                "/v1/report",
                json={
                    "number": number,
                    "device_id": _unique_device(f"dev-score-{i:02d}"),
                    "report_type": "SPAM_CALL",
                },
                headers=_headers(),
            )
            assert resp.status_code == 200
            data = resp.json()
            assert data["report_count"] == i + 1

        response = await client.post(
            "/v1/validate",
            json={"number": number, "device_id": "score-check"},
            headers=_headers(),
        )
        data = response.json()
        assert data["report_count"] == 3


class TestFeedbackEndpoint:
    async def test_feedback_requires_api_key(self, client: AsyncClient):
        response = await client.post(
            "/v1/feedback",
            json={
                "number": "+541112345678",
                "device_id": _unique_device("dev-fb"),
                "feedback_type": "FALSE_POSITIVE",
                "related_verdict": "BLOCK",
            },
        )
        assert response.status_code == 401

    async def test_false_positive_reduces_score(self, client: AsyncClient):
        suffix = str(int(time.time() * 1000))[-6:]
        number = f"+54341{suffix}"
        await client.post(
            "/v1/report",
            json={
                "number": number,
                "device_id": _unique_device("dev-fb-fp-01"),
                "report_type": "SPAM_CALL",
            },
            headers=_headers(),
        )
        await client.post(
            "/v1/report",
            json={
                "number": number,
                "device_id": _unique_device("dev-fb-fp-02"),
                "report_type": "SPAM_CALL",
            },
            headers=_headers(),
        )

        response = await client.post(
            "/v1/feedback",
            json={
                "number": number,
                "device_id": _unique_device("dev-fb-fp-03"),
                "feedback_type": "FALSE_POSITIVE",
                "related_verdict": "BLOCK",
            },
            headers=_headers(),
        )
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "accepted"

        validate = await client.post(
            "/v1/validate",
            json={"number": number, "device_id": "fb-check"},
            headers=_headers(),
        )
        vdata = validate.json()
        assert vdata["spam_score"] <= 10

    async def test_feedback_was_spam_increases_score(self, client: AsyncClient):
        suffix = str(int(time.time() * 1000))[-6:]
        number = f"+54341{suffix}"
        response = await client.post(
            "/v1/feedback",
            json={
                "number": number,
                "device_id": _unique_device("dev-fb-ws"),
                "feedback_type": "WAS_SPAM",
                "related_verdict": "ALLOW",
            },
            headers=_headers(),
        )
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "accepted"
