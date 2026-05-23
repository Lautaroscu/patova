package ar.com.patova.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import ar.com.patova.data.local.CallEventEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import ar.com.patova.ui.theme.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onBack: (() -> Unit)? = null
) {
    val events by viewModel.events.collectAsState()
    var showReportDialog by remember { mutableStateOf<CallEventEntity?>(null) }
    var showFeedbackDialog by remember { mutableStateOf<CallEventEntity?>(null) }

    Scaffold(
        containerColor = Navy900,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Navy850)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    if (onBack != null) {
                        IconButton(onClick = onBack, modifier = Modifier.size(24.dp).padding(end = 8.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Volver",
                                tint = TextPrimary
                            )
                        }
                    }
                    Text(
                        text = "HISTORIAL",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.2.sp
                        ),
                        color = TextPrimary
                    )
                }
                Text(
                    text = "Registro de llamadas evaluadas",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                    color = TextMuted,
                    modifier = Modifier.padding(top = 4.dp, start = if(onBack != null) 24.dp else 0.dp)
                )
            }
        }
    ) { padding ->
        if (events.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Sin historial de llamadas",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Las llamadas evaluadas aparecerán aquí",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
            ) {
                items(events, key = { it.id }) { event ->
                    CallEventItem(
                        event = event,
                        onReport = { showReportDialog = event },
                        onFeedback = { showFeedbackDialog = event }
                    )
                }
            }
        }
    }

    showReportDialog?.let { event ->
        ReportDialog(
            event = event,
            onDismiss = { showReportDialog = null },
            onConfirm = { reportType, description ->
                viewModel.submitReport(event, reportType, description)
                showReportDialog = null
            }
        )
    }

    showFeedbackDialog?.let { event ->
        FeedbackDialog(
            event = event,
            onDismiss = { showFeedbackDialog = null },
            onConfirm = { feedbackType ->
                viewModel.submitFeedback(event, feedbackType)
                showFeedbackDialog = null
            }
        )
    }
}

@Composable
private fun CallEventItem(
    event: CallEventEntity,
    onReport: () -> Unit,
    onFeedback: () -> Unit
) {
    val isBlocked = event.actionTaken == "BLOCKED"
    val icon = if (isBlocked) Icons.Rounded.Close else Icons.Rounded.Check
    val iconBgColor = if (isBlocked) DangerRedBg else SafeGreenBg
    val iconColor = if (isBlocked) DangerRed else SafeGreen
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = androidx.compose.ui.Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBgColor),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text(
                        text = event.numberMasked,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = formatTimestamp(event.occurredAtMillis),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = TextMuted2
                    )
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(
                        text = verdictLabel(event.verdict),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = when (event.verdict) {
                            "BLOCK", "INVALID_PREFIX" -> DangerRed
                            "ALLOW" -> SafeGreen
                            else -> WarningAmber
                        }
                    )
                    Text(
                        text = " · ${actionLabel(event.actionTaken)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                if (event.reason != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = event.reason,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = TextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                if (event.spamScore != null && event.spamScore > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = WarningAmberBg
                    ) {
                        Text(
                            text = "Score ${event.spamScore}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = WarningAmber
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (event.actionTaken == "BLOCKED" && !event.syncedFeedback) {
                        TextButton(onClick = onFeedback) {
                            Text("Falso positivo", color = PremiumBlue)
                        }
                    }
                    if (event.actionTaken != "BLOCKED" && !event.syncedFeedback) {
                        TextButton(onClick = onReport) {
                            Text("Reportar spam", color = DangerRed)
                        }
                    }
                    if (event.actionTaken == "BLOCKED") {
                        TextButton(onClick = onReport) {
                            Text("Reportar spam", color = DangerRed)
                        }
                    }
                }
            }
        }
        Divider(
            modifier = Modifier.padding(start = 44.dp, top = 8.dp),
            thickness = 0.5.dp,
            color = BorderSubtle
        )
    }
}

private fun verdictLabel(verdict: String): String = when (verdict) {
    "BLOCK" -> "Bloqueado"
    "INVALID_PREFIX" -> "Prefijo invalido"
    "ALLOW" -> "Permitido"
    "SUSPECT" -> "Sospechoso"
    "UNKNOWN" -> "Desconocido"
    else -> verdict
}

private fun actionLabel(action: String): String = when (action) {
    "BLOCKED" -> "Llamada bloqueada"
    "ALLOWED" -> "Llamada permitida"
    "FAILED_OPEN" -> "No se pudo verificar · Se dejo pasar"
    else -> action
}

private fun formatTimestamp(millis: Long): String {
    val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    return sdf.format(Date(millis))
}
