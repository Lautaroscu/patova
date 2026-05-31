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
        select(func.count(PhoneNumber.id))
    )

    total_reports = await session.scalar(
        select(func.count(Report.id))
    )

    blocked_today = await session.scalar(
        select(func.count(PhoneNumber.id)).where(
            PhoneNumber.status == "SPAM",
            PhoneNumber.updated_at >= today_start,
        )
    )

    top_stmt = (
        select(PhoneNumber)
        .order_by(PhoneNumber.report_count.desc())
        .limit(10)
    )
    top_result = await session.execute(top_stmt)
    top_numbers = top_result.scalars().all()

    top_reported = [
        {
            "number_e164_masked": mask_e164(p.number_e164),
            "spam_score": p.spam_score,
            "report_count": p.report_count,
            "status": p.status.value if p.status else "UNVERIFIED",
        }
        for p in top_numbers
    ]

    return {
        "total_numbers": total_numbers or 0,
        "total_reports": total_reports or 0,
        "blocked_today": blocked_today or 0,
        "top_reported": top_reported,
    }
