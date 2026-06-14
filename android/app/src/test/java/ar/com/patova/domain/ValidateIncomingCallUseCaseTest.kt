package ar.com.patova.domain

import ar.com.patova.data.api.PatovaApi
import ar.com.patova.data.api.ValidateRequest
import ar.com.patova.data.api.ValidateResponse
import ar.com.patova.data.api.ReportRequest
import ar.com.patova.data.api.ReportResponse
import ar.com.patova.data.api.FeedbackRequest
import ar.com.patova.data.api.FeedbackResponse
import ar.com.patova.data.local.CachedValidationDao
import ar.com.patova.data.local.CachedValidationEntity
import ar.com.patova.data.local.CallEventDao
import ar.com.patova.data.local.CallEventEntity
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

open class FakePatovaApi : PatovaApi {
    override suspend fun validate(request: ValidateRequest) = ValidateResponse(verdict = "ALLOW")
    override suspend fun report(request: ReportRequest) = ReportResponse()
    override suspend fun feedback(request: FeedbackRequest) = FeedbackResponse()
    override suspend fun getConfig(deviceId: String) = ar.com.patova.data.api.DeviceConfigResponse(deviceId, false, emptyList(), emptyList())
    override suspend fun updateConfig(deviceId: String, request: ar.com.patova.data.api.DeviceConfigRequest) = ar.com.patova.data.api.DeviceConfigResponse(deviceId, false, emptyList(), emptyList())
    override suspend fun createPreference(request: ar.com.patova.data.api.CreatePreferenceRequest) = ar.com.patova.data.api.CreatePreferenceResponse("", "", "")
    override suspend fun getSubscriptionMe(userId: String) = ar.com.patova.data.api.SubscriptionMeResponse(userId, false, null)
    override suspend fun sync(request: ar.com.patova.data.api.SyncRequest) = ar.com.patova.data.api.SyncResponse("", "", emptyList(), emptyList())
    override suspend fun getStats() = ar.com.patova.data.api.StatsResponse(0, 0, 0, emptyList())
}

open class FakeCallEventDao : CallEventDao {
    override fun getAllFlow() = flowOf(emptyList<CallEventEntity>())
    override suspend fun insert(entity: CallEventEntity) {}
    override suspend fun getById(id: String): CallEventEntity? = null
    override suspend fun markFeedbackSynced(id: String) {}
    override suspend fun getRecentCallsByHash(phoneHash: String, sinceMillis: Long): List<CallEventEntity> = emptyList()
    override suspend fun getEventsSince(sinceMillis: Long): List<CallEventEntity> = emptyList()
}

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

        val fakeApi = object : FakePatovaApi() {
            override suspend fun validate(request: ValidateRequest): ValidateResponse {
                apiCallCount++
                return ValidateResponse(verdict = "BLOCK")
            }
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

        val useCase = ValidateIncomingCallUseCase(fakeApi, fakeDao, fakeDeviceId, FakeCallEventDao())
        val verdict = useCase.decide("+541112345678")

        assertEquals(0, apiCallCount)
        assertEquals("SUSPECT", verdict)
    }

    @Test
    fun `expired cache calls API`() = runTest {
        var apiCallCount = 0

        val fakeApi = object : FakePatovaApi() {
            override suspend fun validate(request: ValidateRequest): ValidateResponse {
                apiCallCount++
                return ValidateResponse(verdict = "ALLOW")
            }
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

        val useCase = ValidateIncomingCallUseCase(fakeApi, fakeDao, fakeDeviceId, FakeCallEventDao())
        val verdict = useCase.decide("+541112345678")

        assertEquals(1, apiCallCount)
        assertEquals("ALLOW", verdict)
    }

    @Test
    fun `API BLOCK verdict produces block decision`() = runTest {
        val fakeApi = object : FakePatovaApi() {
            override suspend fun validate(request: ValidateRequest): ValidateResponse =
                ValidateResponse(verdict = "BLOCK")
        }

        val useCase = ValidateIncomingCallUseCase(fakeApi, fakeEmptyDao(), fakeDeviceId(), FakeCallEventDao())
        val verdict = useCase.decide("+541112345678")

        assertEquals("BLOCK", verdict)
    }

    @Test
    fun `API INVALID_PREFIX verdict produces block decision`() = runTest {
        val fakeApi = object : FakePatovaApi() {
            override suspend fun validate(request: ValidateRequest): ValidateResponse =
                ValidateResponse(verdict = "INVALID_PREFIX")
        }

        val useCase = ValidateIncomingCallUseCase(
            fakeApi, fakeEmptyDao(), fakeDeviceId(), FakeCallEventDao()
        )
        val verdict = useCase.decide("+5492611234567")

        assertEquals("INVALID_PREFIX", verdict)
    }

    @Test
    fun `API error produces allow decision`() = runTest {
        val fakeApi = object : FakePatovaApi() {
            override suspend fun validate(request: ValidateRequest): ValidateResponse {
                throw java.io.IOException("network failure")
            }
        }

        val useCase = ValidateIncomingCallUseCase(
            fakeApi, fakeEmptyDao(), fakeDeviceId(), FakeCallEventDao()
        )
        val verdict = useCase.decide("+541112345678")

        assertEquals("ALLOW", verdict)
    }

    @Test
    fun `API timeout produces allow decision`() = runTest {
        val fakeApi = object : FakePatovaApi() {
            override suspend fun validate(request: ValidateRequest): ValidateResponse {
                kotlinx.coroutines.delay(5000L)
                return ValidateResponse(verdict = "BLOCK")
            }
        }

        val useCase = ValidateIncomingCallUseCase(
            fakeApi, fakeEmptyDao(), fakeDeviceId(), FakeCallEventDao()
        )
        val verdict = useCase.decide("+541112345678")

        assertEquals("ALLOW", verdict)
    }

    @Test
    fun `API SUSPECT produces SUSPECT verdict`() = runTest {
        val fakeApi = object : FakePatovaApi() {
            override suspend fun validate(request: ValidateRequest): ValidateResponse =
                ValidateResponse(verdict = "SUSPECT")
        }

        val useCase = ValidateIncomingCallUseCase(
            fakeApi, fakeEmptyDao(), fakeDeviceId(), FakeCallEventDao()
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
}
