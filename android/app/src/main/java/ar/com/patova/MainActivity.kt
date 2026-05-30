package ar.com.patova

import android.Manifest
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import ar.com.patova.ui.BlockedCallScreen
import ar.com.patova.ui.DashboardScreen
import ar.com.patova.ui.NetworkScreen
import ar.com.patova.ui.disclosure.DisclosureDeniedScreen
import ar.com.patova.ui.disclosure.DisclosureScreen
import ar.com.patova.ui.disclosure.RequestingPermissionsScreen
import ar.com.patova.ui.history.HistoryScreen
import ar.com.patova.ui.premium.PaywallScreen
import ar.com.patova.ui.settings.SettingsScreen
import ar.com.patova.ui.theme.Navy850
import ar.com.patova.ui.theme.PatovaTheme
import ar.com.patova.ui.theme.PremiumBlue
import ar.com.patova.ui.theme.TextMuted2
import ar.com.patova.ui.theme.TextPrimary
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

private const val PREFS_NAME = "patova_prefs"
private const val KEY_DISCLOSURE_ACCEPTED = "disclosure_accepted"

private enum class OnboardingState {
    DISCLOSURE,
    REQUESTING,
    DENIED,
    APP
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val prefs: SharedPreferences by lazy {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val onboardingState = mutableStateOf(OnboardingState.DISCLOSURE)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.d("Patova", "Permiso de notificaciones concedido: $granted")
        requestPhonePermissions()
    }

    private val phonePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        Log.d("Patova", "Permisos de telefono concedidos: $allGranted")
        if (allGranted) {
            requestCallScreeningRole()
        } else {
            onboardingState.value = OnboardingState.DENIED
        }
    }

    private val roleRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.d("Patova", "Rol de Call Screening concedido por el usuario")
        } else {
            Log.w("Patova", "Rol de Call Screening RECHAZADO por el usuario")
        }
        showApp()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("Patova", "MainActivity: onCreate")

        determineInitialState()

        setContent {
            PatovaTheme {
                when (onboardingState.value) {
                    OnboardingState.DISCLOSURE -> DisclosureScreen(
                        onAccept = {
                            prefs.edit().putBoolean(KEY_DISCLOSURE_ACCEPTED, true).apply()
                            onboardingState.value = OnboardingState.REQUESTING
                            requestNotificationPermissionIfNeeded()
                        }
                    )
                    OnboardingState.REQUESTING -> RequestingPermissionsScreen()
                    OnboardingState.DENIED -> DisclosureDeniedScreen()
                    OnboardingState.APP -> PatovaApp()
                }
            }
        }
    }

    @Composable
    private fun PatovaApp() {
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
                composable(
                    route = "premium",
                    deepLinks = listOf(
                        navDeepLink {
                            uriPattern = "patova://checkout/{status}"
                        }
                    )
                ) { backStackEntry ->
                    val status = backStackEntry.arguments?.getString("status")
                    PaywallScreen(
                        status = status,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        intent?.data?.let { uri ->
            Log.d("Patova", "Deep link recibido: $uri")
            // Aquí podrías navegar o disparar un evento global de refresco
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.d("Patova", "Solicitando permiso de notificaciones...")
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                requestPhonePermissions()
            }
        } else {
            requestPhonePermissions()
        }
    }

    private fun requestPhonePermissions() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            permissionsToRequest.add(Manifest.permission.ANSWER_PHONE_CALLS)
        }

        val allGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            requestCallScreeningRole()
        } else {
            Log.d("Patova", "Solicitando permisos de telefono...")
            phonePermissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun requestCallScreeningRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
            val isRoleHeld = roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)

            Log.d("Patova", "Tiene rol de Call Screening: $isRoleHeld")

            if (!isRoleHeld) {
                Log.d("Patova", "Solicitando rol de Call Screening...")
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                roleRequestLauncher.launch(intent)
            } else {
                showApp()
            }
        } else {
            Log.w("Patova", "Android < Q, no se puede solicitar rol de Call Screening")
            showApp()
        }
    }

    override fun onResume() {
        super.onResume()
        if (onboardingState.value == OnboardingState.DENIED && allCriticalPermissionsGranted()) {
            onboardingState.value = OnboardingState.APP
        }
    }

    private fun determineInitialState() {
        val disclosureAccepted = prefs.getBoolean(KEY_DISCLOSURE_ACCEPTED, false)

        if (disclosureAccepted) {
            if (allCriticalPermissionsGranted()) {
                onboardingState.value = OnboardingState.APP
                requestNotificationPermissionIfNeeded()
            } else {
                // Si ya aceptó la divulgación (backup de Google) pero se reiniciaron los permisos por reinstalación,
                // le volvemos a pedir los permisos dentro de la app en lugar de mandarlo directo a Ajustes.
                onboardingState.value = OnboardingState.REQUESTING
                requestNotificationPermissionIfNeeded()
            }
        }
    }

    private fun showApp() {
        onboardingState.value = OnboardingState.APP
    }

    private fun allCriticalPermissionsGranted(): Boolean {
        val phonePerms = listOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_CONTACTS
        )
        return phonePerms.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}
