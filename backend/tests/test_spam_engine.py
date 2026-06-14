import pytest
from sqlalchemy import delete
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine

from patova.core.config import get_settings
from patova.models.phone_number import PhoneNumber
from patova.services.spam_engine import SpamEngineService

pytestmark = pytest.mark.integration


@pytest.fixture
async def db_session():
    settings = get_settings()
    engine = create_async_engine(settings.database_url, echo=False)
    async_session = async_sessionmaker(engine, expire_on_commit=False)
    async with async_session() as s:
        # Limpiar cualquier residuo de pruebas previas en phone_numbers
        await s.execute(delete(PhoneNumber).where(PhoneNumber.phone_number.in_([
            549111234000, 549111234001, 549111234500, 549111234999,
            549111235123, 549111235456, 549111235000, 549111235999,
            111111111111, 222222222222, 333333333333
        ])))
        await s.commit()
        yield s
    await engine.dispose()


def test_get_block_bounds():
    # Límites matemáticos del bloque (del .000 al .999)
    phone = 5491154386201
    start, end = SpamEngineService.get_block_bounds(phone)
    assert start == 5491154386000
    assert end == 5491154386999

    # Verificar borde de inicio
    start, end = SpamEngineService.get_block_bounds(5491154386000)
    assert start == 5491154386000
    assert end == 5491154386999

    # Verificar borde de fin
    start, end = SpamEngineService.get_block_bounds(5491154386999)
    assert start == 5491154386000
    assert end == 5491154386999


def test_expand_number_to_block():
    phone = 5491154386201
    block_set = SpamEngineService.expand_number_to_block(phone)
    assert len(block_set) == 1000
    assert 5491154386000 in block_set
    assert 5491154386999 in block_set
    assert 5491154386201 in block_set
    assert 5491154385999 not in block_set
    assert 5491154387000 not in block_set


def test_expand_batch_deduplication_and_priority():
    # Dos números del mismo millar y uno de un millar diferente
    raw_numbers = [549111234200, 549111234500, 549111235123]

    result = SpamEngineService.expand_batch(raw_numbers)

    # 2 bloques de 1000 números = 2000 números totales en el payload expandido
    assert len(result) == 2000

    # Convertir a diccionario para búsquedas fáciles
    res_dict = dict(result)

    # Las semillas deben tener is_predicted=False obligatoriamente (prioridad alta)
    assert res_dict[549111234200] is False
    assert res_dict[549111234500] is False
    assert res_dict[549111235123] is False

    # Los números adyacentes generados deben tener is_predicted=True (preventivos)
    assert res_dict[549111234000] is True
    assert res_dict[549111234999] is True
    assert res_dict[549111235000] is True
    assert res_dict[549111235999] is True


async def test_fetch_sorted_callkit_list(db_session: AsyncSession):
    # Insertar algunos números desordenados en la base de datos
    nums_to_insert = [333333333333, 111111111111, 222222222222]
    for n in nums_to_insert:
        phone = PhoneNumber(phone_number=n)
        db_session.add(phone)
    await db_session.commit()

    try:
        # Obtener la lista usando el generador de CallKit
        streamed_nums = []
        async for num in SpamEngineService.fetch_sorted_callkit_list(db_session):
            streamed_nums.append(num)

        # Verificar que todos los números insertados están en el stream
        for n in nums_to_insert:
            assert n in streamed_nums

        # Verificar el ordenamiento estrictamente ascendente (ASC)
        filtered_stream = [num for num in streamed_nums if num in nums_to_insert]
        assert filtered_stream == [111111111111, 222222222222, 333333333333]
    finally:
        # Limpieza
        await db_session.execute(delete(PhoneNumber).where(PhoneNumber.phone_number.in_(nums_to_insert)))
        await db_session.commit()
