package ar.com.numguard.domain

import android.telecom.CallScreeningService.CallResponse

object VerdictDecision {

    fun toCallResponse(verdict: String): CallResponse {
        val builder = CallResponse.Builder()
        when (verdict) {
            "BLOCK", "INVALID_PREFIX" -> {
                builder.setDisallowCall(true)
                builder.setRejectCall(true)
                builder.setSkipCallLog(false)
                builder.setSkipNotification(true)
            }
            else -> {
                builder.setDisallowCall(false)
                builder.setRejectCall(false)
                builder.setSkipCallLog(false)
                builder.setSkipNotification(false)
            }
        }
        return builder.build()
    }

    fun ttlMillisForVerdict(verdict: String): Long = when (verdict) {
        "BLOCK" -> 24L * 60 * 60 * 1000
        "ALLOW" -> 7L * 24 * 60 * 60 * 1000
        "SUSPECT" -> 30L * 60 * 1000
        "UNKNOWN" -> 5L * 60 * 1000
        "INVALID_PREFIX" -> 24L * 60 * 60 * 1000
        else -> 5L * 60 * 1000
    }

    fun maskE164(e164: String): String {
        if (e164.length <= 4) return "****"
        return e164.take(4) + "****" + e164.takeLast(2)
    }
}
