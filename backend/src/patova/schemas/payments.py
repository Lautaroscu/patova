from datetime import datetime
from typing import Optional

from pydantic import BaseModel, Field


class CreatePreferenceRequest(BaseModel):
    plan_id: str = Field(default="premium_monthly", examples=["premium_monthly"])
    email: str = Field(examples=["user@example.com"])
    user_id: str = Field(examples=["usr_998877"])


class CreatePreferenceResponse(BaseModel):
    user_id: str
    preference_id: str
    init_point: str


class SubscriptionDetail(BaseModel):
    id: str
    status: str
    started_at: Optional[datetime] = None
    expires_at: Optional[datetime] = None
    provider: str = "MERCADO_PAGO"
    renewal_enabled: bool = False


class SubscriptionMeResponse(BaseModel):
    user_id: str
    is_premium: bool
    subscription: Optional[SubscriptionDetail] = None


class SubscriptionPlanResponse(BaseModel):
    id: str
    title: str
    subtitle: str
    description: str
    price: float
    currency: str = "ARS"
    formatted_price: str
    button_text: str
    interval: str  # "month" | "year"
    badge: Optional[str] = None
    discount: Optional[str] = None
    equivalent_monthly_price_text: Optional[str] = None
    is_recommended: bool = False

