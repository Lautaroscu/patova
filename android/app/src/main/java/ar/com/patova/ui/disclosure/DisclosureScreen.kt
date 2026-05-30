package ar.com.patova.ui.disclosure

import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotInterested
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ar.com.patova.ui.theme.PremiumBlue
import ar.com.patova.ui.theme.SafeGreen
import ar.com.patova.ui.theme.TextMuted2
import ar.com.patova.ui.theme.TextSecondary
import ar.com.patova.ui.theme.WarningAmber

@Composable
fun DisclosureScreen(
    onAccept: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        DisclosureContent(onAccept = onAccept)
    }
}

@Composable
fun DisclosureDeniedScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        DeniedContent()
    }
}

@Composable
fun RequestingPermissionsScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = PremiumBlue
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Configurando proteccion",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Solicitando los permisos necesarios para proteger tus llamadas...",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DisclosureContent(onAccept: () -> Unit) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Rounded.Shield,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = PremiumBlue
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Tu privacidad es nuestra prioridad",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Antes de comenzar, necesitamos que entiendas como Patova protege tus datos.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        DisclosureCard(
            icon = Icons.Rounded.Phone,
            iconTint = WarningAmber,
            title = "Análisis local de llamadas",
            description = "Patova utiliza el acceso a tu Agenda y Call Log exclusivamente de forma local para identificar llamadas. Tus contactos nunca salen de tu dispositivo."
        )

        Spacer(modifier = Modifier.height(16.dp))

        DisclosureCard(
            icon = Icons.Rounded.Security,
            iconTint = PremiumBlue,
            title = "Hashes criptográficos SHA-256",
            description = "Antes de consultar nuestra base de datos en la nube, los números se transforman en hashes anónimos. Tu número original es privado e irrecuperable."
        )

        Spacer(modifier = Modifier.height(16.dp))

        DisclosureCard(
            icon = Icons.Rounded.Lock,
            iconTint = SafeGreen,
            title = "Cero datos personales",
            description = "Ningun numero telefonico en crudo es almacenado en nuestros servidores. Ningun dato personal identificable es guardado, compartido ni vendido a terceros bajo ninguna circunstancia."
        )

        Spacer(modifier = Modifier.height(16.dp))

        DisclosureCard(
            icon = Icons.Rounded.CheckCircle,
            iconTint = SafeGreen,
            title = "Encriptacion de extremo a extremo",
            description = "Toda comunicacion entre tu dispositivo y los servidores de Patova viaja encriptada mediante TLS. Los hashes nunca se comparten ni venden."
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onAccept,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PremiumBlue
            )
        ) {
            Text(
                text = "Aceptar y Continuar",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { (context as? android.app.Activity)?.finish() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text(
                text = "Denegar",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Al aceptar, entiendes que Patova requiere estos permisos exclusivamente para proteccion contra fraudes telefonicos.",
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted2,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun DisclosureCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = iconTint
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun DeniedContent() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.NotInterested,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Permisos necesarios",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Patova requiere acceso a llamadas y contactos para funcionar. Sin estos permisos, la aplicacion no puede protegerte contra fraudes telefonicos.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Puedes otorgar los permisos manualmente desde los Ajustes del sistema en cualquier momento.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted2,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PremiumBlue
            )
        ) {
            Text(
                text = "Abrir Ajustes",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { (context as? android.app.Activity)?.finish() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = TextSecondary
            )
        ) {
            Text(
                text = "Salir de la aplicacion",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
