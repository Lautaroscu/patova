package ar.com.patova.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ar.com.patova.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PatovaNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_BLOCKED_ID = "patova_blocked_calls"
        const val CHANNEL_BLOCKED_NAME = "Llamadas bloqueadas"
        const val CHANNEL_ERROR_ID = "patova_errors"
        const val CHANNEL_ERROR_NAME = "Errores de verificacion"
        const val CHANNEL_SMART_ID = "patova_smart_filter"
        const val CHANNEL_SMART_NAME = "Filtro inteligente"

        private const val NOTIFICATION_ID_BLOCKED = 1001
        private const val NOTIFICATION_ID_ERROR = 1002
        private const val NOTIFICATION_ID_SMART = 1003
    }

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val blockedChannel = NotificationChannel(
                CHANNEL_BLOCKED_ID,
                CHANNEL_BLOCKED_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de llamadas bloqueadas por Patova"
            }

            val errorChannel = NotificationChannel(
                CHANNEL_ERROR_ID,
                CHANNEL_ERROR_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Errores al verificar llamadas"
                setShowBadge(false)
            }

            val smartChannel = NotificationChannel(
                CHANNEL_SMART_ID,
                CHANNEL_SMART_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones de llamadas silenciadas por el filtro suave"
            }

            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(blockedChannel)
            manager.createNotificationChannel(errorChannel)
            manager.createNotificationChannel(smartChannel)
        }
    }

    fun showBlockedCallNotification(
        numberMasked: String,
        spamScore: Int?,
        verdict: String
    ) {
        if (!hasNotificationPermission()) return

        val scoreText = if (spamScore != null && spamScore > 0) " · Score $spamScore" else ""
        val title = "Llamada bloqueada por Patova"
        val content = "Numero sospechoso: $numberMasked$scoreText"

        if (verdict == "INVALID_PREFIX") {
            val detail = "El prefijo no corresponde a numeracion valida registrada localmente"
            showBlockedNotification(title, "$content\n$detail")
        } else {
            showBlockedNotification(title, content)
        }
    }

    fun showSmartNotification(phoneNumber: String) {
        if (!hasNotificationPermission()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_SMART_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Llamada silenciada por Patova")
            .setContentText("Desconocido: $phoneNumber. Toca para ver.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(false)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_SMART, notification)
    }

    fun showVerificationFailedNotification() {
        if (!hasNotificationPermission()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ERROR_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Verificacion fallida")
            .setContentText("No se pudo verificar la llamada. Patova la dejo pasar por seguridad.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("No se pudo verificar la llamada. Patova la dejo pasar por seguridad.")
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(false)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_ERROR, notification)
    }

    private fun showBlockedNotification(title: String, content: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_BLOCKED_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(false)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_BLOCKED, notification)
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
