package ar.com.patova.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.com.patova.data.local.CallEventDao
import ar.com.patova.data.local.CallEventEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val callEventDao: CallEventDao
) : ViewModel() {

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val current = callEventDao.getAllFlow().first()
                if (current.isEmpty()) {
                    val now = System.currentTimeMillis()
                    callEventDao.insert(
                        CallEventEntity(
                            id = UUID.randomUUID().toString(),
                            numberHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                            numberMasked = "+54 11 4987-XXXX",
                            verdict = "BLOCK",
                            spamScore = 95,
                            reason = "Bot wangiri",
                            occurredAtMillis = now - 2 * 60 * 1000, // hace 2m
                            actionTaken = "BLOCKED",
                            syncedFeedback = false
                        )
                    )
                    callEventDao.insert(
                        CallEventEntity(
                            id = UUID.randomUUID().toString(),
                            numberHash = "f5a5c602008fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b856",
                            numberMasked = "+54 11 5032-YYYY",
                            verdict = "ALLOW",
                            spamScore = 0,
                            reason = "ENACOM",
                            occurredAtMillis = now - 60 * 60 * 1000, // hace 1h
                            actionTaken = "ALLOWED",
                            syncedFeedback = false
                        )
                    )
                    callEventDao.insert(
                        CallEventEntity(
                            id = UUID.randomUUID().toString(),
                            numberHash = "a3c4f98108fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b857",
                            numberMasked = "+54 351 422-ZZZZ",
                            verdict = "SUSPECT",
                            spamScore = 67,
                            reason = "Score 67",
                            occurredAtMillis = now - 3 * 60 * 60 * 1000, // hace 3h
                            actionTaken = "ALLOWED",
                            syncedFeedback = false
                        )
                    )
                }
            } catch (e: Exception) {
                // Fallback silencioso en caso de inicialización concurrente o fallos
            }
        }
    }

    val state: StateFlow<DashboardState> = callEventDao.getAllFlow()
        .map { events ->
            // En una app real, acá filtraríamos por "esta semana"
            val blocked = events.count { it.verdict == "BLOCK" || it.verdict == "INVALID_PREFIX" || it.actionTaken == "BLOCKED" }
            val verified = events.count { it.verdict == "ALLOW" && it.actionTaken == "ALLOWED" }
            val suspicious = events.count { it.verdict == "SUSPECT" }
            
            val recent = events.take(15).map { entity ->
                val isBlocked = entity.verdict == "BLOCK" || entity.verdict == "INVALID_PREFIX" || entity.actionTaken == "BLOCKED"
                val isVerified = entity.verdict == "ALLOW"
                val isSuspicious = entity.verdict == "SUSPECT"
                
                val statusText = when {
                    isBlocked -> "Bloqueado"
                    isVerified -> "Verificado"
                    isSuspicious -> "Sospechoso"
                    else -> "Desconocido"
                }

                CallItemState(
                    number = entity.numberMasked,
                    status = statusText,
                    detail = entity.reason ?: "Sin detalle",
                    time = formatTimeAgo(entity.occurredAtMillis),
                    isBlocked = isBlocked,
                    isVerified = isVerified,
                    isSuspicious = isSuspicious
                )
            }
            
            DashboardState(
                blockedCountThisWeek = blocked,
                verifiedCount = verified,
                suspiciousCount = suspicious,
                recentCalls = recent
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardState()
        )

    private fun formatTimeAgo(timeMillis: Long): String {
        val diff = System.currentTimeMillis() - timeMillis
        val minutes = diff / (1000 * 60)
        val hours = minutes / 60
        val days = hours / 24

        return when {
            minutes < 1 -> "recién"
            minutes < 60 -> "hace ${minutes}m"
            hours < 24 -> "hace ${hours}h"
            days == 1L -> "ayer"
            else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timeMillis))
        }
    }
}
