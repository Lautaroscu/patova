package ar.com.patova.ui.theme

import androidx.compose.ui.graphics.Color

// Identidad "patovica": negro-verdoso profundo + lima vivo, la misma paleta
// del landing (patova-mascot / hero section), en vez del navy-azul generico
// que tenia la app antes. Se mantienen los nombres historicos (Navy*, PremiumBlue)
// para no romper referencias en el resto de las pantallas: solo cambian los valores.
val Navy950 = Color(0xFF070A08)
val Navy900 = Color(0xFF0C120D)
val Navy850 = Color(0xFF0F1712)
val Navy800 = Color(0xFF161F18)
val Navy700 = Color(0xFF243428)

val BlockedBg = Navy900
val PremiumBg = Color(0xFF0D1A10)

val TextPrimary = Color(0xFFF2F7EF)
val TextSecondary = Color(0xFF9DB49A)
val TextMuted = Color(0xFF5E7A5C)
val TextMuted2 = Color(0xFF425C40)

val DangerRed = Color(0xFFE24B4A)
val SafeGreen = Color(0xFF1D9E75)
val WarningAmber = Color(0xFFEF9F27)

// Color hero de la marca: el lima del mascota/landing. PremiumBlue queda como
// alias para no tocar los archivos que ya lo referencian en el resto de la app.
val BrandLime = Color(0xFFB4E33D)
val PremiumBlue = BrandLime

val DangerRedBg = DangerRed.copy(alpha = 0.15f)
val SafeGreenBg = SafeGreen.copy(alpha = 0.15f)
val WarningAmberBg = WarningAmber.copy(alpha = 0.15f)
val PremiumBlueBg = PremiumBlue.copy(alpha = 0.15f)

val BorderSubtle = Color(0xFF1C2A1E)
val DividerColor = Color(0xFF1A2A1C)
