package ar.com.numguard.domain

import ar.com.numguard.data.api.NumGuardApi
import ar.com.numguard.data.api.ValidateRequest
import ar.com.numguard.data.api.ValidateResponse
import ar.com.numguard.data.api.ReportRequest
import ar.com.numguard.data.api.ReportResponse
import ar.com.numguard.data.api.FeedbackRequest
import ar.com.numguard.data.api.FeedbackResponse
import ar.com.numguard.data.local.CachedValidationDao
import ar.com.numguard.data.local.CachedValidationEntity
import ar.com.numguard.data.local.CallEventDao
import ar.com.numguard.data.local.CallEventEntity
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ValidateIncomingCallUseCaseTest {

    private val testHash = PhoneHashing.sha256("+541112345678")

    @Test
    fun `hash is not equal to raw number`() {
        val raw = "+541112345678"
        val hash = PhoneHashing.sha256(raw)
        assertNotEquals(raw, hash)
        assertEquals(64, hash.length)
    }

    @Test
    fun `cache hit returns cached verdict without calling API`() = runTest {
        var apiCallCount = 0

        val fakeApi = object : NumGuardApi {
            override suspend fun validate(request: ValidateRequest): ValidateResponse {
                apiCallCount++
                return ValidateResponse(verdict = "BLOCK")
            }
            override suspend fun report(request: ReportRequest) = ReportResponse()
            override suspend fun feedback(request: FeedbackRequest) = FeedbackResponse()
        }

        val fakeDao = object : CachedValidationDao {
            private val store = mutableMapOf<String, CachedValidationEntity>()

            override suspend fun getByHash(numberHash: String) = store[numberHash]
            override suspend fun insert(entity: CachedValidationEntity) {
                store[entity.numberHash] = entity
            }
            override suspend fun deleteExpired(now: Long) {}
        }

        val now = System.currentTimeMillis()
        fakeDao.insert(
            CachedValidationEntity(
                numberHash = testHash,
                verdict = "SUSPECT",
                cachedAtMillis = now,
                expiresAtMillis = now + 30 * 60 * 1000L
            )
        )

        val fakeDeviceId = object : DeviceIdProvider {
            override fun getDeviceIdHash(): String = "test-device-hash"
        }

        val useCase = ValidateIncomingCallUseCase(fakeApi, fakeDao, fakeDeviceId, fakeEmptyCallEventDao())
        val verdict = useCase.decide("+541112345678")

        assertEquals(0, apiCallCount)
        assertEquals("SUSPECT", verdict)
    }

    @Test
    fun `expired cache calls API`() = runTest {
        var apiCallCount = 0

        val fakeApi = object : NumGuardApi {
            override suspend fun validate(request: ValidateRequest): ValidateResponse {
                apiCallCount++
                return ValidateResponse(verdict = "ALLOW")
            }
            override suspend fun report(request: ReportRequest) = ReportResponse()
            override suspend fun feedback(request: FeedbackRequest) = FeedbackResponse()
        }

        val fakeDao = object : CachedValidationDao {
            private val store = mutableMapOf<String, CachedValidationEntity>()

            override suspend fun getByHash(numberHash: String) = store[numberHash]
            override suspend fun insert(entity: CachedValidationEntity) {
                store[entity.numberHash] = entity
            }
            override suspend fun deleteExpired(now: Long) {}
        }

        val now = System.currentTimeMillis()
        fakeDao.insert(
            CachedValidationEntity(
                numberHash = testHash,
                verdict = "BLOCK",
                cachedAtMillis = now - 25 * 60 * 60 * 1000L,
                expiresAtMillis = now - 1
            )
        )

        val fakeDeviceId = object : DeviceIdProvider {
            override fun getDeviceIdHash(): String = "test-device-hash"
        }

        val useCase = ValidateIncomingCallUseCase(fakeApi, fakeDao, fakeDeviceId, fakeEmptyCallEventDao())
        val verdict = useCase.decide("+541112345678")

        assertEquals(1, apiCallCount)
        assertEquals("ALLOW", verdict)
    }

    @Test
    fun `API BLOCK verdict produces block decision`() = runTest {
        val fakeApi = object : NumGuardApi {
            override suspend fun validate(request: ValidateRequest): ValidateResponse =
                ValidateResponse(verdict = "BLOCK")
            override suspend fun report(request: ReportRequest) = ReportResponse()
            override suspend fun feedback(request: FeedbackRequest) = FeedbackResponse()
        }

        val useCase = ValidateIncomingCallUseCase(fakeApi, fakeEmptyDao(), fakeDeviceId(), fakeEmptyCallEventDao())
        val verdict = useCase.decide("+541112345678")

        assertEquals("BLOCK", verdict)
    }

    @Test
    fun `API INVALID_PREFIX verdict produces block decision`() = runTest {
        val fakeApi = object : NumGuardApi {
            override suspend fun validate(request: ValidateRequest): ValidateResponse =
                ValidateResponse(verdict = "INVALID_PREFIX")
            override suspend fun report(request: ReportRequest) = ReportResponse()
            override suspend fun feedback(request: FeedbackRequest) = FeedbackResponse()
        }

        val useCase = ValidateIncomingCallUseCase(
            fakeApi, fakeEmptyDao(), fakeDeviceId(), fakeEmptyCallEventDao()
        )
        val verdict = useCase.decide("+5492611234567")

        assertEquals("INVALID_PREFIX", verdict)
    }

    @Test
    fun `API error produces allow decision`() = runTest {
        val fakeApi = object : NumGuardApi {
            override suspend fun validate(request: ValidateRequest): ValidateResponse {
                throw java.io.IOException("network failure")
            }
            override suspend fun report(request: ReportRequest) = ReportResponse()
            override suspend fun feedback(request: FeedbackRequest) = FeedbackResponse()
        }

        val useCase = ValidateIncomingCallUseCase(
            fakeApi, fakeEmptyDao(), fakeDeviceId(), fakeEmptyCallEventDao()
        )
        val verdict = useCase.decide("+541112345678")

        assertEquals("ALLOW", verdict)
    }

    @Test
    fun `API timeout produces allow decision`() = runTest {
        val fakeApi = object : NumGuardApi {
            override suspend fun validate(request: ValidateRequest): ValidateResponse {
                kotlinx.coroutines.delay(5000L)
                return ValidateResponse(verdict = "BLOCK")
            }
            override suspend fun report(request: ReportRequest) = ReportResponse()
            override suspend fun feedback(request: FeedbackRequest) = FeedbackResponse()
        }

        val useCase = ValidateIncomingCallUseCase(
            fakeApi, fakeEmptyDao(), fakeDeviceId(), fakeEmptyCallEventDao()
        )
        val verdict = useCase.decide("+541112345678")

        assertEquals("ALLOW", verdict)
    }

    @Test
    fun `API SUSPECT produces SUSPECT verdict`() = runTest {
        val fakeApi = object : NumGuardApi {
            override suspend fun validate(request: ValidateRequest): ValidateResponse =
                ValidateResponse(verdict = "SUSPECT")
            override suspend fun report(request: ReportRequest) = ReportResponse()
            override suspend fun feedback(request: FeedbackRequest) = FeedbackResponse()
        }

        val useCase = ValidateIncomingCallUseCase(
            fakeApi, fakeEmptyDao(), fakeDeviceId(), fakeEmptyCallEventDao()
        )
        val verdict = useCase.decide("+541112345678")

        assertEquals("SUSPECT", verdict)
    }

    private fun fakeEmptyDao() = object : CachedValidationDao {
        override suspend fun getByHash(numberHash: String) = null
        override suspend fun insert(entity: CachedValidationEntity) {}
        override suspend fun deleteExpired(now: Long) {}
    }

    private fun fakeDeviceId() = object : DeviceIdProvider {
        override fun getDeviceIdHash(): String = "test-device-hash"
    }

    private fun fakeEmptyCallEventDao() = object : CallEventDao {
        override fun getAllFlow() = flowOf(emptyList<CallEventEntity>())
        override suspend fun insert(entity: CallEventEntity) {}
        override suspend fun getById(id: String): CallEventEntity? = null
        override suspend fun markFeedbackSynced(id: String) {}
    }
}
