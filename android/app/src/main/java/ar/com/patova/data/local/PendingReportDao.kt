package ar.com.patova.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PendingReportDao {

    @Query("SELECT * FROM pending_reports")
    suspend fun getAll(): List<PendingReportEntity>

    @Query("SELECT * FROM pending_reports LIMIT 1")
    suspend fun getFirst(): PendingReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PendingReportEntity)

    @Query("DELETE FROM pending_reports WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE pending_reports SET retry_count = retry_count + 1 WHERE id = :id")
    suspend fun incrementRetry(id: String)

    @Query("DELETE FROM pending_reports WHERE retry_count >= 3")
    suspend fun deleteExcessiveRetries()
}
