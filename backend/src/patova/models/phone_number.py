import uuid
from datetime import UTC, datetime

from sqlalchemy import BigInteger, CheckConstraint, DateTime, ForeignKey, Integer, SmallInteger, String
from sqlalchemy.dialects.postgresql import JSONB, UUID
from sqlalchemy.ext.hybrid import hybrid_property
from sqlalchemy.orm import Mapped, mapped_column

from .base import Base
from .enums import NumberSource, NumberStatus


class PhoneNumber(Base):
    __tablename__ = "phone_numbers"
    __table_args__ = (
        CheckConstraint(
            "spam_score >= 0 AND spam_score <= 100", name="ck_phone_numbers_spam_score"
        ),
    )

    phone_number: Mapped[int] = mapped_column(BigInteger, primary_key=True)
    is_predicted: Mapped[bool] = mapped_column(default=False, server_default="false", nullable=False)
    
    prefix_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True), ForeignKey("area_prefixes.id"), nullable=True
    )
    status: Mapped[NumberStatus] = mapped_column(default=NumberStatus.UNVERIFIED)
    spam_score: Mapped[int] = mapped_column(SmallInteger, default=0)
    report_count: Mapped[int] = mapped_column(Integer, default=0)
    source: Mapped[NumberSource] = mapped_column(default=NumberSource.SEED)
    metadata_: Mapped[dict] = mapped_column("metadata", JSONB, default=dict)
    first_seen_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), nullable=False, default=lambda: datetime.now(UTC)
    )
    last_reported_at: Mapped[datetime | None] = mapped_column(
        DateTime(timezone=True), nullable=True
    )
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=lambda: datetime.now(UTC)
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(UTC),
        onupdate=lambda: datetime.now(UTC),
    )

    @hybrid_property
    def number_e164(self) -> str:
        return f"+{self.phone_number}"

    @number_e164.setter
    def number_e164(self, val: str) -> None:
        self.phone_number = int(val.lstrip("+"))

