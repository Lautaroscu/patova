from datetime import UTC, datetime

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from patova.models.area_prefix import AreaPrefix
from patova.models.enums import NumberSource, NumberStatus
from patova.models.feedback_event import FeedbackEvent
from patova.models.phone_number import PhoneNumber
from patova.models.report import Report
from patova.schemas.feedback import FeedbackRequest, FeedbackResponse
from patova.schemas.reports import ReportRequest, ReportResponse
from patova.services.phone_normalization import (
    extract_argentina_prefix,
    normalize_to_e164,
)


async def _get_or_create_phone(
    session: AsyncSession, normalized: str
) -> PhoneNumber:
    stmt = select(PhoneNumber).where(PhoneNumber.number_e164 == normalized).limit(1)
    result = await session.execute(stmt)
    phone = result.scalar_one_or_none()

    if phone is not None:
        return phone

    prefix_candidate = extract_argentina_prefix(normalized)
    prefix_id = None
    if prefix_candidate:
        prefix_stmt = select(AreaPrefix.id).where(AreaPrefix.prefix == prefix_candidate)
        prefix_result = await session.execute(prefix_stmt)
        row = prefix_result.scalar_one_or_none()
        if row:
            prefix_id = row

    local_number = normalized[3:] if normalized.startswith("+54") else normalized.lstrip("+")

    phone = PhoneNumber(
        number_e164=normalized,
        number_local=local_number,
        prefix_id=prefix_id,
        status=NumberStatus.UNVERIFIED,
        spam_score=0,
        report_count=0,
        source=NumberSource.CROWDSOURCE,
    )
    session.add(phone)
    await session.flush()
    return phone


async def process_report(
    session: AsyncSession,
    request: ReportRequest,
    ip_hash: str | None,
) -> ReportResponse:
    normalized = normalize_to_e164(request.number)

    phone = await _get_or_create_phone(session, normalized)

    report = Report(
        phone_number_id=phone.id,
        reporter_device_id=request.device_id,
        report_type=request.report_type,
        description=request.description,
        call_duration_sec=request.call_duration_sec,
        call_time=request.call_time,
        ip_hash=ip_hash,
    )
    session.add(report)

    phone.report_count = (phone.report_count or 0) + 1
    phone.last_reported_at = datetime.now(UTC)

    await session.commit()

    return ReportResponse(
        status="accepted",
        number_e164=normalized,
        new_spam_score=phone.spam_score or 0,
        report_count=phone.report_count,
    )


async def process_feedback(
    session: AsyncSession,
    request: FeedbackRequest,
) -> FeedbackResponse:
    normalized = normalize_to_e164(request.number)

    stmt = select(PhoneNumber).where(PhoneNumber.number_e164 == normalized).limit(1)
    result = await session.execute(stmt)
    phone = result.scalar_one_or_none()

    if phone is None:
        phone = await _get_or_create_phone(session, normalized)

    event = FeedbackEvent(
        phone_number_id=phone.id,
        reporter_device_id=request.device_id,
        feedback_type=request.feedback_type,
        related_verdict=request.related_verdict,
        timestamp=request.timestamp,
    )
    session.add(event)

    await session.commit()

    return FeedbackResponse(
        status="accepted",
        number_e164=normalized,
        feedback_type=request.feedback_type.value,
        new_spam_score=phone.spam_score or 0,
    )
