package ar.com.patova.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.com.patova.ui.theme.*

@Composable
fun BlockedCallScreen(
    onBack: () -> Unit = {},
    onConfirmBlock: () -> Unit = {},
    onAllowAnyway: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BlockedBg)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Top label
            Text(
                text = "LLAMADA FRENADA",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                ),
                color = DangerRed,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // El patovica frenó la llamada en la puerta
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(RoundedCornerShape(50))
                    .background(DangerRedBg),
                contentAlignment = Alignment.Center
            ) {
                PatovaMascot(
                    modifier = Modifier.size(44.dp),
                    color = DangerRed
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Phone number
            Text(
                text = "+54 11 4XXX-XXXX",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                ),
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Reason pill
            PillTag(
                text = "Bot detectado · Comunidad Patova",
                backgroundColor = DangerRedBg,
                textColor = DangerRed
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Divider
            Divider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 0.5.dp,
                color = DividerColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Risk score section
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Score de riesgo",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Navy800)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.91f)
                            .fillMaxSize()
                            .clip(RoundedCornerShape(3.dp))
                            .background(DangerRed)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "91 / 100",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    color = DangerRed
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Reports section
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Reportado por",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                )
                Text(
                    text = "2.647 usuarios Patova",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = TextSecondary
                )
            }
        }

        // Buttons
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = onConfirmBlock,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DangerRed,
                    contentColor = TextPrimary
                )
            ) {
                Text(
                    text = "Confirmar bloqueo",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onAllowAnyway,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = TextMuted
                ),
                border = BorderStroke(0.5.dp, BorderSubtle)
            ) {
                Text(
                    text = "Permitir de todas formas",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 14.sp
                    ),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}
