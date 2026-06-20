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


def _admin_headers():
    return {
        "X-Patova-Key": get_settings().patova_admin_key,
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

    async def test_stats_concurrency_tracking(self, client: AsyncClient):
        # 1. Realizar dos siembras consecutivas del mismo número
        payload = {"seed_number": "+5491112359876"}
        res1 = await client.post("/v1/admin/seed-range", json=payload, headers=_admin_headers())
        assert res1.status_code == 200

        res2 = await client.post("/v1/admin/seed-range", json=payload, headers=_admin_headers())
        assert res2.status_code == 200

        # 2. Consultar las estadísticas y verificar total_imports y top_concurrence
        response = await client.get("/v1/stats", headers=_headers())
        assert response.status_code == 200
        data = response.json()

        assert "total_imports" in data
        assert data["total_imports"] >= 2
        assert "top_concurrence" in data
        assert isinstance(data["top_concurrence"], list)

        # Buscar el número en el top_concurrence (por su formato enmascarado o valor)
        found = False
        for item in data["top_concurrence"]:
            if item["phone_number"] == 541112359876:
                assert item["import_count"] >= 2
                assert item["concurrency_percentage"] > 0
                found = True
                break
        assert found, "El número sembrado no se encontró en el top de concurrencias"


class TestAdminDashboard:
    async def test_admin_requires_api_key(self, client: AsyncClient):
        response = await client.get("/admin")
        assert response.status_code == 401

    async def test_admin_returns_html(self, client: AsyncClient):
        response = await client.get("/admin", auth=("admin", get_settings().patova_admin_key))
        assert response.status_code == 200
        assert "text/html" in response.headers["content-type"]
        assert "Patova Admin" in response.text

    async def test_admin_contains_cards(self, client: AsyncClient):
        response = await client.get("/admin", auth=("admin", get_settings().patova_admin_key))
        html = response.text
        assert "Total de Números" in html
        assert "Total de Reportes" in html
        assert "Bloqueados Hoy" in html



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
