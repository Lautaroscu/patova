from datetime import UTC, datetime
from unittest.mock import AsyncMock, MagicMock

import pytest

from patova.schemas.sync import (
    BlacklistDeltaEntry,
    BlacklistEntryIn,
    CanonicalPreferences,
    LocalChanges,
    LocalPreferences,
    SyncRequest,
    SyncResponse,
    WhitelistDeltaEntry,
    WhitelistEntryIn,
)
from patova.services.sync import sync_behavior

pytestmark = pytest.mark.unit


class _MockResult:
    def __init__(self, scalar_value=None, scalars_list=None):
        self._scalar = scalar_value
        self._scalars = scalars_list or []

    def scalar_one_or_none(self):
        return self._scalar

    def scalars(self):
        return self

    def all(self):
        return self._scalars


class TestSyncRequestSchema:
    def test_minimal_request_valid(self):
        req = SyncRequest(
            user_id="user-1",
            client_last_sync_timestamp=datetime(2026, 5, 18, 0, 0, 0, tzinfo=UTC),
            local_changes=LocalChanges(),
        )
        assert req.user_id == "user-1"
        assert req.local_changes.preferences is None
        assert req.local_changes.new_whitelist_entries == []
        assert req.local_changes.new_blacklist_entries == []

    def test_full_request_valid(self):
        req = SyncRequest(
            user_id="user-1",
            client_last_sync_timestamp=datetime(2026, 5, 18, 0, 0, 0, tzinfo=UTC),
            local_changes=LocalChanges(
                preferences=LocalPreferences(
                    strict_mode=True,
                    block_unknown=False,
                    spam_threshold=0.75,
                    sync_enabled=True,
                    updated_at=datetime(2026, 5, 18, 1, 20, 0, tzinfo=UTC),
                ),
                new_whitelist_entries=[
                    WhitelistEntryIn(
                        phone_hash="a1b2c3d4",
                        label="Familia",
                        added_at=datetime(2026, 5, 18, 1, 10, 0, tzinfo=UTC),
                    ),
                ],
                new_blacklist_entries=[
                    BlacklistEntryIn(
                        phone_hash="f9e8d7c6",
                        reason="Molesto",
                        added_at=datetime(2026, 5, 18, 1, 15, 0, tzinfo=UTC),
                    ),
                ],
            ),
        )
        assert req.local_changes.preferences is not None
        assert len(req.local_changes.new_whitelist_entries) == 1
        assert len(req.local_changes.new_blacklist_entries) == 1

    def test_whitelist_delta_schema(self):
        entry = WhitelistDeltaEntry(
            phone_hash="z9y8x7w6",
            label="Global Allow",
            added_at=datetime(2026, 5, 17, 23, 50, 0, tzinfo=UTC),
        )
        assert entry.phone_hash == "z9y8x7w6"

    def test_blacklist_delta_schema(self):
        entry = BlacklistDeltaEntry(
            phone_hash="z9y8x7w6",
            reason="Global Block",
            added_at=datetime(2026, 5, 17, 23, 50, 0, tzinfo=UTC),
        )
        assert entry.phone_hash == "z9y8x7w6"

    def test_sync_response_schema(self):
        response = SyncResponse(
            sync_timestamp=datetime(2026, 5, 18, 1, 40, 0, tzinfo=UTC),
            sync_status="SUCCESS",
            canonical_preferences=CanonicalPreferences(
                strict_mode=True,
                block_unknown=False,
                spam_threshold=0.75,
                sync_enabled=True,
                updated_at=datetime(2026, 5, 18, 1, 20, 0, tzinfo=UTC),
            ),
            whitelist_delta=[],
            blacklist_delta=[
                BlacklistDeltaEntry(
                    phone_hash="z9y8x7w6",
                    reason="Global Block",
                    added_at=datetime(2026, 5, 17, 23, 50, 0, tzinfo=UTC),
                ),
            ],
        )
        assert response.sync_status == "SUCCESS"
        assert len(response.blacklist_delta) == 1
        assert len(response.whitelist_delta) == 0


