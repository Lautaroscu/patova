package ar.com.numguard.screening

import android.content.Intent
import android.os.IBinder
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import ar.com.numguard.domain.ValidateIncomingCallUseCase
import ar.com.numguard.domain.VerdictDecision
import ar.com.numguard.domain.ConfigRepository
import ar.com.numguard.domain.PhoneHashing
import ar.com.numguard.notifications.NumGuardNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class NumGuardScreeningService : CallScreeningService() {

    @Inject
    lateinit var validateUseCase: ValidateIncomingCallUseCase

    @Inject
    lateinit var notificationManager: NumGuardNotificationManager

    @Inject
    lateinit var configRepository: ConfigRepository

    override fun onScreenCall(callDetails: Call.Details) {
        val startTime = System.currentTimeMillis()
        Log.i("NumGuard", "!!! onScreenCall disparado !!!")
        val e164 = normalizeNumber(callDetails)
        Log.i("NumGuard", "Llamada entrante detectada de: $e164")
        val masked = VerdictDecision.maskE164(e164)
        val phoneHash = PhoneHashing.sha256(e164)

        val verdict = runBlocking(Dispatchers.IO) {
            checkCall(e164, phoneHash, masked)
        }

        val response = VerdictDecision.toCallResponse(verdict)
        respondToCall(callDetails, response)

        val elapsed = System.currentTimeMillis() - startTime
        Log.i("NumGuard", "Screening completado en ${elapsed}ms - Veredicto: $verdict")
    }

    private suspend fun checkCall(e164: String, phoneHash: String, masked: String): String =
        withContext(Dispatchers.IO) {
            if (configRepository.isInWhitelist(phoneHash)) {
                Log.i("NumGuard", "PASO 0: Numero en whitelist, permitiendo: $masked")
                return@withContext "ALLOW"
            }

            if (configRepository.isInBlacklist(phoneHash)) {
                Log.i("NumGuard", "PASO 0.5: Numero en blacklist, bloqueando: $masked")
                notificationManager.showBlockedCallNotification(masked, 100, "BLACKLISTED")
                return@withContext "BLOCK"
            }

            if (isContact(e164)) {
                Log.i("NumGuard", "PASO 1: Llamada de contacto permitida: $masked")
                return@withContext "ALLOW"
            }

            val prefs = configRepository.getPreferences()
            if (prefs?.strictMode == true) {
                Log.i("NumGuard", "PASO 2: Strict Mode activo, bloqueando numero no-contacto: $masked")
                notificationManager.showBlockedCallNotification(masked, 100, "STRICT_MODE")
                return@withContext "BLOCK"
            }

            if (prefs?.blockUnknown == true && (e164.isBlank() || e164.length < 4)) {
                Log.i("NumGuard", "PASO 3: Block Unknown activo, bloqueando numero desconocido: $masked")
                notificationManager.showBlockedCallNotification(masked, 100, "UNKNOWN_NUMBER")
                return@withContext "BLOCK"
            }

            if (isLocalRuleAllowed(e164)) {
                Log.i("NumGuard", "PASO 4: Llamada permitida por regla local (prefijo permitido): $masked")
                return@withContext "ALLOW"
            }

            if (isLocalRuleBlocked(e164)) {
                Log.i("NumGuard", "PASO 5: Bloqueado por regla local: $masked")
                notificationManager.showBlockedCallNotification(masked, null, "LOCAL_RULE")
                return@withContext "BLOCK"
            }

            val (verdict, isError) = try {
                Log.i("NumGuard", "PASO 6: Validando con DB Local y Servidor: $masked")
                val v = validateUseCase.decide(e164)
                Log.i("NumGuard", "Resultado obtenido: $v")
                Pair(v, false)
            } catch (e: Exception) {
                Log.e("NumGuard", "ERROR CRITICO en screening para $masked: ${e.message}", e)
                Pair("ALLOW", true)
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

            verdict
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
