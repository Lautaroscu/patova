package ar.com.numguard.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CallEventDao {

    @Query("SELECT * FROM call_events ORDER BY occurred_at_millis DESC")
    fun getAllFlow(): Flow<List<CallEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: CallEventEntity)

    @Query("SELECT * FROM call_events WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CallEventEntity?

    @Query("UPDATE call_events SET synced_feedback = 1 WHERE id = :id")
    suspend fun markFeedbackSynced(id: String)
}
