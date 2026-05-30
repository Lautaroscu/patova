package ar.com.patova.ui.premium

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
import ar.com.patova.ui.theme.DangerRed
import ar.com.patova.ui.theme.Navy800
import ar.com.patova.ui.theme.Navy850
import ar.com.patova.ui.theme.Navy900
import ar.com.patova.ui.theme.Navy950
import ar.com.patova.ui.theme.PremiumBlue
import ar.com.patova.ui.theme.SafeGreen
import ar.com.patova.ui.theme.TextMuted
import ar.com.patova.ui.theme.TextPrimary
import ar.com.patova.ui.theme.TextSecondary
import ar.com.patova.ui.theme.WarningAmber

private val GoldGradient = Brush.verticalGradient(
    listOf(
        Color(0xFFD4A843),
        Color(0xFFC8963E),
        Color(0xFFF0D070)
    )
)

private val GoldMetallicGradient = Brush.verticalGradient(
    listOf(
        Color(0xFFFFE9A6), // Champagne gold brilliant
        Color(0xFFD4AF37), // Metallic gold standard
        Color(0xFFA67C1E)  // Dark bronze/gold shadow
    )
)

private val SpaceDeepGradient = Brush.verticalGradient(
    listOf(
        Color(0xFF07090E), // Space deep navy
        Color(0xFF0F131F), // Space medium navy
        Color(0xFF171C2E)  // Space rich navy/indigo
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
    status: String? = null,
    viewModel: PaywallViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var emailInput by remember { mutableStateOf("TESTUSER2124106455@testuser.com") }
    var showSuccessDialog by remember { mutableStateOf(false) }

    LaunchedEffect(status) {
        if (status == "success") {
            viewModel.refreshSubscriptionStatus()
            showSuccessDialog = true
        }
    }

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
                    isLoading = uiState.isLoading,
                    canSubscribe = emailInput.isNotBlank() && emailInput.contains("@")
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

            Spacer(modifier = Modifier.height(16.dp))

            TrustBadges()
        }

        if (showSuccessDialog) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { showSuccessDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .border(2.dp, GoldMetallicGradient, RoundedCornerShape(20.dp)),
                    colors = CardDefaults.cardColors(containerColor = Navy950),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Icono de blindaje dorado premium
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(GoldMetallicGradient)
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(Navy900),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Shield,
                                    contentDescription = null,
                                    tint = Color(0xFFF0D070),
                                    modifier = Modifier.size(44.dp)
                                )
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color(0xFFF0D070),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "¡Ya sos Patova Premium!",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFF0D070),
                                letterSpacing = 0.3.sp
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "¡Felicitaciones! Tu protección activa está al 100%. Patova va a filtrar y rechazar de forma automática y silenciosa el spam por vos para que vuelvas a tener paz mental.",
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        Button(
                            onClick = { showSuccessDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4A843), contentColor = Navy950),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "¡Excelente!",
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
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
            .height(240.dp)
            .background(if (isPremium) GoldVioletGradient else SpaceDeepGradient),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isPremium) Navy950.copy(alpha = 0.55f)
                    else Color.Black.copy(alpha = 0.4f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                // Escudo o estrella dorada con borde fino y cristalino
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .border(1.dp, Color(0xFFD4A843).copy(alpha = 0.3f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPremium) Icons.Outlined.Star else Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = Color(0xFFF0D070),
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (isPremium) "Patova Premium" else "Recuperá la paz en tu teléfono",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp,
                        fontSize = if (isPremium) 22.sp else 20.sp
                    ),
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                if (isPremium && subscriptionStatus != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (subscriptionStatus == "ACTIVE") "Tu protección activa está al 100%"
                        else "Estado de la suscripción: $subscriptionStatus",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFF0D070)
                        ),
                        textAlign = TextAlign.Center
                    )
                    if (expiresAtFormatted != null) {
                        Text(
                            text = "Validez hasta el: $expiresAtFormatted",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = TextMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Olvidate de las llamadas spam a la hora de la siesta, de laburar o de cenar. Patova las ataja en silencio por vos.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        ),
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
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
                    feature = "Identificación de spam",
                    free = "Básica",
                    premium = "Filtro Automático + IA",
                    premiumColor = Color(0xFFF0D070)
                )
                FeatureRow(
                    feature = "Base de datos ENACOM",
                    free = "Manual básica",
                    premium = "Completa en tiempo real",
                    premiumColor = Color(0xFFF0D070)
                )
                FeatureRow(
                    feature = "Bloqueo de llamadas",
                    free = "Te avisa (tenés que colgar vos)",
                    premium = "Bloqueo silencioso inteligente",
                    premiumColor = Color(0xFFF0D070),
                    freeIcon = Icons.Outlined.Block,
                    freeColor = WarningAmber
                )
                FeatureRow(
                    feature = "Detección de estafas",
                    free = "",
                    premium = "Filtro Avanzado (Vishing)",
                    premiumColor = Color(0xFFF0D070),
                    freeIcon = Icons.Outlined.Close,
                    freeColor = DangerRed
                )
                FeatureRow(
                    feature = "Reportes comunitarios",
                    free = "Solo 3 diarios",
                    premium = "Ilimitados",
                    premiumColor = Color(0xFFF0D070)
                )
                FeatureRow(
                    feature = "Modo Offline",
                    free = "",
                    premium = "Hasta 7 días sin internet",
                    premiumColor = Color(0xFFF0D070),
                    freeIcon = Icons.Outlined.Close,
                    freeColor = DangerRed
                )
                FeatureRow(
                    feature = "Estadísticas",
                    free = "",
                    premium = "Dashboard total de ahorro",
                    premiumColor = Color(0xFFF0D070),
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
    isLoading: Boolean,
    canSubscribe: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
    ) {
        // Tarjeta del Plan Mensual (Diseño Elegante y Sobrio)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .border(1.dp, Color(0xFF1E2538), RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(
                containerColor = Navy800.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Plan Mensual",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = "Sin compromiso · Cancelá cuando quieras",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = TextMuted
                        )
                    }
                    Text(
                        text = "$1.000",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Menos de lo que sale un café al paso ($1.000/mes) para liberarte del spam.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Button(
                    onClick = onMonthly,
                    enabled = !isLoading && canSubscribe,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Navy900,
                        contentColor = Color(0xFFF0D070),
                        disabledContainerColor = Navy900.copy(alpha = 0.2f),
                        disabledContentColor = TextMuted
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD4A843).copy(alpha = 0.4f))
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color(0xFFF0D070),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Suscribirme · $1.000/mes",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Tarjeta del Plan Anual (El Destacado con Glow Dorado y Destaque)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, GoldMetallicGradient, RoundedCornerShape(16.dp))
                .background(Navy800.copy(alpha = 0.95f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Badge de Recomendado
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(GoldMetallicGradient)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "RECOMENDADO · EL MÁS ELEGIDO",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Navy950
                            )
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFD4A843).copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "AHORRÁ 34%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF0D070)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Plan Anual",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFFF0D070)
                        )
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
                            text = "equivale a $800/mes",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Equivale a menos de lo que cuesta un alfajor por mes para tener paz mental todo el año.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Button(
                    onClick = onAnnual,
                    enabled = !isLoading && canSubscribe,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD4A843),
                        contentColor = Navy950,
                        disabledContainerColor = Color(0xFFD4A843).copy(alpha = 0.2f),
                        disabledContentColor = TextMuted
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
                                fontWeight = FontWeight.ExtraBold
                            ),
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrustBadges() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TrustBadgeItem(icon = Icons.Outlined.Security, text = "Pago seguro vía MercadoPago")
        Spacer(modifier = Modifier.width(6.dp))
        TrustBadgeItem(icon = Icons.Outlined.Speed, text = "Activación inmediata")
        Spacer(modifier = Modifier.width(6.dp))
        TrustBadgeItem(icon = Icons.Outlined.Check, text = "Cancelación simple")
    }
}

@Composable
private fun TrustBadgeItem(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFFF0D070),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = TextMuted,
            maxLines = 1
        )
    }
}
