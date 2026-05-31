import pytest
from httpx import ASGITransport, AsyncClient

from patova.core.config import get_settings
from patova.main import create_app
from patova.services.stats_service import mask_e164

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
        "X-Patova-Key": get_settings().patova_api_key,
        "Content-Type": "application/json",
    }


class TestStatsEndpoint:
    async def test_stats_requires_api_key(self, client: AsyncClient):
        response = await client.get("/v1/stats")
        assert response.status_code == 401

    async def test_stats_with_key_returns_structure(self, client: AsyncClient):
        response = await client.get("/v1/stats", headers=_headers())
        assert response.status_code == 200
        data = response.json()
        assert "total_numbers" in data
        assert "total_reports" in data
        assert "blocked_today" in data
        assert "top_reported" in data
        assert isinstance(data["top_reported"], list)

    async def test_stats_top_numbers_are_masked(self, client: AsyncClient):
        response = await client.get("/v1/stats", headers=_headers())
        data = response.json()
        for item in data["top_reported"]:
            masked = item["number_e164_masked"]
            assert "****" in masked


class TestAdminDashboard:
    async def test_admin_requires_api_key(self, client: AsyncClient):
        response = await client.get("/admin")
        assert response.status_code == 401

    async def test_admin_returns_html(self, client: AsyncClient):
        response = await client.get("/admin", headers=_headers())
        assert response.status_code == 200
        assert "text/html" in response.headers["content-type"]
        assert "Patova Admin" in response.text

    async def test_admin_contains_cards(self, client: AsyncClient):
        response = await client.get("/admin", headers=_headers())
        html = response.text
        assert "Total Numbers" in html
        assert "Total Reports" in html
        assert "Blocked Today" in html


class TestMetricsEndpoint:
    async def test_metrics_returns_200(self, client: AsyncClient):
        response = await client.get("/metrics", follow_redirects=True)
        assert response.status_code == 200
        assert "patova" in response.text

    async def test_metrics_show_validate_counts_after_usage(self, client: AsyncClient):
        await client.post(
            "/v1/validate",
            json={"number": "+541112345678", "device_id": "metrics-test-dev"},
            headers=_headers(),
        )
        response = await client.get("/metrics", follow_redirects=True)
        assert response.status_code == 200
        assert "patova_validate_requests_total" in response.text
        assert "patova_validate_latency_seconds" in response.text

    async def test_metrics_show_report_counts_after_usage(self, client: AsyncClient):
        await client.post(
            "/v1/report",
            json={
                "number": "+541112345678",
                "device_id": "metrics-report-dev",
                "report_type": "SPAM_CALL",
            },
            headers=_headers(),
        )
        response = await client.get("/metrics", follow_redirects=True)
        assert response.status_code == 200
        assert "patova_reports_total" in response.text


class TestMasking:
    def test_mask_standard_number(self):
        assert mask_e164("+5491112345678") == "+5491****5678"

    def test_mask_short_number(self):
        assert mask_e164("+541112") == "+54****"

    def test_mask_already_masked(self):
        masked = mask_e164("+541112345678")
        assert len(masked) == len("+5411****5678")
        assert "1234" not in masked

    def test_mask_non_e164(self):
        assert mask_e164("not_a_number") == "not_a_number"
