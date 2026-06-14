import asyncio

from sqlalchemy import select

from patova.db.redis import get_redis_client
from patova.db.session import _get_async_sessionmaker
from patova.models.enums import FeedbackType, NumberStatus
from patova.models.feedback_event import FeedbackEvent
from patova.models.phone_number import PhoneNumber
from patova.models.report import Report
from patova.services.cache_service import invalidate_validate_cache
from patova.services.scoring_service import (
    calculate_spam_score,
    get_reports_last_24h,
    should_auto_block,
)
from patova.workers.celery_app import app


def _run_async(coro):
    return asyncio.run(coro)


@app.task(name="recalculate_spam_score")
def recalculate_spam_score(number_e164: str):
    async def _recalculate():
        sessionmaker = _get_async_sessionmaker()
        async with sessionmaker() as session:
            phone_number_val = int(number_e164.lstrip("+"))
            stmt = select(PhoneNumber).where(
                PhoneNumber.phone_number == phone_number_val
            ).limit(1)
            result = await session.execute(stmt)
            phone = result.scalar_one_or_none()
            if phone is None:
                return

            recent_stmt = (
                select(Report)
                .where(Report.phone_number == phone.phone_number)
                .order_by(Report.created_at.desc())
                .limit(500)
            )
            recent_result = await session.execute(recent_stmt)
            all_reports = list(recent_result.scalars().all())
            reports_24h = get_reports_last_24h(all_reports)

            new_score = calculate_spam_score(reports_24h)

            feedback_stmt = (
                select(FeedbackEvent)
                .where(FeedbackEvent.phone_number == phone.phone_number)
            )
            feedback_result = await session.execute(feedback_stmt)
            for event in feedback_result.scalars().all():
                if event.feedback_type == FeedbackType.FALSE_POSITIVE:
                    new_score = max(0, new_score - 20)
                elif event.feedback_type == FeedbackType.WAS_SPAM:
                    new_score = min(100, new_score + 15)

            phone.spam_score = new_score

            if should_auto_block(reports_24h):
                phone.status = NumberStatus.SPAM

            await session.commit()

    _run_async(_recalculate())


@app.task(name="invalidate_validate_cache")
def invalidate_validate_cache_task(number_e164: str):
    async def _invalidate():
        redis_client = get_redis_client()
        await invalidate_validate_cache(redis_client, number_e164)
        await redis_client.aclose()

    _run_async(_invalidate())
