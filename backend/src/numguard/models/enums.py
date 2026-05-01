import enum


class NumberStatus(str, enum.Enum):
    CLEAN = "CLEAN"
    SUSPECT = "SUSPECT"
    SPAM = "SPAM"
    UNVERIFIED = "UNVERIFIED"


class NumberSource(str, enum.Enum):
    ENACOM = "ENACOM"
    CROWDSOURCE = "CROWDSOURCE"
    AI_AGENT = "AI_AGENT"
    MANUAL = "MANUAL"
    SEED = "SEED"


class ReportType(str, enum.Enum):
    SPAM_CALL = "SPAM_CALL"
    ROBOCALL = "ROBOCALL"
    SCAM = "SCAM"
    FRAUD = "FRAUD"
    OTHER = "OTHER"


class FeedbackType(str, enum.Enum):
    FALSE_POSITIVE = "FALSE_POSITIVE"
    WAS_SPAM = "WAS_SPAM"
    CORRECT_BLOCK = "CORRECT_BLOCK"
    CORRECT_ALLOW = "CORRECT_ALLOW"
