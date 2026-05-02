from unittest.mock import MagicMock

import pytest

from numguard.models.area_prefix import AreaPrefix
from numguard.models.enums import NumberStatus
from numguard.models.phone_number import PhoneNumber
from numguard.schemas.validation import Verdict
from numguard.services.validation_service import (
    _decide_verdict,
    _LookupResult,
    _prefix_zone,
)

pytestmark = pytest.mark.unit


class TestPrefixZone:
    def test_zone_with_different_city_and_province(self):
        prefix = MagicMock(spec=AreaPrefix)
        prefix.city = "Buenos Aires"
        prefix.province = "CABA"
        assert _prefix_zone(prefix) == "Buenos Aires - CABA"

    def test_zone_with_same_city_and_province(self):
        prefix = MagicMock(spec=AreaPrefix)
        prefix.city = "Cordoba"
        prefix.province = "Cordoba"
        assert _prefix_zone(prefix) == "Cordoba"

    def test_zone_none(self):
        assert _prefix_zone(None) is None


class TestDecideVerdict:
    @pytest.fixture
    def settings(self):
        s = MagicMock()
        s.block_score_min = 61
        s.suspect_score_min = 21
        return s

    def test_unparseable_number(self, settings):
        lookup = _LookupResult(normalized=None)
        verdict, reason = _decide_verdict(lookup, settings)
        assert verdict == Verdict.UNKNOWN
        assert reason == "UNPARSEABLE_NUMBER"

    def test_invalid_prefix_missing(self, settings):
        lookup = _LookupResult(normalized="+541112345678", prefix=None)
        verdict, reason = _decide_verdict(lookup, settings)
        assert verdict == Verdict.INVALID_PREFIX
        assert reason == "INVALID_PREFIX"

    def test_invalid_prefix_not_valid(self, settings):
        prefix = MagicMock(spec=AreaPrefix)
        prefix.is_valid = False
        lookup = _LookupResult(normalized="+541112345678", prefix=prefix)
        verdict, reason = _decide_verdict(lookup, settings)
        assert verdict == Verdict.INVALID_PREFIX

    def test_unknown_no_phone_record(self, settings):
        prefix = MagicMock(spec=AreaPrefix)
        prefix.is_valid = True
        lookup = _LookupResult(normalized="+541112345678", prefix=prefix, phone=None)
        verdict, reason = _decide_verdict(lookup, settings)
        assert verdict == Verdict.UNKNOWN
        assert reason == "UNKNOWN_NUMBER"

    def test_block_by_spam_status(self, settings):
        prefix = MagicMock(spec=AreaPrefix)
        prefix.is_valid = True
        phone = MagicMock(spec=PhoneNumber)
        phone.status = NumberStatus.SPAM
        phone.spam_score = 100
        lookup = _LookupResult(normalized="+541112345678", prefix=prefix, phone=phone)
        verdict, reason = _decide_verdict(lookup, settings)
        assert verdict == Verdict.BLOCK
        assert reason == "HIGH_REPORT_VOLUME"

    def test_block_by_high_score(self, settings):
        prefix = MagicMock(spec=AreaPrefix)
        prefix.is_valid = True
        phone = MagicMock(spec=PhoneNumber)
        phone.status = NumberStatus.UNVERIFIED
        phone.spam_score = 80
        lookup = _LookupResult(normalized="+541112345678", prefix=prefix, phone=phone)
        verdict, reason = _decide_verdict(lookup, settings)
        assert verdict == Verdict.BLOCK

    def test_suspect_score_range(self, settings):
        prefix = MagicMock(spec=AreaPrefix)
        prefix.is_valid = True
        phone = MagicMock(spec=PhoneNumber)
        phone.status = NumberStatus.UNVERIFIED
        phone.spam_score = 50
        lookup = _LookupResult(normalized="+541112345678", prefix=prefix, phone=phone)
        verdict, reason = _decide_verdict(lookup, settings)
        assert verdict == Verdict.SUSPECT
        assert reason == "SUSPICIOUS_SCORE"

    def test_allow_clean(self, settings):
        prefix = MagicMock(spec=AreaPrefix)
        prefix.is_valid = True
        phone = MagicMock(spec=PhoneNumber)
        phone.status = NumberStatus.CLEAN
        phone.spam_score = 0
        lookup = _LookupResult(normalized="+541112345678", prefix=prefix, phone=phone)
        verdict, reason = _decide_verdict(lookup, settings)
        assert verdict == Verdict.ALLOW
        assert reason == "CLEAN"

    def test_block_threshold_boundary(self, settings):
        prefix = MagicMock(spec=AreaPrefix)
        prefix.is_valid = True
        phone = MagicMock(spec=PhoneNumber)
        phone.status = NumberStatus.UNVERIFIED
        phone.spam_score = 61
        lookup = _LookupResult(normalized="+541112345678", prefix=prefix, phone=phone)
        verdict, reason = _decide_verdict(lookup, settings)
        assert verdict == Verdict.BLOCK

    def test_suspect_lower_boundary(self, settings):
        prefix = MagicMock(spec=AreaPrefix)
        prefix.is_valid = True
        phone = MagicMock(spec=PhoneNumber)
        phone.status = NumberStatus.UNVERIFIED
        phone.spam_score = 21
        lookup = _LookupResult(normalized="+541112345678", prefix=prefix, phone=phone)
        verdict, reason = _decide_verdict(lookup, settings)
        assert verdict == Verdict.SUSPECT
