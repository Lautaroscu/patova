from datetime import UTC, datetime, timedelta
from unittest.mock import MagicMock

import pytest

from numguard.services.scoring_service import (
    calculate_spam_score,
    get_reports_last_24h,
    should_auto_block,
)

pytestmark = pytest.mark.unit


def _make_reports(count: int, hours_ago: int = 0) -> list[MagicMock]:
    reports = []
    for i in range(count):
        r = MagicMock()
        r.reporter_device_id = f"device-{i}"
        r.created_at = datetime.now(UTC) - timedelta(hours=hours_ago)
        reports.append(r)
    return reports


class TestCalculateSpamScore:
    def test_single_report_score(self):
        reports = _make_reports(1)
        score = calculate_spam_score(reports)
        assert score == 10

    def test_three_reports_score(self):
        reports = _make_reports(3)
        score = calculate_spam_score(reports)
        assert score == 30

    def test_five_reports_score(self):
        reports = _make_reports(5)
        score = calculate_spam_score(reports)
        assert score == 40

    def test_ten_reports_score(self):
        reports = _make_reports(10)
        score = calculate_spam_score(reports)
        assert score == 65

    def test_eleven_reports_score(self):
        reports = _make_reports(11)
        score = calculate_spam_score(reports)
        assert int(score) == int((30 + 35 + 2) * 1.5)

    def test_fifteen_reports_score(self):
        reports = _make_reports(15)
        score = calculate_spam_score(reports)
        assert score == 100

    def test_score_capped_at_100(self):
        reports = _make_reports(50)
        score = calculate_spam_score(reports)
        assert score == 100


class TestShouldAutoBlock:
    def test_under_threshold(self):
        reports = _make_reports(30)
        assert should_auto_block(reports) is False

    def test_at_threshold(self):
        reports = _make_reports(50)
        assert should_auto_block(reports) is True

    def test_above_threshold(self):
        reports = _make_reports(60)
        assert should_auto_block(reports) is True


class TestGetReportsLast24h:
    def test_filters_old_reports(self):
        recent = _make_reports(3, hours_ago=1)
        old = _make_reports(5, hours_ago=25)
        all_reports = recent + old
        filtered = get_reports_last_24h(all_reports)
        assert len(filtered) == 3

    def test_all_recent(self):
        recent = _make_reports(5, hours_ago=0)
        filtered = get_reports_last_24h(recent)
        assert len(filtered) == 5
