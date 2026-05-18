package ar.com.numguard.screening

import android.content.Intent
import android.os.IBinder
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import ar.com.numguard.domain.ValidateIncomingCallUseCase
import ar.com.numguard.domain.VerdictDecision
import ar.com.numguard.domain.ConfigRepository
import ar.com.numguard.notifications.NumGuardNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class NumGuardScreeningService : CallScreeningService() {

    @Inject
    lateinit var validateUseCase: ValidateIncomingCallUseCase

    @Inject
    lateinit var notificationManager: NumGuardNotificationManager

    @Inject
    lateinit var configRepository: ConfigRepository

    override fun onCreate() {
        super.onCreate()
        Log.i("NumGuard", "NumGuardScreeningService: onCreate")
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.i("NumGuard", "NumGuardScreeningService: onBind - Intent action: ${intent?.action}")
        return super.onBind(intent)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.i("NumGuard", "NumGuardScreeningService: onUnbind")
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        Log.i("NumGuard", "NumGuardScreeningService: onDestroy")
        super.onDestroy()
    }

    override fun onScreenCall(callDetails: Call.Details) {
        Log.i("NumGuard", "!!! onScreenCall disparado !!!")
        val e164 = normalizeNumber(callDetails)
        Log.i("NumGuard", "Llamada entrante detectada de: $e164")
        val masked = VerdictDecision.maskE164(e164)

        // PASO 1: Verificar si es Contacto (simulado)
        if (isContact(e164)) {
            Log.i("NumGuard", "PASO 1: Llamada de contacto permitida: $e164")
            respondToCall(callDetails, VerdictDecision.toCallResponse("ALLOW"))
            return
        }

        // PASO 1.5: Configuración de prefijos permitidos (Lista blanca local)
        if (isLocalRuleAllowed(e164)) {
            Log.i("NumGuard", "PASO 1.5: Llamada permitida por regla local (prefijo permitido): $e164")
            respondToCall(callDetails, VerdictDecision.toCallResponse("ALLOW"))
            return
        }

        // PASO 2: Verificar Reglas Locales (Modo Estricto, prefijos bloqueados, etc)
        if (isLocalRuleBlocked(e164)) {
            Log.i("NumGuard", "PASO 2: Bloqueado por regla local: $e164")
            notificationManager.showBlockedCallNotification(masked, null, "LOCAL_RULE")
            respondToCall(callDetails, VerdictDecision.toCallResponse("BLOCK"))
            return
        }

        // PASO 3: Validación con la Base Local de Spam y luego Server (Cloud Check -> ENACOM)
        val (verdict, isError) = kotlinx.coroutines.runBlocking {
            try {
                Log.i("NumGuard", "PASO 3: Validando con DB Local y Servidor: $e164")
                val v = validateUseCase.decide(e164)
                Log.i("NumGuard", "Resultado obtenido: $v")
                Pair(v, false)
            } catch (e: Exception) {
                Log.e("NumGuard", "ERROR CRITICO en screening para $e164: ${e.message}", e)
                Pair("ALLOW", true)
            }
        }

        if (isError) {
            notificationManager.showVerificationFailedNotification()
        } else {
            when (verdict) {
                "BLOCK", "INVALID_PREFIX" -> {
                    Log.i("NumGuard", "ACCION: Bloqueando llamada (Veredicto: $verdict)")
                    notificationManager.showBlockedCallNotification(
                        numberMasked = masked,
                        spamScore = null,
                        verdict = verdict
                    )
                }
                else -> {
                    Log.i("NumGuard", "ACCION: Permitiendo llamada (Veredicto: $verdict)")
                }
            }
        }

        val response = VerdictDecision.toCallResponse(verdict)
        respondToCall(callDetails, response)
    }

    private fun normalizeNumber(callDetails: Call.Details): String {
        val handle = callDetails.handle
        if (handle == null) {
            Log.w("NumGuard", "Advertencia: El handle de la llamada es nulo")
            return ""
        }
        val raw = handle.schemeSpecificPart ?: ""
        return raw.filter { it.isDigit() }
    }

    private fun isContact(number: String): Boolean {
        // TODO: Implementar lectura real de contactos mediante ContactsContract
        return false 
    }

    private fun isLocalRuleAllowed(number: String): Boolean {
        val allowedPrefixes = configRepository.getAllowedPrefixes()
        for (prefix in allowedPrefixes) {
            if (number.startsWith(prefix) || number.contains(prefix)) return true
        }
        return false
    }

    private fun isLocalRuleBlocked(number: String): Boolean {
        if (configRepository.getBlockNonContacts()) {
            return true
        }
        
        val blockedPrefixes = configRepository.getBlockedPrefixes()
        for (prefix in blockedPrefixes) {
            if (number.startsWith(prefix) || number.contains(prefix)) return true
        }
        
        return false
    }
}
