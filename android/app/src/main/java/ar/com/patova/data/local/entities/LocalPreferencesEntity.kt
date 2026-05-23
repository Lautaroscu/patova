package ar.com.patova.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_preferences")
data class LocalPreferencesEntity(
    @PrimaryKey
    val id: Int = 1,

    @ColumnInfo(name = "strict_mode")
    val strictMode: Boolean = false,

    @ColumnInfo(name = "block_unknown")
    val blockUnknown: Boolean = false,

    @ColumnInfo(name = "spam_threshold")
    val spamThreshold: Float = 0.75f,

    @ColumnInfo(name = "sync_enabled")
    val syncEnabled: Boolean = true,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
