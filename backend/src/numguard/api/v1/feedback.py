from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from numguard.api.deps import verify_api_key
from numguard.db.session import get_session
from numguard.schemas.feedback import FeedbackRequest, FeedbackResponse
from numguard.services.report_service import process_feedback
from numguard.workers.tasks import invalidate_validate_cache_task, recalculate_spam_score

router = APIRouter()


@router.post("/feedback", response_model=FeedbackResponse)
async def feedback(
    body: FeedbackRequest,
    session: AsyncSession = Depends(get_session),
    _api_key: str = Depends(verify_api_key),
):
    response = await process_feedback(session, body)

    recalculate_spam_score.delay(response.number_e164)
    invalidate_validate_cache_task.delay(response.number_e164)

    return response
