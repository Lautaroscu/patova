import pytest

from patova.services.phone_normalization import (
    extract_argentina_prefix,
    normalize_to_e164,
)

pytestmark = pytest.mark.unit


class TestNormalizeE164:
    def test_international_format(self):
        assert normalize_to_e164("+541112345678") == "+541112345678"

    def test_local_format_with_15(self):
        result = normalize_to_e164("15 6123 4567", region="AR")
        assert result == "+541161234567"

    def test_with_spaces_and_hyphens(self):
        result = normalize_to_e164("+54 351-123-4567")
        assert result == "+543511234567"

    def test_argentina_mobile_with_9(self):
        assert normalize_to_e164("+54 9 11 6123 4567") == "+541161234567"
        assert normalize_to_e164("+5493511234567") == "+543511234567"

    def test_argentina_mobile_with_15_local_prefix(self):
        assert normalize_to_e164("351 15 123 4567", region="AR") == "+543511234567"
        assert normalize_to_e164("0351 15 123 4567", region="AR") == "+543511234567"

    def test_invalid_number_returns_none(self):
        assert normalize_to_e164("0000") is None

    def test_empty_string_returns_none(self):
        assert normalize_to_e164("") is None


class TestExtractPrefix:
    def test_caba_mobile(self):
        assert extract_argentina_prefix("+541112345678") == "011"

    def test_la_plata_landline(self):
        assert extract_argentina_prefix("+542214567890") == "0221"

    def test_cordoba_landline(self):
        assert extract_argentina_prefix("+543511234567") == "0351"

    def test_non_argentina_returns_none(self):
        assert extract_argentina_prefix("+15551234567") is None

    def test_invalid_returns_none(self):
        assert extract_argentina_prefix("notanumber") is None
