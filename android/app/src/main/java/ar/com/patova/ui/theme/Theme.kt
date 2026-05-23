package ar.com.patova.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val PatovaColorScheme = darkColorScheme(
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
fun PatovaTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Navy900.toArgb()
            window.navigationBarColor = Navy900.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = PatovaColorScheme,
        typography = PatovaTypography,
        content = content
    )
}
