package ar.com.patova.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ar.com.patova.ui.theme.BorderSubtle
import ar.com.patova.ui.theme.DangerRed
import ar.com.patova.ui.theme.DangerRedBg
import ar.com.patova.ui.theme.Navy700
import ar.com.patova.ui.theme.Navy800
import ar.com.patova.ui.theme.Navy850
import ar.com.patova.ui.theme.Navy900
import ar.com.patova.ui.theme.PremiumBlue
import ar.com.patova.ui.theme.PremiumBlueBg
import ar.com.patova.ui.theme.SafeGreen
import ar.com.patova.ui.theme.SafeGreenBg
import ar.com.patova.ui.theme.TextMuted
import ar.com.patova.ui.theme.TextMuted2
import ar.com.patova.ui.theme.TextPrimary
import ar.com.patova.ui.theme.TextSecondary
import ar.com.patova.ui.theme.WarningAmber
import ar.com.patova.ui.theme.WarningAmberBg
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val scrollState = rememberScrollState()
    val preferences by viewModel.preferences.collectAsState()
    val whitelistEntries by viewModel.whitelistEntries.collectAsState()
    val blacklistEntries by viewModel.blacklistEntries.collectAsState()
    val showAddWhitelist by viewModel.showAddWhitelistDialog.collectAsState()
    val showAddBlacklist by viewModel.showAddBlacklistDialog.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy900)
            .verticalScroll(scrollState)
    ) {
        SettingsHeader()

        Spacer(modifier = Modifier.height(20.dp))

        StrictModeSection(
            enabled = preferences?.strictMode ?: false,
            onToggle = { viewModel.updateStrictMode(it) }
        )

        BlockUnknownSection(
            enabled = preferences?.blockUnknown ?: false,
            onToggle = { viewModel.updateBlockUnknown(it) }
        )

        SpamThresholdSection(
            threshold = preferences?.spamThreshold ?: 0.75f,
            onThresholdChange = { viewModel.updateSpamThreshold(it) }
        )

        SyncEnabledSection(
            enabled = preferences?.syncEnabled ?: true,
            onToggle = { viewModel.updateSyncEnabled(it) }
        )

        WhitelistSection(
            entries = whitelistEntries,
            onAddClick = { viewModel.showAddWhitelistDialog() },
            onDeleteClick = { viewModel.removeWhitelistEntry(it) }
        )

        BlacklistSection(
            entries = blacklistEntries,
            onAddClick = { viewModel.showAddBlacklistDialog() },
            onDeleteClick = { viewModel.removeBlacklistEntry(it) }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }

    if (showAddWhitelist) {
        AddWhitelistDialog(
            phone = viewModel.whitelistPhoneInput.collectAsState().value,
            label = viewModel.whitelistLabelInput.collectAsState().value,
            onPhoneChange = { viewModel.updateWhitelistPhoneInput(it) },
            onLabelChange = { viewModel.updateWhitelistLabelInput(it) },
            onConfirm = { viewModel.addWhitelistEntry() },
            onDismiss = { viewModel.dismissAddWhitelistDialog() }
        )
    }

    if (showAddBlacklist) {
        AddBlacklistDialog(
            phone = viewModel.blacklistPhoneInput.collectAsState().value,
            reason = viewModel.blacklistReasonInput.collectAsState().value,
            onPhoneChange = { viewModel.updateBlacklistPhoneInput(it) },
            onReasonChange = { viewModel.updateBlacklistReasonInput(it) },
            onConfirm = { viewModel.addBlacklistEntry() },
            onDismiss = { viewModel.dismissAddBlacklistDialog() }
        )
    }
}

