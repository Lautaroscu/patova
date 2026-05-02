package ar.com.numguard.ui

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(onNavigateToHistory: () -> Unit = {}) {
    val context = LocalContext.current
    val isConfigured = remember { mutableStateOf(checkIfScreeningConfigured(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "NumGuard",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isConfigured.value) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.errorContainer
                }
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (isConfigured.value) {
                        "NumGuard esta configurado como app de screening"
                    } else {
                        "NumGuard NO esta configurado como app de screening"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isConfigured.value) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isConfigured.value) {
                        "NumGuard esta activo y filtrara las llamadas entrantes automaticamente."
                    } else {
                        "Sin este permiso, NumGuard no puede analizar ni bloquear llamadas. " +
                                "Activa el permiso de screening para empezar a protegerte."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isConfigured.value) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                openScreeningSettings(context)
                isConfigured.value = checkIfScreeningConfigured(context)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (isConfigured.value) {
                    "Abrir configuracion de screening"
                } else {
                    "Activar NumGuard como app de screening"
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onNavigateToHistory,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Historial de llamadas")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Instrucciones manuales:\n\n" +
                    "Ajustes > Aplicaciones > Aplicaciones predeterminadas > " +
                    "Identificador de llamadas y spam / Screening de llamadas > Selecciona NumGuard",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
