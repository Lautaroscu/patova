from datetime import datetime

from pydantic import BaseModel, Field

from patova.models.enums import ReportType


class ReportRequest(BaseModel):
    number: str = Field(..., min_length=3, max_length=30)
    device_id: str = Field(..., min_length=1, max_length=256)
    report_type: ReportType = ReportType.SPAM_CALL
    description: str | None = Field(None, max_length=500)
    call_duration_sec: int | None = None
    call_time: datetime | None = None


class ReportResponse(BaseModel):
    status: str
    number_e164: str
    new_spam_score: int
    report_count: int
