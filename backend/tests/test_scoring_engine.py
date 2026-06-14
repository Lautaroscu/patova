import hashlib
from datetime import UTC, datetime, timedelta
from unittest.mock import MagicMock

import pytest

from patova.services.scoring.engine import (
    _classify_state,
    _sigscore,
    _describe_flags,
    _severity_label,
    compute_reputation,
    filter_sybil_reports,
)

pytestmark = pytest.mark.unit

NOW = datetime(2026, 5, 17, 12, 0, 0, tzinfo=UTC)


def _make_report(
    device_id: str = "device-a",
    report_type: str = "SPAM_CALL",
    hours_ago: float = 0.0,
    ip_hash: str | None = None,
    phone_number: str | None = None,
    phone_number_id: str | None = None,
) -> MagicMock:
    phone_val = phone_number or phone_number_id or "00000000-0000-0000-0000-000000000001"
    r = MagicMock()
    r.reporter_device_id = device_id
    r.report_type = MagicMock()
    r.report_type.value = report_type
    r.created_at = NOW - timedelta(hours=hours_ago)
    r.ip_hash = ip_hash
    r.phone_number = phone_val
    r.phone_number_id = phone_val
    return r


class TestSigScore:
    def test_zero_gives_zero(self):
        assert _sigscore(0.0) == 0.0

    def test_negative_gives_zero(self):
        assert _sigscore(-5.0) == 0.0

    def test_eight_gives_around_point_five(self):
        s = _sigscore(5.0)
        assert 0.49 < s < 0.51

    def test_twenty_approaches_one(self):
        s = _sigscore(20.0)
        assert s > 0.95


class TestClassifyState:
    def test_safe(self):
        assert _classify_state(0.0) == "SAFE"
        assert _classify_state(0.14) == "SAFE"

    def test_likely_safe(self):
        assert _classify_state(0.15) == "LIKELY_SAFE"
        assert _classify_state(0.29) == "LIKELY_SAFE"

    def test_unknown(self):
        assert _classify_state(0.30) == "UNKNOWN"
        assert _classify_state(0.49) == "UNKNOWN"

    def test_suspicious(self):
        assert _classify_state(0.50) == "SUSPICIOUS"
        assert _classify_state(0.69) == "SUSPICIOUS"

    def test_likely_spam(self):
        assert _classify_state(0.70) == "LIKELY_SPAM"
        assert _classify_state(0.84) == "LIKELY_SPAM"

    def test_blocked(self):
        assert _classify_state(0.85) == "BLOCKED"
        assert _classify_state(1.0) == "BLOCKED"


class TestFilterSybilReports:
    def test_unique_device_per_number_passes(self):
        reports = [
            _make_report(device_id="d1", phone_number_id="p1"),
            _make_report(device_id="d2", phone_number_id="p1"),
        ]
        clean, removed = filter_sybil_reports(reports)
        assert len(clean) == 2
        assert removed == 0

    def test_duplicate_device_number_removed(self):
        reports = [
            _make_report(device_id="d1", phone_number_id="p1"),
            _make_report(device_id="d1", phone_number_id="p1"),
            _make_report(device_id="d2", phone_number_id="p1"),
        ]
        clean, removed = filter_sybil_reports(reports)
        assert len(clean) == 2
        assert removed == 1

    def test_suspicious_ip_removed(self):
        reports = []
        for i in range(15):
            reports.append(
                _make_report(device_id=f"d{i}", ip_hash="bad-ip", phone_number_id=f"p{i}")
            )
        clean, removed = filter_sybil_reports(reports)
        assert removed == 15
        assert len(clean) == 0

    def test_mixed_good_and_sybil(self):
        reports = [
            _make_report(device_id="d1", ip_hash="good-ip", phone_number_id="p1"),
            _make_report(device_id="d1", ip_hash="good-ip", phone_number_id="p1"),
        ]
        for i in range(12):
            reports.append(
                _make_report(device_id=f"sybil-{i}", ip_hash="bad-ip", phone_number_id=f"p{i}")
            )
        clean, removed = filter_sybil_reports(reports)
        assert removed == 13
        assert len(clean) == 1


class TestDescribeFlags:
    def test_fraud_flagged(self):
        reports = [_make_report(report_type="FRAUD")]
        flags = _describe_flags(reports)
        assert "FRAUD_OR_SCAM_REPORTED" in flags

    def test_high_volume_flagged(self):
        reports = [_make_report(device_id=f"d{i}") for i in range(12)]
        flags = _describe_flags(reports)
        assert "HIGH_REPORT_VOLUME" in flags

    def test_burst_flagged(self):
        reports = [
            _make_report(device_id="d1", hours_ago=0.1),
            _make_report(device_id="d2", hours_ago=0.2),
            _make_report(device_id="d3", hours_ago=0.3),
        ]
        flags = _describe_flags(reports, now=NOW)
        assert "HIGH_CALL_FREQUENCY_BURST" in flags

    def test_community_fallback(self):
        reports = [_make_report(device_id="d1")]
        flags = _describe_flags(reports)
        assert "COMMUNITY_REPORTS" in flags


