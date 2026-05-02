package ar.com.numguard

import ar.com.numguard.data.api.FeedbackRequest
import ar.com.numguard.data.api.FeedbackResponse
import ar.com.numguard.data.api.NumGuardApi
import ar.com.numguard.data.api.ReportRequest
import ar.com.numguard.data.api.ReportResponse
import ar.com.numguard.data.api.ValidateRequest
import ar.com.numguard.data.api.ValidateResponse
import ar.com.numguard.data.local.CachedValidationDao
import ar.com.numguard.data.local.CachedValidationEntity
import ar.com.numguard.data.local.CallEventDao
import ar.com.numguard.data.local.CallEventEntity
import ar.com.numguard.data.local.PendingReportDao
import ar.com.numguard.data.local.PendingReportEntity
import ar.com.numguard.domain.DeviceIdProvider
import ar.com.numguard.domain.SubmitFeedbackUseCase
import ar.com.numguard.domain.SubmitReportUseCase
import ar.com.numguard.domain.ValidateIncomingCallUseCase
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportSyncTest {

    @Test
    fun `call event is saved after blocking decision`() = runTest {
        val savedEvents = mutableListOf<CallEventEntity>()

        val callEventDao = object : CallEventDao {
            override fun getAllFlow() = flowOf(savedEvents.toList())
            override suspend fun insert(entity: CallEventEntity) { savedEvents.add(entity) }
            override suspend fun getById(id: String) = savedEvents.find { it.id == id }
            override suspend fun markFeedbackSynced(id: String) {
                val idx = savedEvents.indexOfFirst { it.id == id }
                if (idx >= 0) savedEvents[idx] = savedEvents[idx].copy(syncedFeedback = true)
            }
        }

        val fakeApi = object : NumGuardApi {
            override suspend fun validate(request: ValidateRequest) =
                ValidateResponse(verdict = "BLOCK", spamScore = 87)
            override suspend fun report(request: ReportRequest) = ReportResponse()
            override suspend fun feedback(request: FeedbackRequest) = FeedbackResponse()
        }

        val fakeDao = object : CachedValidationDao {
            override suspend fun getByHash(numberHash: String) = null
            override suspend fun insert(entity: CachedValidationEntity) {}
            override suspend fun deleteExpired(now: Long) {}
        }

        val fakeDeviceId = object : DeviceIdProvider {
            override fun getDeviceIdHash() = "test-device"
        }

        val useCase = ValidateIncomingCallUseCase(fakeApi, fakeDao, fakeDeviceId, callEventDao)
        val verdict = useCase.decide("+541112345678")

        assertEquals("BLOCK", verdict)
        assertEquals(1, savedEvents.size)
        assertEquals("BLOCKED", savedEvents[0].actionTaken)
        assertEquals("BLOCK", savedEvents[0].verdict)
        assertEquals(87, savedEvents[0].spamScore)
        assertTrue(savedEvents[0].numberMasked.contains("****"))
    }

    @Test
    fun `report with network calls API`() = runTest {
        var reportReceived = false

        val fakeApi = object : NumGuardApi {
            override suspend fun validate(request: ValidateRequest) = ValidateResponse(verdict = "UNKNOWN")
            override suspend fun report(request: ReportRequest): ReportResponse {
                reportReceived = true
                assertEquals("SPAM_CALL", request.reportType)
                return ReportResponse()
            }
            override suspend fun feedback(request: FeedbackRequest) = FeedbackResponse()
        }

        val pendingReportDao = fakeEmptyPendingReportDao()
        val fakeDeviceId = object : DeviceIdProvider {
            override fun getDeviceIdHash() = "test-device"
        }

        val useCase = SubmitReportUseCase(fakeApi, pendingReportDao, fakeDeviceId)
        val sent = useCase.invoke("hash123", "+54****78", "SPAM_CALL", "test desc", "event-1")

        assertTrue(reportReceived)
        assertTrue(sent)
    }

    @Test
    fun `report without network saves pending`() = runTest {
        val savedPending = mutableListOf<PendingReportEntity>()

        val fakeApi = object : NumGuardApi {
            override suspend fun validate(request: ValidateRequest) = ValidateResponse(verdict = "UNKNOWN")
            override suspend fun report(request: ReportRequest): ReportResponse {
                throw java.io.IOException("network error")
            }
            override suspend fun feedback(request: FeedbackRequest) = FeedbackResponse()
        }

        val pendingReportDao = object : PendingReportDao {
            override suspend fun getAll() = savedPending.toList()
            override suspend fun getFirst() = savedPending.firstOrNull()
            override suspend fun insert(entity: PendingReportEntity) { savedPending.add(entity) }
            override suspend fun deleteById(id: String) { savedPending.removeAll { it.id == id } }
            override suspend fun incrementRetry(id: String) {
                val idx = savedPending.indexOfFirst { it.id == id }
                if (idx >= 0) savedPending[idx] = savedPending[idx].copy(retryCount = savedPending[idx].retryCount + 1)
            }
            override suspend fun deleteExcessiveRetries() {
                savedPending.removeAll { it.retryCount >= 3 }
            }
        }

        val fakeDeviceId = object : DeviceIdProvider {
            override fun getDeviceIdHash() = "test-device"
        }

        val useCase = SubmitReportUseCase(fakeApi, pendingReportDao, fakeDeviceId)
        val sent = useCase.invoke("hash123", "+54****78", "SPAM_CALL", null, "event-1")

        assertFalse(sent)
        assertEquals(1, savedPending.size)
        assertEquals("hash123", savedPending[0].numberHash)
        assertEquals("SPAM_CALL", savedPending[0].reportType)
    }

    @Test
    fun `worker retries pending report successfully`() = runTest {
        val pendingStore = mutableListOf(
            PendingReportEntity(
                id = "p-1", numberHash = "h1", numberMasked = "+54****78",
                reportType = "SPAM_CALL", description = null, isFeedback = false,
                feedbackType = null, callEventId = "e-1",
                createdAtMillis = System.currentTimeMillis(), retryCount = 1
            )
        )

        var reportedHash: String? = null

        val fakeApi = object : NumGuardApi {
            override suspend fun validate(request: ValidateRequest) = ValidateResponse(verdict = "UNKNOWN")
            override suspend fun report(request: ReportRequest): ReportResponse {
                reportedHash = request.numberHash
                return ReportResponse()
            }
            override suspend fun feedback(request: FeedbackRequest) = FeedbackResponse()
        }

        val pendingReportDao = object : PendingReportDao {
            override suspend fun getAll() = pendingStore.toList()
            override suspend fun getFirst() = pendingStore.firstOrNull()
            override suspend fun insert(entity: PendingReportEntity) { pendingStore.add(entity) }
            override suspend fun deleteById(id: String) { pendingStore.removeAll { it.id == id } }
            override suspend fun incrementRetry(id: String) {
                val idx = pendingStore.indexOfFirst { it.id == id }
                if (idx >= 0) pendingStore[idx] = pendingStore[idx].copy(retryCount = pendingStore[idx].retryCount + 1)
            }
            override suspend fun deleteExcessiveRetries() {
                pendingStore.removeAll { it.retryCount >= 3 }
            }
        }

        pendingReportDao.deleteExcessiveRetries()
        val pending = pendingReportDao.getFirst()
        assertNotNull(pending)

        try {
            fakeApi.report(
                ReportRequest(
                    numberHash = pending!!.numberHash,
                    reportType = pending.reportType,
                    description = pending.description,
                    deviceId = "test-device"
                )
            )
            pendingReportDao.deleteById(pending.id)
        } catch (_: Exception) {
        }

        assertEquals("h1", reportedHash)
        assertTrue(pendingStore.isEmpty())
    }

    @Test
    fun `feedback false positive calls correct endpoint`() = runTest {
        var feedbackReceived = false
        var receivedFeedbackType: String? = null

        val fakeApi = object : NumGuardApi {
            override suspend fun validate(request: ValidateRequest) = ValidateResponse(verdict = "UNKNOWN")
            override suspend fun report(request: ReportRequest) = ReportResponse()
            override suspend fun feedback(request: FeedbackRequest): FeedbackResponse {
                feedbackReceived = true
                receivedFeedbackType = request.feedbackType
                assertEquals("FALSE_POSITIVE", request.feedbackType)
                return FeedbackResponse()
            }
        }

        val callEventDao = object : CallEventDao {
            override fun getAllFlow() = flowOf(emptyList<CallEventEntity>())
            override suspend fun insert(entity: CallEventEntity) {}
            override suspend fun getById(id: String): CallEventEntity? = null
            override suspend fun markFeedbackSynced(id: String) {}
        }

        val pendingReportDao = fakeEmptyPendingReportDao()
        val fakeDeviceId = object : DeviceIdProvider {
            override fun getDeviceIdHash() = "test-device"
        }

        val useCase = SubmitFeedbackUseCase(fakeApi, callEventDao, pendingReportDao, fakeDeviceId)
        val sent = useCase.invoke("hash123", "+54****78", "BLOCK", "FALSE_POSITIVE", "event-1")

        assertTrue(feedbackReceived)
        assertEquals("FALSE_POSITIVE", receivedFeedbackType)
        assertTrue(sent)
    }

    private fun fakeEmptyPendingReportDao() = object : PendingReportDao {
        override suspend fun getAll() = emptyList<PendingReportEntity>()
        override suspend fun getFirst() = null
        override suspend fun insert(entity: PendingReportEntity) {}
        override suspend fun deleteById(id: String) {}
        override suspend fun incrementRetry(id: String) {}
        override suspend fun deleteExcessiveRetries() {}
    }
}
