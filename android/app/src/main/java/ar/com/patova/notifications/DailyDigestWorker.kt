package ar.com.patova.notifications

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ar.com.patova.data.local.CallEventDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DailyDigestWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val callEventDao: CallEventDao,
    private val notificationManager: PatovaNotificationManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.i("Patova", "Iniciando DailyDigestWorker para resumen de llamadas bloqueadas...")
        try {
            val since24h = System.currentTimeMillis() - 24 * 60 * 60 * 1000
            val recentEvents = callEventDao.getEventsSince(since24h)

            val blockedToday = recentEvents.count {
                it.actionTaken == "BLOCKED" || it.verdict == "BLOCK" || it.verdict == "INVALID_PREFIX"
            }

            if (blockedToday > 0) {
                Log.i("Patova", "DailyDigestWorker: Se encontraron $blockedToday llamadas bloqueadas hoy. Disparando resumen.")
                notificationManager.showDailyDigestNotification(blockedToday)
            } else {
                Log.i("Patova", "DailyDigestWorker: No hubo llamadas bloqueadas hoy. Silencio absoluto.")
            }
            return Result.success()
        } catch (e: Exception) {
            Log.e("Patova", "Error en DailyDigestWorker", e)
            return Result.failure()
        }
    }
}
