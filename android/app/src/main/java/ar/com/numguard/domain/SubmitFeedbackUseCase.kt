package ar.com.numguard.domain

import ar.com.numguard.data.api.FeedbackRequest
import ar.com.numguard.data.api.NumGuardApi
import ar.com.numguard.data.local.CallEventDao
import ar.com.numguard.data.local.PendingReportDao
import ar.com.numguard.data.local.PendingReportEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubmitFeedbackUseCase @Inject constructor(
    private val api: NumGuardApi,
    private val callEventDao: CallEventDao,
    private val pendingReportDao: PendingReportDao,
    private val deviceIdProvider: DeviceIdProvider
) {
    suspend fun invoke(
        numberHash: String,
        numberMasked: String,
        originalVerdict: String,
        feedbackType: String,
        callEventId: String
    ): Boolean {
        return try {
            api.feedback(
                FeedbackRequest(
                    numberHash = numberHash,
                    originalVerdict = originalVerdict,
                    feedbackType = feedbackType,
                    deviceId = deviceIdProvider.getDeviceIdHash()
                )
            )
            callEventDao.markFeedbackSynced(callEventId)
            true
        } catch (_: Exception) {
            val pending = PendingReportEntity(
                id = UUID.randomUUID().toString(),
                numberHash = numberHash,
                numberMasked = numberMasked,
                reportType = "",
                description = null,
                isFeedback = true,
                feedbackType = feedbackType,
                callEventId = callEventId,
                createdAtMillis = System.currentTimeMillis(),
                retryCount = 0
            )
            pendingReportDao.insert(pending)
            false
        }
    }
}
