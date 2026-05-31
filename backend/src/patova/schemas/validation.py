from datetime import datetime
from enum import StrEnum

from pydantic import BaseModel, Field


class CallDirection(StrEnum):
    INCOMING = "INCOMING"
    OUTGOING = "OUTGOING"


class Verdict(StrEnum):
    ALLOW = "ALLOW"
    SUSPECT = "SUSPECT"
    BLOCK = "BLOCK"
    UNKNOWN = "UNKNOWN"
    INVALID_PREFIX = "INVALID_PREFIX"


class ValidateRequest(BaseModel):
    number: str = Field(..., min_length=3, max_length=30)
    device_id: str = Field(..., min_length=1, max_length=256)
    call_direction: CallDirection = CallDirection.INCOMING
    timestamp: datetime | None = None


class ValidateResponse(BaseModel):
    verdict: Verdict
    spam_score: int = 0
    reason: str
    report_count: int = 0
    prefix_valid: bool
    prefix_zone: str | None = None
    operator: str | None = None
    cached: bool = False
    latency_ms: float = 0.0


class NumberLookupResponse(BaseModel):
    number_e164: str
    status: str
    spam_score: int
    report_count: int
    prefix_zone: str | None = None


class PrefixItem(BaseModel):
    prefix: str
    city: str
    province: str
    is_mobile: bool


class PrefixesResponse(BaseModel):
    items: list[PrefixItem]
    count: int
