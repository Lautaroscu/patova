from datetime import datetime

from pydantic import BaseModel, Field

from numguard.models.enums import FeedbackType


class FeedbackRequest(BaseModel):
    number: str = Field(..., min_length=3, max_length=30)
    device_id: str = Field(..., min_length=1, max_length=256)
    feedback_type: FeedbackType
    related_verdict: str = Field(..., max_length=20)
    timestamp: datetime | None = None


class FeedbackResponse(BaseModel):
    status: str
    number_e164: str
    feedback_type: str
    new_spam_score: int
