package ar.com.patova.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CachedValidationDao {

    @Query("SELECT * FROM cached_validations WHERE number_hash = :numberHash LIMIT 1")
    suspend fun getByHash(numberHash: String): CachedValidationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CachedValidationEntity)

    @Query("DELETE FROM cached_validations WHERE expires_at_millis < :now")
    suspend fun deleteExpired(now: Long)
}
