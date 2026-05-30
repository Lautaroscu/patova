package ar.com.patova.ui.history

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.provider.CallLog
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import ar.com.patova.data.local.CachedValidationDao
import ar.com.patova.data.local.CallEventDao
import ar.com.patova.data.local.CallEventEntity
import ar.com.patova.data.local.PremiumCacheManager
import ar.com.patova.data.local.daos.BlacklistDao
import ar.com.patova.data.local.daos.WhitelistDao
import ar.com.patova.domain.PhoneHashing
import ar.com.patova.domain.SubmitFeedbackUseCase
import ar.com.patova.domain.SubmitReportUseCase
import ar.com.patova.domain.VerdictDecision
import ar.com.patova.sync.PendingReportsWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    application: Application,
    private val callEventDao: CallEventDao,
    private val submitReportUseCase: SubmitReportUseCase,
    private val submitFeedbackUseCase: SubmitFeedbackUseCase,
    private val whitelistDao: WhitelistDao,
    private val blacklistDao: BlacklistDao,
    private val cachedValidationDao: CachedValidationDao,
    private val premiumCache: PremiumCacheManager
) : AndroidViewModel(application) {

    val events: StateFlow<List<CallEventEntity>> = callEventDao.getAllFlow()
        .map { dbEvents ->
            mergeWithSystemCallLog(dbEvents)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private fun mergeWithSystemCallLog(dbEvents: List<CallEventEntity>): List<CallEventEntity> {
        val context = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            return dbEvents
        }

        val systemCalls = mutableListOf<CallEventEntity>()

        try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                arrayOf(
                    CallLog.Calls._ID,
                    CallLog.Calls.NUMBER,
                    CallLog.Calls.DATE,
                    CallLog.Calls.TYPE
                ),
                null,
                null,
                "${CallLog.Calls.DATE} DESC LIMIT 80"
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndex(CallLog.Calls._ID)
                val numberCol = c.getColumnIndex(CallLog.Calls.NUMBER)
                val dateCol = c.getColumnIndex(CallLog.Calls.DATE)
                val typeCol = c.getColumnIndex(CallLog.Calls.TYPE)

                while (c.moveToNext()) {
                    val id = if (idCol >= 0) c.getLong(idCol) else System.currentTimeMillis()
                    val number = if (numberCol >= 0) c.getString(numberCol) ?: "" else ""
                    val date = if (dateCol >= 0) c.getLong(dateCol) else System.currentTimeMillis()
                    val type = if (typeCol >= 0) c.getInt(typeCol) else CallLog.Calls.INCOMING_TYPE

                    if (number.isBlank()) continue

                    val hash = PhoneHashing.sha256(number)
                    val masked = VerdictDecision.maskE164(number)

                    val isOutgoing = type == CallLog.Calls.OUTGOING_TYPE
                    val isMissed = type == CallLog.Calls.MISSED_TYPE
                    val isBlocked = type == CallLog.Calls.BLOCKED_TYPE

                    val action = if (isBlocked) "BLOCKED" else "ALLOWED"
                    val verdict = if (isBlocked) "BLOCK" else "ALLOW"

                    val defaultReason = when {
                        isOutgoing -> "Llamada saliente"
                        isMissed -> "Llamada perdida"
                        isBlocked -> "Bloqueada por spam"
                        else -> "Llamada entrante"
                    }

                    systemCalls.add(
                        CallEventEntity(
                            id = "sys_$id",
                            numberHash = hash,
                            numberMasked = masked,
                            verdict = verdict,
                            spamScore = if (isBlocked) 95 else 0,
                            reason = defaultReason,
                            occurredAtMillis = date,
                            actionTaken = action,
                            syncedFeedback = false
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("Patova", "Error leyendo CallLog del sistema", e)
            return dbEvents
        }

        val mergedList = mutableListOf<CallEventEntity>()
        mergedList.addAll(dbEvents)

        for (sysCall in systemCalls) {
            val alreadyScreened = dbEvents.any { dbEv ->
                dbEv.numberHash == sysCall.numberHash &&
                        Math.abs(dbEv.occurredAtMillis - sysCall.occurredAtMillis) < 8000
            }

            if (!alreadyScreened) {
                val enrichedCall = runBlocking(Dispatchers.IO) {
                    enrichCallWithLocalData(sysCall)
                }
                mergedList.add(enrichedCall)
            }
        }

        return mergedList.sortedByDescending { it.occurredAtMillis }
    }

    private suspend fun enrichCallWithLocalData(call: CallEventEntity): CallEventEntity {
        val hash = call.numberHash

        val whitelistEntry = whitelistDao.getByHash(hash)
        if (whitelistEntry != null) {
            return call.copy(
                verdict = "ALLOW",
                spamScore = 0,
                reason = "Contacto seguro · ${whitelistEntry.label}",
                actionTaken = "ALLOWED"
            )
        }

        val blacklistEntry = blacklistDao.getByHash(hash)
        if (blacklistEntry != null) {
            return call.copy(
                verdict = "BLOCK",
                spamScore = 100,
                reason = "Bloqueado: ${blacklistEntry.reason}",
                actionTaken = "BLOCKED"
            )
        }

        val cached = cachedValidationDao.getByHash(hash)
        if (cached != null) {
            val isBlocked = cached.verdict == "BLOCK" || cached.verdict == "INVALID_PREFIX"
            val isVerified = cached.verdict == "ALLOW"
            val isSuspicious = cached.verdict == "SUSPECT"

            val newAction = if (isBlocked) "BLOCKED" else "ALLOWED"
            val newStatus = when {
                isBlocked -> "BLOCK"
                isVerified -> "ALLOW"
                isSuspicious -> "SUSPECT"
                else -> cached.verdict
            }
            return call.copy(
                verdict = newStatus,
                spamScore = cached.spamScore,
                reason = cached.reason ?: call.reason,
                actionTaken = newAction
            )
        }

        return call
    }

    fun submitReport(
        event: CallEventEntity,
        reportType: String,
        description: String?
    ) {
        val isPremium = premiumCache.premiumAvailableOffline
        if (!isPremium && premiumCache.getReportsCountToday() >= 3) {
            android.widget.Toast.makeText(
                getApplication(),
                "Límite de 3 reportes diarios alcanzado para el plan Gratuito. ¡Pasate a Premium!",
                android.widget.Toast.LENGTH_LONG
            ).show()
            return
        }

        viewModelScope.launch {
            val sent = submitReportUseCase.invoke(
                numberHash = event.numberHash,
                numberMasked = event.numberMasked,
                reportType = reportType,
                description = description,
                callEventId = event.id
            )
            if (!sent) enqueueSyncWork()

            if (!isPremium) {
                premiumCache.incrementReportsCountToday()
            }
        }
    }

    fun submitFeedback(
        event: CallEventEntity,
        feedbackType: String
    ) {
        viewModelScope.launch {
            val sent = submitFeedbackUseCase.invoke(
                numberHash = event.numberHash,
                numberMasked = event.numberMasked,
                originalVerdict = event.verdict,
                feedbackType = feedbackType,
                callEventId = event.id
            )
            if (!sent) enqueueSyncWork()
        }
    }

    private fun enqueueSyncWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<PendingReportsWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                10L,
                TimeUnit.SECONDS
            )
            .build()

        WorkManager.getInstance(getApplication())
            .enqueueUniqueWork(
                "pending_reports_sync",
                ExistingWorkPolicy.REPLACE,
                request
            )
    }
}
