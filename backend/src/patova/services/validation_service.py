import time
from dataclasses import dataclass

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from patova.core.config import get_settings
from patova.models.area_prefix import AreaPrefix
from patova.models.enums import NumberStatus
from patova.models.phone_number import PhoneNumber
from patova.schemas.validation import ValidateResponse, Verdict
from patova.services.cache_service import set_cached_result
from patova.services.phone_normalization import extract_argentina_prefix


@dataclass
class _LookupResult:
    phone: PhoneNumber | None = None
    prefix: AreaPrefix | None = None
    normalized: str | None = None


async def _lookup_db(
    session: AsyncSession, normalized: str | None
) -> _LookupResult:
    if not normalized:
        return _LookupResult(normalized=None)

    prefix_candidate = extract_argentina_prefix(normalized)
    prefix = None
    if prefix_candidate:
        prefix_stmt = select(AreaPrefix).where(
            AreaPrefix.prefix == prefix_candidate,
            AreaPrefix.is_valid,
        )
        prefix_result = await session.execute(prefix_stmt)
        prefix = prefix_result.scalar_one_or_none()

    stmt = (
        select(PhoneNumber)
        .where(PhoneNumber.number_e164 == normalized)
        .limit(1)
    )
    result = await session.execute(stmt)
    phone = result.scalar_one_or_none()

    if phone is None and prefix is None and prefix_candidate:
        prefix_stmt = select(AreaPrefix).where(
            AreaPrefix.prefix == prefix_candidate,
        )
        prefix_result = await session.execute(prefix_stmt)
        prefix = prefix_result.scalar_one_or_none()

    return _LookupResult(phone=phone, prefix=prefix, normalized=normalized)


def _decide_verdict(
    lookup: _LookupResult, settings
) -> tuple[Verdict, str]:
    if lookup.normalized is None:
        return Verdict.UNKNOWN, "UNPARSEABLE_NUMBER"

    if lookup.prefix is None:
        return Verdict.INVALID_PREFIX, "INVALID_PREFIX"
    if not lookup.prefix.is_valid:
        return Verdict.INVALID_PREFIX, "INVALID_PREFIX"

    if lookup.phone is None:
        # 3. Cloud Check a ENACOM (Simulado por ahora)
        # Si el número no está en nuestra DB, le preguntamos a ENACOM
        enacom_spam = _mock_enacom_check(lookup.normalized)
        if enacom_spam:
            return Verdict.BLOCK, "ENACOM_SPAM_REGISTRY"
        return Verdict.UNKNOWN, "UNKNOWN_NUMBER"

    phone = lookup.phone

    if phone.status == NumberStatus.SPAM or phone.spam_score >= settings.block_score_min:
        return Verdict.BLOCK, "HIGH_REPORT_VOLUME"

    if phone.spam_score >= settings.suspect_score_min:
        return Verdict.SUSPECT, "SUSPICIOUS_SCORE"

    if phone.status == NumberStatus.CLEAN:
        return Verdict.ALLOW, "CLEAN"

    return Verdict.UNKNOWN, "UNKNOWN_NUMBER"


def _prefix_zone(prefix: AreaPrefix | None) -> str | None:
    if prefix is None:
        return None
    if prefix.province and prefix.province != prefix.city:
        return f"{prefix.city} - {prefix.province}"
    return prefix.city


async def validate_number(
    session: AsyncSession,
    redis_client,
    normalized: str | None,
) -> ValidateResponse:
    t_start = time.perf_counter()
    settings = get_settings()

    lookup = await _lookup_db(session, normalized)
    verdict, reason = _decide_verdict(lookup, settings)

    response = ValidateResponse(
        verdict=verdict,
        spam_score=lookup.phone.spam_score if lookup.phone else 0,
        reason=reason,
        report_count=lookup.phone.report_count if lookup.phone else 0,
        prefix_valid=lookup.prefix is not None and lookup.prefix.is_valid,
        prefix_zone=_prefix_zone(lookup.prefix),
        operator=lookup.prefix.operator if lookup.prefix else None,
        cached=False,
        latency_ms=round((time.perf_counter() - t_start) * 1000, 2),
    )

    if lookup.normalized and settings.validate_cache_enabled:
        cache_data = {
            "verdict": verdict.value,
            "spam_score": response.spam_score,
            "reason": reason,
            "report_count": response.report_count,
            "prefix_valid": response.prefix_valid,
            "prefix_zone": response.prefix_zone,
            "operator": response.operator,
        }
        await set_cached_result(redis_client, lookup.normalized, cache_data, verdict.value)

    return response

def _mock_enacom_check(number: str) -> bool:
    """
    Simulación de consulta a ENACOM en tiempo real.
    Devuelve True si ENACOM reporta que es spam.
    """
    # Ejemplo: Si el número termina en 9999, simulamos que es spam de ENACOM
    if number and number.endswith("9999"):
        return True
    return False
