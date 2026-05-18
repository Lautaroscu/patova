import hashlib

from fastapi import APIRouter, Depends
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from numguard.db.session import get_session
from numguard.models.phone_number import PhoneNumber
from numguard.models.report import Report
from numguard.schemas.reputation import ReputationExplainability, SpamReputationResponse
from numguard.services.scoring.engine import compute_reputation

router = APIRouter()


def _phone_hash(number_e164: str) -> str:
    return hashlib.sha256(number_e164.encode()).hexdigest()


@router.get(
    "/reputation/{phone_input}",
    response_model=SpamReputationResponse,
    tags=["spam-intel"],
)
async def get_reputation(
    phone_input: str,
    session: AsyncSession = Depends(get_session),
):
    phone_hash = phone_input
    if len(phone_input) != 64:
        phone_hash = _phone_hash(phone_input)

    stmt = (
        select(PhoneNumber)
        .where(PhoneNumber.number_e164 == phone_input)
        .limit(1)
    )
    result = await session.execute(stmt)
    phone = result.scalar_one_or_none()

    raw_reports: list[Report] = []

    if phone is not None:
        reports_stmt = (
            select(Report)
            .where(Report.phone_number_id == phone.id)
            .order_by(Report.created_at.desc())
            .limit(500)
        )
        reports_result = await session.execute(reports_stmt)
        raw_reports = list(reports_result.scalars().all())

    result_obj = compute_reputation(phone_hash, raw_reports)

    return SpamReputationResponse(
        phone_hash=result_obj.phone_hash,
        reputation_score=result_obj.reputation_score,
        reputation_state=result_obj.reputation_state,
        total_reports=result_obj.total_reports,
        unique_reporters=result_obj.unique_reporters,
        confidence=result_obj.confidence,
        explainability=ReputationExplainability(
            heuristic_flags=result_obj.explainability.get("heuristic_flags", []),
            community_severity=result_obj.explainability.get("community_severity", "LOW"),
            description=result_obj.explainability.get("description", ""),
        ),
        last_seen=result_obj.last_seen,
    )
