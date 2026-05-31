import hashlib

import phonenumbers
from phonenumbers import NumberParseException, PhoneNumberFormat

_ARG_REGION = "AR"
_ARG_CODE = 54
_KNOWN_PREFIXES = {
    "11", "220", "221", "222", "223", "224", "225", "226", "227", "228", "229",
    "230", "231", "232", "233", "234", "235", "236", "237", "238", "239",
    "247", "249",
    "260", "261", "262", "263", "264", "265", "266",
    "280",
    "290", "291", "292", "293", "294", "295", "296", "297", "298",
    "299",
    "336", "340", "341", "342", "343", "344", "345", "346", "347", "348", "349",
    "351", "352", "353", "354", "356", "357", "358",
    "362", "364", "370", "371", "372", "373", "374", "375", "376", "377", "378",
    "379",
    "380", "381", "382", "383", "384", "385", "386", "387", "388", "389",
    "400",
}


def normalize_to_e164(raw_number: str, region: str = _ARG_REGION) -> str | None:
    try:
        parsed = phonenumbers.parse(raw_number, region)
    except NumberParseException:
        return None
    if not phonenumbers.is_valid_number(parsed) and not _is_plausible(parsed):
        return None
    return phonenumbers.format_number(parsed, PhoneNumberFormat.E164)


def _is_plausible(parsed: phonenumbers.PhoneNumber) -> bool:
    if parsed.country_code != _ARG_CODE:
        return bool(phonenumbers.is_possible_number(parsed))
    national = str(parsed.national_number)
    length = len(national)
    if length < 8 or length > 10:
        return False
    if national.startswith("9") and length not in (9, 10):
        return False
    return True


def extract_argentina_prefix(number_e164: str) -> str | None:
    try:
        parsed = phonenumbers.parse(number_e164, _ARG_REGION)
    except NumberParseException:
        return None
    if parsed.country_code != _ARG_CODE:
        return None
    national = str(parsed.national_number)
    if national.startswith("9") and len(national) >= 3:
        national = national[1:]
    for length in (4, 3, 2):
        if len(national) > length:
            candidate = national[:length]
            if candidate in _KNOWN_PREFIXES:
                if len(candidate) <= 3 and not candidate.startswith("0"):
                    return "0" + candidate
                return candidate
    return None


def hash_for_log(value: str) -> str:
    return hashlib.sha256(value.encode()).hexdigest()[:8]
