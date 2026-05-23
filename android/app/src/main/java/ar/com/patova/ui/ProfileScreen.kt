package ar.com.patova.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import ar.com.patova.ui.theme.*

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = hiltViewModel()) {
    val scrollState = rememberScrollState()
    val blockNonContacts by viewModel.blockNonContacts.collectAsState()
    val allowedPrefixes by viewModel.allowedPrefixes.collectAsState()
    val blockedPrefixes by viewModel.blockedPrefixes.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Navy900)
            .verticalScroll(scrollState)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Navy850)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(
                text = "PERFIL",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.2.sp
                ),
                color = TextPrimary
            )
            Text(
                text = "Gestión de cuenta y preferencias",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                color = TextMuted,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Profile Header
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Navy800),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Usuario PATOVA",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = PremiumBlueBg
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = PremiumBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Plan Gratuito",
                        style = MaterialTheme.typography.labelSmall,
                        color = PremiumBlue
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Settings list
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "PREFERENCIAS DE BLOQUEO",
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                color = TextMuted,
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )

            // Block Non-Contacts Toggle
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                color = Navy800,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(DangerRedBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Rounded.Security, contentDescription = null, tint = DangerRed, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Bloquear Desconocidos", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = TextPrimary)
                        Text(text = "Solo permitir contactos", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Switch(
                        checked = blockNonContacts,
                        onCheckedChange = { viewModel.updateBlockNonContacts(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = PremiumBlue, checkedTrackColor = PremiumBlueBg)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Allowed Prefixes Input
            OutlinedTextField(
                value = allowedPrefixes,
                onValueChange = { viewModel.updateAllowedPrefixes(it) },
                label = { Text("Prefijos Permitidos (ej: +54911, 011)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SafeGreen,
                    unfocusedBorderColor = Navy700,
                    focusedLabelColor = SafeGreen,
                    unfocusedLabelColor = TextMuted,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                textStyle = MaterialTheme.typography.bodyMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            // Blocked Prefixes Input
            OutlinedTextField(
                value = blockedPrefixes,
                onValueChange = { viewModel.updateBlockedPrefixes(it) },
                label = { Text("Prefijos Bloqueados (ej: +44, 0800)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DangerRed,
                    unfocusedBorderColor = Navy700,
                    focusedLabelColor = DangerRed,
                    unfocusedLabelColor = TextMuted,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                textStyle = MaterialTheme.typography.bodyMedium
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Premium CTA
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            color = Navy800,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Mejorar a Premium", style = MaterialTheme.typography.titleMedium, color = PremiumBlue)
                    Text(text = "Protección avanzada impulsada por IA", style = MaterialTheme.typography.bodySmall, color = TextMuted)
                }
                Icon(imageVector = Icons.Rounded.ChevronRight, contentDescription = null, tint = TextMuted2)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: androidx.compose.ui.graphics.Color,
    iconBg: androidx.compose.ui.graphics.Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
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
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = TextPrimary)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Icon(imageVector = Icons.Rounded.ChevronRight, contentDescription = null, tint = TextMuted2)
        }
    }
}
