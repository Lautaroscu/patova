package ar.com.numguard.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PremiumCacheManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "numguard_premium_secure",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    var isPremium: Boolean
        get() = prefs.getBoolean("is_premium", false)
        private set(value) = prefs.edit().putBoolean("is_premium", value).apply()

    var subscriptionStatus: String
        get() = prefs.getString("subscription_status", "INACTIVE") ?: "INACTIVE"
        private set(value) = prefs.edit().putString("subscription_status", value).apply()

    var expiresAtMillis: Long
        get() = prefs.getLong("expires_at_millis", 0L)
        private set(value) = prefs.edit().putLong("expires_at_millis", value).apply()

    var lastSyncMillis: Long
        get() = prefs.getLong("last_sync_millis", 0L)
        private set(value) = prefs.edit().putLong("last_sync_millis", value).apply()

    var userId: String
        get() = prefs.getString("user_id", "") ?: ""
        private set(value) = prefs.edit().putString("user_id", value).apply()

    var userEmail: String
        get() = prefs.getString("user_email", "") ?: ""
        set(value) = prefs.edit().putString("user_email", value).apply()

    val premiumAvailableOffline: Boolean
        get() {
            if (!isPremium) return false
            if (subscriptionStatus != "ACTIVE") return false
            val now = System.currentTimeMillis()
            val expiresAt = expiresAtMillis
            if (now >= expiresAt) {
                clearPremiumState()
                return false
            }
            val lastSync = lastSyncMillis
            val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
            if (lastSync == 0L) return false
            return (now - lastSync) <= sevenDaysMs
        }

    fun cachePremiumState(
        isPremium: Boolean,
        status: String,
        expiresAt: Long,
        userId: String
    ) {
        this.isPremium = isPremium
        this.subscriptionStatus = status
        this.expiresAtMillis = expiresAt
        this.lastSyncMillis = System.currentTimeMillis()
        this.userId = userId
    }

    fun clearPremiumState() {
        this.isPremium = false
        this.subscriptionStatus = "INACTIVE"
        this.expiresAtMillis = 0L
    }

    fun updateLastSync() {
        this.lastSyncMillis = System.currentTimeMillis()
    }
}
