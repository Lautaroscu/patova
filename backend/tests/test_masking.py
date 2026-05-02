import pytest

from numguard.services.stats_service import mask_e164

pytestmark = pytest.mark.unit


class TestMaskE164:
    def test_argentina_mobile(self):
        result = mask_e164("+5491112345678")
        assert result == "+5491****5678"

    def test_argentina_fixed(self):
        result = mask_e164("+541112345678")
        assert result == "+5411****5678"

    def test_middle_digits_hidden(self):
        result = mask_e164("+541112345678")
        assert "*" in result[5:8]
        assert "1234" not in result

    def test_short_number(self):
        result = mask_e164("+541112")
        assert result == "+54****"

    def test_exact_boundary(self):
        result = mask_e164("+54112345")
        assert result == "+5411****2345"

    def test_null_safe(self):
        assert mask_e164(None) is None

    def test_empty_string(self):
        assert mask_e164("") == ""

    def test_non_e164(self):
        assert mask_e164("some_text") == "some_text"
