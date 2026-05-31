import pytest
from httpx import ASGITransport, AsyncClient

from patova.core.config import get_settings
from patova.main import create_app

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


class TestAdminDashboard:
    async def test_dashboard_loads(self, client: AsyncClient):
        response = await client.get("/admin", headers=_headers())
        assert response.status_code == 200
        assert "</html>" in response.text

    async def test_dashboard_shows_top_reported(self, client: AsyncClient):
        response = await client.get("/admin", headers=_headers())
        assert "Top Reported" in response.text
