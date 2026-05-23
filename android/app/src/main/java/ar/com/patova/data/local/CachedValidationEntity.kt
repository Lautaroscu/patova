package ar.com.patova.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_validations")
data class CachedValidationEntity(
    @PrimaryKey
    @ColumnInfo(name = "number_hash")
    val numberHash: String,

    @ColumnInfo(name = "number_e164_masked")
    val numberE164Masked: String? = null,

    @ColumnInfo(name = "verdict")
    val verdict: String,

    @ColumnInfo(name = "spam_score")
    val spamScore: Int = 0,

    @ColumnInfo(name = "reason")
    val reason: String? = null,

    @ColumnInfo(name = "report_count")
    val reportCount: Int = 0,

    @ColumnInfo(name = "prefix_zone")
    val prefixZone: String? = null,

    @ColumnInfo(name = "cached_at_millis")
    val cachedAtMillis: Long,

    @ColumnInfo(name = "expires_at_millis")
    val expiresAtMillis: Long
)
