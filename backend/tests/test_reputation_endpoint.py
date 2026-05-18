import pytest
from httpx import ASGITransport, AsyncClient

from numguard.core.config import get_settings
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


class TestSpamReputationEndpoint:
    async def test_requires_api_key(self, client: AsyncClient):
        resp = await client.get("/v1/spam/reputation/+541112345678")
        assert resp.status_code == 401

    async def test_returns_unknown_for_unknown_hash(self, client: AsyncClient):
        resp = await client.get(
            "/v1/spam/reputation/e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            headers=_headers(),
        )
        assert resp.status_code == 200
        data = resp.json()
        assert data["phone_hash"] == "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        assert data["reputation_state"] == "UNKNOWN"
        assert data["reputation_score"] == 0.0
        assert "explainability" in data

    async def test_accepts_plain_number(self, client: AsyncClient):
        resp = await client.get(
            "/v1/spam/reputation/+541112345678",
            headers=_headers(),
        )
        assert resp.status_code == 200
        data = resp.json()
        assert len(data["phone_hash"]) == 64

    async def test_response_schema_fields(self, client: AsyncClient):
        resp = await client.get(
            "/v1/spam/reputation/+541112345678",
            headers=_headers(),
        )
        assert resp.status_code == 200
        data = resp.json()
        for field in (
            "phone_hash",
            "reputation_score",
            "reputation_state",
            "total_reports",
            "unique_reporters",
            "confidence",
            "explainability",
            "last_seen",
        ):
            assert field in data, f"Missing field: {field}"

        assert 0.0 <= data["reputation_score"] <= 1.0
        assert 0.0 <= data["confidence"] <= 1.0
        assert data["reputation_state"] in (
            "SAFE", "LIKELY_SAFE", "UNKNOWN", "SUSPICIOUS", "LIKELY_SPAM", "BLOCKED"
        )
