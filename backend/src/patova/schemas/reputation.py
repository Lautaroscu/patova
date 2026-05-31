from pydantic import BaseModel, Field


class ReputationExplainability(BaseModel):
    heuristic_flags: list[str] = Field(default_factory=list)
    community_severity: str = "LOW"
    description: str = ""


class SpamReputationResponse(BaseModel):
    phone_hash: str
    reputation_score: float = Field(ge=0.0, le=1.0)
    reputation_state: str
    total_reports: int = 0
    unique_reporters: int = 0
    confidence: float = Field(ge=0.0, le=1.0)
    explainability: ReputationExplainability
    last_seen: str | None = None
