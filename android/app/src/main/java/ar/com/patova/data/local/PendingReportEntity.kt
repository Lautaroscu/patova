package ar.com.patova.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_reports")
data class PendingReportEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "number_hash")
    val numberHash: String,

    @ColumnInfo(name = "number_masked")
    val numberMasked: String,

    @ColumnInfo(name = "report_type")
    val reportType: String,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "is_feedback")
    val isFeedback: Boolean = false,

    @ColumnInfo(name = "feedback_type")
    val feedbackType: String? = null,

    @ColumnInfo(name = "call_event_id")
    val callEventId: String,

    @ColumnInfo(name = "created_at_millis")
    val createdAtMillis: Long,

    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0
)
