package ar.com.patova.data.local

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalConfigManager @Inject constructor(
    private val encryptedPrefs: EncryptedPreferencesManager
) {
    var blockNonContacts: Boolean
        get() = encryptedPrefs.getBoolean("block_non_contacts", false)
        set(value) = encryptedPrefs.putBoolean("block_non_contacts", value)

    var allowedPrefixes: List<String>
        get() {
            val jsonString = encryptedPrefs.getString("allowed_prefixes", "[]") ?: "[]"
            return try { Json.decodeFromString(jsonString) } catch (e: Exception) { emptyList() }
        }
        set(value) {
            val jsonString = Json.encodeToString(value)
            encryptedPrefs.putString("allowed_prefixes", jsonString)
        }

    var blockedPrefixes: List<String>
        get() {
            val jsonString = encryptedPrefs.getString("blocked_prefixes", "[]") ?: "[]"
            return try { Json.decodeFromString(jsonString) } catch (e: Exception) { emptyList() }
        }
        set(value) {
            val jsonString = Json.encodeToString(value)
            encryptedPrefs.putString("blocked_prefixes", jsonString)
        }
}
