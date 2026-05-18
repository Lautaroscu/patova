package ar.com.numguard.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalConfigManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("numguard_config", Context.MODE_PRIVATE)

    var blockNonContacts: Boolean
        get() = prefs.getBoolean("block_non_contacts", false)
        set(value) = prefs.edit().putBoolean("block_non_contacts", value).apply()

    var allowedPrefixes: List<String>
        get() {
            val jsonString = prefs.getString("allowed_prefixes", "[]") ?: "[]"
            return try { Json.decodeFromString(jsonString) } catch (e: Exception) { emptyList() }
        }
        set(value) {
            val jsonString = Json.encodeToString(value)
            prefs.edit().putString("allowed_prefixes", jsonString).apply()
        }

    var blockedPrefixes: List<String>
        get() {
            val jsonString = prefs.getString("blocked_prefixes", "[]") ?: "[]"
            return try { Json.decodeFromString(jsonString) } catch (e: Exception) { emptyList() }
        }
        set(value) {
            val jsonString = Json.encodeToString(value)
            prefs.edit().putString("blocked_prefixes", jsonString).apply()
        }
}
