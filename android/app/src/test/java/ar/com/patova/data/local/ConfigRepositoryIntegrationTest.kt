package ar.com.patova.data.local

import ar.com.patova.data.local.daos.BlacklistDao
import ar.com.patova.data.local.daos.LocalPreferencesDao
import ar.com.patova.data.local.daos.WhitelistDao
import ar.com.patova.data.local.entities.BlacklistEntity
import ar.com.patova.data.local.entities.LocalPreferencesEntity
import ar.com.patova.data.local.entities.WhitelistEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DaoIntegrationTest {

    @Test
    fun `whitelist insert and exists works together`() = runTest {
        val dao = FakeWhitelistDaoForTest()
        dao.insert(WhitelistEntity("hashA", "Familia", 1000L))

        assertTrue(dao.exists("hashA") > 0)
        assertEquals("Familia", dao.getByHash("hashA")?.label)
    }

    @Test
    fun `blacklist insert and delete works together`() = runTest {
        val dao = FakeBlacklistDaoForTest()
        dao.insert(BlacklistEntity("hashB", "Spam", 2000L))
        assertTrue(dao.exists("hashB") > 0)

        dao.deleteByHash("hashB")
        assertTrue(dao.exists("hashB") == 0)
    }

    @Test
    fun `preferences partial update preserves unchanged values`() = runTest {
        val dao = FakePreferencesDaoForTest()

        val initial = LocalPreferencesEntity(strictMode = true, blockUnknown = false, spamThreshold = 0.5f, updatedAt = 1L)
        dao.upsert(initial)

        val updated = initial.copy(blockUnknown = true, updatedAt = 2L)
        dao.upsert(updated)

        val result = dao.get()
        assertNotNull(result)
        assertTrue(result!!.strictMode)
        assertTrue(result.blockUnknown)
        assertEquals(0.5f, result.spamThreshold)
        assertEquals(2L, result.updatedAt)
    }

    @Test
    fun `whitelist observe emits sorted by addedAt desc`() = runTest {
        val dao = FakeWhitelistDaoForTest()
        dao.insert(WhitelistEntity("older", "Old", 1000L))
        dao.insert(WhitelistEntity("newer", "New", 5000L))
        dao.insert(WhitelistEntity("middle", "Mid", 3000L))

        val list = dao.observeAll().first()
        assertEquals(3, list.size)
        assertEquals("newer", list[0].phoneHash)
        assertEquals("middle", list[1].phoneHash)
        assertEquals("older", list[2].phoneHash)
    }

    @Test
    fun `blacklist entries are independent from whitelist`() = runTest {
        val whitelistDao = FakeWhitelistDaoForTest()
        val blacklistDao = FakeBlacklistDaoForTest()

        whitelistDao.insert(WhitelistEntity("shared", "Trusted", 1L))
        blacklistDao.insert(BlacklistEntity("shared", "Blocked", 2L))

        assertTrue(whitelistDao.exists("shared") > 0)
        assertTrue(blacklistDao.exists("shared") > 0)
    }

    @Test
    fun `observe preferences emits initial null then updated`() = runTest {
        val dao = FakePreferencesDaoForTest()
        val collected = mutableListOf<LocalPreferencesEntity?>()

        val job = launch(StandardTestDispatcher()) {
            dao.observe().collect { collected.add(it) }
        }
        advanceUntilIdle()

        assertEquals(1, collected.size)
        assertEquals(null, collected[0])

        dao.upsert(LocalPreferencesEntity(strictMode = true, updatedAt = 100L))
        advanceUntilIdle()

        assertEquals(2, collected.size)
        assertTrue(collected[1]!!.strictMode)

        job.cancel()
    }
}

private class FakePreferencesDaoForTest : LocalPreferencesDao {
    private var stored: LocalPreferencesEntity? = null
    private val _flow = MutableStateFlow<LocalPreferencesEntity?>(null)

    override suspend fun get() = stored
    override fun observe(): Flow<LocalPreferencesEntity?> = _flow
    override suspend fun upsert(prefs: LocalPreferencesEntity) {
        stored = prefs
        _flow.value = prefs
    }
}

private class FakeWhitelistDaoForTest : WhitelistDao {
    private val store = mutableMapOf<String, WhitelistEntity>()
    private val _flow = MutableStateFlow<List<WhitelistEntity>>(emptyList())

    override fun observeAll(): Flow<List<WhitelistEntity>> = _flow
    override suspend fun getByHash(phoneHash: String) = store[phoneHash]
    override suspend fun exists(phoneHash: String) = if (store.containsKey(phoneHash)) 1 else 0
    override suspend fun insert(entity: WhitelistEntity) {
        store[entity.phoneHash] = entity
        _flow.value = store.values.sortedByDescending { it.addedAt }
    }
    override suspend fun deleteByHash(phoneHash: String) {
        store.remove(phoneHash)
        _flow.value = store.values.sortedByDescending { it.addedAt }
    }
}

private class FakeBlacklistDaoForTest : BlacklistDao {
    private val store = mutableMapOf<String, BlacklistEntity>()
    private val _flow = MutableStateFlow<List<BlacklistEntity>>(emptyList())

    override fun observeAll(): Flow<List<BlacklistEntity>> = _flow
    override suspend fun getByHash(phoneHash: String) = store[phoneHash]
    override suspend fun exists(phoneHash: String) = if (store.containsKey(phoneHash)) 1 else 0
    override suspend fun insert(entity: BlacklistEntity) {
        store[entity.phoneHash] = entity
        _flow.value = store.values.sortedByDescending { it.addedAt }
    }
    override suspend fun deleteByHash(phoneHash: String) {
        store.remove(phoneHash)
        _flow.value = store.values.sortedByDescending { it.addedAt }
    }
}
