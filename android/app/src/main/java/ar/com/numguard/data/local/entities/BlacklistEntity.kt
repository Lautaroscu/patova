package ar.com.numguard.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blacklist")
data class BlacklistEntity(
    @PrimaryKey
    @ColumnInfo(name = "phone_hash")
    val phoneHash: String,

    @ColumnInfo(name = "reason")
    val reason: String,

    @ColumnInfo(name = "added_at")
    val addedAt: Long
)
