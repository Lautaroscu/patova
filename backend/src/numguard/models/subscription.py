import uuid

from sqlalchemy import Boolean, Column, DateTime, String, func

from numguard.models.base import Base


class Subscription(Base):
    __tablename__ = "subscriptions"

    id = Column(String(64), primary_key=True, default=lambda: f"sub_{uuid.uuid4().hex[:12]}")
    user_id = Column(String(128), nullable=False, index=True)
    external_reference = Column(String(256), nullable=True)
    preference_id = Column(String(256), nullable=True)
    payment_id = Column(String(256), nullable=True)
    status = Column(String(32), nullable=False, default="PENDING", index=True)
    plan_id = Column(String(64), nullable=False)
    started_at = Column(DateTime(timezone=True), nullable=True)
    expires_at = Column(DateTime(timezone=True), nullable=True)
    provider = Column(String(32), nullable=False, default="MERCADO_PAGO")
    renewal_enabled = Column(Boolean, nullable=False, default=False)
    created_at = Column(
        DateTime(timezone=True), nullable=False, server_default=func.now()
    )
    updated_at = Column(
        DateTime(timezone=True), nullable=False, server_default=func.now(), onupdate=func.now()
    )


class WebhookEvent(Base):
    __tablename__ = "webhook_events"

    id = Column(String(256), primary_key=True)
    event_type = Column(String(64), nullable=False)
    payment_id = Column(String(256), nullable=True)
    processed_at = Column(DateTime(timezone=True), nullable=False, server_default=func.now())
