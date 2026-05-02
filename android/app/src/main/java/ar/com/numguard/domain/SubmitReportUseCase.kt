package ar.com.numguard.domain

import ar.com.numguard.data.api.NumGuardApi
import ar.com.numguard.data.api.ReportRequest
import ar.com.numguard.data.local.PendingReportDao
import ar.com.numguard.data.local.PendingReportEntity
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubmitReportUseCase @Inject constructor(
    private val api: NumGuardApi,
    private val pendingReportDao: PendingReportDao,
    private val deviceIdProvider: DeviceIdProvider
) {
    suspend fun invoke(
        numberHash: String,
        numberMasked: String,
        reportType: String,
        description: String?,
        callEventId: String
    ): Boolean {
        return try {
            api.report(
                ReportRequest(
                    numberHash = numberHash,
                    reportType = reportType,
                    description = description,
                    deviceId = deviceIdProvider.getDeviceIdHash()
                )
            )
            true
        } catch (_: Exception) {
            val pending = PendingReportEntity(
                id = UUID.randomUUID().toString(),
                numberHash = numberHash,
                numberMasked = numberMasked,
                reportType = reportType,
                description = description,
                isFeedback = false,
                feedbackType = null,
                callEventId = callEventId,
                createdAtMillis = System.currentTimeMillis(),
                retryCount = 0
            )
            pendingReportDao.insert(pending)
            false
        }
    }
}