class TestSeverityLabel:
    def test_high_with_fraud(self):
        reports = [_make_report(report_type="FRAUD")]
        assert _severity_label(reports) == "HIGH"

    def test_medium_with_volume(self):
        reports = [_make_report(device_id=f"d{i}") for i in range(15)]
        assert _severity_label(reports) == "MEDIUM"

    def test_low_default(self):
        reports = [_make_report()]
        assert _severity_label(reports) == "LOW"


class TestComputeReputation:
    PHONE_HASH = hashlib.sha256(b"+541112345678").hexdigest()

    def test_no_reports_unknown(self):
        result = compute_reputation(self.PHONE_HASH, [], now=NOW)
        assert result.reputation_score == 0.0
        assert result.reputation_state == "UNKNOWN"
        assert result.total_reports == 0

    def test_single_report_low_score(self):
        reports = [_make_report(device_id="d1", report_type="SPAM_CALL")]
        result = compute_reputation(self.PHONE_HASH, reports, now=NOW)
        assert result.total_reports == 1
        assert result.unique_reporters == 1
        assert result.reputation_score < 0.3

    def test_fraud_weighted_heavier(self):
        fraud_reports = [
            _make_report(device_id=f"df{i}", report_type="FRAUD", hours_ago=0.1)
            for i in range(5)
        ]
        spam_reports = [
            _make_report(device_id=f"ds{i+10}", report_type="SPAM_CALL", hours_ago=0.1)
            for i in range(5)
        ]
        result = compute_reputation(self.PHONE_HASH, fraud_reports + spam_reports, now=NOW)
        assert result.reputation_score > 0.25

    def test_diversity_penalty_same_device(self):
        reports = [_make_report(device_id="d1") for _ in range(10)]
        result = compute_reputation(self.PHONE_HASH, reports, now=NOW)
        assert result.total_reports == 1

    def test_many_unique_reporters_high_score(self):
        reports = [_make_report(device_id=f"d{i}", report_type="SCAM", hours_ago=0.5) for i in range(30)]
        result = compute_reputation(self.PHONE_HASH, reports, now=NOW)
        assert result.reputation_score > 0.85
        assert result.reputation_state == "BLOCKED"

    def test_old_reports_low_weight(self):
        reports = [_make_report(device_id=f"d{i}", hours_ago=80.0) for i in range(20)]
        result = compute_reputation(self.PHONE_HASH, reports, now=NOW)
        assert result.reputation_score < 0.3

    def test_explainability_present(self):
        reports = [
            _make_report(device_id="d1", report_type="FRAUD", hours_ago=0.1),
            _make_report(device_id="d2", report_type="ROBOCALL", hours_ago=0.2),
        ]
        result = compute_reputation(self.PHONE_HASH, reports, now=NOW)
        assert "heuristic_flags" in result.explainability
        assert "community_severity" in result.explainability
        assert "description" in result.explainability
        assert result.explainability["community_severity"] == "HIGH"

    def test_confidence_increases_with_unique_reporters(self):
        reports_1 = [_make_report(device_id="d1") for _ in range(1)]
        reports_5 = [_make_report(device_id=f"d{i}") for i in range(5)]
        results_1 = compute_reputation(self.PHONE_HASH, reports_1, now=NOW)
        results_5 = compute_reputation(self.PHONE_HASH, reports_5, now=NOW)
        assert results_5.confidence > results_1.confidence

    def test_confidence_capped_at_one(self):
        reports = [_make_report(device_id=f"d{i}") for i in range(50)]
        result = compute_reputation(self.PHONE_HASH, reports, now=NOW)
        assert result.confidence <= 1.0

    def test_anti_sybil_same_device_does_not_alter_score(self):
        reports_unique = [_make_report(device_id=f"d{i}", report_type="FRAUD") for i in range(5)]
        result_clean = compute_reputation(self.PHONE_HASH, reports_unique, now=NOW)

        reports_poisoned = reports_unique.copy()
        for _ in range(20):
            reports_poisoned.append(_make_report(device_id="attacker", report_type="FRAUD"))
        result_poisoned = compute_reputation(self.PHONE_HASH, reports_poisoned, now=NOW)

        assert abs(result_clean.reputation_score - result_poisoned.reputation_score) < 0.15

    def test_anti_sybil_suspicious_ip_bulk_ignored(self):
        reports_good = [_make_report(device_id=f"d{i}", report_type="FRAUD", hours_ago=0.1) for i in range(5)]
        reports_bad = [_make_report(device_id=f"bad-{i}", ip_hash="evil-ip", report_type="OTHER", hours_ago=0.1) for i in range(20)]
        result = compute_reputation(self.PHONE_HASH, reports_good + reports_bad, now=NOW)
        assert result.total_reports == 5
