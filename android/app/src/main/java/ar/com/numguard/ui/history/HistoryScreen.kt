package ar.com.numguard.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import ar.com.numguard.data.local.CallEventEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val events by viewModel.events.collectAsState()
    var showReportDialog by remember { mutableStateOf<CallEventEntity?>(null) }
    var showFeedbackDialog by remember { mutableStateOf<CallEventEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
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
                Text(
                    text = "Sin historial de llamadas",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Las llamadas evaluadas apareceran aqui",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(events, key = { it.id }) { event ->
                    CallEventCard(
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
private fun CallEventCard(
    event: CallEventEntity,
    onReport: () -> Unit,
    onFeedback: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (event.actionTaken) {
                "BLOCKED" -> MaterialTheme.colorScheme.errorContainer
                "FAILED_OPEN" -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = event.numberMasked,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatTimestamp(event.occurredAtMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = verdictLabel(event.verdict),
                        style = MaterialTheme.typography.bodyMedium,
                        color = when (event.verdict) {
                            "BLOCK", "INVALID_PREFIX" -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        text = actionLabel(event.actionTaken),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                if (event.spamScore != null && event.spamScore > 0) {
                    Text(
                        text = "Score ${event.spamScore}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (event.reason != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = event.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (event.actionTaken == "BLOCKED" && !event.syncedFeedback) {
                    TextButton(onClick = onFeedback) {
                        Text("Falso positivo")
                    }
                }
                if (event.actionTaken != "BLOCKED" && !event.syncedFeedback) {
                    TextButton(onClick = onReport) {
                        Text("Reportar spam")
                    }
                }
                if (event.actionTaken == "BLOCKED") {
                    TextButton(onClick = onReport) {
                        Text("Reportar spam")
                    }
                }
            }
        }
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
