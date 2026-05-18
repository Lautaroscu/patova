from datetime import datetime, timedelta, timezone

from fastapi import APIRouter, Depends, HTTPException, Query, Request
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from numguard.api.deps import verify_api_key
from numguard.db.redis import get_redis_client
from numguard.db.session import get_session
from numguard.models.enums import SubscriptionStatus
from numguard.models.subscription import Subscription, WebhookEvent
from numguard.schemas.payments import (
    CreatePreferenceRequest,
    CreatePreferenceResponse,
    SubscriptionDetail,
    SubscriptionMeResponse,
)
from numguard.services.mp.client import MercadoPagoError, get_mp_client

router = APIRouter(tags=["payments"])

_WEBHOOK_IDEMPOTENCY_TTL = 60 * 60 * 24 * 30  # 30 days


@router.post("/payments/create-preference", response_model=CreatePreferenceResponse)
async def create_preference(
    payload: CreatePreferenceRequest,
    session: AsyncSession = Depends(get_session),
    _api_key: str = Depends(verify_api_key),
):
    mp = get_mp_client()

    try:
        result = await mp.create_preference(
            plan_id=payload.plan_id,
            email=payload.email,
            user_id=payload.user_id,
        )
    except MercadoPagoError as exc:
        raise HTTPException(status_code=502, detail=f"MercadoPago error: {exc}") from exc

    now = datetime.now(timezone.utc)
    plan_duration_days = 30 if payload.plan_id != "premium_annual" else 365

    subscription = Subscription(
        user_id=payload.user_id,
        external_reference=result["external_reference"],
        preference_id=result["preference_id"],
        status=SubscriptionStatus.PENDING,
        plan_id=payload.plan_id,
        provider="MERCADO_PAGO",
        renewal_enabled=False,
        started_at=None,
        expires_at=now + timedelta(days=plan_duration_days),
    )
    session.add(subscription)
    await session.commit()

    return CreatePreferenceResponse(
        user_id=payload.user_id,
        preference_id=result["preference_id"],
        init_point=result["init_point"],
    )


@router.post("/payments/webhook/mp")
async def mp_webhook(
    request: Request,
    session: AsyncSession = Depends(get_session),
):
    try:
        body = await request.json()
    except Exception:
        raise HTTPException(status_code=400, detail="Invalid JSON payload")

    payment_id = None
    event_id = None

    if "data" in body and "id" in body["data"]:
        payment_id = str(body["data"]["id"])
    if "id" in body:
        event_id = str(body["id"])

    webhook_id = payment_id or event_id
    if not webhook_id:
        raise HTTPException(status_code=400, detail="No payment ID or event ID found")

    redis = get_redis_client()
    idempotency_key = f"mp_webhook:{webhook_id}"
    already_processed = await redis.exists(idempotency_key)
    if already_processed:
        return {"status": "already_processed", "id": webhook_id}

    await redis.setex(idempotency_key, _WEBHOOK_IDEMPOTENCY_TTL, "processed")

    webhook_event = WebhookEvent(
        id=webhook_id,
        event_type=body.get("type", body.get("action", "unknown")),
        payment_id=payment_id,
    )
    session.add(webhook_event)

    if not payment_id:
        await session.commit()
        return {"status": "recorded_no_payment", "id": webhook_id}

    mp = get_mp_client()
    try:
        payment_data = await mp.get_payment(payment_id)
    except MercadoPagoError as exc:
        await session.commit()
        raise HTTPException(status_code=502, detail=f"MP verification failed: {exc}") from exc

    mp_status = payment_data.get("status", "")
    external_ref = payment_data.get("external_reference", "")

    if mp_status == "approved":
        user_id = external_ref.rsplit("_", 1)[0] if "_" in external_ref else external_ref

        stmt = (
            select(Subscription)
            .where(
                Subscription.user_id == user_id,
                Subscription.status == SubscriptionStatus.PENDING,
            )
            .order_by(Subscription.created_at.desc())
            .limit(1)
        )
        result = await session.execute(stmt)
        subscription = result.scalar_one_or_none()

        if subscription is not None:
            now = datetime.now(timezone.utc)
            plan_duration_days = 30 if subscription.plan_id != "premium_annual" else 365
            subscription.status = SubscriptionStatus.ACTIVE  # type: ignore[assignment]
            subscription.payment_id = payment_id  # type: ignore[assignment]
            subscription.started_at = now  # type: ignore[assignment]
            subscription.expires_at = now + timedelta(days=plan_duration_days)  # type: ignore[assignment]
            subscription.renewal_enabled = True  # type: ignore[assignment]

    elif mp_status in ("rejected", "cancelled", "refunded"):
        user_id = external_ref.rsplit("_", 1)[0] if "_" in external_ref else external_ref
        stmt = (
            select(Subscription)
            .where(
                Subscription.user_id == user_id,
                Subscription.status == SubscriptionStatus.PENDING,
            )
            .order_by(Subscription.created_at.desc())
            .limit(1)
        )
        result = await session.execute(stmt)
        subscription = result.scalar_one_or_none()
        if subscription is not None:
            subscription.status = SubscriptionStatus.CANCELLED  # type: ignore[assignment]
            subscription.payment_id = payment_id  # type: ignore[assignment]

    await session.commit()
    return {"status": "processed", "mp_status": mp_status, "id": webhook_id}


@router.get("/subscriptions/me", response_model=SubscriptionMeResponse)
async def get_subscription_me(
    user_id: str = Query(..., description="User identifier"),
    session: AsyncSession = Depends(get_session),
    _api_key: str = Depends(verify_api_key),
):
    stmt = (
        select(Subscription)
        .where(Subscription.user_id == user_id)
        .order_by(Subscription.created_at.desc())
        .limit(1)
    )
    result = await session.execute(stmt)
    subscription = result.scalar_one_or_none()

    if subscription is None:
        return SubscriptionMeResponse(
            user_id=user_id,
            is_premium=False,
            subscription=None,
        )

    is_premium = bool(subscription.status == SubscriptionStatus.ACTIVE)

    sub_detail = SubscriptionDetail(
        id=str(subscription.id),
        status=str(subscription.status),
        started_at=subscription.started_at,  # type: ignore[arg-type]
        expires_at=subscription.expires_at,  # type: ignore[arg-type]
        provider=str(subscription.provider),
        renewal_enabled=bool(subscription.renewal_enabled),
    )

    return SubscriptionMeResponse(
        user_id=str(subscription.user_id),
        is_premium=is_premium,
        subscription=sub_detail,
    )
