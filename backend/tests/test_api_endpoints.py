import gzip
import io
import pytest
from httpx import ASGITransport, AsyncClient
from sqlalchemy import delete, select
from sqlalchemy.ext.asyncio import async_sessionmaker, create_async_engine

from patova.core.config import get_settings
from patova.main import create_app
from patova.models.phone_number import PhoneNumber
from patova.models.report import Report


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
        # Limpieza de números de prueba antes
        await s.execute(delete(Report).where((Report.phone_number.between(549111234000, 549111235999)) | (Report.phone_number.between(541112340000, 541112359999))))
        await s.execute(delete(PhoneNumber).where((PhoneNumber.phone_number.between(549111234000, 549111235999)) | (PhoneNumber.phone_number.between(541112340000, 541112359999))))
        await s.commit()
        yield s
        # Limpieza después
        await s.execute(delete(Report).where((Report.phone_number.between(549111234000, 549111235999)) | (Report.phone_number.between(541112340000, 541112359999))))
        await s.execute(delete(PhoneNumber).where((PhoneNumber.phone_number.between(549111234000, 549111235999)) | (PhoneNumber.phone_number.between(541112340000, 541112359999))))
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


class TestApiEndpoints:
    async def test_reports_batch_endpoint(self, client: AsyncClient, clean_db):
        # 1. Enviar lote de reportes
        payload = {
            "device_id": "test-android-batch-device",
            "numbers": [549111234200, 549111234500]  # Pertenecen al mismo millar: 549111234000
        }

        # Intentar sin API Key
        response = await client.post("/v1/reports/batch", json=payload)
        assert response.status_code == 401

        # Enviar con API Key
        response = await client.post("/v1/reports/batch", json=payload, headers=_headers())
        assert response.status_code == 201
        data = response.json()
        assert data["status"] == "success"
        # Debería haber insertado 1000 números nuevos (el bloque completo)
        assert data["numeros_nuevos_guardados"] == 1000

        # Verificar en la base de datos que las semillas tienen is_predicted=False
        stmt = select(PhoneNumber).where(PhoneNumber.phone_number.in_([549111234200, 549111234500]))
        res = await clean_db.execute(stmt)
        phones = res.scalars().all()
        assert len(phones) == 2
        for p in phones:
            assert p.is_predicted is False

        # Verificar que un número adyacente tiene is_predicted=True
        stmt = select(PhoneNumber).where(PhoneNumber.phone_number == 549111234000)
        res = await clean_db.execute(stmt)
        phone_adj = res.scalar_one_or_none()
        assert phone_adj is not None
        assert phone_adj.is_predicted is True

        # Enviar de nuevo las mismas semillas (ON CONFLICT DO UPDATE)
        response = await client.post("/v1/reports/batch", json=payload, headers=_headers())
        assert response.status_code == 201
        data = response.json()
        assert data["status"] == "success"
        # Ya no debería haber insertado números nuevos
        assert data["numeros_nuevos_guardados"] == 0

    async def test_admin_seed_range_endpoint(self, client: AsyncClient, clean_db):
        payload = {
            "seed_number": "+5491112351234"  # Base del millar: 549111235000
        }

        # Intentar sin API Key
        response = await client.post("/v1/admin/seed-range", json=payload)
        assert response.status_code == 401

        # Enviar con API Key
        response = await client.post("/v1/admin/seed-range", json=payload, headers=_admin_headers())
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "success"
        assert data["seed_procesado"] == 541112351234
        assert data["bloque_base_calculado"] == 541112351000
        assert data["registros_bd_impactados"] == 1000

    async def test_downloads_callkit_endpoint(self, client: AsyncClient, clean_db):
        # Insertar algunos números fuera de orden
        nums = [549111234010, 549111234005, 549111234015]
        for n in nums:
            clean_db.add(PhoneNumber(phone_number=n))
        await clean_db.commit()

        # Descargar la lista
        response = await client.get("/v1/downloads/callkit")
        assert response.status_code == 200
        assert response.headers["Content-Type"] == "application/gzip"

        # Leer y descomprimir el stream de Gzip
        content = response.content
        with gzip.GzipFile(fileobj=io.BytesIO(content), mode="rb") as f:
            decompressed = f.read().decode("utf-8")

        # Dividir por líneas y filtrar vacías
        lines = [int(line) for line in decompressed.split("\n") if line.strip()]

        # Verificar que nuestros números insertados están en el stream
        for n in nums:
            assert n in lines

        # Verificar que el stream completo esté ordenado de menor a mayor
        assert lines == sorted(lines)

    async def test_report_public_endpoint(self, client: AsyncClient, clean_db):
        import uuid
        dev_id = f"test-web-{uuid.uuid4()}"
        payload = {
            "number": "+5491112349999",
            "device_id": dev_id,
            "report_type": "SPAM_CALL",
            "description": "Llamada molesta de publicidad de prueba"
        }


        # 1. Enviar reporte público (debe responder 200 OK y registrar el reporte sin X-Patova-Key)
        response = await client.post("/v1/report/public", json=payload)
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "accepted"
        assert data["number_e164"] == "+541112349999"

        # 2. Re-enviar el mismo reporte (debe fallar por reporte duplicado con 429)
        response = await client.post("/v1/report/public", json=payload)
        assert response.status_code == 429
        assert "Reporte duplicado" in response.json()["detail"]

    async def test_admin_delete_block_endpoint(self, client: AsyncClient, clean_db):
        # 1. Sembrar el rango
        payload_seed = {
            "seed_number": "+5491112345000"
        }
        res_seed = await client.post("/v1/admin/seed-range", json=payload_seed, headers=_admin_headers())
        assert res_seed.status_code == 200
        data_seed = res_seed.json()
        assert data_seed["status"] == "success"

        # Verificar que existen en la BD
        stmt = select(PhoneNumber).where(PhoneNumber.phone_number == 541112345000)
        res = await clean_db.execute(stmt)
        phone = res.scalar_one_or_none()
        assert phone is not None

        # Verificar predictivo
        stmt_pred = select(PhoneNumber).where(PhoneNumber.phone_number == 541112345100)
        res_pred = await clean_db.execute(stmt_pred)
        phone_pred = res_pred.scalar_one_or_none()
        assert phone_pred is not None
        assert phone_pred.is_predicted is True

        # 2. Revertir/Eliminar el bloque
        payload_delete = {
            "phone_number": "+5491112345000"
        }
        # Sin header admin (debe dar 401)
        res_del_unauth = await client.post("/v1/admin/delete-block", json=payload_delete)
        assert res_del_unauth.status_code == 401

        # Con header admin
        res_del = await client.post("/v1/admin/delete-block", json=payload_delete, headers=_admin_headers())
        assert res_del.status_code == 200
        data_del = res_del.json()
        assert data_del["status"] == "success"
        assert data_del["registros_eliminados"] == 1000

        # Verificar que se hayan borrado de la BD
        stmt_after = select(PhoneNumber).where(PhoneNumber.phone_number == 541112345000)
        res_after = await clean_db.execute(stmt_after)
        assert res_after.scalar_one_or_none() is None

        stmt_pred_after = select(PhoneNumber).where(PhoneNumber.phone_number == 541112345100)
        res_pred_after = await clean_db.execute(stmt_pred_after)
        assert res_pred_after.scalar_one_or_none() is None


