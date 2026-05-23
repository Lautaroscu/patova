package ar.com.patova.ui

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.com.patova.ui.theme.*

data class DashboardState(
    val blockedCountThisWeek: Int = 0,
    val verifiedCount: Int = 0,
    val suspiciousCount: Int = 0,
    val recentCalls: List<CallItemState> = emptyList()
)

data class CallItemState(
    val number: String,
    val status: String,
    val detail: String,
    val time: String,
    val isBlocked: Boolean = false,
    val isVerified: Boolean = false,
    val isSuspicious: Boolean = false
)

@Composable
fun DashboardScreen(
    state: DashboardState = DashboardState(
        blockedCountThisWeek = 47,
        verifiedCount = 12,
        suspiciousCount = 3,
        recentCalls = listOf(
            CallItemState("+54 11 4XXX-XXXX", "Bloqueado", "Bot wangiri", "hace 2m", isBlocked = true),
            CallItemState("+54 11 5YYY-YYYY", "Verificado", "ENACOM", "hace 1h", isVerified = true),
            CallItemState("+54 351 4ZZZ-ZZZ", "Sospechoso", "Score 67", "hace 3h", isSuspicious = true)
        )
    ),
    onNavigateToBlockedCall: () -> Unit = {},
    onNavigateToPremium: () -> Unit = {}
) {
    val context = LocalContext.current
    val isConfigured = remember { mutableStateOf(checkIfScreeningConfigured(context)) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Navy900)
            .verticalScroll(scrollState)
    ) {
        DashboardHeader()

        if (!isConfigured.value) {
            ScreeningWarningBanner(
                onConfigure = {
                    openScreeningSettings(context)
                    isConfigured.value = checkIfScreeningConfigured(context)
                }
            )
        }

        DashboardHero(
            blockedCount = state.blockedCountThisWeek,
            isConfigured = isConfigured.value
        )

        StatsRow(
            blocked = state.blockedCountThisWeek,
            verified = state.verifiedCount,
            suspicious = state.suspiciousCount
        )

        RecentCallsSection(
            calls = state.recentCalls,
            onCallClick = onNavigateToBlockedCall
        )

        PremiumCtaBanner(onClick = onNavigateToPremium)

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun DashboardHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Navy850)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = "PATOVA",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.2.sp
            ),
            color = TextPrimary
        )
        Text(
            text = "Protección activa",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 13.sp),
            color = TextMuted
        )
    }
}

@Composable
private fun ScreeningWarningBanner(onConfigure: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .clickable { onConfigure() },
        color = DangerRedBg,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = DangerRed,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Configuración pendiente",
                    style = MaterialTheme.typography.labelMedium,
                    color = DangerRed
                )
                Text(
                    text = "Tocá para activar Patova como app de screening",
                    style = MaterialTheme.typography.bodySmall,
                    color = DangerRed.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun DashboardHero(
    blockedCount: Int,
    isConfigured: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Navy800,
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(52.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Security,
                    contentDescription = null,
                    tint = PremiumBlue,
                    modifier = Modifier.size(52.dp)
                )
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = SafeGreen,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$blockedCount",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "llamadas bloqueadas esta semana",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    color = TextMuted
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            PillTag(
                text = "Protección activa",
                backgroundColor = SafeGreenBg,
                textColor = SafeGreen
            )
        }
    }
}

@Composable
private fun StatsRow(
    blocked: Int,
    verified: Int,
    suspicious: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Navy850)
    ) {
        StatItem(
            value = "$blocked",
            label = "Bloqueadas",
            valueColor = DangerRed,
            modifier = Modifier.weight(1f),
            showRightBorder = true
        )
        StatItem(
            value = "$verified",
            label = "Verificadas",
            valueColor = SafeGreen,
            modifier = Modifier.weight(1f),
            showRightBorder = true
        )
        StatItem(
            value = "$suspicious",
            label = "Sospechosas",
            valueColor = WarningAmber,
            modifier = Modifier.weight(1f),
            showRightBorder = false
        )
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    showRightBorder: Boolean = false
) {
    Box(modifier = modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = valueColor,
                textAlign = TextAlign.Center
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = TextMuted,
                textAlign = TextAlign.Center
            )
        }
        if (showRightBorder) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(0.6f)
                    .width(0.5.dp)
                    .background(BorderSubtle)
            )
        }
    }
}

@Composable
private fun RecentCallsSection(
    calls: List<CallItemState>,
    onCallClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = "ÚLTIMAS LLAMADAS",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 13.sp,
                letterSpacing = 0.6.sp
            ),
            color = TextMuted,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        if (calls.isEmpty()) {
            Text(
                text = "No hay llamadas recientes",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = TextSecondary,
                modifier = Modifier.padding(vertical = 16.dp, horizontal = 4.dp)
            )
        } else {
            calls.forEach { call ->
                val icon = when {
                    call.isBlocked -> Icons.Rounded.Close
                    call.isVerified -> Icons.Rounded.Check
                    else -> Icons.Rounded.Warning
                }
                val iconBgColor = when {
                    call.isBlocked -> DangerRedBg
                    call.isVerified -> SafeGreenBg
                    else -> WarningAmberBg
                }
                val iconColor = when {
                    call.isBlocked -> DangerRed
                    call.isVerified -> SafeGreen
                    else -> WarningAmber
                }
                CallItem(
                    icon = icon,
                    iconBgColor = iconBgColor,
                    iconColor = iconColor,
                    number = call.number,
                    status = call.status,
                    statusColor = iconColor,
                    detail = call.detail,
                    time = call.time,
                    onClick = onCallClick
                )
            }
        }
    }
}

@Composable
private fun CallItem(
    icon: ImageVector,
    iconBgColor: Color,
    iconColor: Color,
    number: String,
    status: String,
    statusColor: Color,
    detail: String,
    time: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(12.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = number,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontSize = 13.sp
                ),
                color = TextSecondary
            )
            Row {
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = statusColor
                )
                Text(
                    text = " · $detail",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = TextMuted
                )
            }
        }

        Text(
            text = time,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                color = TextMuted2
            )
        )
    }

    Divider(
        modifier = Modifier.padding(start = 32.dp),
        thickness = 0.5.dp,
        color = BorderSubtle
    )
}

@Composable
private fun PremiumCtaBanner(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clickable { onClick() },
        color = Navy800,
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Descubrí PATOVA Premium",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 15.sp
                    ),
                    color = PremiumBlue
                )
                Text(
                    text = "Estadísticas avanzadas y protección total",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = TextMuted
                )
            }
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = PremiumBlue.copy(alpha = 0.6f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun PillTag(
    text: String,
    backgroundColor: Color,
    textColor: Color
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = backgroundColor
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
            color = textColor
        )
    }
}

private fun checkIfScreeningConfigured(context: Context): Boolean {
    return try {
        val roleManager = context.getSystemService(RoleManager::class.java)
        roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
    } catch (_: Exception) {
        false
    }
}

private fun openScreeningSettings(context: Context) {
    try {
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING)) {
            val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
            context.startActivity(intent)
            return
        }
    } catch (_: Exception) {
    }

    try {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        } else {
            Intent(Settings.ACTION_SETTINGS)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}
