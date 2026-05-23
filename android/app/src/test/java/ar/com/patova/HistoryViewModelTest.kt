package ar.com.patova

import ar.com.patova.data.local.CallEventDao
import ar.com.patova.data.local.CallEventEntity
import ar.com.patova.data.local.PendingReportDao
import ar.com.patova.data.local.PendingReportEntity
import ar.com.patova.domain.DeviceIdProvider
import ar.com.patova.domain.SubmitFeedbackUseCase
import ar.com.patova.domain.SubmitReportUseCase
import ar.com.patova.domain.ValidateIncomingCallUseCase
import ar.com.patova.data.api.PatovaApi
import ar.com.patova.data.api.ReportRequest
import ar.com.patova.data.api.ReportResponse
import ar.com.patova.data.api.FeedbackRequest
import ar.com.patova.data.api.FeedbackResponse
import ar.com.patova.data.api.ValidateRequest
import ar.com.patova.data.api.ValidateResponse
import ar.com.patova.data.local.CachedValidationDao
import ar.com.patova.data.local.CachedValidationEntity
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryViewModelTest {

    @Test
    fun `events are stored with masked number after block`() = runTest {
        val savedEvents = mutableListOf<CallEventEntity>()

        val callEventDao = object : CallEventDao {
            override fun getAllFlow() = flowOf(savedEvents.toList())
            override suspend fun insert(entity: CallEventEntity) { savedEvents.add(entity) }
            override suspend fun getById(id: String) = savedEvents.find { it.id == id }
            override suspend fun markFeedbackSynced(id: String) {}
        }

        val fakeApi = object : PatovaApi {
            override suspend fun validate(request: ValidateRequest) =
                ValidateResponse(verdict = "BLOCK", spamScore = 92, reason = "reported spam")
            override suspend fun report(request: ReportRequest) = ReportResponse()
            override suspend fun feedback(request: FeedbackRequest) = FeedbackResponse()
        }

        val fakeDao = object : CachedValidationDao {
            override suspend fun getByHash(numberHash: String) = null
            override suspend fun insert(entity: CachedValidationEntity) {}
            override suspend fun deleteExpired(now: Long) {}
        }

        val useCase = ValidateIncomingCallUseCase(
            fakeApi, fakeDao,
            object : DeviceIdProvider { override fun getDeviceIdHash() = "dev" },
            callEventDao
        )

        useCase.decide("+5492611234567")

        assertEquals(1, savedEvents.size)
        val event = savedEvents[0]
        assertEquals("BLOCK", event.verdict)
        assertEquals("BLOCKED", event.actionTaken)
        assertEquals(92, event.spamScore)
        assertEquals("reported spam", event.reason)
        assertTrue(event.numberMasked.contains("****"))
        assertTrue(!event.numberMasked.contains("5492611234567"))
    }

    @Test
    fun `feedback marks event as synced`() = runTest {
        val events = mutableListOf(
            CallEventEntity(
                id = "e-1", numberHash = "h1", numberMasked = "+54****78",
                verdict = "BLOCK", spamScore = 50, reason = null,
                occurredAtMillis = System.currentTimeMillis(),
                actionTaken = "BLOCKED", syncedFeedback = false
            )
        )

        val callEventDao = object : CallEventDao {
            override fun getAllFlow() = flowOf(events.toList())
            override suspend fun insert(entity: CallEventEntity) {}
            override suspend fun getById(id: String) = events.find { it.id == id }
            override suspend fun markFeedbackSynced(id: String) {
                val idx = events.indexOfFirst { it.id == id }
                if (idx >= 0) events[idx] = events[idx].copy(syncedFeedback = true)
            }
        }

        val fakeApi = object : PatovaApi {
            override suspend fun validate(request: ValidateRequest) = ValidateResponse(verdict = "BLOCK")
            override suspend fun report(request: ReportRequest) = ReportResponse()
            override suspend fun feedback(request: FeedbackRequest): FeedbackResponse {
                assertEquals("FALSE_POSITIVE", request.feedbackType)
                assertEquals("h1", request.numberHash)
                return FeedbackResponse()
            }
        }

        val pendingDao = object : PendingReportDao {
            override suspend fun getAll() = emptyList<PendingReportEntity>()
            override suspend fun getFirst() = null
            override suspend fun insert(entity: PendingReportEntity) {}
            override suspend fun deleteById(id: String) {}
            override suspend fun incrementRetry(id: String) {}
            override suspend fun deleteExcessiveRetries() {}
        }

        val useCase = SubmitFeedbackUseCase(
            fakeApi, callEventDao, pendingDao,
            object : DeviceIdProvider { override fun getDeviceIdHash() = "dev" }
        )

        val sent = useCase.invoke("h1", "+54****78", "BLOCK", "FALSE_POSITIVE", "e-1")

        assertTrue(sent)
        assertTrue(events[0].syncedFeedback)
    }

    @Test
    fun `failed verification event is saved as FAILED_OPEN`() = runTest {
        val savedEvents = mutableListOf<CallEventEntity>()

        val callEventDao = object : CallEventDao {
            override fun getAllFlow() = flowOf(savedEvents.toList())
            override suspend fun insert(entity: CallEventEntity) { savedEvents.add(entity) }
            override suspend fun getById(id: String) = savedEvents.find { it.id == id }
            override suspend fun markFeedbackSynced(id: String) {}
        }

        val fakeApi = object : PatovaApi {
            override suspend fun validate(request: ValidateRequest): ValidateResponse {
                throw java.io.IOException("timeout")
            }
            override suspend fun report(request: ReportRequest) = ReportResponse()
            override suspend fun feedback(request: FeedbackRequest) = FeedbackResponse()
        }

        val fakeDao = object : CachedValidationDao {
            override suspend fun getByHash(numberHash: String) = null
            override suspend fun insert(entity: CachedValidationEntity) {}
            override suspend fun deleteExpired(now: Long) {}
        }

        val useCase = ValidateIncomingCallUseCase(
            fakeApi, fakeDao,
            object : DeviceIdProvider { override fun getDeviceIdHash() = "dev" },
            callEventDao
        )

        val verdict = useCase.decide("+541112345678")

        assertEquals("ALLOW", verdict)
        assertEquals(1, savedEvents.size)
        assertEquals("FAILED_OPEN", savedEvents[0].actionTaken)
        assertEquals("UNKNOWN", savedEvents[0].verdict)
    }

    @Test
    fun `report offline saves as pending with correct type`() = runTest {
        val pendingStore = mutableListOf<PendingReportEntity>()

        val fakeApi = object : PatovaApi {
            override suspend fun validate(request: ValidateRequest) = ValidateResponse(verdict = "UNKNOWN")
            override suspend fun report(request: ReportRequest): ReportResponse {
                throw java.io.IOException("offline")
            }
            override suspend fun feedback(request: FeedbackRequest) = FeedbackResponse()
        }

        val pendingDao = object : PendingReportDao {
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

        val useCase = SubmitReportUseCase(
            fakeApi, pendingDao,
            object : DeviceIdProvider { override fun getDeviceIdHash() = "dev" }
        )

        val sent = useCase.invoke("h-xyz", "+54****12", "SCAM", "posible estafa", "ev-1")

        assertFalse(sent)
        assertEquals(1, pendingStore.size)
        assertEquals("h-xyz", pendingStore[0].numberHash)
        assertEquals("SCAM", pendingStore[0].reportType)
    }
}
