package ar.com.numguard.domain

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceIdProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : DeviceIdProvider {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("numguard_prefs", Context.MODE_PRIVATE)

    override fun getDeviceIdHash(): String {
        val uuid = getOrCreateUuid()
        return sha256(uuid)
    }

    private fun getOrCreateUuid(): String {
        val existing = prefs.getString(KEY_DEVICE_UUID, null)
        if (existing != null) return existing
        val newUuid = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_UUID, newUuid).apply()
        return newUuid
    }

    companion object {
        private const val KEY_DEVICE_UUID = "device_uuid"

        fun sha256(input: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(input.toByteArray())
                .joinToString("") { "%02x".format(it) }
    }
}
