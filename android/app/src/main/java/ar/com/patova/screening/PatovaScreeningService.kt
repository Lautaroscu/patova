package ar.com.patova.screening

import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import ar.com.patova.data.local.CachedValidationDao
import ar.com.patova.data.local.CallEventDao
import ar.com.patova.data.local.entities.LocalPreferencesEntity
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
    lateinit var cachedValidationDao: CachedValidationDao

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

        val phoneHash = PhoneHashing.sha256(rawNumber)

        // Paso 1: Comprobar Lista Blanca Manual (Whitelist)
        val isWhitelisted = runBlocking(Dispatchers.IO) { configRepository.isInWhitelist(phoneHash) }
        if (isWhitelisted) {
            Log.i("Patova", "Paso 1: Número en lista blanca. Permitido.")
            respondToCall(callDetails, allow = true, silence = false)
            return
        }

        // Paso 2: Comprobar Agenda de Contactos
        if (isNumberInContacts(rawNumber)) {
            Log.i("Patova", "Paso 2: Número en contactos. Permitido.")
            respondToCall(callDetails, allow = true, silence = false)
            return
        }

        // Paso 3: Comprobar relación previa en el Call Log o emergencias
        if (hasRecentRelationship(rawNumber)) {
            Log.i("Patova", "Paso 3: Relación recíproca o llamada previa exitosa detectada. Permitido.")
            respondToCall(callDetails, allow = true, silence = false)
            return
        }

        if (isEmergencyCall(phoneHash)) {
            Log.i("Patova", "Paso 3b: EMERGENCIA DETECTADA (Segunda llamada en < 5 min). Haciendo sonar.")
            respondToCall(callDetails, allow = true, silence = false)
            return
        }

        // Paso 4: Comprobar Lista Negra Manual (Blacklist)
        val isBlacklisted = runBlocking(Dispatchers.IO) { configRepository.isInBlacklist(phoneHash) }
        if (isBlacklisted) {
            Log.w("Patova", "Paso 4: Número en Lista Negra manual. Bloqueando.")
            saveCallEvent(phoneHash, rawNumber, "BLOCK", 100, "Spam confirmado por Lista Negra", "BLOCKED")
            respondToCall(callDetails, allow = false, silence = false)
            return
        }

        // Obtener preferencias y estado premium
        val prefs = runBlocking(Dispatchers.IO) { configRepository.getPreferences() } ?: LocalPreferencesEntity()
        val isPremium = premiumCache.premiumAvailableOffline

        // Paso 5: Bloquear Desconocidos
        if (prefs.blockUnknown) {
            Log.w("Patova", "Paso 5: Bloquear Desconocidos activo. Bloqueando.")
            saveCallEvent(phoneHash, rawNumber, "BLOCK", null, "Bloqueado por filtro de desconocidos", "BLOCKED")
            respondToCall(callDetails, allow = false, silence = false)
            return
        }

        // Paso 6: Modo Estricto (Solo permite contactos, whitelist y verificados por ENACOM)
        if (prefs.strictMode) {
            Log.i("Patova", "Paso 6: Modo Estricto activo. Consultando estado...")
            val verdict = runBlocking(Dispatchers.IO) { validateUseCase.decide(rawNumber) }
            val cached = runBlocking(Dispatchers.IO) { cachedValidationDao.getByHash(phoneHash) }
            if (cached != null && cached.reason == "ENACOM" && verdict == "ALLOW") {
                Log.i("Patova", "Modo Estricto: Número verificado por ENACOM. Permitido.")
                respondToCall(callDetails, allow = true, silence = false)
            } else {
                Log.w("Patova", "Modo Estricto: Número desconocido no verificado por ENACOM. Bloqueando.")
                runBlocking(Dispatchers.IO) {
                    val recent = callEventDao.getRecentCallsByHash(phoneHash, System.currentTimeMillis() - 5000L)
                    val latest = recent.maxByOrNull { it.occurredAtMillis }
                    if (latest != null) {
                        callEventDao.insert(latest.copy(
                            verdict = "BLOCK",
                            actionTaken = "BLOCKED",
                            reason = "Modo Estricto: No verificado por ENACOM"
                        ))
                    } else {
                        callEventDao.insert(
                            ar.com.patova.data.local.CallEventEntity(
                                id = java.util.UUID.randomUUID().toString(),
                                numberHash = phoneHash,
                                numberMasked = ar.com.patova.domain.VerdictDecision.maskE164(rawNumber),
                                verdict = "BLOCK",
                                spamScore = cached?.spamScore,
                                reason = "Modo Estricto: No verificado por ENACOM",
                                occurredAtMillis = System.currentTimeMillis(),
                                actionTaken = "BLOCKED"
                            )
                        )
                    }
                }
                respondToCall(callDetails, allow = false, silence = false)
            }
            return
        }

        // Paso 7: Validación normal (Consulta API / Caché)
        val verdict = runBlocking(Dispatchers.IO) { validateUseCase.decide(rawNumber) }
        val cached = runBlocking(Dispatchers.IO) { cachedValidationDao.getByHash(phoneHash) }
        val spamScore = cached?.spamScore ?: 0
        val thresholdPercent = (prefs.spamThreshold * 100).toInt()
        val isSpamByThreshold = spamScore >= thresholdPercent

        if (verdict == "BLOCK" || verdict == "INVALID_PREFIX" || isSpamByThreshold) {
            val finalReason = if (isSpamByThreshold) {
                cached?.reason ?: "Spam detectado por umbral ($spamScore% >= $thresholdPercent%)"
            } else {
                cached?.reason ?: "Spam bloqueado automáticamente"
            }

            if (isPremium) {
                Log.w("Patova", "Filtro Normal: Spam confirmado (Premium). Bloqueando en silencio.")
                runBlocking(Dispatchers.IO) {
                    val recent = callEventDao.getRecentCallsByHash(phoneHash, System.currentTimeMillis() - 5000L)
                    val latest = recent.maxByOrNull { it.occurredAtMillis }
                    if (latest != null) {
                        callEventDao.insert(latest.copy(
                            verdict = "BLOCK",
                            actionTaken = "BLOCKED",
                            reason = finalReason
                        ))
                    } else {
                        callEventDao.insert(
                            ar.com.patova.data.local.CallEventEntity(
                                id = java.util.UUID.randomUUID().toString(),
                                numberHash = phoneHash,
                                numberMasked = ar.com.patova.domain.VerdictDecision.maskE164(rawNumber),
                                verdict = "BLOCK",
                                spamScore = spamScore,
                                reason = finalReason,
                                occurredAtMillis = System.currentTimeMillis(),
                                actionTaken = "BLOCKED"
                            )
                        )
                    }
                }
                respondToCall(callDetails, allow = false, silence = false)
            } else {
                Log.w("Patova", "Filtro Normal: Spam confirmado (Gratuito). Haciendo sonar y alertando.")
                runBlocking(Dispatchers.IO) {
                    val recent = callEventDao.getRecentCallsByHash(phoneHash, System.currentTimeMillis() - 5000L)
                    val latest = recent.maxByOrNull { it.occurredAtMillis }
                    if (latest != null) {
                        callEventDao.insert(latest.copy(
                            verdict = "BLOCK",
                            reason = "$finalReason (Plan Gratuito)"
                        ))
                    }
                }
                respondToCall(callDetails, allow = true, silence = false)
                notificationManager.showSpamWarningNotification(rawNumber)
            }
            return
        }

        // Paso 8: Desconocido sin reportes o bajo umbral -> Silenciado inteligente para Premium
        if (isPremium) {
            if (verdict == "SUSPECT" || verdict == "UNKNOWN") {
                Log.i("Patova", "Filtro Normal: Número sospechoso/desconocido (Premium). Silenciando en segundo plano.")
                runBlocking(Dispatchers.IO) {
                    val recent = callEventDao.getRecentCallsByHash(phoneHash, System.currentTimeMillis() - 5000L)
                    val latest = recent.maxByOrNull { it.occurredAtMillis }
                    if (latest != null) {
                        callEventDao.insert(latest.copy(
                            reason = "Silenciado inteligente"
                        ))
                    }
                }
                respondToCall(callDetails, allow = true, silence = true)
            } else {
                Log.i("Patova", "Filtro Normal: Número permitido/seguro. Haciendo sonar.")
                respondToCall(callDetails, allow = true, silence = false)
            }
        } else {
            Log.i("Patova", "Filtro Normal: Plan Gratuito. Haciendo sonar normalmente.")
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
