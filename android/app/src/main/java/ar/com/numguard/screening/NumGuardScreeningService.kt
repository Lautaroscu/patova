package ar.com.numguard.screening

import android.telecom.Call
import android.telecom.CallScreeningService
import ar.com.numguard.domain.ValidateIncomingCallUseCase
import ar.com.numguard.domain.VerdictDecision
import ar.com.numguard.notifications.NumGuardNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NumGuardScreeningService : CallScreeningService() {

    @Inject
    lateinit var validateUseCase: ValidateIncomingCallUseCase

    @Inject
    lateinit var notificationManager: NumGuardNotificationManager

    override fun onScreenCall(callDetails: Call.Details) {
        val e164 = normalizeNumber(callDetails)
        val masked = VerdictDecision.maskE164(e164)

        val (verdict, isError) = kotlinx.coroutines.runBlocking {
            try {
                val v = validateUseCase.decide(e164)
                Pair(v, false)
            } catch (_: Exception) {
                Pair("ALLOW", true)
            }
        }

        if (isError) {
            notificationManager.showVerificationFailedNotification()
        } else {
            when (verdict) {
                "BLOCK", "INVALID_PREFIX" -> {
                    notificationManager.showBlockedCallNotification(
                        numberMasked = masked,
                        spamScore = null,
                        verdict = verdict
                    )
                }
            }
        }

        val response = VerdictDecision.toCallResponse(verdict)
        respondToCall(callDetails, response)
    }

    private fun normalizeNumber(callDetails: Call.Details): String {
        val handle = callDetails.handle
        val raw = handle?.schemeSpecificPart ?: ""
        return raw.filter { it.isDigit() || it == '+' }
            .let { if (it.startsWith('+')) it else "+$it" }
    }
}
