package ar.com.numguard

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ar.com.numguard.ui.BlockedCallScreen
import ar.com.numguard.ui.DashboardScreen
import ar.com.numguard.ui.NetworkScreen
import ar.com.numguard.ui.PremiumScreen
import ar.com.numguard.ui.history.HistoryScreen
import ar.com.numguard.ui.settings.SettingsScreen
import ar.com.numguard.ui.theme.Navy850
import ar.com.numguard.ui.theme.NumGuardTheme
import ar.com.numguard.ui.theme.PremiumBlue
import ar.com.numguard.ui.theme.TextMuted2
import ar.com.numguard.ui.theme.TextPrimary
import dagger.hilt.android.AndroidEntryPoint

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem("dashboard", "Inicio", Icons.Rounded.Home),
    BottomNavItem("history", "Historial", Icons.Rounded.History),
    BottomNavItem("network", "Red", Icons.Rounded.Language),
    BottomNavItem("settings", "Config", Icons.Rounded.Settings)
)

private val tabRoutes = bottomNavItems.map { it.route }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.d("NumGuard", "Permiso de notificaciones concedido: $granted")
    }

    private val roleRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.d("NumGuard", "Rol de Call Screening concedido por el usuario")
        } else {
            Log.w("NumGuard", "Rol de Call Screening RECHAZADO por el usuario")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("NumGuard", "MainActivity: onCreate")

        requestNotificationPermissionIfNeeded()
        requestCallScreeningRole()

        setContent {
            NumGuardTheme {
                NumGuardApp()
            }
        }
    }

    @Composable
    private fun NumGuardApp() {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route
        val showBottomBar = currentRoute in tabRoutes

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Navy850,
            bottomBar = {
                if (showBottomBar) {
                    NavigationBar(
                        containerColor = Navy850,
                        contentColor = TextPrimary
                    ) {
                        bottomNavItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    if (currentRoute != item.route) {
                                        navController.navigate(item.route) {
                                            popUpTo("dashboard") { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PremiumBlue,
                                    selectedTextColor = PremiumBlue,
                                    unselectedIconColor = TextMuted2,
                                    unselectedTextColor = TextMuted2,
                                    indicatorColor = PremiumBlue.copy(alpha = 0.12f)
                                )
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = "dashboard",
                modifier = Modifier.padding(paddingValues)
            ) {
                composable("dashboard") {
                    DashboardScreen(
                        onNavigateToBlockedCall = {
                            navController.navigate("blocked_call")
                        },
                        onNavigateToPremium = {
                            navController.navigate("premium")
                        }
                    )
                }
                composable("history") {
                    HistoryScreen(onBack = null)
                }
                composable("network") {
                    NetworkScreen()
                }
                composable("settings") {
                    SettingsScreen()
                }
                composable("blocked_call") {
                    BlockedCallScreen(
                        onBack = { navController.popBackStack() },
                        onConfirmBlock = { navController.popBackStack() },
                        onAllowAnyway = { navController.popBackStack() }
                    )
                }
                composable("premium") {
                    PremiumScreen(
                        onActivate = { navController.popBackStack() }
                    )
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("NumGuard", "Solicitando permiso de notificaciones...")
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun requestCallScreeningRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
            val isRoleHeld = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)

            Log.d("NumGuard", "Tiene rol de Call Screening: $isRoleHeld")

            if (!isRoleHeld) {
                Log.d("NumGuard", "Solicitando rol de Call Screening...")
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                roleRequestLauncher.launch(intent)
            }
        } else {
            Log.w("NumGuard", "La versión de Android es menor a Q, no se puede solicitar el rol de Call Screening")
        }
    }
}
