from datetime import UTC, datetime

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from patova.models.phone_number import PhoneNumber
from patova.models.report import Report


def mask_e164(number: str) -> str:
    if not number or not number.startswith("+"):
        return number
    digits = number[1:]
    if len(digits) <= 6:
        return number[:3] + "****"
    return number[:5] + "****" + number[-4:]


async def get_stats(session: AsyncSession) -> dict:
    today_start = datetime.now(UTC).replace(hour=0, minute=0, second=0, microsecond=0)

    total_numbers = await session.scalar(
        select(func.count(PhoneNumber.phone_number))
    )

    total_reports = await session.scalar(
        select(func.count(Report.id))
    )

    blocked_today = await session.scalar(
        select(func.count(PhoneNumber.phone_number)).where(
            PhoneNumber.status == "SPAM",
            PhoneNumber.updated_at >= today_start,
        )
    )

    total_imports = await session.scalar(
        select(func.sum(PhoneNumber.import_count))
    ) or 0

    top_stmt = (
        select(PhoneNumber)
        .order_by(PhoneNumber.report_count.desc())
        .limit(10)
    )
    top_result = await session.execute(top_stmt)
    top_numbers = top_result.scalars().all()

    top_reported = [
        {
            "number_e164": p.number_e164,
            "number_e164_masked": mask_e164(p.number_e164),
            "phone_number": p.phone_number,
            "spam_score": p.spam_score,
            "report_count": p.report_count,
            "status": p.status.value if p.status else "UNVERIFIED",
            "is_predicted": p.is_predicted,
            "source": p.source.value if p.source else "SEED",
        }
        for p in top_numbers
    ]

    top_concurrence_stmt = (
        select(PhoneNumber)
        .where(PhoneNumber.import_count > 0)
        .order_by(PhoneNumber.import_count.desc())
        .limit(10)
    )
    top_concurrence_result = await session.execute(top_concurrence_stmt)
    top_concurrence_numbers = top_concurrence_result.scalars().all()

    top_concurrence = [
        {
            "number_e164": p.number_e164,
            "number_e164_masked": mask_e164(p.number_e164),
            "phone_number": p.phone_number,
            "spam_score": p.spam_score,
            "import_count": p.import_count,
            "concurrency_percentage": round((p.import_count / total_imports) * 100, 2) if total_imports > 0 else 0.0,
            "status": p.status.value if p.status else "UNVERIFIED",
            "is_predicted": p.is_predicted,
            "source": p.source.value if p.source else "SEED",
        }
        for p in top_concurrence_numbers
    ]

    # 1. Status Distribution
    status_counts = await session.execute(
        select(PhoneNumber.status, func.count(PhoneNumber.phone_number))
        .group_by(PhoneNumber.status)
    )
    status_dist = {status.value if hasattr(status, "value") else str(status): count for status, count in status_counts.all()}

    # 2. Source Distribution
    source_counts = await session.execute(
        select(PhoneNumber.source, func.count(PhoneNumber.phone_number))
        .group_by(PhoneNumber.source)
    )
    source_dist = {source.value if hasattr(source, "value") else str(source): count for source, count in source_counts.all()}

    # 3. Report Type Distribution
    report_type_counts = await session.execute(
        select(Report.report_type, func.count(Report.id))
        .group_by(Report.report_type)
    )
    report_dist = {rt.value if hasattr(rt, "value") else str(rt): count for rt, count in report_type_counts.all()}

    # 4. Reports over the last 7 days
    from datetime import timedelta
    seven_days_ago = today_start - timedelta(days=7)
    reports_by_day = await session.execute(
        select(func.date(Report.created_at), func.count(Report.id))
        .where(Report.created_at >= seven_days_ago)
        .group_by(func.date(Report.created_at))
        .order_by(func.date(Report.created_at))
    )
    reports_history = {str(day): count for day, count in reports_by_day.all()}
    
    history_labels = []
    history_values = []
    for i in range(6, -1, -1):
        day = (today_start - timedelta(days=i)).date()
        day_str = day.strftime("%Y-%m-%d")
        label_str = day.strftime("%d/%m")
        count = reports_history.get(day_str) or 0
        history_labels.append(label_str)
        history_values.append(count)

    return {
        "total_numbers": total_numbers or 0,
        "total_reports": total_reports or 0,
        "blocked_today": blocked_today or 0,
        "total_imports": total_imports,
        "top_reported": top_reported,
        "top_concurrence": top_concurrence,
        "status_dist": status_dist,
        "source_dist": source_dist,
        "report_dist": report_dist,
        "history_labels": history_labels,
        "history_values": history_values,
    }