class TestSyncService:
    def _make_session(self, scalar_value=None):
        session = AsyncMock()
        session.execute.return_value = _MockResult(scalar_value=scalar_value)
        session.add = MagicMock()
        session.flush = AsyncMock()
        return session

    @pytest.mark.asyncio
    async def test_first_sync_no_server_data_accepts_client(self):
        session = self._make_session(scalar_value=None)

        result = await sync_behavior(
            session=session,
            user_id="user-1",
            client_last_sync_timestamp=datetime(2026, 5, 18, 0, 0, 0, tzinfo=UTC),
            local_changes=LocalChanges(
                preferences=LocalPreferences(
                    strict_mode=True,
                    block_unknown=True,
                    spam_threshold=0.80,
                    sync_enabled=True,
                    updated_at=datetime(2026, 5, 18, 1, 20, 0, tzinfo=UTC),
                ),
            ),
        )
        assert result.sync_status == "SUCCESS"
        assert result.canonical_preferences.strict_mode is True
        assert result.canonical_preferences.block_unknown is True
        assert result.canonical_preferences.spam_threshold == 0.80

    @pytest.mark.asyncio
    async def test_last_write_wins_client_newer(self):
        from patova.models.user_preferences import UserPreferences

        server_prefs = UserPreferences(
            user_id="user-1",
            strict_mode=False,
            block_unknown=False,
            spam_threshold=0.50,
            sync_enabled=False,
            updated_at=datetime(2026, 5, 17, 12, 0, 0, tzinfo=UTC),
        )

        session = self._make_session(scalar_value=server_prefs)

        result = await sync_behavior(
            session=session,
            user_id="user-1",
            client_last_sync_timestamp=datetime(2026, 5, 18, 0, 0, 0, tzinfo=UTC),
            local_changes=LocalChanges(
                preferences=LocalPreferences(
                    strict_mode=True,
                    block_unknown=True,
                    spam_threshold=0.80,
                    sync_enabled=True,
                    updated_at=datetime(2026, 5, 18, 1, 20, 0, tzinfo=UTC),
                ),
            ),
        )
        assert result.canonical_preferences.strict_mode is True
        assert result.canonical_preferences.spam_threshold == 0.80

    @pytest.mark.asyncio
    async def test_last_write_wins_server_newer(self):
        from patova.models.user_preferences import UserPreferences

        server_prefs = UserPreferences(
            user_id="user-1",
            strict_mode=True,
            block_unknown=True,
            spam_threshold=0.90,
            sync_enabled=True,
            updated_at=datetime(2026, 5, 18, 10, 0, 0, tzinfo=UTC),
        )

        session = self._make_session(scalar_value=server_prefs)

        result = await sync_behavior(
            session=session,
            user_id="user-1",
            client_last_sync_timestamp=datetime(2026, 5, 18, 0, 0, 0, tzinfo=UTC),
            local_changes=LocalChanges(
                preferences=LocalPreferences(
                    strict_mode=False,
                    block_unknown=False,
                    spam_threshold=0.30,
                    sync_enabled=False,
                    updated_at=datetime(2026, 5, 17, 23, 0, 0, tzinfo=UTC),
                ),
            ),
        )
        assert result.canonical_preferences.strict_mode is True
        assert result.canonical_preferences.spam_threshold == 0.90

    @pytest.mark.asyncio
    async def test_no_local_preferences_returns_server(self):
        from patova.models.user_preferences import UserPreferences

        server_prefs = UserPreferences(
            user_id="user-1",
            strict_mode=True,
            block_unknown=False,
            spam_threshold=0.60,
            sync_enabled=True,
            updated_at=datetime(2026, 5, 17, 12, 0, 0, tzinfo=UTC),
        )

        session = self._make_session(scalar_value=server_prefs)

        result = await sync_behavior(
            session=session,
            user_id="user-1",
            client_last_sync_timestamp=datetime(2026, 5, 18, 0, 0, 0, tzinfo=UTC),
            local_changes=LocalChanges(),
        )
        assert result.canonical_preferences.strict_mode is True
        assert result.canonical_preferences.spam_threshold == 0.60

    @pytest.mark.asyncio
    async def test_whitelist_entries_merged_without_duplicates(self):
        session = self._make_session(scalar_value=None)

        result = await sync_behavior(
            session=session,
            user_id="user-1",
            client_last_sync_timestamp=datetime(2026, 5, 18, 0, 0, 0, tzinfo=UTC),
            local_changes=LocalChanges(
                new_whitelist_entries=[
                    WhitelistEntryIn(
                        phone_hash="hash1",
                        label="Familia",
                        added_at=datetime(2026, 5, 18, 1, 10, 0, tzinfo=UTC),
                    ),
                    WhitelistEntryIn(
                        phone_hash="hash2",
                        label="Trabajo",
                        added_at=datetime(2026, 5, 18, 1, 11, 0, tzinfo=UTC),
                    ),
                ],
            ),
        )
        assert result.sync_status == "SUCCESS"
        assert len(result.whitelist_delta) == 0

    @pytest.mark.asyncio
    async def test_blacklist_entries_merged_without_duplicates(self):
        session = self._make_session(scalar_value=None)

        result = await sync_behavior(
            session=session,
            user_id="user-1",
            client_last_sync_timestamp=datetime(2026, 5, 18, 0, 0, 0, tzinfo=UTC),
            local_changes=LocalChanges(
                new_blacklist_entries=[
                    BlacklistEntryIn(
                        phone_hash="hash3",
                        reason="Molesto",
                        added_at=datetime(2026, 5, 18, 1, 15, 0, tzinfo=UTC),
                    ),
                ],
            ),
        )
        assert result.sync_status == "SUCCESS"
        assert len(result.blacklist_delta) == 0

    @pytest.mark.asyncio
    async def test_delta_returns_server_entries_client_lacks(self):
        from patova.models.blacklist_entry import BlacklistEntry

        server_entry = BlacklistEntry(
            user_id="user-1",
            phone_hash="global_hash",
            reason="Global Block",
            added_at=datetime(2026, 5, 17, 23, 50, 0, tzinfo=UTC),
        )

        none_result = _MockResult(scalar_value=None)
        scalars_result = _MockResult(scalars_list=[server_entry])

        call_count = [0]

        async def execute_side_effect(*args, **kwargs):
            call_count[0] += 1
            count = call_count[0]
            if count == 4:
                return scalars_result
            return none_result

        session = AsyncMock()
        session.execute = execute_side_effect
        session.add = MagicMock()
        session.flush = AsyncMock()

        result = await sync_behavior(
            session=session,
            user_id="user-1",
            client_last_sync_timestamp=datetime(2026, 5, 17, 0, 0, 0, tzinfo=UTC),
            local_changes=LocalChanges(
                new_blacklist_entries=[
                    BlacklistEntryIn(
                        phone_hash="hash3",
                        reason="Molesto",
                        added_at=datetime(2026, 5, 18, 1, 15, 0, tzinfo=UTC),
                    ),
                ],
            ),
        )
        assert len(result.blacklist_delta) == 1
        assert result.blacklist_delta[0].phone_hash == "global_hash"
        assert result.blacklist_delta[0].reason == "Global Block"
