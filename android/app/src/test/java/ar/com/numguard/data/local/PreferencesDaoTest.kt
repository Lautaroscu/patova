package ar.com.numguard.data.local

import ar.com.numguard.data.local.entities.LocalPreferencesEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferencesDaoTest {

    private val fakeDao = FakeLocalPreferencesDao()

    @Test
    fun `upsert and get preferences returns the same values`() = runTest {
        fakeDao.upsert(
            LocalPreferencesEntity(
                strictMode = true,
                blockUnknown = false,
                spamThreshold = 0.5f,
                syncEnabled = true,
                updatedAt = 1000L
            )
        )

        val prefs = fakeDao.get()
        assertNotNull(prefs)
        assertTrue(prefs!!.strictMode)
        assertEquals(false, prefs.blockUnknown)
        assertEquals(0.5f, prefs.spamThreshold)
        assertEquals(true, prefs.syncEnabled)
        assertEquals(1000L, prefs.updatedAt)
    }

    @Test
    fun `get returns null when no preferences stored`() = runTest {
        assertNull(fakeDao.get())
    }

    @Test
    fun `observe emits updated preferences`() = runTest {
        fakeDao.upsert(
            LocalPreferencesEntity(
                strictMode = false,
                blockUnknown = true,
                spamThreshold = 0.8f,
                syncEnabled = false,
                updatedAt = 2000L
            )
        )

        val observed = fakeDao.observe().first()
        assertNotNull(observed)
        assertEquals(false, observed!!.strictMode)
        assertEquals(true, observed.blockUnknown)
        assertEquals(0.8f, observed.spamThreshold)
        assertEquals(false, observed.syncEnabled)
    }

    @Test
    fun `upsert overwrites existing preferences`() = runTest {
        fakeDao.upsert(
            LocalPreferencesEntity(
                strictMode = false,
                updatedAt = 1000L
            )
        )

        fakeDao.upsert(
            LocalPreferencesEntity(
                strictMode = true,
                updatedAt = 2000L
            )
        )

        val prefs = fakeDao.get()
        assertNotNull(prefs)
        assertTrue(prefs!!.strictMode)
        assertEquals(2000L, prefs.updatedAt)
    }

    @Test
    fun `default preferences have sensible defaults`() {
        val default = LocalPreferencesEntity()
        assertEquals(false, default.strictMode)
        assertEquals(false, default.blockUnknown)
        assertEquals(0.75f, default.spamThreshold)
        assertEquals(true, default.syncEnabled)
    }
}

private class FakeLocalPreferencesDao : ar.com.numguard.data.local.daos.LocalPreferencesDao {
    private var stored: LocalPreferencesEntity? = null

    override suspend fun get(): LocalPreferencesEntity? = stored

    override fun observe(): Flow<LocalPreferencesEntity?> = flowOf(stored)

    override suspend fun upsert(prefs: LocalPreferencesEntity) {
        stored = prefs
    }
}
