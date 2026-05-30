package ar.com.patova.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ar.com.patova.data.api.TopReportedNumber
import ar.com.patova.ui.theme.*

@Composable
fun NetworkScreen(
    viewModel: NetworkViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

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
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "RED DE CONFIANZA",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.2.sp
                        ),
                        color = TextPrimary
                    )
                    Text(
                        text = "Protección comunitaria en tiempo real",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
                        color = TextMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Badge de estado de datos: LIVE (Conexión Real) vs LOCAL (Social proof fallback/offline)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (uiState.isRealData) SafeGreen.copy(alpha = 0.15f)
                            else PremiumBlue.copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (uiState.isRealData) "LIVE" else "LOCAL",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (uiState.isRealData) SafeGreen else PremiumBlue
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Indicador de Carga si está cargando por primera vez
        if (uiState.isLoading && uiState.topReported.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PremiumBlue)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(PremiumBlueBg, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Language,
                        contentDescription = null,
                        tint = PremiumBlue,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = uiState.totalUsers,
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary
                )
                Text(
                    text = "usuarios protegiendo la red hoy",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tarjetas de Estadísticas Principales
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NetworkStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Update,
                    iconColor = SafeGreen,
                    iconBg = SafeGreenBg,
                    title = uiState.lastUpdate,
                    subtitle = "Última act."
                )
                NetworkStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.Share,
                    iconColor = DangerRed,
                    iconBg = DangerRedBg,
                    title = uiState.totalReports,
                    subtitle = "Reportes"
                )
                NetworkStatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Rounded.People,
                    iconColor = WarningAmber,
                    iconBg = WarningAmberBg,
                    title = uiState.newToday,
                    subtitle = "Bloqueos hoy"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Tarjeta de Contribución Personal
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = Navy800,
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tu contribución",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Reportes validados", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Text(text = uiState.userReportsCount, color = PremiumBlue, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Rango comunitario", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                        Text(text = uiState.userRank, color = WarningAmber, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PremiumBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Invitar a la comunidad", modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }

            // Sección de Números Más Reportados en Argentina
            if (uiState.topReported.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                TopReportedList(numbers = uiState.topReported)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TopReportedList(numbers: List<TopReportedNumber>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "NÚMEROS MÁS REPORTADOS EN ARGENTINA",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 12.sp,
                letterSpacing = 0.6.sp,
                color = TextMuted
            ),
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Navy800,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                numbers.forEachIndexed { index, number ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${index + 1}.",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFF0D070),
                                modifier = Modifier.width(24.dp)
                            )
                            Column {
                                Text(
                                    text = number.numberE164Masked,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Score de Spam: ${number.spamScore}%",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = TextMuted
                                )
                            }
                        }

                        // Badge de Denuncias
                        val isSpam = number.status == "SPAM"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isSpam) DangerRed.copy(alpha = 0.15f)
                                    else WarningAmber.copy(alpha = 0.15f)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${number.reportCount} denuncias",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSpam) DangerRed else WarningAmber
                                )
                            )
                        }
                    }
                    if (index < numbers.size - 1) {
                        Divider(
                            color = Color(0xFF1E2538),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NetworkStatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconColor: androidx.compose.ui.graphics.Color,
    iconBg: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String
) {
    Surface(
        modifier = modifier,
        color = Navy800,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(iconBg, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = TextMuted
            )
        }
    }
}
