import pytest
from httpx import ASGITransport, AsyncClient

from patova.core.config import get_settings
from patova.main import create_app

REQUIRED_KEYS = [
    "verdict",
    "spam_score",
    "reason",
    "report_count",
    "prefix_valid",
    "prefix_zone",
    "operator",
    "cached",
    "latency_ms",
]


@pytest.fixture(scope="module")
def _app():
    return create_app()


@pytest.fixture(scope="module")
async def client(_app):
    transport = ASGITransport(app=_app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        yield ac


def _auth_headers():
    return {
        "X-Patova-Key": get_settings().patova_api_key,
        "Content-Type": "application/json",
    }


@pytest.mark.contract
@pytest.mark.integration
class TestValidateContract:
    async def test_all_required_keys_present_on_clean(self, client: AsyncClient):
        response = await client.post(
            "/v1/validate",
            json={
                "number": "+541112345678",
                "device_id": "contract-test-clean",
                "call_direction": "INCOMING",
            },
            headers=_auth_headers(),
        )
        assert response.status_code == 200
        data = response.json()
        for key in REQUIRED_KEYS:
            assert key in data, f"Missing key '{key}' in response: {data}"

    async def test_all_required_keys_present_on_unknown(self, client: AsyncClient):
        response = await client.post(
            "/v1/validate",
            json={
                "number": "+541199988877",
                "device_id": "contract-test-unknown",
                "call_direction": "INCOMING",
            },
            headers=_auth_headers(),
        )
        assert response.status_code == 200
        data = response.json()
        for key in REQUIRED_KEYS:
            assert key in data, f"Missing key '{key}' in response: {data}"

    async def test_null_fields_allowed(self, client: AsyncClient):
        response = await client.post(
            "/v1/validate",
            json={
                "number": "+541199988877",
                "device_id": "contract-test-null",
                "call_direction": "INCOMING",
            },
            headers=_auth_headers(),
        )
        assert response.status_code == 200
        data = response.json()
        assert data["prefix_zone"] is None
        assert data["operator"] is None

    async def test_cached_field_boolean(self, client: AsyncClient):
        response = await client.post(
            "/v1/validate",
            json={
                "number": "+541112345678",
                "device_id": "contract-test-bool",
                "call_direction": "INCOMING",
            },
            headers=_auth_headers(),
        )
        assert response.status_code == 200
        assert isinstance(response.json()["cached"], bool)

    async def test_latency_ms_is_number(self, client: AsyncClient):
        response = await client.post(
            "/v1/validate",
            json={
                "number": "+541112345678",
                "device_id": "contract-test-latency",
                "call_direction": "INCOMING",
            },
            headers=_auth_headers(),
        )
        assert response.status_code == 200
        assert isinstance(response.json()["latency_ms"], (int, float))

    async def test_verdict_is_valid_enum(self, client: AsyncClient):
        response = await client.post(
            "/v1/validate",
            json={
                "number": "+541112345678",
                "device_id": "contract-test-verdict",
                "call_direction": "INCOMING",
            },
            headers=_auth_headers(),
        )
        assert response.status_code == 200
        verdict = response.json()["verdict"]
        assert verdict in {"ALLOW", "SUSPECT", "BLOCK", "UNKNOWN", "INVALID_PREFIX"}

    async def test_spam_score_is_integer(self, client: AsyncClient):
        response = await client.post(
            "/v1/validate",
            json={
                "number": "+541112345678",
                "device_id": "contract-test-int",
                "call_direction": "INCOMING",
            },
            headers=_auth_headers(),
        )
        assert response.status_code == 200
        spam_score = response.json()["spam_score"]
        assert isinstance(spam_score, int)
        assert 0 <= spam_score <= 100

    async def test_report_count_is_integer(self, client: AsyncClient):
        response = await client.post(
            "/v1/validate",
            json={
                "number": "+541112345678",
                "device_id": "contract-test-count",
                "call_direction": "INCOMING",
            },
            headers=_auth_headers(),
        )
        assert response.status_code == 200
        assert isinstance(response.json()["report_count"], int)

    async def test_prefix_valid_is_boolean(self, client: AsyncClient):
        response = await client.post(
            "/v1/validate",
            json={
                "number": "+541112345678",
                "device_id": "contract-test-prefix",
                "call_direction": "INCOMING",
            },
            headers=_auth_headers(),
        )
        assert response.status_code == 200
        assert isinstance(response.json()["prefix_valid"], bool)

    async def test_reason_is_string(self, client: AsyncClient):
        response = await client.post(
            "/v1/validate",
            json={
                "number": "+541112345678",
                "device_id": "contract-test-reason",
                "call_direction": "INCOMING",
            },
            headers=_auth_headers(),
        )
        assert response.status_code == 200
        assert isinstance(response.json()["reason"], str)
