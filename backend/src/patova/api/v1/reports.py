from fastapi import APIRouter, Depends, HTTPException, Request
from sqlalchemy.ext.asyncio import AsyncSession

from patova.api.deps import verify_api_key
from patova.core.metrics import reports_total
from patova.db.redis import get_redis_client
from patova.db.session import get_session
from patova.schemas.reports import ReportRequest, ReportResponse
from patova.services.abuse_guard import check_dedupe, check_report_quota, record_report
from patova.services.report_service import process_report
from patova.workers.tasks import invalidate_validate_cache_task, recalculate_spam_score

router = APIRouter()


def _hash_ip(request: Request) -> str | None:
    forwarded = request.headers.get("X-Forwarded-For")
    if forwarded:
        ip = forwarded.split(",")[0].strip()
    else:
        ip = request.client.host if request.client else None
    if not ip:
        return None
    import hashlib

    return hashlib.sha256(ip.encode()).hexdigest()


@router.post("/report", response_model=ReportResponse)
async def report(
    request: Request,
    body: ReportRequest,
    session: AsyncSession = Depends(get_session),
    _api_key: str = Depends(verify_api_key),
):
    redis_client = get_redis_client()

    if not await check_report_quota(redis_client, body.device_id):
        raise HTTPException(
            status_code=429,
            detail="Daily report limit reached for this device",
        )

    if not await check_dedupe(redis_client, body.device_id, body.number):
        raise HTTPException(
            status_code=429,
            detail=(
                "Duplicate report: same device already reported "
                "this number in the last 24 hours"
            ),
        )

    ip_hash = _hash_ip(request)
    response = await process_report(session, body, ip_hash)

    await record_report(redis_client, body.device_id, body.number)
    reports_total.inc()

    recalculate_spam_score.delay(str(response.number_e164))
    invalidate_validate_cache_task.delay(response.number_e164)

    return response
