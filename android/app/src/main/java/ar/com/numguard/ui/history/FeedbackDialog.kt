package ar.com.numguard.ui.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ar.com.numguard.data.local.CallEventEntity

@Composable
fun FeedbackDialog(
    event: CallEventEntity,
    onDismiss: () -> Unit,
    onConfirm: (feedbackType: String) -> Unit
) {
    var selectedType by remember {
        mutableStateOf(
            if (event.actionTaken == "BLOCKED") "FALSE_POSITIVE" else "WAS_SPAM"
        )
    }

    val feedbackOptions = if (event.actionTaken == "BLOCKED") {
        listOf(
            "FALSE_POSITIVE" to "Era una llamada legitima (falso positivo)",
            "WAS_SPAM" to "Era spam, bien bloqueada"
        )
    } else {
        listOf(
            "WAS_SPAM" to "Era spam pero no fue bloqueada"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Feedback") },
        text = {
            Column {
                Text(
                    text = "Numero: ${event.numberMasked}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                feedbackOptions.forEach { (type, label) ->
                    TextButton(
                        onClick = { selectedType = type },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = selectedType == type,
                            onClick = { selectedType = type }
                        )
                        Text(
                            text = label,
                            modifier = Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedType) }) {
                Text("Enviar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
