from datetime import UTC, datetime

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from patova.models.blacklist_entry import BlacklistEntry
from patova.models.user_preferences import UserPreferences
from patova.models.whitelist_entry import WhitelistEntry
from patova.schemas.sync import (
    BlacklistDeltaEntry,
    CanonicalPreferences,
    LocalChanges,
    LocalPreferences,
    SyncResponse,
    WhitelistDeltaEntry,
)


async def sync_behavior(
    session: AsyncSession,
    user_id: str,
    client_last_sync_timestamp: datetime | None,
    local_changes: LocalChanges | None,
) -> SyncResponse:
    if local_changes is None:
        local_changes = LocalChanges()

    if client_last_sync_timestamp is None:
        client_last_sync_timestamp = datetime(1970, 1, 1, tzinfo=UTC)
    elif client_last_sync_timestamp.tzinfo is None:
        client_last_sync_timestamp = client_last_sync_timestamp.replace(tzinfo=UTC)

    # Convert whitelist_entries added_at to timezone-aware UTC if they are naive
    for entry in local_changes.new_whitelist_entries:
        if entry.added_at.tzinfo is None:
            entry.added_at = entry.added_at.replace(tzinfo=UTC)

    # Convert blacklist_entries added_at to timezone-aware UTC if they are naive
    for entry in local_changes.new_blacklist_entries:
        if entry.added_at.tzinfo is None:
            entry.added_at = entry.added_at.replace(tzinfo=UTC)

    # If preferences is not None, ensure updated_at is timezone-aware
    if local_changes.preferences is not None and local_changes.preferences.updated_at.tzinfo is None:
        local_changes.preferences.updated_at = local_changes.preferences.updated_at.replace(tzinfo=UTC)

    canonical_prefs = await _resolve_preferences(session, user_id, local_changes.preferences)
    await _merge_whitelist_entries(session, user_id, local_changes.new_whitelist_entries)
    await _merge_blacklist_entries(session, user_id, local_changes.new_blacklist_entries)

    whitelist_delta = await _compute_whitelist_delta(
        session, user_id, client_last_sync_timestamp, local_changes.new_whitelist_entries
    )
    blacklist_delta = await _compute_blacklist_delta(
        session, user_id, client_last_sync_timestamp, local_changes.new_blacklist_entries
    )

    return SyncResponse(
        sync_timestamp=datetime.now(UTC),
        sync_status="SUCCESS",
        canonical_preferences=canonical_prefs,
        whitelist_delta=whitelist_delta,
        blacklist_delta=blacklist_delta,
    )


async def _resolve_preferences(
    session: AsyncSession,
    user_id: str,
    local_prefs: LocalPreferences | None,
) -> CanonicalPreferences:
    stmt = select(UserPreferences).where(UserPreferences.user_id == user_id)
    result = await session.execute(stmt)
    server_prefs = result.scalar_one_or_none()

    if local_prefs is None:
        if server_prefs is not None:
            return _to_canonical(server_prefs)
        return CanonicalPreferences(
            strict_mode=False,
            block_unknown=False,
            spam_threshold=0.75,
            sync_enabled=True,
            updated_at=datetime.now(UTC),
        )

    if server_prefs is None:
        server_prefs = UserPreferences(
            user_id=user_id,
            strict_mode=local_prefs.strict_mode,
            block_unknown=local_prefs.block_unknown,
            spam_threshold=local_prefs.spam_threshold,
            sync_enabled=local_prefs.sync_enabled,
            updated_at=local_prefs.updated_at,
        )
        session.add(server_prefs)
        await session.flush()
        return _to_canonical(server_prefs)

    if local_prefs.updated_at > server_prefs.updated_at:
        server_prefs.strict_mode = local_prefs.strict_mode
        server_prefs.block_unknown = local_prefs.block_unknown
        server_prefs.spam_threshold = local_prefs.spam_threshold
        server_prefs.sync_enabled = local_prefs.sync_enabled
        server_prefs.updated_at = local_prefs.updated_at
        await session.flush()

    return _to_canonical(server_prefs)


async def _merge_whitelist_entries(
    session: AsyncSession,
    user_id: str,
    entries: list,
) -> None:
    if not entries:
        return
    for entry in entries:
        stmt = select(WhitelistEntry).where(
            WhitelistEntry.user_id == user_id,
            WhitelistEntry.phone_hash == entry.phone_hash,
        )
        result = await session.execute(stmt)
        existing = result.scalar_one_or_none()
        if existing is None:
            session.add(
                WhitelistEntry(
                    user_id=user_id,
                    phone_hash=entry.phone_hash,
                    label=entry.label,
                    added_at=entry.added_at,
                )
            )
    await session.flush()


async def _merge_blacklist_entries(
    session: AsyncSession,
    user_id: str,
    entries: list,
) -> None:
    if not entries:
        return
    for entry in entries:
        stmt = select(BlacklistEntry).where(
            BlacklistEntry.user_id == user_id,
            BlacklistEntry.phone_hash == entry.phone_hash,
        )
        result = await session.execute(stmt)
        existing = result.scalar_one_or_none()
        if existing is None:
            session.add(
                BlacklistEntry(
                    user_id=user_id,
                    phone_hash=entry.phone_hash,
                    reason=entry.reason,
                    added_at=entry.added_at,
                )
            )
    await session.flush()


async def _compute_whitelist_delta(
    session: AsyncSession,
    user_id: str,
    client_last_sync_timestamp: datetime,
    client_entries: list,
) -> list[WhitelistDeltaEntry]:
    client_hashes = {e.phone_hash for e in client_entries}
    stmt = (
        select(WhitelistEntry)
        .where(
            WhitelistEntry.user_id == user_id,
            WhitelistEntry.added_at > client_last_sync_timestamp,
        )
    )
    result = await session.execute(stmt)
    server_entries = result.scalars().all()

    delta = []
    for entry in server_entries:
        if entry.phone_hash not in client_hashes:
            delta.append(
                WhitelistDeltaEntry(
                    phone_hash=entry.phone_hash,
                    label=entry.label,
                    added_at=entry.added_at,
                )
            )
    return delta


async def _compute_blacklist_delta(
    session: AsyncSession,
    user_id: str,
    client_last_sync_timestamp: datetime,
    client_entries: list,
) -> list[BlacklistDeltaEntry]:
    client_hashes = {e.phone_hash for e in client_entries}
    stmt = (
        select(BlacklistEntry)
        .where(
            BlacklistEntry.user_id == user_id,
            BlacklistEntry.added_at > client_last_sync_timestamp,
        )
    )
    result = await session.execute(stmt)
    server_entries = result.scalars().all()

    delta = []
    for entry in server_entries:
        if entry.phone_hash not in client_hashes:
            delta.append(
                BlacklistDeltaEntry(
                    phone_hash=entry.phone_hash,
                    reason=entry.reason,
                    added_at=entry.added_at,
                )
            )
    return delta


def _to_canonical(prefs: UserPreferences) -> CanonicalPreferences:
    return CanonicalPreferences(
        strict_mode=prefs.strict_mode,
        block_unknown=prefs.block_unknown,
        spam_threshold=prefs.spam_threshold,
        sync_enabled=prefs.sync_enabled,
        updated_at=prefs.updated_at,
    )
