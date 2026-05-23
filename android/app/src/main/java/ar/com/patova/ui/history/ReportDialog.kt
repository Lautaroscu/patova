package ar.com.patova.ui.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import ar.com.patova.data.local.CallEventEntity

@Composable
fun ReportDialog(
    event: CallEventEntity,
    onDismiss: () -> Unit,
    onConfirm: (reportType: String, description: String?) -> Unit
) {
    val reportTypes = listOf(
        "SPAM_CALL" to "Llamada spam",
        "ROBOCALL" to "Robocall",
        "SCAM" to "Estafa",
        "FRAUD" to "Fraude",
        "OTHER" to "Otro"
    )
    var selectedType by remember { mutableStateOf("SPAM_CALL") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reportar numero") },
        text = {
            Column {
                Text(
                    text = "Numero: ${event.numberMasked}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Tipo de spam:", style = MaterialTheme.typography.labelMedium)
                reportTypes.forEach { (type, label) ->
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
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripcion (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        selectedType,
                        description.ifBlank { null }
                    )
                }
            ) {
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
