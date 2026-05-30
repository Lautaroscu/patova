package ar.com.patova.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
class NotificationReceiver : BroadcastReceiver() {

    @Inject
    lateinit var pendingReportDao: PendingReportDao

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val number = intent.getStringExtra("number") ?: return

        when (action) {
            "ACTION_MARK_NOT_SPAM" -> {
                handleNotSpam(number)
            }
        }
    }

    private fun handleNotSpam(number: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val entity = PendingReportEntity(
                id = UUID.randomUUID().toString(),
                numberHash = PhoneHashing.sha256(number),
                numberMasked = number.take(number.length - 4) + "****",
                reportType = "FALSE_POSITIVE",
                description = "El usuario marcó este número como 'No es Spam' desde la notificación",
                createdAtMillis = System.currentTimeMillis(),
                callEventId = "",
                isFeedback = true,
                feedbackType = "FALSE_POSITIVE"
            )
            pendingReportDao.insert(entity)
            Log.i("Patova", "Feedback de FALSO POSITIVO encolado")
        }
    }
}
