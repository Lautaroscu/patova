package ar.com.patova.screening

import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import ar.com.patova.data.local.CallEventDao
import ar.com.patova.domain.ConfigRepository
import ar.com.patova.domain.PhoneHashing
import ar.com.patova.domain.ValidateIncomingCallUseCase
import ar.com.patova.notifications.PatovaNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class PatovaScreeningService : CallScreeningService() {

    @Inject
    lateinit var validateUseCase: ValidateIncomingCallUseCase

    @Inject
    lateinit var notificationManager: PatovaNotificationManager

    @Inject
    lateinit var configRepository: ConfigRepository

    @Inject
    lateinit var callEventDao: CallEventDao

    @Inject
    lateinit var premiumCache: ar.com.patova.data.local.PremiumCacheManager

    override fun onScreenCall(callDetails: Call.Details) {
        val rawNumber = callDetails.handle?.schemeSpecificPart ?: ""
        Log.d("Patova", "Evaluando llamada entrante: $rawNumber")

        // Paso 1: Comprobar Agenda de Contactos
        if (isNumberInContacts(rawNumber)) {
            Log.i("Patova", "Paso 1: Número en contactos. Permitido.")
            respondToCall(callDetails, allow = true, silence = false)
            return
        }

        // Paso 2: Comprobar relación previa en el Call Log
        if (hasRecentRelationship(rawNumber)) {
            Log.i("Patova", "Paso 2: Relación recíproca o llamada previa exitosa detectada. Permitido.")
            respondToCall(callDetails, allow = true, silence = false)
            return
        }

        val phoneHash = PhoneHashing.sha256(rawNumber)
        val isPremium = premiumCache.premiumAvailableOffline

        // Paso 3: Comprobar base de datos local (Room)
        val isLocalSpam = checkLocalDatabaseForSpam(phoneHash)
        if (isLocalSpam) {
            if (isPremium) {
                Log.w("Patova", "Paso 3: Spam Confirmado (Premium). BLOQUEANDO en silencio.")
                saveCallEvent(phoneHash, rawNumber, "BLOCK", 100, "Spam bloqueado automáticamente", "BLOCKED")
                respondToCall(callDetails, allow = false, silence = false) // Bloqueo duro silencioso
            } else {
                Log.w("Patova", "Paso 3: Spam Confirmado (Gratuito). Dejando pasar y alertando.")
                saveCallEvent(phoneHash, rawNumber, "BLOCK", 100, "Spam detectado (Plan Gratuito)", "ALLOWED")
                respondToCall(callDetails, allow = true, silence = false) // Deja pasar para que suene
                notificationManager.showSpamWarningNotification(rawNumber) // Disparar alerta invasiva de spam
            }
            return
        }

        // Paso 4: Filtro de Emergencia (Doble Llamada en < 5 min)
        if (isEmergencyCall(phoneHash)) {
            Log.i("Patova", "Paso 4: EMERGENCIA DETECTADA (Segunda llamada en < 5 min). Haciendo sonar.")
            respondToCall(callDetails, allow = true, silence = false)
            return
        }

        // Paso 5: Desconocido sin reportes -> Silenciado inteligente (Filtro Suave)
        if (isPremium) {
            Log.i("Patova", "Paso 5: Número desconocido absoluto (Premium). Silenciando silenciosamente en segundo plano.")
            saveCallEvent(phoneHash, rawNumber, "SUSPECT", 65, "Silenciado inteligente", "ALLOWED")
            respondToCall(callDetails, allow = true, silence = true)
        } else {
            Log.i("Patova", "Paso 5: Número desconocido absoluto (Gratuito). Haciendo sonar normalmente.")
            respondToCall(callDetails, allow = true, silence = false)
        }
        
        // Guardar para monitoreo de robocall
        markForRobocallMonitoring(rawNumber)
    }

    private fun markForRobocallMonitoring(phoneNumber: String) {
        val prefs = getSharedPreferences("robocall_monitoring", MODE_PRIVATE)
        prefs.edit().apply {
            putString("monitored_number", phoneNumber)
            putLong("start_time", System.currentTimeMillis())
            apply()
        }
    }

    private fun isNumberInContacts(phoneNumber: String): Boolean {
        if (phoneNumber.isBlank()) return false
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI, 
            Uri.encode(phoneNumber)
        )
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        return try {
            contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                cursor.moveToFirst()
            } ?: false
        } catch (e: Exception) {
            Log.e("Patova", "Error al consultar contactos", e)
            false
        }
    }

    private fun hasRecentRelationship(phoneNumber: String): Boolean {
        if (phoneNumber.isBlank()) return false
        val uri = CallLog.Calls.CONTENT_URI
        val selection = "${CallLog.Calls.NUMBER} = ? AND (${CallLog.Calls.TYPE} = ? OR (${CallLog.Calls.TYPE} = ? AND ${CallLog.Calls.DURATION} > 8))"
        val selectionArgs = arrayOf(
            phoneNumber, 
            CallLog.Calls.OUTGOING_TYPE.toString(), 
            CallLog.Calls.INCOMING_TYPE.toString()
        )
        
        return try {
            contentResolver.query(uri, null, selection, selectionArgs, null)?.use { cursor ->
                cursor.count > 0
            } ?: false
        } catch (e: Exception) {
            Log.e("Patova", "Error al consultar Call Log", e)
            false
        }
    }

    private fun isEmergencyCall(phoneHash: String): Boolean {
        return runBlocking(Dispatchers.IO) {
            val fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000)
            val recentCalls = callEventDao.getRecentCallsByHash(phoneHash, fiveMinutesAgo)
            recentCalls.isNotEmpty()
        }
    }

    private fun checkLocalDatabaseForSpam(phoneHash: String): Boolean {
        return runBlocking(Dispatchers.IO) {
            configRepository.isInBlacklist(phoneHash)
        }
    }

    private fun respondToCall(callDetails: Call.Details, allow: Boolean, silence: Boolean) {
        val response = CallResponse.Builder().apply {
            setDisallowCall(!allow)
            setRejectCall(!allow)
            setSkipCallLog(false)
            setSkipNotification(!allow && silence) // Solo podemos saltar notificación si bloqueamos
            setSilenceCall(silence)
        }.build()
        respondToCall(callDetails, response)
    }

    private fun saveCallEvent(
        hash: String,
        number: String,
        verdict: String,
        spamScore: Int?,
        reason: String?,
        actionTaken: String
    ) {
        runBlocking(Dispatchers.IO) {
            try {
                callEventDao.insert(
                    ar.com.patova.data.local.CallEventEntity(
                        id = java.util.UUID.randomUUID().toString(),
                        numberHash = hash,
                        numberMasked = ar.com.patova.domain.VerdictDecision.maskE164(number),
                        verdict = verdict,
                        spamScore = spamScore,
                        reason = reason,
                        occurredAtMillis = System.currentTimeMillis(),
                        actionTaken = actionTaken,
                        syncedFeedback = false
                    )
                )
            } catch (e: Exception) {
                Log.e("Patova", "Error al guardar evento de llamada en base de datos", e)
            }
        }
    }
}
