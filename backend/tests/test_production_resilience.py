import asyncio
from datetime import datetime, timedelta, UTC
import pytest
from httpx import ASGITransport, AsyncClient
from sqlalchemy import delete, select
from sqlalchemy.ext.asyncio import async_sessionmaker, create_async_engine

from patova.core.config import get_settings
from patova.main import create_app
from patova.models.blocked_call_log import BlockedCallLog
from patova.models.enums import NumberStatus
from patova.models.phone_number import PhoneNumber

pytestmark = pytest.mark.integration


@pytest.fixture(scope="module")
def _app():
    return create_app()


@pytest.fixture(scope="module")
async def client(_app):
    transport = ASGITransport(app=_app)
    async with AsyncClient(transport=transport, base_url="http://test") as ac:
        yield ac


@pytest.fixture
async def clean_db():
    settings = get_settings()
    engine = create_async_engine(settings.database_url, echo=False)
    async_session = async_sessionmaker(engine, expire_on_commit=False)
    async with async_session() as s:
        # Limpiar números y logs de prueba antes
        await s.execute(delete(BlockedCallLog).where(BlockedCallLog.phone_number.between(549111234000, 549111235999)))
        await s.execute(delete(PhoneNumber).where(PhoneNumber.phone_number.between(549111234000, 549111235999)))
        await s.commit()
        yield s
        # Limpiar después
        await s.execute(delete(BlockedCallLog).where(BlockedCallLog.phone_number.between(549111234000, 549111235999)))
        await s.execute(delete(PhoneNumber).where(PhoneNumber.phone_number.between(549111234000, 549111235999)))
        await s.commit()
    await engine.dispose()


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


class TestProductionResilience:
    async def test_downloads_callkit_delta(self, client: AsyncClient, clean_db):
        now_dt = datetime.now(UTC)
        ten_days_ago = now_dt - timedelta(days=10)
        five_days_ago = now_dt - timedelta(days=5)

        # 1. Insertar un número antiguo (hace 10 días)
        phone_old = PhoneNumber(
            phone_number=549111234100,
            status=NumberStatus.SPAM,
            created_at=ten_days_ago,
            updated_at=ten_days_ago,
        )
        # 2. Insertar un número nuevo (hace 1 día)
        phone_new = PhoneNumber(
            phone_number=549111234200,
            status=NumberStatus.SPAM,
            created_at=now_dt - timedelta(days=1),
            updated_at=now_dt - timedelta(days=1),
        )

        clean_db.add_all([phone_old, phone_new])
        await clean_db.commit()

        # Consultar delta sin since (debería traer todos como added)
        response = await client.get("/v1/downloads/callkit/delta")
        assert response.status_code == 200
        data = response.json()
        assert 549111234100 in data["added"]
        assert 549111234200 in data["added"]
        assert 549111234100 not in data["removed"]
        assert 549111234200 not in data["removed"]

        # Consultar delta con since = hace 5 días
        since_str = five_days_ago.strftime("%Y-%m-%dT%H:%M:%SZ")
        response = await client.get(f"/v1/downloads/callkit/delta?since={since_str}")
        assert response.status_code == 200
        data = response.json()
        # El número antiguo no debería estar en added (se creó hace 10 días)
        assert 549111234100 not in data["added"]
        # El número nuevo sí debería estar en added
        assert 549111234200 in data["added"]
        assert 549111234100 not in data["removed"]
        assert 549111234200 not in data["removed"]

        # 3. Remover el número antiguo (marcarlo como CLEAN en una fecha reciente)
        # Esto setea updated_at al momento de la actualización (ahora)
        # pero mantiene created_at hace 10 días (antes de since)
        phone_old.status = NumberStatus.CLEAN
        phone_old.updated_at = datetime.now(UTC)
        clean_db.add(phone_old)
        await clean_db.commit()

        # Consultar delta nuevamente con since = hace 5 días
        response = await client.get(f"/v1/downloads/callkit/delta?since={since_str}")
        assert response.status_code == 200
        data = response.json()
        # Debería aparecer en removed ya que dejó de ser SPAM
        assert 549111234100 in data["removed"]

    async def test_reports_batch_abuse_protection(self, client: AsyncClient, clean_db):
        # 1. Test batch size limit (> 50 numbers)
        payload = {
            "device_id": "abuse-device-id",
            "numbers": list(range(5491161234000, 5491161234055))  # 55 números (> 50)
        }
        response = await client.post("/v1/reports/batch", json=payload, headers=_headers())
        assert response.status_code == 400
        assert "supera el límite" in response.json()["detail"]

        # 2. Test rate limit (max 5/minute)
        valid_payload = {
            "device_id": "abuse-device-id-2",
            "numbers": [5491161234100]
        }

        # Realizar 5 peticiones exitosas consecutivas
        for _ in range(5):
            response = await client.post("/v1/reports/batch", json=valid_payload, headers=_headers())
            assert response.status_code in (201, 429)

        # La 6ta petición debería retornar 429
        response = await client.post("/v1/reports/batch", json=valid_payload, headers=_headers())
        assert response.status_code == 429

    async def test_data_pruning(self, client: AsyncClient, clean_db):
        now_dt = datetime.now(UTC)
        old_dt = now_dt - timedelta(days=200)  # > 6 meses

        # 1. Antiguo predictivo SIN hits (debería ser purgado)
        phone_old_pred = PhoneNumber(
            phone_number=549111234300,
            is_predicted=True,
            created_at=old_dt,
        )

        # 2. Antiguo predictivo CON hits (debería mantenerse)
        phone_old_pred_with_hits = PhoneNumber(
            phone_number=549111234400,
            is_predicted=True,
            created_at=old_dt,
        )
        clean_db.add_all([phone_old_pred, phone_old_pred_with_hits])
        await clean_db.flush()

        log = BlockedCallLog(
            phone_number=549111234400,
            device_id="test-device-prune",
        )
        clean_db.add(log)

        # 3. Nuevo predictivo SIN hits (debería mantenerse, < 6 meses)
        phone_new_pred = PhoneNumber(
            phone_number=549111234500,
            is_predicted=True,
            created_at=now_dt,
        )
        clean_db.add(phone_new_pred)
        await clean_db.commit()

        # Invocar endpoint de pruning
        response = await client.post("/v1/admin/prune", headers=_admin_headers())
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "success"
        # Debería haber purgado exactamente 1 número (549111234300)
        assert data["registros_purgados"] == 1

        # Verificar qué números siguen existiendo en la base de datos
        stmt = select(PhoneNumber.phone_number).where(
            PhoneNumber.phone_number.in_([549111234300, 549111234400, 549111234500])
        )
        res = await clean_db.execute(stmt)
        remaining = list(res.scalars().all())

        assert 549111234300 not in remaining
        assert 549111234400 in remaining
        assert 549111234500 in remaining
