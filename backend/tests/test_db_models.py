import pytest
from sqlalchemy.exc import IntegrityError
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine

from patova.core.config import get_settings
from patova.models.area_prefix import AreaPrefix
from patova.models.enums import NumberSource, NumberStatus, ReportType
from patova.models.phone_number import PhoneNumber
from patova.models.report import Report

pytestmark = pytest.mark.integration


@pytest.fixture
async def session():
    settings = get_settings()
    engine = create_async_engine(settings.database_url, echo=False)
    async_session = async_sessionmaker(engine, expire_on_commit=False)
    async with async_session() as s:
        yield s
    await engine.dispose()


async def test_create_area_prefix(session: AsyncSession):
    prefix = AreaPrefix(
        prefix="999",
        city="TestCity",
        province="TestProv",
        operator="TestOp",
        is_mobile=False,
    )
    session.add(prefix)
    await session.commit()
    try:
        assert prefix.id is not None
        assert prefix.prefix == "999"
    finally:
        await session.delete(prefix)
        await session.commit()


async def test_create_phone_number(session: AsyncSession):
    prefix = AreaPrefix(prefix="888", city="TestCity", province="TestProv")
    session.add(prefix)
    await session.flush()

    phone = PhoneNumber(
        number_e164="+549991234567",
        number_local="9991234567",
        prefix_id=prefix.id,
        status=NumberStatus.CLEAN,
        source=NumberSource.ENACOM,
        spam_score=0,
    )
    session.add(phone)
    await session.commit()
    try:
        assert phone.id is not None
        assert phone.prefix_id == prefix.id
        assert phone.status == NumberStatus.CLEAN
    finally:
        await session.delete(phone)
        await session.delete(prefix)
        await session.commit()


async def test_create_report(session: AsyncSession):
    prefix = AreaPrefix(prefix="777", city="TestCity", province="TestProv")
    session.add(prefix)
    await session.flush()

    phone = PhoneNumber(
        number_e164="+549981234567",
        number_local="9981234567",
        prefix_id=prefix.id,
    )
    session.add(phone)
    await session.flush()

    report = Report(
        phone_number_id=phone.id,
        reporter_device_id="device-test-db",
        report_type=ReportType.SPAM_CALL,
        description="Test report",
        call_duration_sec=30,
    )
    session.add(report)
    await session.commit()
    try:
        assert report.id is not None
        assert report.phone_number_id == phone.id
        assert report.report_type == ReportType.SPAM_CALL
    finally:
        await session.delete(report)
        await session.delete(phone)
        await session.delete(prefix)
        await session.commit()


async def test_spam_score_constraint(session: AsyncSession):
    phone = PhoneNumber(
        number_e164="+549971234567",
        number_local="9971234567",
        spam_score=150,
    )
    session.add(phone)
    try:
        with pytest.raises(IntegrityError):
            await session.flush()
    finally:
        await session.rollback()
