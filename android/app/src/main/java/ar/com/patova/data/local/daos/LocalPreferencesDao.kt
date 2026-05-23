package ar.com.patova.data.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ar.com.patova.data.local.entities.LocalPreferencesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalPreferencesDao {

    @Query("SELECT * FROM local_preferences WHERE id = 1")
    suspend fun get(): LocalPreferencesEntity?

    @Query("SELECT * FROM local_preferences WHERE id = 1")
    fun observe(): Flow<LocalPreferencesEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(prefs: LocalPreferencesEntity)
}
