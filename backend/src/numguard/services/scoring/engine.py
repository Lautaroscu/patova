import math
from dataclasses import dataclass, field
from datetime import UTC, datetime

from numguard.models.report import Report

# ---------------------------------------------------------------------------
# Weight constants — documented per the spec:
#   FRAUD = 1.0    SCAM = 0.9    ROBOCALL = 0.7
#   SPAM_CALL = 0.5   TELEMARKETING = 0.5   OTHER = 0.3
# ---------------------------------------------------------------------------
_REPORT_TYPE_WEIGHTS: dict[str, float] = {
    "FRAUD": 1.0,
    "SCAM": 0.9,
    "ROBOCALL": 0.7,
    "SPAM_CALL": 0.5,
    "OTHER": 0.3,
}

# Max IP reports per day before a reporter IP is flagged as Sybil
_MAX_IP_REPORTS_PER_DAY = 10
# Max reports from a single device for a single number before ignoring surplus
_MAX_DEDUPE_DEVICE_REPORTS_PER_NUMBER = 1


def _report_type_weight(report: Report) -> float:
    return _REPORT_TYPE_WEIGHTS.get(str(report.report_type.value), 0.3)


def _recency_factor(report: Report, now: datetime | None = None) -> float:
    now = now or datetime.now(UTC)
    age_hours = (now - report.created_at).total_seconds() / 3600.0
    if age_hours < 1:
        return 1.0
    if age_hours < 6:
        return 0.85
    if age_hours < 24:
        return 0.6
    if age_hours < 72:
        return 0.3
    return 0.1


def _sigscore(x: float) -> float:
    if x <= 0:
        return 0.0
    return 1.0 / (1.0 + math.exp(-0.5 * (x - 5.0)))


def _classify_state(score: float) -> str:
    if score < 0.15:
        return "SAFE"
    if score < 0.30:
        return "LIKELY_SAFE"
    if score < 0.50:
        return "UNKNOWN"
    if score < 0.70:
        return "SUSPICIOUS"
    if score < 0.85:
        return "LIKELY_SPAM"
    return "BLOCKED"


@dataclass
class ReputationResult:
    phone_hash: str
    reputation_score: float
    reputation_state: str
    total_reports: int
    unique_reporters: int
    confidence: float
    explainability: dict = field(default_factory=dict)
    last_seen: str | None = None


def _describe_flags(reports: list[Report], now: datetime | None = None) -> list[str]:
    now = now or datetime.now(UTC)
    flags: list[str] = []
    types = {r.report_type.value for r in reports}
    if "FRAUD" in types or "SCAM" in types:
        flags.append("FRAUD_OR_SCAM_REPORTED")
    if len(reports) >= 10:
        flags.append("HIGH_REPORT_VOLUME")
    recent = [r for r in reports if (now - r.created_at).total_seconds() < 7200]
    if len(recent) >= 3:
        flags.append("HIGH_CALL_FREQUENCY_BURST")
    if not flags:
        flags.append("COMMUNITY_REPORTS")
    return flags


def _severity_label(reports: list[Report]) -> str:
    if any(r.report_type.value in ("FRAUD", "SCAM") for r in reports):
        return "HIGH"
    if len(reports) >= 10:
        return "MEDIUM"
    return "LOW"


def filter_sybil_reports(reports: list[Report]) -> tuple[list[Report], int]:
    ip_counts: dict[str, int] = {}
    for r in reports:
        if r.ip_hash:
            ip_counts[r.ip_hash] = ip_counts.get(r.ip_hash, 0) + 1
    suspicious_ips = {ip for ip, c in ip_counts.items() if c > _MAX_IP_REPORTS_PER_DAY}
    seen_device_number: set[tuple[str, str]] = set()
    filtered: list[Report] = []
    sybil_removed = 0
    for r in reports:
        if r.ip_hash and r.ip_hash in suspicious_ips:
            sybil_removed += 1
            continue
        key = (r.reporter_device_id or "", str(r.phone_number_id))
        if key in seen_device_number:
            sybil_removed += 1
            continue
        seen_device_number.add(key)
        filtered.append(r)
    return filtered, sybil_removed


def compute_reputation(
    phone_hash: str,
    reports: list[Report],
    now: datetime | None = None,
) -> ReputationResult:
    now = now or datetime.now(UTC)

    clean_reports, sybil_removed = filter_sybil_reports(reports)

    total_reports = len(clean_reports)
    unique_reporters = len({r.reporter_device_id for r in clean_reports})

    if total_reports == 0:
        return ReputationResult(
            phone_hash=phone_hash,
            reputation_score=0.0,
            reputation_state="UNKNOWN",
            total_reports=0,
            unique_reporters=0,
            confidence=0.5,
            explainability={
                "heuristic_flags": [],
                "community_severity": "NONE",
                "description": "No reports received for this number.",
            },
            last_seen=None,
        )

    weighted_sum = sum(
        _report_type_weight(r) * _recency_factor(r, now) for r in clean_reports
    )

    diversity_ratio = unique_reporters / max(total_reports, 1)
    diversity_penalty = 1.0 - 0.25 * (1.0 - diversity_ratio)

    raw_score = _sigscore(weighted_sum) * diversity_penalty
    reputation_score = round(min(1.0, max(0.0, raw_score)), 4)

    reputation_state = _classify_state(reputation_score)

    confidence = round(min(1.0, 0.5 + (unique_reporters / 20.0) * 0.5), 4)

    heuristic_flags = _describe_flags(clean_reports, now)
    community_severity = _severity_label(clean_reports)

    sorted_reports = sorted(clean_reports, key=lambda r: r.created_at, reverse=True)
    last_seen = sorted_reports[0].created_at.isoformat() if sorted_reports else None

    flags_desc = ", ".join(f.replace("_", " ").lower() for f in heuristic_flags)

    return ReputationResult(
        phone_hash=phone_hash,
        reputation_score=reputation_score,
        reputation_state=reputation_state,
        total_reports=total_reports,
        unique_reporters=unique_reporters,
        confidence=confidence,
        explainability={
            "heuristic_flags": heuristic_flags,
            "community_severity": community_severity,
            "description": (
                f"Reportado {total_reports} veces por {unique_reporters} "
                f"usuarios únicos. Flags: {flags_desc}. "
                f"{sybil_removed} reportes Sybil ignorados."
                if sybil_removed
                else ""
            ),
        },
        last_seen=last_seen,
    )


class ReputationEngine:
    @staticmethod
    def evaluate(phone_hash: str, reports: list[Report]) -> ReputationResult:
        return compute_reputation(phone_hash, reports)
