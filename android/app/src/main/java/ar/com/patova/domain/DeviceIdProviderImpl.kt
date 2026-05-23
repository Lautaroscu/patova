package ar.com.patova.domain

import ar.com.patova.data.local.EncryptedPreferencesManager
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceIdProviderImpl @Inject constructor(
    private val encryptedPrefs: EncryptedPreferencesManager
) : DeviceIdProvider {

    override fun getDeviceIdHash(): String {
        val uuid = getOrCreateUuid()
        return sha256(uuid)
    }

    private fun getOrCreateUuid(): String {
        val existing = encryptedPrefs.getString(EncryptedPreferencesManager.KEY_DEVICE_UUID)
        if (existing != null) return existing
        val newUuid = UUID.randomUUID().toString()
        encryptedPrefs.putString(EncryptedPreferencesManager.KEY_DEVICE_UUID, newUuid)
        return newUuid
    }

    companion object {
        fun sha256(input: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest(input.toByteArray())
                .joinToString("") { "%02x".format(it) }
    }
}
