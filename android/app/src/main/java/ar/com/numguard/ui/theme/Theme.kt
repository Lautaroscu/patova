package ar.com.numguard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NumGuardColorScheme = darkColorScheme(
    primary = PremiumBlue,
    onPrimary = TextPrimary,
    primaryContainer = PremiumBlueBg,
    onPrimaryContainer = PremiumBlue,
    secondary = TextSecondary,
    onSecondary = Navy900,
    tertiary = WarningAmber,
    onTertiary = Navy900,
    tertiaryContainer = WarningAmberBg,
    onTertiaryContainer = WarningAmber,
    background = Navy900,
    onBackground = TextPrimary,
    surface = Navy850,
    onSurface = TextPrimary,
    surfaceVariant = Navy800,
    onSurfaceVariant = TextSecondary,
    error = DangerRed,
    onError = TextPrimary,
    errorContainer = DangerRedBg,
    onErrorContainer = DangerRed,
    outline = BorderSubtle,
    outlineVariant = DividerColor,
    inverseSurface = TextPrimary,
    inverseOnSurface = Navy900,
    inversePrimary = PremiumBlue,
    scrim = Navy950,
    surfaceTint = PremiumBlue.copy(alpha = 0.05f)
)

@Composable
fun NumGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NumGuardColorScheme,
        typography = PatovaTypography,
        content = content
    )
}
