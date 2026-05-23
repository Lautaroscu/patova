package ar.com.patova.domain

import ar.com.patova.data.api.FeedbackRequest
import ar.com.patova.data.api.PatovaApi
import ar.com.patova.data.local.CallEventDao
import ar.com.patova.data.local.PendingReportDao
import ar.com.patova.data.local.PendingReportEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubmitFeedbackUseCase @Inject constructor(
    private val api: PatovaApi,
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
