from enum import StrEnum


class NumberStatus(StrEnum):
    CLEAN = "CLEAN"
    SUSPECT = "SUSPECT"
    SPAM = "SPAM"
    UNVERIFIED = "UNVERIFIED"


class NumberSource(StrEnum):
    ENACOM = "ENACOM"
    CROWDSOURCE = "CROWDSOURCE"
    AI_AGENT = "AI_AGENT"
    MANUAL = "MANUAL"
    SEED = "SEED"


class ReportType(StrEnum):
    SPAM_CALL = "SPAM_CALL"
    ROBOCALL = "ROBOCALL"
    SCAM = "SCAM"
    FRAUD = "FRAUD"
    OTHER = "OTHER"


class FeedbackType(StrEnum):
    FALSE_POSITIVE = "FALSE_POSITIVE"
    WAS_SPAM = "WAS_SPAM"
    CORRECT_BLOCK = "CORRECT_BLOCK"
    CORRECT_ALLOW = "CORRECT_ALLOW"


class SubscriptionStatus(StrEnum):
    PENDING = "PENDING"
    ACTIVE = "ACTIVE"
    CANCELLED = "CANCELLED"
    EXPIRED = "EXPIRED"


class PlanId(StrEnum):
    PREMIUM_MONTHLY = "premium_monthly"
    PREMIUM_ANNUAL = "premium_annual"
