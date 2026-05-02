package ar.com.numguard.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_events")
data class CallEventEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "number_hash")
    val numberHash: String,

    @ColumnInfo(name = "number_masked")
    val numberMasked: String,

    @ColumnInfo(name = "verdict")
    val verdict: String,

    @ColumnInfo(name = "spam_score")
    val spamScore: Int? = null,

    @ColumnInfo(name = "reason")
    val reason: String? = null,

    @ColumnInfo(name = "occurred_at_millis")
    val occurredAtMillis: Long,

    @ColumnInfo(name = "action_taken")
    val actionTaken: String,

    @ColumnInfo(name = "synced_feedback")
    val syncedFeedback: Boolean = false
)
