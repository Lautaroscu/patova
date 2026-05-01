from datetime import UTC, datetime, timedelta

from numguard.core.config import get_settings
from numguard.models.report import Report


def _count_unique_reporters(reports: list[Report]) -> int:
    return len({r.reporter_device_id for r in reports})


def calculate_spam_score(reports_in_24h: list[Report]) -> int:
    unique_count = _count_unique_reporters(reports_in_24h)

    weight = 0
    for i in range(1, unique_count + 1):
        if i <= 3:
            weight += 10
        elif i <= 10:
            weight += 5
        else:
            weight += 2

    recency_factor = 1.5 if unique_count > 10 else 1.0

    return min(100, int(weight * recency_factor))


def should_auto_block(reports_in_24h: list[Report]) -> bool:
    settings = get_settings()
    unique_count = _count_unique_reporters(reports_in_24h)
    return unique_count >= settings.auto_block_threshold


def get_reports_last_24h(reports: list[Report]) -> list[Report]:
    cutoff = datetime.now(UTC) - timedelta(hours=24)
    return [r for r in reports if r.created_at >= cutoff]
