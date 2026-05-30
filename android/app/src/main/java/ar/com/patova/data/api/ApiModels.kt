package ar.com.patova.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ValidateRequest(
    val number: String,
    @SerialName("device_id")
    val deviceId: String,
    @SerialName("call_direction")
    val callDirection: String = "INCOMING",
    val timestamp: String? = null
)

@Serializable
data class ValidateResponse(
    val verdict: String,
    @SerialName("spam_score")
    val spamScore: Int? = 0,
    val reason: String? = null,
    @SerialName("report_count")
    val reportCount: Int? = 0,
    @SerialName("prefix_valid")
    val prefixValid: Boolean? = false,
    @SerialName("prefix_zone")
    val prefixZone: String? = null,
    val operator: String? = null,
    val cached: Boolean? = false,
    @SerialName("latency_ms")
    val latencyMs: Float? = 0f
)

@Serializable
data class TopReportedNumber(
    @SerialName("number_e164_masked")
    val numberE164Masked: String,
    @SerialName("spam_score")
    val spamScore: Int,
    @SerialName("report_count")
    val reportCount: Int,
    val status: String
)

@Serializable
data class StatsResponse(
    @SerialName("total_numbers")
    val totalNumbers: Int,
    @SerialName("total_reports")
    val totalReports: Int,
    @SerialName("blocked_today")
    val blockedToday: Int,
    @SerialName("top_reported")
    val topReported: List<TopReportedNumber>
)

