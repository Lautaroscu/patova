package ar.com.patova.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedPreferencesManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = try {
        createEncryptedPrefs(context)
    } catch (t: Throwable) {
        android.util.Log.e("Patova", "Error inicializando EncryptedSharedPreferences, purgando datos...", t)
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                context.deleteSharedPreferences(ENCRYPTED_PREFS_FILE)
            } else {
                val file = java.io.File(context.filesDir.parent, "shared_prefs/${ENCRYPTED_PREFS_FILE}.xml")
                if (file.exists()) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("Patova", "No se pudo eliminar el archivo de preferencias corrupto", e)
        }
        
        try {
            createEncryptedPrefs(context)
        } catch (retryException: Throwable) {
            android.util.Log.e("Patova", "Fallo catastrófico de Keystore. Fallback a SharedPreferences estándar.", retryException)
            context.getSharedPreferences(ENCRYPTED_PREFS_FILE, Context.MODE_PRIVATE)
        }
    }

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        return EncryptedSharedPreferences.create(
            context,
            ENCRYPTED_PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getString(key: String, defaultValue: String? = null): String? =
        prefs.getString(key, defaultValue)

    fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean =
        prefs.getBoolean(key, defaultValue)

    fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun getLong(key: String, defaultValue: Long = 0L): Long =
        prefs.getLong(key, defaultValue)

    fun putLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    fun getFloat(key: String, defaultValue: Float = 0f): Float =
        prefs.getFloat(key, defaultValue)

    fun putFloat(key: String, value: Float) {
        prefs.edit().putFloat(key, value).apply()
    }

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val ENCRYPTED_PREFS_FILE = "patova_secure_prefs"
        const val KEY_DEVICE_UUID = "device_uuid"
        const val KEY_AUTH_TOKEN = "auth_token"
        const val KEY_USER_ID = "user_id"
    }
}
