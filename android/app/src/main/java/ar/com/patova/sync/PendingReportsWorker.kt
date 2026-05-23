package ar.com.patova.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ar.com.patova.data.api.FeedbackRequest
import ar.com.patova.data.api.PatovaApi
import ar.com.patova.data.api.ReportRequest
import ar.com.patova.data.local.CallEventDao
import ar.com.patova.data.local.PendingReportDao
import ar.com.patova.domain.DeviceIdProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class PendingReportsWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val api: PatovaApi,
    private val pendingReportDao: PendingReportDao,
    private val callEventDao: CallEventDao,
    private val deviceIdProvider: DeviceIdProvider
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        pendingReportDao.deleteExcessiveRetries()

        while (true) {
            val pending = pendingReportDao.getFirst() ?: break

            val success = try {
                if (pending.isFeedback) {
                    api.feedback(
                        FeedbackRequest(
                            numberHash = pending.numberHash,
                            originalVerdict = "",
                            feedbackType = pending.feedbackType ?: "",
                            deviceId = deviceIdProvider.getDeviceIdHash()
                        )
                    )
                    callEventDao.markFeedbackSynced(pending.callEventId)
                } else {
                    api.report(
                        ReportRequest(
                            numberHash = pending.numberHash,
                            reportType = pending.reportType,
                            description = pending.description,
                            deviceId = deviceIdProvider.getDeviceIdHash()
                        )
                    )
                }
                true
            } catch (_: Exception) {
                false
            }

            if (success) {
                pendingReportDao.deleteById(pending.id)
            } else {
                pendingReportDao.incrementRetry(pending.id)
                return Result.retry()
            }
        }

        return Result.success()
    }
}
