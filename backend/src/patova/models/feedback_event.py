import uuid
from datetime import UTC, datetime

from sqlalchemy import BigInteger, DateTime, ForeignKey, String
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column

from .base import Base
from .enums import FeedbackType


class FeedbackEvent(Base):
    __tablename__ = "feedback_events"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, default=uuid.uuid4
    )
    phone_number: Mapped[int] = mapped_column(
        BigInteger, ForeignKey("phone_numbers.phone_number"), nullable=False
    )
    reporter_device_id: Mapped[str] = mapped_column(String(64), nullable=False)
    feedback_type: Mapped[FeedbackType] = mapped_column(nullable=False)
    related_verdict: Mapped[str] = mapped_column(String(20), nullable=False)
    timestamp: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(UTC)
    )
