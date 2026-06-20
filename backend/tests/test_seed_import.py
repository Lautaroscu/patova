import subprocess
import sys
from pathlib import Path

import pytest
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine

from patova.core.config import get_settings
from patova.models.area_prefix import AreaPrefix
from patova.models.enums import NumberStatus
from patova.models.phone_number import PhoneNumber

pytestmark = pytest.mark.integration

REPO_ROOT = Path(__file__).resolve().parent.parent.parent
SEED_SCRIPT = str(REPO_ROOT / "scripts" / "seed_import.py")


def _run_seed_command(*args: str) -> subprocess.CompletedProcess:
    return subprocess.run(
        [sys.executable, SEED_SCRIPT, *args],
        capture_output=True,
        text=True,
    )


@pytest.fixture
async def session():
    settings = get_settings()
    engine = create_async_engine(settings.database_url, echo=False)
    async_session = async_sessionmaker(engine, expire_on_commit=False)
    async with async_session() as s:
        yield s
    await engine.dispose()


async def test_import_prefixes(session: AsyncSession):
    prefixes_file = str(REPO_ROOT / "scripts" / "data_samples" / "prefixes_sample.csv")
    result = _run_seed_command("import-prefixes", "--file", prefixes_file)
    assert "Prefixes:" in result.stdout, result.stderr

    stmt = select(AreaPrefix).where(AreaPrefix.prefix == "011")
    row = (await session.execute(stmt)).scalar_one_or_none()
    assert row is not None
    assert row.city == "Buenos Aires"


async def test_import_clean_numbers(session: AsyncSession):
    _run_seed_command(
        "import-prefixes",
        "--file",
        str(REPO_ROOT / "scripts" / "data_samples" / "prefixes_sample.csv"),
    )

    result = _run_seed_command(
        "import-numbers",
        "--file",
        str(REPO_ROOT / "scripts" / "data_samples" / "numbers_clean_sample.csv"),
        "--default-status",
        "CLEAN",
    )
    assert "Numbers:" in result.stdout, result.stderr

    stmt = select(PhoneNumber).where(PhoneNumber.phone_number == 541112345678)
    row = (await session.execute(stmt)).scalar_one_or_none()
    assert row is not None
    assert row.status == NumberStatus.CLEAN


async def test_import_spam_numbers(session: AsyncSession):
    _run_seed_command(
        "import-prefixes",
        "--file",
        str(REPO_ROOT / "scripts" / "data_samples" / "prefixes_sample.csv"),
    )

    result = _run_seed_command(
        "import-numbers",
        "--file",
        str(REPO_ROOT / "scripts" / "data_samples" / "numbers_spam_sample.csv"),
        "--default-status",
        "SPAM",
    )
    assert "Numbers:" in result.stdout, result.stderr

    stmt = select(PhoneNumber).where(PhoneNumber.phone_number == 541199999999)
    row = (await session.execute(stmt)).scalar_one_or_none()
    assert row is not None
    assert row.status == NumberStatus.SPAM
    assert row.spam_score == 90


async def test_import_suspect_numbers(session: AsyncSession):
    _run_seed_command(
        "import-prefixes",
        "--file",
        str(REPO_ROOT / "scripts" / "data_samples" / "prefixes_sample.csv"),
    )

    result = _run_seed_command(
        "import-numbers",
        "--file",
        str(REPO_ROOT / "scripts" / "data_samples" / "numbers_suspect_sample.csv"),
        "--default-status",
        "SUSPECT",
    )
    assert "Numbers:" in result.stdout, result.stderr

    stmt = select(PhoneNumber).where(PhoneNumber.phone_number == 543515555555)
    row = (await session.execute(stmt)).scalar_one_or_none()
    assert row is not None
    assert row.status == NumberStatus.SUSPECT
    assert row.spam_score == 50


async def test_idempotent_prefix_import(session: AsyncSession):
    prefixes_file = str(REPO_ROOT / "scripts" / "data_samples" / "prefixes_sample.csv")
    _run_seed_command("import-prefixes", "--file", prefixes_file)
    result2 = _run_seed_command("import-prefixes", "--file", prefixes_file)
    assert "Prefixes:" in result2.stdout, result2.stderr

    stmt = select(AreaPrefix).where(AreaPrefix.prefix == "011")
    result = await session.execute(stmt)
    rows = result.scalars().all()
    assert len(rows) == 1


async def test_idempotent_number_import(session: AsyncSession):
    _run_seed_command(
        "import-prefixes",
        "--file",
        str(REPO_ROOT / "scripts" / "data_samples" / "prefixes_sample.csv"),
    )

    numbers_file = str(REPO_ROOT / "scripts" / "data_samples" / "numbers_clean_sample.csv")
    _run_seed_command("import-numbers", "--file", numbers_file, "--default-status", "CLEAN")
    _run_seed_command("import-numbers", "--file", numbers_file, "--default-status", "CLEAN")

    stmt = select(PhoneNumber).where(PhoneNumber.phone_number == 541112345678)
    result = await session.execute(stmt)
    rows = result.scalars().all()
    assert len(rows) == 1
