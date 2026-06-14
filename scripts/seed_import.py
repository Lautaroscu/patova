import asyncio
import hashlib

import pandas as pd
import typer
from sqlalchemy import func, literal_column, select, text
from sqlalchemy.dialects.postgresql import insert as pg_insert
from sqlalchemy.ext.asyncio import async_sessionmaker

from patova.db.session import _get_async_sessionmaker
from patova.models.area_prefix import AreaPrefix
from patova.models.enums import NumberSource, NumberStatus
from patova.models.phone_number import PhoneNumber
from patova.services.phone_normalization import extract_argentina_prefix, hash_for_log, normalize_to_e164

app = typer.Typer(help="NumGuard seed data import CLI")


def _csv_bool(value) -> bool:
    if value is None:
        return False
    s = str(value).strip().lower()
    return s in ("true", "1", "yes", "si")


def _extract_local(number_e164: str) -> str:
    if number_e164.startswith("+54"):
        return number_e164[3:]
    if number_e164.startswith("+"):
        return number_e164[1:]
    return number_e164


def _hash_for_log(value: str) -> str:
    return hashlib.sha256(value.encode()).hexdigest()[:8]


async def _upsert_prefixes(
    sessionmaker: async_sessionmaker, file: str, batch_size: int = 500
) -> dict:
    total = 0
    inserted = 0
    updated = 0
    skipped = 0

    async with sessionmaker() as session:
        with pd.read_csv(
            file, chunksize=batch_size, dtype=str, keep_default_na=False, na_filter=False
        ) as reader:
            for chunk in reader:
                for _idx, row in chunk.iterrows():
                    total += 1
                    prefix_val = str(row["prefix"]).strip()
                    if not prefix_val:
                        skipped += 1
                        continue

                    operator_val = row.get("operator")
                    if operator_val and str(operator_val) != "nan":
                        operator_val = str(operator_val)
                    else:
                        operator_val = None

                    stmt = pg_insert(AreaPrefix).values(
                        prefix=prefix_val,
                        city=str(row.get("city", "")).strip(),
                        province=str(row.get("province", "")).strip(),
                        operator=operator_val,
                        is_mobile=_csv_bool(row.get("is_mobile")),
                        is_valid=_csv_bool(row.get("is_valid", True)),
                    )
                    stmt = stmt.on_conflict_do_update(
                        index_elements=["prefix"],
                        set_=dict(
                            city=stmt.excluded.city,
                            province=stmt.excluded.province,
                            operator=stmt.excluded.operator,
                            is_mobile=stmt.excluded.is_mobile,
                            is_valid=stmt.excluded.is_valid,
                        ),
                    )
                    stmt = stmt.returning(literal_column("xmax"))
                    result = await session.execute(stmt)
                    row_result = result.fetchone()
                    xmax_val = row_result[0] if row_result else None
                    if xmax_val == 0:
                        inserted += 1
                    else:
                        updated += 1

                await session.commit()

    return {"total": total, "inserted": inserted, "updated": updated, "skipped": skipped}


async def _upsert_numbers(
    sessionmaker: async_sessionmaker,
    file: str,
    status: NumberStatus,
    batch_size: int = 500,
) -> dict:
    total = 0
    inserted = 0
    updated = 0
    invalid = 0

    async with sessionmaker() as session:
        with pd.read_csv(
            file, chunksize=batch_size, dtype=str, keep_default_na=False, na_filter=False
        ) as reader:
            for chunk in reader:
                for _idx, row in chunk.iterrows():
                    total += 1
                    raw = str(row["number"]).strip()
                    normalized = normalize_to_e164(raw)
                    if not normalized:
                        invalid += 1
                        continue

                    local_number = _extract_local(normalized)
                    prefix_candidate = extract_argentina_prefix(normalized)

                    prefix_id = None
                    if prefix_candidate:
                        prefix_stmt = select(AreaPrefix.id).where(
                            AreaPrefix.prefix == prefix_candidate
                        )
                        prefix_result = await session.execute(prefix_stmt)
                        prefix_id = prefix_result.scalar_one_or_none()

                    extra_meta = {}
                    if "metadata_name" in row:
                        extra_meta["name"] = str(row["metadata_name"])
                    if "metadata_type" in row:
                        extra_meta["type"] = str(row["metadata_type"])

                    spam_score_str = str(row.get("spam_score", "0")).strip()
                    try:
                        spam_score = min(max(int(spam_score_str), 0), 100)
                    except (ValueError, TypeError):
                        spam_score = 0

                    report_count_str = str(row.get("report_count", "0")).strip()
                    try:
                        report_count = int(report_count_str)
                    except (ValueError, TypeError):
                        report_count = 0

                    stmt = pg_insert(PhoneNumber).values(
                        phone_number=int(normalized.lstrip("+")),
                        prefix_id=prefix_id,
                        status=status,
                        spam_score=spam_score,
                        report_count=report_count,
                        source=NumberSource.SEED,
                        metadata_=extra_meta,
                    )
                    stmt = stmt.on_conflict_do_update(
                        index_elements=["phone_number"],
                        set_={
                            "status": stmt.excluded.status,
                            "spam_score": stmt.excluded.spam_score,
                            "report_count": stmt.excluded.report_count,
                            "prefix_id": stmt.excluded.prefix_id,
                            "metadata": text("EXCLUDED.metadata"),
                            "updated_at": func.now(),
                        },
                    )
                    stmt = stmt.returning(literal_column("xmax"))
                    result = await session.execute(stmt)
                    row_result = result.fetchone()
                    xmax_val = row_result[0] if row_result else None
                    if xmax_val == 0:
                        inserted += 1
                    else:
                        updated += 1

                await session.commit()

    return {
        "total": total,
        "inserted": inserted,
        "updated": updated,
        "invalid": invalid,
    }


@app.command()
def import_prefixes(
    file: str = typer.Option(..., "--file", help="CSV file with prefixes"),
):
    async_session = _get_async_sessionmaker()
    counts = asyncio.run(_upsert_prefixes(async_session, file))
    print(
        f"Prefixes: {counts['total']} rows read, "
        f"{counts['inserted']} inserted, "
        f"{counts['updated']} updated, "
        f"{counts['skipped']} skipped"
    )


@app.command()
def import_numbers(
    file: str = typer.Option(..., "--file", help="CSV file with phone numbers"),
    default_status: str = typer.Option(
        "CLEAN", "--default-status", help="Default status: CLEAN, SPAM, SUSPECT, or UNVERIFIED"
    ),
):
    status = NumberStatus(default_status.upper())
    async_session = _get_async_sessionmaker()
    counts = asyncio.run(_upsert_numbers(async_session, file, status))
    print(
        f"Numbers: {counts['total']} rows read, "
        f"{counts['inserted']} inserted, "
        f"{counts['updated']} updated, "
        f"{counts['invalid']} invalid"
    )


@app.command()
def validate_file(
    file: str = typer.Option(..., "--file", help="CSV file to validate"),
):
    total = 0
    valid = 0
    invalid = 0

    with pd.read_csv(
        file, chunksize=500, dtype=str, keep_default_na=False, na_filter=False
    ) as reader:
        for chunk in reader:
            for _idx, row in chunk.iterrows():
                total += 1
                raw = str(row["number"]).strip()
                normalized = normalize_to_e164(raw)
                if normalized:
                    valid += 1
                else:
                    invalid += 1
                    print(f"Invalid: ...{hash_for_log(raw)}")

    print(f"Validation: {total} total, {valid} valid, {invalid} invalid")


if __name__ == "__main__":
    app()
