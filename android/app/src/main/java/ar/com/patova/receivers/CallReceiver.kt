package ar.com.patova.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import ar.com.patova.data.local.PendingReportDao
import ar.com.patova.data.local.PendingReportEntity
import ar.com.patova.domain.PhoneHashing
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class CallReceiver : BroadcastReceiver() {

    @Inject
    lateinit var pendingReportDao: PendingReportDao

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        Log.d("Patova", "Cambio de estado telefónico: $state")

        when (state) {
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                // Llamada contestada, cancelar monitoreo de robocall
                clearMonitoring(context)
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                checkRobocall(context)
            }
        }
    }

    private fun checkRobocall(context: Context) {
        val prefs = context.getSharedPreferences("robocall_monitoring", Context.MODE_PRIVATE)
        val monitoredNumber = prefs.getString("monitored_number", null) ?: return
        val startTime = prefs.getLong("start_time", 0L)
        val duration = System.currentTimeMillis() - startTime

        Log.d("Patova", "Llamada finalizada. Duración del timbrado: ${duration}ms")

        if (duration < 5000) {
            Log.w("Patova", "ROBOCALL DETECTADA: $monitoredNumber (Cortó en ${duration}ms)")
            reportRobocall(monitoredNumber)
        }

        clearMonitoring(context)
    }

    private fun reportRobocall(number: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val entity = PendingReportEntity(
                id = UUID.randomUUID().toString(),
                numberHash = PhoneHashing.sha256(number),
                numberMasked = number.take(number.length - 4) + "****",
                reportType = "ROBOCALL",
                description = "Llamada fantasma (silenciada) que cortó en menos de 5s",
                createdAtMillis = System.currentTimeMillis(),
                callEventId = "" // Podríamos vincularlo si guardamos el id en el screening
            )
            pendingReportDao.insert(entity)
            Log.i("Patova", "Reporte de ROBOCALL encolado localmente")
        }
    }

    private fun clearMonitoring(context: Context) {
        val prefs = context.getSharedPreferences("robocall_monitoring", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
