package ar.com.numguard.data.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ar.com.numguard.data.local.entities.BlacklistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlacklistDao {

    @Query("SELECT * FROM blacklist ORDER BY added_at DESC")
    fun observeAll(): Flow<List<BlacklistEntity>>

    @Query("SELECT * FROM blacklist WHERE phone_hash = :phoneHash LIMIT 1")
    suspend fun getByHash(phoneHash: String): BlacklistEntity?

    @Query("SELECT COUNT(*) FROM blacklist WHERE phone_hash = :phoneHash")
    suspend fun exists(phoneHash: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BlacklistEntity)

    @Query("DELETE FROM blacklist WHERE phone_hash = :phoneHash")
    suspend fun deleteByHash(phoneHash: String)
}
