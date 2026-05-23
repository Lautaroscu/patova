package ar.com.patova.data.local

import ar.com.patova.data.local.daos.BlacklistDao
import ar.com.patova.data.local.daos.WhitelistDao
import ar.com.patova.data.local.entities.BlacklistEntity
import ar.com.patova.data.local.entities.WhitelistEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WhitelistDaoTest {

    @Test
    fun `insert and exists works correctly`() = runTest {
        val dao = FakeWhitelistDao()
        dao.insert(WhitelistEntity("hash123", "Familia", 1000L))

        assertTrue(dao.exists("hash123") > 0)
        assertTrue(dao.exists("nonexistent") == 0)
    }

    @Test
    fun `getByHash returns correct entity`() = runTest {
        val dao = FakeWhitelistDao()
        val entity = WhitelistEntity("hash456", "Trabajo", 2000L)
        dao.insert(entity)

        val retrieved = dao.getByHash("hash456")
        assert(retrieved != null)
        assertEquals("hash456", retrieved!!.phoneHash)
        assertEquals("Trabajo", retrieved.label)
        assertEquals(2000L, retrieved.addedAt)
    }

    @Test
    fun `deleteByHash removes entity`() = runTest {
        val dao = FakeWhitelistDao()
        dao.insert(WhitelistEntity("hash789", "Test", 3000L))
        assertTrue(dao.exists("hash789") > 0)

        dao.deleteByHash("hash789")
        assertTrue(dao.exists("hash789") == 0)
    }

    @Test
    fun `observeAll reflects inserts and deletes`() = runTest {
        val dao = FakeWhitelistDao()
        dao.insert(WhitelistEntity("hash1", "A", 1L))
        dao.insert(WhitelistEntity("hash2", "B", 2L))

        var list = dao.observeAll().first()
        assertEquals(2, list.size)

        dao.deleteByHash("hash1")
        list = dao.observeAll().first()
        assertEquals(1, list.size)
        assertEquals("hash2", list[0].phoneHash)
    }

    @Test
    fun `observeAll is ordered by addedAt descending`() = runTest {
        val dao = FakeWhitelistDao()
        dao.insert(WhitelistEntity("hash_old", "Old", 1000L))
        dao.insert(WhitelistEntity("hash_new", "New", 5000L))

        val list = dao.observeAll().first()
        assertEquals("hash_new", list[0].phoneHash)
        assertEquals("hash_old", list[1].phoneHash)
    }
}

class BlacklistDaoTest {

    @Test
    fun `insert and exists works correctly`() = runTest {
        val dao = FakeBlacklistDao()
        dao.insert(BlacklistEntity("hashSpam", "Molesto", 1000L))

        assertTrue(dao.exists("hashSpam") > 0)
        assertTrue(dao.exists("clean") == 0)
    }

    @Test
    fun `getByHash returns correct entity`() = runTest {
        val dao = FakeBlacklistDao()
        val entity = BlacklistEntity("hashScam", "Estafa", 2000L)
        dao.insert(entity)

        val retrieved = dao.getByHash("hashScam")
        assert(retrieved != null)
        assertEquals("hashScam", retrieved!!.phoneHash)
        assertEquals("Estafa", retrieved.reason)
        assertEquals(2000L, retrieved.addedAt)
    }

    @Test
    fun `deleteByHash removes entity from blacklist`() = runTest {
        val dao = FakeBlacklistDao()
        dao.insert(BlacklistEntity("hashDel", "Spam", 3000L))
        assertTrue(dao.exists("hashDel") > 0)

        dao.deleteByHash("hashDel")
        assertTrue(dao.exists("hashDel") == 0)
    }

    @Test
    fun `observeAll reflects blacklist changes`() = runTest {
        val dao = FakeBlacklistDao()
        dao.insert(BlacklistEntity("hashB1", "Razon 1", 1L))
        dao.insert(BlacklistEntity("hashB2", "Razon 2", 2L))

        val list = dao.observeAll().first()
        assertEquals(2, list.size)

        dao.deleteByHash("hashB1")
        assertEquals(1, dao.observeAll().first().size)
    }
}

private class FakeWhitelistDao : WhitelistDao {
    private val store = mutableMapOf<String, WhitelistEntity>()
    private val _flow = MutableStateFlow<List<WhitelistEntity>>(emptyList())

    override fun observeAll(): Flow<List<WhitelistEntity>> = _flow.map { it.sortedByDescending { e -> e.addedAt } }

    override suspend fun getByHash(phoneHash: String): WhitelistEntity? = store[phoneHash]

    override suspend fun exists(phoneHash: String): Int = if (store.containsKey(phoneHash)) 1 else 0

    override suspend fun insert(entity: WhitelistEntity) {
        store[entity.phoneHash] = entity
        _flow.value = store.values.toList()
    }

    override suspend fun deleteByHash(phoneHash: String) {
        store.remove(phoneHash)
        _flow.value = store.values.toList()
    }
}

private class FakeBlacklistDao : BlacklistDao {
    private val store = mutableMapOf<String, BlacklistEntity>()
    private val _flow = MutableStateFlow<List<BlacklistEntity>>(emptyList())

    override fun observeAll(): Flow<List<BlacklistEntity>> = _flow.map { it.sortedByDescending { e -> e.addedAt } }

    override suspend fun getByHash(phoneHash: String): BlacklistEntity? = store[phoneHash]

    override suspend fun exists(phoneHash: String): Int = if (store.containsKey(phoneHash)) 1 else 0

    override suspend fun insert(entity: BlacklistEntity) {
        store[entity.phoneHash] = entity
        _flow.value = store.values.toList()
    }

    override suspend fun deleteByHash(phoneHash: String) {
        store.remove(phoneHash)
        _flow.value = store.values.toList()
    }
}
