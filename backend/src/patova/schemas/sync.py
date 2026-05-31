from datetime import datetime

from pydantic import BaseModel, Field


class LocalPreferences(BaseModel):
    strict_mode: bool
    block_unknown: bool
    spam_threshold: float
    sync_enabled: bool
    updated_at: datetime


class WhitelistEntryIn(BaseModel):
    phone_hash: str = Field(..., min_length=1, max_length=128)
    label: str | None = Field(None, max_length=200)
    added_at: datetime


class BlacklistEntryIn(BaseModel):
    phone_hash: str = Field(..., min_length=1, max_length=128)
    reason: str | None = Field(None, max_length=500)
    added_at: datetime


class LocalChanges(BaseModel):
    preferences: LocalPreferences | None = None
    new_whitelist_entries: list[WhitelistEntryIn] = Field(default_factory=list)
    new_blacklist_entries: list[BlacklistEntryIn] = Field(default_factory=list)


class SyncRequest(BaseModel):
    user_id: str = Field(..., min_length=1, max_length=256)
    client_last_sync_timestamp: datetime
    local_changes: LocalChanges


class CanonicalPreferences(BaseModel):
    strict_mode: bool
    block_unknown: bool
    spam_threshold: float
    sync_enabled: bool
    updated_at: datetime


class WhitelistDeltaEntry(BaseModel):
    phone_hash: str
    label: str | None = None
    added_at: datetime


class BlacklistDeltaEntry(BaseModel):
    phone_hash: str
    reason: str | None = None
    added_at: datetime


class SyncResponse(BaseModel):
    sync_timestamp: datetime
    sync_status: str
    canonical_preferences: CanonicalPreferences
    whitelist_delta: list[WhitelistDeltaEntry] = Field(default_factory=list)
    blacklist_delta: list[BlacklistDeltaEntry] = Field(default_factory=list)
