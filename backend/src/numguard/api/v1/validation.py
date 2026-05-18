import time

from fastapi import APIRouter, Depends, HTTPException, Request
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from numguard.api.deps import verify_api_key
from numguard.core.config import get_settings
from numguard.core.metrics import (
    validate_cache_hits_total,
    validate_latency_seconds,
    validate_requests_total,
)
from numguard.db.redis import get_redis_client
from numguard.db.session import get_session
from numguard.models.area_prefix import AreaPrefix
from numguard.models.phone_number import PhoneNumber
from numguard.schemas.validation import (
    NumberLookupResponse,
    PrefixesResponse,
    PrefixItem,
    ValidateRequest,
    ValidateResponse,
    Verdict,
)
from numguard.services.cache_service import get_cached_result
from numguard.services.phone_normalization import normalize_to_e164
from numguard.services.rate_limit import get_limiter
from numguard.services.validation_service import _prefix_zone, validate_number

router = APIRouter()


@router.post("/validate", response_model=ValidateResponse)
@get_limiter().limit(f"{get_settings().rate_limit_per_ip}/minute")
async def validate(
    request: Request,
    body: ValidateRequest,
    session: AsyncSession = Depends(get_session),
    _api_key: str = Depends(verify_api_key),
):
    t_start = time.perf_counter()
    redis_client = get_redis_client()
    normalized = normalize_to_e164(body.number)

    if normalized:
        cached = await get_cached_result(redis_client, normalized)
        if cached is not None:
            latency = round((time.perf_counter() - t_start) * 1000, 2)
            verdict_value = cached.get("verdict", "UNKNOWN")
            validate_cache_hits_total.inc()
            validate_requests_total.labels(verdict=verdict_value).inc()
            validate_latency_seconds.observe(latency / 1000)
            print(f">>> VALIDACION (CACHED): Numero={normalized} | Veredicto={verdict_value} | Motivo={cached.get('reason')}")
            return ValidateResponse(
                verdict=Verdict(verdict_value),
                spam_score=cached.get("spam_score", 0),
                reason=cached.get("reason", "CACHED"),
                report_count=cached.get("report_count", 0),
                prefix_valid=cached.get("prefix_valid", False),
                prefix_zone=cached.get("prefix_zone"),
                operator=cached.get("operator"),
                cached=True,
                latency_ms=latency,
            )

    response = await validate_number(session, redis_client, normalized)
    print(f">>> VALIDACION: Numero={normalized} | Veredicto={response.verdict.value} | Motivo={response.reason}")
    latency = response.latency_ms
    validate_requests_total.labels(verdict=response.verdict.value).inc()
    validate_latency_seconds.observe(latency / 1000)
    return response


@router.get("/number/{e164}", response_model=NumberLookupResponse)
async def number_lookup(
    e164: str,
    session: AsyncSession = Depends(get_session),
):
    stmt = select(PhoneNumber).where(PhoneNumber.number_e164 == e164).limit(1)
    result = await session.execute(stmt)
    phone = result.scalar_one_or_none()

    if phone is None:
        raise HTTPException(status_code=404, detail="Number not found")

    prefix_zone = None
    if phone.prefix_id:
        prefix_stmt = select(AreaPrefix).where(AreaPrefix.id == phone.prefix_id)
        prefix_result = await session.execute(prefix_stmt)
        prefix = prefix_result.scalar_one_or_none()
        if prefix:
            prefix_zone = _prefix_zone(prefix)

    return NumberLookupResponse(
        number_e164=phone.number_e164,
        status=phone.status.value if phone.status else "UNVERIFIED",
        spam_score=phone.spam_score,
        report_count=phone.report_count,
        prefix_zone=prefix_zone,
    )


@router.get("/prefixes", response_model=PrefixesResponse)
async def list_prefixes(
    session: AsyncSession = Depends(get_session),
):
    stmt = select(AreaPrefix).where(AreaPrefix.is_valid).order_by(AreaPrefix.prefix)
    result = await session.execute(stmt)
    prefixes = result.scalars().all()

    items = [
        PrefixItem(
            prefix=p.prefix,
            city=p.city,
            province=p.province,
            is_mobile=p.is_mobile,
        )
        for p in prefixes
    ]

    return PrefixesResponse(items=items, count=len(items))
