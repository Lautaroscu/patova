import pytest
from httpx import ASGITransport, AsyncClient

from numguard.core.config import get_settings
from numguard.main import create_app


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


class TestValidateEndpoint:
    async def test_api_key_required(self, client: AsyncClient):
        response = await client.post("/v1/validate", json={"number": "+541112345678"})
        assert response.status_code == 401

    async def test_clean_number_returns_allow(self, client: AsyncClient):
        response = await client.post(
            "/v1/validate",
            json={
                "number": "+541112345678",
                "device_id": "test_device_1",
                "call_direction": "INCOMING",
            },
            headers=_headers(),
        )
        assert response.status_code == 200
        data = response.json()
        assert data["verdict"] == "ALLOW"
        assert data["reason"] == "CLEAN"
        assert data["spam_score"] == 0
        assert data["prefix_valid"] is True

    async def test_spam_number_returns_block(self, client: AsyncClient):
        response = await client.post(
            "/v1/validate",
            json={
                "number": "+541199999999",
                "device_id": "test_device_2",
                "call_direction": "INCOMING",
            },
            headers=_headers(),
        )
        assert response.status_code == 200
        data = response.json()
        assert data["verdict"] == "BLOCK"
        assert data["spam_score"] == 90

    async def test_suspect_number_returns_suspect(self, client: AsyncClient):
        response = await client.post(
            "/v1/validate",
            json={
                "number": "+543515555555",
                "device_id": "test_device_3",
                "call_direction": "INCOMING",
            },
            headers=_headers(),
        )
        assert response.status_code == 200
        data = response.json()
        assert data["verdict"] == "SUSPECT"

    async def test_unknown_number_returns_unknown(self, client: AsyncClient):
        response = await client.post(
            "/v1/validate",
            json={
                "number": "+541122222222",
                "device_id": "test_device_4",
                "call_direction": "INCOMING",
            },
            headers=_headers(),
        )
        assert response.status_code == 200
        data = response.json()
        assert data["verdict"] == "UNKNOWN"

    async def test_invalid_prefix_returns_invalid_prefix(self, client: AsyncClient):
        response = await client.post(
            "/v1/validate",
            json={
                "number": "+541552222222",
                "device_id": "test_device_5",
                "call_direction": "INCOMING",
            },
            headers=_headers(),
        )
        assert response.status_code == 200
        data = response.json()
        assert data["verdict"] == "INVALID_PREFIX"

    async def test_second_call_returns_cached_true(self, client: AsyncClient):
        await client.post(
            "/v1/validate",
            json={
                "number": "+541112345678",
                "device_id": "test_device_cached",
                "call_direction": "INCOMING",
            },
            headers=_headers(),
        )
        response = await client.post(
            "/v1/validate",
            json={
                "number": "+541112345678",
                "device_id": "test_device_cached",
                "call_direction": "INCOMING",
            },
            headers=_headers(),
        )
        assert response.status_code == 200
        data = response.json()
        assert data["cached"] is True

    async def test_response_includes_latency_ms(self, client: AsyncClient):
        response = await client.post(
            "/v1/validate",
            json={
                "number": "+541112345678",
                "device_id": "test_device_latency",
                "call_direction": "INCOMING",
            },
            headers=_headers(),
        )
        assert response.status_code == 200
        data = response.json()
        assert "latency_ms" in data
        assert isinstance(data["latency_ms"], (int, float))


class TestNumberLookup:
    async def test_lookup_returns_data(self, client: AsyncClient):
        response = await client.get("/v1/number/+541112345678")
        assert response.status_code == 200
        data = response.json()
        assert data["number_e164"] == "+541112345678"
        assert data["status"] == "CLEAN"
        assert data["spam_score"] == 0

    async def test_lookup_not_found_returns_404(self, client: AsyncClient):
        response = await client.get("/v1/number/+541188888888")
        assert response.status_code == 404

    async def test_lookup_no_api_key_required(self, client: AsyncClient):
        response = await client.get("/v1/number/+541112345678")
        assert response.status_code == 200


class TestPrefixesEndpoint:
    async def test_prefixes_returns_valid(self, client: AsyncClient):
        response = await client.get("/v1/prefixes")
        assert response.status_code == 200
        data = response.json()
        assert data["count"] >= 1
        for item in data["items"]:
            assert "prefix" in item
            assert "city" in item
            assert "province" in item
            assert "is_mobile" in item

    async def test_prefixes_no_api_key_required(self, client: AsyncClient):
        response = await client.get("/v1/prefixes")
        assert response.status_code == 200
