package ar.com.numguard.screening

import android.telecom.Call
import android.telecom.CallScreeningService

class NumGuardScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val response = CallResponse.Builder()
            .setDisallowCall(false)
            .setRejectCall(false)
            .setSkipCallLog(false)
            .setSkipNotification(false)
            .build()

        respondToCall(callDetails, response)
    }
}
