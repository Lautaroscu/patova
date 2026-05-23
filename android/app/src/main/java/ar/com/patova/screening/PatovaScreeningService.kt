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

        // Paso 3: Comprobar base de datos local (Room)
        val isLocalSpam = checkLocalDatabaseForSpam(phoneHash)
        if (isLocalSpam) {
            Log.w("Patova", "Paso 3: Spam Confirmado localmente. BLOQUEADO.")
            respondToCall(callDetails, allow = false, silence = false) // Bloqueo duro
            return
        }

        // Paso 4: Filtro de Emergencia (Doble Llamada en < 5 min)
        if (isEmergencyCall(phoneHash)) {
            Log.i("Patova", "Paso 4: EMERGENCIA DETECTADA (Segunda llamada en < 5 min). Haciendo sonar.")
            respondToCall(callDetails, allow = true, silence = false)
            return
        }

        // Paso 5: Desconocido sin reportes -> Silenciado inteligente (Filtro Suave)
        Log.i("Patova", "Paso 5: Número desconocido absoluto. Silenciando y alertando de forma suave.")
        respondToCall(callDetails, allow = true, silence = true)
        
        // Disparar UI de banner flotante o notificación interactiva
        notificationManager.showSmartNotification(rawNumber)
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
            setSkipNotification(silence)
            setSilenceCall(silence)
        }.build()
        respondToCall(callDetails, response)
    }
}
