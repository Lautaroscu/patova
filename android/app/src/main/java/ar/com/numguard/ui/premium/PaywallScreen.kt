package ar.com.numguard.ui.premium

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import ar.com.numguard.ui.theme.DangerRed
import ar.com.numguard.ui.theme.Navy800
import ar.com.numguard.ui.theme.Navy850
import ar.com.numguard.ui.theme.Navy900
import ar.com.numguard.ui.theme.Navy950
import ar.com.numguard.ui.theme.PremiumBlue
import ar.com.numguard.ui.theme.SafeGreen
import ar.com.numguard.ui.theme.TextMuted
import ar.com.numguard.ui.theme.TextPrimary
import ar.com.numguard.ui.theme.TextSecondary
import ar.com.numguard.ui.theme.WarningAmber

private val GoldGradient = Brush.verticalGradient(
    listOf(
        Color(0xFFD4A843),
        Color(0xFFC8963E),
        Color(0xFFF0D070)
    )
)

private val VioletGradient = Brush.verticalGradient(
    listOf(
        Color(0xFF7C3AED),
        Color(0xFF5B21B6),
        Color(0xFF8B5CF6)
    )
)

private val GoldVioletGradient = Brush.horizontalGradient(
    listOf(
        Color(0xFF7C3AED),
        Color(0xFFA855F7),
        Color(0xFFD4A843),
        Color(0xFFF0D070)
    )
)

private val GlassBg = Color(0x0DFFFFFF)

@Composable
fun PaywallScreen(
    viewModel: PaywallViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var emailInput by remember { mutableStateOf("") }

    LaunchedEffect(uiState.initPointUrl) {
        val url = uiState.initPointUrl ?: return@LaunchedEffect
        try {
            val builder = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .setColorScheme(CustomTabsIntent.COLOR_SCHEME_DARK)
                .build()
            builder.launchUrl(context, Uri.parse(url))
        } catch (_: Exception) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }
        viewModel.onCheckoutComplete()
    }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Navy950, Navy900, Navy850)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HeroSection(
                isPremium = uiState.isPremium,
                subscriptionStatus = uiState.subscriptionStatus,
                expiresAtFormatted = uiState.expiresAtFormatted
            )

            if (uiState.isPremium) {
                ActiveSubscriptionCard(
                    status = uiState.subscriptionStatus ?: "ACTIVE",
                    expiresAt = uiState.expiresAtFormatted,
                    isOffline = uiState.premiumAvailableOffline
                )
            } else {
                FeatureComparisonSection()

                Spacer(modifier = Modifier.height(12.dp))

                EmailInputSection(
                    email = emailInput,
                    onEmailChange = { emailInput = it }
                )

                Spacer(modifier = Modifier.height(10.dp))

                PlanCards(
                    onMonthly = {
                        if (emailInput.isNotBlank()) {
                            viewModel.setUserEmail(emailInput.trim())
                        }
                        viewModel.activatePremium("premium_monthly")
                    },
                    onAnnual = {
                        if (emailInput.isNotBlank()) {
                            viewModel.setUserEmail(emailInput.trim())
                        }
                        viewModel.activatePremium("premium_annual")
                    },
                    isLoading = uiState.isLoading
                )
            }

            AnimatedVisibility(
                visible = uiState.errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                uiState.errorMessage?.let { error ->
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = WarningAmber,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Pagos procesados por MercadoPago · Cancelacion sin costo",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}

@Composable
private fun HeroSection(
    isPremium: Boolean,
    subscriptionStatus: String?,
    expiresAtFormatted: String?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(GoldVioletGradient),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Navy950.copy(alpha = 0.55f)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = if (isPremium) Icons.Outlined.Star
                    else Icons.Outlined.Shield,
                    contentDescription = null,
                    tint = Color(0xFFF0D070),
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isPremium) "PATOVA Premium" else "Desbloquea PATOVA Premium",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                if (isPremium && subscriptionStatus != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (subscriptionStatus == "ACTIVE") "Tu suscripcion esta activa"
                        else "Estado: $subscriptionStatus",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 14.sp,
                            color = Color(0xFFF0D070)
                        ),
                        textAlign = TextAlign.Center
                    )
                    if (expiresAtFormatted != null) {
                        Text(
                            text = "Vence: $expiresAtFormatted",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp
                            ),
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Proteccion total contra llamadas spam y estafas",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 14.sp,
                            color = Color(0xFFD4A843)
                        ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveSubscriptionCard(
    status: String,
    expiresAt: String?,
    isOffline: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Navy800),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = SafeGreen,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Suscripcion $status",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Medium,
                        color = SafeGreen
                    )
                )
            }

            if (expiresAt != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Valida hasta: $expiresAt",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = TextSecondary
                )
            }

            if (isOffline) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Modo offline activo · Sincroniza en menos de 7 dias",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = WarningAmber
                )
            }
        }
    }
}

