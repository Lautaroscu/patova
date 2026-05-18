package ar.com.numguard.domain

import ar.com.numguard.data.api.DeviceConfigRequest
import ar.com.numguard.data.api.NumGuardApi
import ar.com.numguard.data.local.LocalConfigManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class ConfigRepository @Inject constructor(
    private val localConfigManager: LocalConfigManager,
    private val api: NumGuardApi,
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("numguard_device", Context.MODE_PRIVATE)

    val deviceId: String
        get() {
            var id = prefs.getString("device_id", null)
            if (id == null) {
                id = UUID.randomUUID().toString()
                prefs.edit().putString("device_id", id).apply()
            }
            return id
        }

    suspend fun syncConfig() = withContext(Dispatchers.IO) {
        try {
            val response = api.getConfig(deviceId)
            localConfigManager.blockNonContacts = response.blockNonContacts
            localConfigManager.allowedPrefixes = response.allowedPrefixes
            localConfigManager.blockedPrefixes = response.blockedPrefixes
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateConfig(
        blockNonContacts: Boolean,
        allowedPrefixes: List<String>,
        blockedPrefixes: List<String>
    ) = withContext(Dispatchers.IO) {
        // Update local first for immediate feedback
        localConfigManager.blockNonContacts = blockNonContacts
        localConfigManager.allowedPrefixes = allowedPrefixes
        localConfigManager.blockedPrefixes = blockedPrefixes

        try {
            val request = DeviceConfigRequest(
                blockNonContacts = blockNonContacts,
                allowedPrefixes = allowedPrefixes,
                blockedPrefixes = blockedPrefixes
            )
            api.updateConfig(deviceId, request)
        } catch (e: Exception) {
            e.printStackTrace()
            // In a real app we might want to schedule a sync worker if this fails
        }
    }
    
    fun getBlockNonContacts(): Boolean = localConfigManager.blockNonContacts
    fun getAllowedPrefixes(): List<String> = localConfigManager.allowedPrefixes
    fun getBlockedPrefixes(): List<String> = localConfigManager.blockedPrefixes
}
