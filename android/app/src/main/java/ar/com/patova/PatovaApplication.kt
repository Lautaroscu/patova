package ar.com.patova

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import ar.com.patova.notifications.DailyDigestWorker
import ar.com.patova.sync.BehaviorSyncWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class PatovaApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        scheduleSyncWorkers()
    }

    private fun scheduleSyncWorkers() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<BehaviorSyncWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "behavior_sync",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )

        val digestRequest = PeriodicWorkRequestBuilder<DailyDigestWorker>(24, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "daily_digest",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            digestRequest
        )
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