@Composable
private fun FeatureComparisonSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = "COMPARATIVA DE FUNCIONES",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 13.sp,
                letterSpacing = 0.6.sp,
                color = TextMuted
            ),
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Navy800.copy(alpha = 0.7f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                FeatureRow(
                    feature = "Identificacion de spam",
                    free = "Basica",
                    premium = "Avanzada + IA",
                    premiumColor = SafeGreen
                )
                FeatureRow(
                    feature = "Base de datos ENACOM",
                    free = "Limitada",
                    premium = "Completa",
                    premiumColor = SafeGreen
                )
                FeatureRow(
                    feature = "Bloqueo automatico",
                    free = "Solo numeros conocidos",
                    premium = "100% personalizable",
                    premiumColor = SafeGreen
                )
                FeatureRow(
                    feature = "Deteccion de vishing",
                    free = "",
                    premium = "Incluida",
                    premiumColor = SafeGreen,
                    freeIcon = Icons.Outlined.Close,
                    freeColor = DangerRed
                )
                FeatureRow(
                    feature = "Reportes comunitarios",
                    free = "3/dia",
                    premium = "Ilimitados",
                    premiumColor = SafeGreen
                )
                FeatureRow(
                    feature = "Modo offline Premium",
                    free = "",
                    premium = "7 dias sin conexion",
                    premiumColor = SafeGreen,
                    freeIcon = Icons.Outlined.Close,
                    freeColor = DangerRed
                )
                FeatureRow(
                    feature = "Estadisticas avanzadas",
                    free = "",
                    premium = "Dashboard completo",
                    premiumColor = SafeGreen,
                    freeIcon = Icons.Outlined.Close,
                    freeColor = DangerRed
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(
    feature: String,
    free: String,
    premium: String,
    premiumColor: Color,
    freeIcon: ImageVector = if (free.isNotBlank()) Icons.Outlined.Check else Icons.Outlined.Close,
    freeColor: Color = if (free.isNotBlank()) TextSecondary else DangerRed
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = feature,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            ),
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )

        Row(
            modifier = Modifier.width(100.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = freeIcon,
                contentDescription = null,
                tint = freeColor,
                modifier = Modifier.size(14.dp)
            )
            if (free.isNotBlank()) {
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = free,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = freeColor
                )
            }
        }

        Row(
            modifier = Modifier.width(120.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = premiumColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = premium,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = premiumColor
            )
        }
    }
}

@Composable
private fun EmailInputSection(
    email: String,
    onEmailChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
    ) {
        Text(
            text = "Tu email para el pago",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 12.sp,
                color = TextMuted
            ),
            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, PremiumBlue.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                .background(Navy850)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = email,
                onValueChange = onEmailChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = TextPrimary,
                    fontSize = 15.sp
                ),
                decorationBox = { innerTextField ->
                    if (email.isEmpty()) {
                        Text(
                            text = "tu@email.com",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextMuted,
                                fontSize = 15.sp
                            )
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

@Composable
private fun PlanCards(
    onMonthly: () -> Unit,
    onAnnual: () -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = Navy800.copy(alpha = 0.8f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Plan Mensual",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = "Sin compromiso · Cancela cuando quieras",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = TextMuted
                        )
                    }
                    Text(
                        text = "$1.000",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = PremiumBlue
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onMonthly,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PremiumBlue,
                        contentColor = TextPrimary
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = TextPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Suscribirme · $1.000/mes",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .border(1.5.dp, Color(0xFFD4A843).copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(
                containerColor = Navy800.copy(alpha = 0.9f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Plan Anual",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFD4A843).copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "-34%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF0D070)
                                    )
                                )
                            }
                        }
                        Text(
                            text = "2 meses gratis · Mejor valor",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = TextMuted
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "$9.600",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF0D070)
                            )
                        )
                        Text(
                            text = "/ano ($800/mes)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onAnnual,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD4A843),
                        contentColor = Navy950
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Navy950,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Suscribirme · $800/mes (anual)",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
