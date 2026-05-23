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
