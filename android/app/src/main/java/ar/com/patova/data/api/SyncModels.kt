package ar.com.patova.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SyncRequest(
    @SerialName("user_id") val userId: String,
    @SerialName("client_last_sync_timestamp") val lastSyncTimestamp: String?,
    @SerialName("local_changes") val localChanges: LocalChanges? = null
)

@Serializable
data class LocalChanges(
    @SerialName("preferences") val preferences: Map<String, String>? = null,
    @SerialName("new_whitelist_entries") val newWhitelist: List<WhitelistEntry>? = null,
    @SerialName("new_blacklist_entries") val newBlacklist: List<BlacklistEntry>? = null
)

@Serializable
data class WhitelistEntry(
    @SerialName("phone_hash") val phoneHash: String,
    @SerialName("label") val label: String?,
    @SerialName("added_at") val addedAt: String
)

@Serializable
data class BlacklistEntry(
    @SerialName("phone_hash") val phoneHash: String,
    @SerialName("reason") val reason: String?,
    @SerialName("added_at") val addedAt: String
)

@Serializable
data class SyncResponse(
    @SerialName("sync_timestamp") val syncTimestamp: String,
    @SerialName("sync_status") val syncStatus: String,
    @SerialName("whitelist_delta") val whitelistDelta: List<WhitelistEntry>,
    @SerialName("blacklist_delta") val blacklistDelta: List<BlacklistEntry>
)
