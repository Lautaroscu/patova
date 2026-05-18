package ar.com.numguard.domain

import android.util.Log
import ar.com.numguard.data.api.NumGuardApi
import ar.com.numguard.data.api.ValidateRequest
import ar.com.numguard.data.local.CachedValidationDao
import ar.com.numguard.data.local.CachedValidationEntity
import ar.com.numguard.data.local.CallEventDao
import ar.com.numguard.data.local.CallEventEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ValidateIncomingCallUseCase @Inject constructor(
    private val api: NumGuardApi,
    private val dao: CachedValidationDao,
    private val deviceIdProvider: DeviceIdProvider,
    private val callEventDao: CallEventDao
) {
    suspend fun decide(e164: String): String = withContext(Dispatchers.IO) {
        Log.d("NumGuard", "ValidateUseCase: Iniciando validación para $e164")
        val hash = PhoneHashing.sha256(e164)
        val cached = dao.getByHash(hash)
        val now = System.currentTimeMillis()

        if (cached != null && cached.expiresAtMillis > now) {
            Log.d("NumGuard", "ValidateUseCase: Cache HIT para $e164. Veredicto: ${cached.verdict}")
            saveCallEvent(
                hash = hash,
                masked = cached.numberE164Masked ?: VerdictDecision.maskE164(e164),
                verdict = cached.verdict,
                spamScore = cached.spamScore,
                reason = cached.reason,
                actionTaken = verdictToAction(cached.verdict)
            )
            return@withContext cached.verdict
        }

        Log.d("NumGuard", "ValidateUseCase: Cache MISS para $e164. Consultando API...")

        try {
            withTimeout(4500L) {
                val response = api.validate(
                    ValidateRequest(
                        number = e164,
                        deviceId = deviceIdProvider.getDeviceIdHash(),
                        timestamp = Instant.now().toString()
                    )
                )

                Log.d("NumGuard", "ValidateUseCase: API respondió con veredicto: ${response.verdict}")
                val masked = VerdictDecision.maskE164(e164)
                val ttl = VerdictDecision.ttlMillisForVerdict(response.verdict)
                dao.insert(
                    CachedValidationEntity(
                        numberHash = hash,
                        numberE164Masked = masked,
                        verdict = response.verdict,
                        spamScore = response.spamScore ?: 0,
                        reason = response.reason,
                        reportCount = response.reportCount ?: 0,
                        prefixZone = response.prefixZone,
                        cachedAtMillis = now,
                        expiresAtMillis = now + ttl
                    )
                )

                saveCallEvent(
                    hash = hash,
                    masked = masked,
                    verdict = response.verdict,
                    spamScore = response.spamScore,
                    reason = response.reason,
                    actionTaken = verdictToAction(response.verdict)
                )

                response.verdict
            }
        } catch (e: Exception) {
            Log.e("NumGuard", "ValidateUseCase: Fallo la validación de $e164: ${e.message}", e)
            val masked = VerdictDecision.maskE164(e164)
            saveCallEvent(
                hash = hash,
                masked = masked,
                verdict = "UNKNOWN",
                spamScore = null,
                reason = "Error: ${e.message}",
                actionTaken = "FAILED_OPEN"
            )
            "ALLOW"
        }
    }

    private suspend fun saveCallEvent(
        hash: String,
        masked: String,
        verdict: String,
        spamScore: Int?,
        reason: String?,
        actionTaken: String
    ) {
        Log.d("NumGuard", "ValidateUseCase: Guardando evento de llamada. Acción: $actionTaken")
        callEventDao.insert(
            CallEventEntity(
                id = UUID.randomUUID().toString(),
                numberHash = hash,
                numberMasked = masked,
                verdict = verdict,
                spamScore = spamScore,
                reason = reason,
                occurredAtMillis = System.currentTimeMillis(),
                actionTaken = actionTaken,
                syncedFeedback = false
            )
        )
    }

    private fun verdictToAction(verdict: String): String = when (verdict) {
        "BLOCK", "INVALID_PREFIX" -> "BLOCKED"
        else -> "ALLOWED"
    }
}