@Composable
private fun SettingsHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Navy850)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = "CONFIGURACIÓN",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.2.sp
            ),
            color = TextPrimary
        )
        Text(
            text = "Personalizá tu protección",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
            color = TextMuted,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun StrictModeSection(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    SectionLabel("FILTROS DE SEGURIDAD")

    SettingsToggleCard(
        icon = Icons.Rounded.Shield,
        iconColor = DangerRed,
        iconBg = DangerRedBg,
        title = "Modo Estricto",
        subtitle = "Solo permite contactos, whitelist y verificados por ENACOM",
        checked = enabled,
        onCheckedChange = onToggle
    )
}

@Composable
private fun BlockUnknownSection(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    SettingsToggleCard(
        icon = Icons.Rounded.Close,
        iconColor = WarningAmber,
        iconBg = WarningAmberBg,
        title = "Bloquear Desconocidos",
        subtitle = "Bloquea números ocultos y desconocidos automáticamente",
        checked = enabled,
        onCheckedChange = onToggle
    )
}

@Composable
private fun SpamThresholdSection(threshold: Float, onThresholdChange: (Float) -> Unit) {
    SectionLabel("SENSIBILIDAD SPAM")

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        color = Navy800,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Umbral de Spam",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = TextPrimary
                )
                Text(
                    text = "${(threshold * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = when {
                        threshold >= 0.8f -> DangerRed
                        threshold >= 0.5f -> WarningAmber
                        else -> SafeGreen
                    }
                )
            }
            Text(
                text = "A mayor porcentaje, más estricto el filtro de spam",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Slider(
                value = threshold,
                onValueChange = onThresholdChange,
                valueRange = 0f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = PremiumBlue,
                    activeTrackColor = PremiumBlue,
                    inactiveTrackColor = Navy700
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0%", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                Text("100%", style = MaterialTheme.typography.labelSmall, color = TextMuted)
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun SyncEnabledSection(enabled: Boolean, onToggle: (Boolean) -> Unit) {
    SectionLabel("SINCRONIZACIÓN")

    SettingsToggleCard(
        icon = Icons.Rounded.Sync,
        iconColor = PremiumBlue,
        iconBg = PremiumBlueBg,
        title = "Sincronización en la Nube",
        subtitle = "Sincroniza tus listas y preferencias con el servidor",
        checked = enabled,
        onCheckedChange = onToggle
    )
}

@Composable
private fun WhitelistSection(
    entries: List<ar.com.patova.data.local.entities.WhitelistEntity>,
    onAddClick: () -> Unit,
    onDeleteClick: (String) -> Unit
) {
    SectionLabel("LISTA BLANCA")

    ListSection(
        entries = entries.map { ListEntry(it.phoneHash, it.label, it.addedAt) },
        onAddClick = onAddClick,
        onDeleteClick = onDeleteClick,
        addButtonLabel = "Agregar a Lista Blanca",
        addButtonColor = SafeGreen,
        addButtonBg = SafeGreenBg,
        emptyMessage = "No hay números en la lista blanca"
    )
}

@Composable
private fun BlacklistSection(
    entries: List<ar.com.patova.data.local.entities.BlacklistEntity>,
    onAddClick: () -> Unit,
    onDeleteClick: (String) -> Unit
) {
    SectionLabel("LISTA NEGRA")

    ListSection(
        entries = entries.map { ListEntry(it.phoneHash, it.reason, it.addedAt) },
        onAddClick = onAddClick,
        onDeleteClick = onDeleteClick,
        addButtonLabel = "Agregar a Lista Negra",
        addButtonColor = DangerRed,
        addButtonBg = DangerRedBg,
        emptyMessage = "No hay números en la lista negra"
    )
}

private data class ListEntry(
    val fullHash: String,
    val label: String,
    val addedAt: Long,
    val displayHash: String = fullHash.take(12) + "…"
)

@Composable
private fun ListSection(
    entries: List<ListEntry>,
    onAddClick: () -> Unit,
    onDeleteClick: (String) -> Unit,
    addButtonLabel: String,
    addButtonColor: androidx.compose.ui.graphics.Color,
    addButtonBg: androidx.compose.ui.graphics.Color,
    emptyMessage: String
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        color = Navy800,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (entries.isEmpty()) {
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                )
            } else {
                entries.forEachIndexed { index, entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.displayHash,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontSize = 12.sp
                                ),
                                color = TextSecondary
                            )
                            Text(
                                text = entry.label + " · " + formatTimestamp(entry.addedAt),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = TextMuted
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Eliminar",
                            tint = DangerRed.copy(alpha = 0.7f),
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { onDeleteClick(entry.fullHash) }
                        )
                    }
                    if (index < entries.lastIndex) {
                        Divider(color = BorderSubtle, thickness = 0.5.dp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onAddClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = addButtonBg),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = addButtonColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = addButtonLabel,
                    color = addButtonColor,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
        color = TextMuted,
        modifier = Modifier.padding(
            start = 18.dp,
            end = 18.dp,
            top = 16.dp,
            bottom = 8.dp
        )
    )
}

@Composable
private fun SettingsToggleCard(
    icon: ImageVector,
    iconColor: androidx.compose.ui.graphics.Color,
    iconBg: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clickable { onCheckedChange(!checked) },
        color = Navy800,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = PremiumBlue,
                    checkedTrackColor = PremiumBlueBg
                )
            )
        }
    }

    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun AddWhitelistDialog(
    phone: String,
    label: String,
    onPhoneChange: (String) -> Unit,
    onLabelChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Navy850,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text("Agregar a Lista Blanca", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    label = { Text("Número de teléfono") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SafeGreen,
                        unfocusedBorderColor = Navy700,
                        focusedLabelColor = SafeGreen,
                        unfocusedLabelColor = TextMuted,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = label,
                    onValueChange = onLabelChange,
                    label = { Text("Etiqueta (ej: Familia)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SafeGreen,
                        unfocusedBorderColor = Navy700,
                        focusedLabelColor = SafeGreen,
                        unfocusedLabelColor = TextMuted,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = PremiumBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextMuted)
            }
        }
    )
}

@Composable
private fun AddBlacklistDialog(
    phone: String,
    reason: String,
    onPhoneChange: (String) -> Unit,
    onReasonChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Navy850,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = {
            Text("Agregar a Lista Negra", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                OutlinedTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    label = { Text("Número de teléfono") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DangerRed,
                        unfocusedBorderColor = Navy700,
                        focusedLabelColor = DangerRed,
                        unfocusedLabelColor = TextMuted,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    label = { Text("Motivo (ej: Molesto)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DangerRed,
                        unfocusedBorderColor = Navy700,
                        focusedLabelColor = DangerRed,
                        unfocusedLabelColor = TextMuted,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = PremiumBlue),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextMuted)
            }
        }
    )
}

private fun formatTimestamp(millis: Long): String {
    return try {
        val sdf = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
        sdf.format(Date(millis))
    } catch (_: Exception) {
        ""
    }
}
