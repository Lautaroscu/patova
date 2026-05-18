package ar.com.numguard.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "whitelist")
data class WhitelistEntity(
    @PrimaryKey
    @ColumnInfo(name = "phone_hash")
    val phoneHash: String,

    @ColumnInfo(name = "label")
    val label: String,

    @ColumnInfo(name = "added_at")
    val addedAt: Long
)
