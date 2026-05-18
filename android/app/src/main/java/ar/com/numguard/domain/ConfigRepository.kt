package ar.com.numguard.domain

import ar.com.numguard.data.api.DeviceConfigRequest
import ar.com.numguard.data.api.NumGuardApi
import ar.com.numguard.data.local.LocalConfigManager
import ar.com.numguard.data.local.daos.BlacklistDao
import ar.com.numguard.data.local.daos.LocalPreferencesDao
import ar.com.numguard.data.local.daos.WhitelistDao
import ar.com.numguard.data.local.entities.BlacklistEntity
import ar.com.numguard.data.local.entities.LocalPreferencesEntity
import ar.com.numguard.data.local.entities.WhitelistEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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
    private val preferencesDao: LocalPreferencesDao,
    private val whitelistDao: WhitelistDao,
    private val blacklistDao: BlacklistDao,
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
        }
    }

    fun getBlockNonContacts(): Boolean = localConfigManager.blockNonContacts
    fun getAllowedPrefixes(): List<String> = localConfigManager.allowedPrefixes
    fun getBlockedPrefixes(): List<String> = localConfigManager.blockedPrefixes

    suspend fun getPreferences(): LocalPreferencesEntity? = preferencesDao.get()

    fun observePreferences(): Flow<LocalPreferencesEntity?> = preferencesDao.observe()

    suspend fun updatePreferences(
        strictMode: Boolean? = null,
        blockUnknown: Boolean? = null,
        spamThreshold: Float? = null,
        syncEnabled: Boolean? = null
    ) {
        val current = preferencesDao.get() ?: LocalPreferencesEntity()
        preferencesDao.upsert(
            current.copy(
                strictMode = strictMode ?: current.strictMode,
                blockUnknown = blockUnknown ?: current.blockUnknown,
                spamThreshold = spamThreshold ?: current.spamThreshold,
                syncEnabled = syncEnabled ?: current.syncEnabled,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    fun observeWhitelist(): Flow<List<WhitelistEntity>> = whitelistDao.observeAll()

    suspend fun isInWhitelist(phoneHash: String): Boolean = whitelistDao.exists(phoneHash) > 0

    suspend fun addToWhitelist(phoneHash: String, label: String) {
        whitelistDao.insert(
            WhitelistEntity(
                phoneHash = phoneHash,
                label = label,
                addedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeFromWhitelist(phoneHash: String) {
        whitelistDao.deleteByHash(phoneHash)
    }

    fun observeBlacklist(): Flow<List<BlacklistEntity>> = blacklistDao.observeAll()

    suspend fun isInBlacklist(phoneHash: String): Boolean = blacklistDao.exists(phoneHash) > 0

    suspend fun addToBlacklist(phoneHash: String, reason: String) {
        blacklistDao.insert(
            BlacklistEntity(
                phoneHash = phoneHash,
                reason = reason,
                addedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun removeFromBlacklist(phoneHash: String) {
        blacklistDao.deleteByHash(phoneHash)
    }
}
