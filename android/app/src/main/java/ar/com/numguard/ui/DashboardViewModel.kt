package ar.com.numguard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.com.numguard.data.local.CallEventDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val callEventDao: CallEventDao
) : ViewModel() {

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
                    isVerified -> "Permitido"
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
