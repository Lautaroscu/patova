package ar.com.patova.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PremiumCacheManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = try {
        createEncryptedPrefs(context)
    } catch (t: Throwable) {
        android.util.Log.e("Patova", "Error inicializando Premium SharedPreferences, purgando datos...", t)
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                context.deleteSharedPreferences(PREFS_FILE_NAME)
            } else {
                val file = java.io.File(context.filesDir.parent, "shared_prefs/${PREFS_FILE_NAME}.xml")
                if (file.exists()) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("Patova", "No se pudo eliminar el archivo de preferencias premium corrupto", e)
        }

        try {
            createEncryptedPrefs(context)
        } catch (retryException: Throwable) {
            android.util.Log.e("Patova", "Fallo catastrófico de Keystore en PremiumCache. Fallback a SharedPreferences estándar.", retryException)
            context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
        }
    }

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        return EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        private const val PREFS_FILE_NAME = "patova_premium_secure_prefs"
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

    fun getReportsCountToday(): Int {
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val lastReportDate = prefs.getString("last_report_date", "")
        if (lastReportDate != todayStr) {
            prefs.edit().putString("last_report_date", todayStr).putInt("reports_count_today", 0).apply()
            return 0
        }
        return prefs.getInt("reports_count_today", 0)
    }

    fun incrementReportsCountToday() {
        val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        val count = getReportsCountToday()
        prefs.edit().putString("last_report_date", todayStr).putInt("reports_count_today", count + 1).apply()
    }
}
