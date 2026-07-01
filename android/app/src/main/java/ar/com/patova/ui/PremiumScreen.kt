package ar.com.patova.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.com.patova.ui.theme.*

@Composable
fun PremiumScreen(
    onActivate: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PremiumBg)
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Week label
        Text(
            text = "Tu primera semana con PATOVA",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.6.sp
            ),
            color = TextMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Big number
        Text(
            text = "47",
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Medium
            ),
            color = PremiumBlue,
            textAlign = TextAlign.Center
        )

        Text(
            text = "llamadas que nunca te molestaron",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp
            ),
            color = TextMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Breakdown card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Navy850,
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                BreakdownRow(
                    label = "País extranjero",
                    value = "23",
                    showDivider = true
                )
                BreakdownRow(
                    label = "Vishing detectado",
                    value = "11",
                    showDivider = true
                )
                BreakdownRow(
                    label = "Reiteradas no atendidas",
                    value = "5",
                    showDivider = true
                )
                BreakdownRow(
                    label = "Reconocimiento de firma",
                    value = "4",
                    showDivider = false
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Trial ending notice
        Text(
            text = "Tu prueba gratuita termina en 24 hs.",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
            color = TextMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // CTA Button
        Button(
            onClick = onActivate,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PremiumBlue,
                contentColor = TextPrimary
            )
        ) {
            Text(
                text = "Activar PATOVA Premium",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Price text
        Text(
            text = "\$1.000 / mes · 2 meses gratis si pagás anual",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
            color = TextMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // MercadoPago row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Pagá con",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = TextMuted2
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Mercado",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                ),
                // Azul de marca de Mercado Pago (no el accent de Patova):
                // este texto imita su isologo de dos tonos, no debe seguir el theme.
                color = Color(0xFF00AAE4)
            )
            Text(
                text = "Pago",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = Color(0xFF00B1EA)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    value: String,
    showDivider: Boolean
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = TextMuted
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = DangerRed
            )
        }
        if (showDivider) {
            Divider(
                modifier = Modifier.padding(vertical = 2.dp),
                thickness = 0.5.dp,
                color = DividerColor
            )
        }
    }
}
