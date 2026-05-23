package ar.com.patova.data.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ar.com.patova.data.local.entities.WhitelistEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WhitelistDao {

    @Query("SELECT * FROM whitelist ORDER BY added_at DESC")
    fun observeAll(): Flow<List<WhitelistEntity>>

    @Query("SELECT * FROM whitelist WHERE phone_hash = :phoneHash LIMIT 1")
    suspend fun getByHash(phoneHash: String): WhitelistEntity?

    @Query("SELECT COUNT(*) FROM whitelist WHERE phone_hash = :phoneHash")
    suspend fun exists(phoneHash: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: WhitelistEntity)

    @Query("DELETE FROM whitelist WHERE phone_hash = :phoneHash")
    suspend fun deleteByHash(phoneHash: String)
}
