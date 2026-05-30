package ar.com.patova.sync

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ar.com.patova.data.api.PatovaApi
import ar.com.patova.data.api.SyncRequest
import ar.com.patova.data.local.daos.BlacklistDao
import ar.com.patova.data.local.daos.WhitelistDao
import ar.com.patova.data.local.entities.BlacklistEntity
import ar.com.patova.data.local.entities.WhitelistEntity
import ar.com.patova.data.local.EncryptedPreferencesManager
import ar.com.patova.domain.DeviceIdProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@HiltWorker
class BehaviorSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val api: PatovaApi,
    private val blacklistDao: BlacklistDao,
    private val whitelistDao: WhitelistDao,
    private val deviceIdProvider: DeviceIdProvider,
    private val prefsManager: EncryptedPreferencesManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.i("Patova", "Iniciando BehaviorSyncWorker...")

        val userId = prefsManager.getString(EncryptedPreferencesManager.KEY_USER_ID) ?: run {
            val deviceId = deviceIdProvider.getDeviceIdHash()
            "usr_${deviceId.takeLast(12)}"
        }

        val lastSync = prefsManager.getString("last_behavior_sync_timestamp")

        try {
            val response = api.sync(
                SyncRequest(
                    userId = userId,
                    lastSyncTimestamp = lastSync,
                    localChanges = null // Por ahora solo bajamos el delta global
                )
            )

            // Procesar Blacklist Delta
            response.blacklistDelta.forEach { entry ->
                blacklistDao.insert(
                    BlacklistEntity(
                        phoneHash = entry.phoneHash,
                        reason = entry.reason ?: "GLOBAL_SPAM",
                        addedAt = parseTimestamp(entry.addedAt)
                    )
                )
            }

            // Procesar Whitelist Delta
            response.whitelistDelta.forEach { entry ->
                whitelistDao.insert(
                    WhitelistEntity(
                        phoneHash = entry.phoneHash,
                        label = entry.label ?: "VERIFIED",
                        addedAt = parseTimestamp(entry.addedAt)
                    )
                )
            }

            prefsManager.putString("last_behavior_sync_timestamp", response.syncTimestamp)
            Log.i("Patova", "Sincronización de comportamiento exitosa: ${response.syncTimestamp}")
            
            return Result.success()
        } catch (e: Exception) {
            Log.e("Patova", "Error en BehaviorSyncWorker", e)
            return Result.retry()
        }
    }

    private fun parseTimestamp(iso: String): Long {
        return try {
            val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            fmt.timeZone = TimeZone.getTimeZone("UTC")
            fmt.parse(iso.substringBefore("+").substringBefore("Z"))?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
}
