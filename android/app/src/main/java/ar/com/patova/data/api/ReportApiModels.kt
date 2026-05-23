package ar.com.patova.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReportRequest(
    @SerialName("number_hash")
    val numberHash: String,
    @SerialName("report_type")
    val reportType: String,
    val description: String? = null,
    @SerialName("device_id")
    val deviceId: String
)

@Serializable
data class FeedbackRequest(
    @SerialName("number_hash")
    val numberHash: String,
    @SerialName("original_verdict")
    val originalVerdict: String,
    @SerialName("feedback_type")
    val feedbackType: String,
    @SerialName("device_id")
    val deviceId: String
)

@Serializable
data class ReportResponse(
    val received: Boolean = true
)

@Serializable
data class FeedbackResponse(
    val received: Boolean = true
)
