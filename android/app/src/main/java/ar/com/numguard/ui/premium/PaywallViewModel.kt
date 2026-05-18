package ar.com.numguard.ui.premium

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.com.numguard.data.api.CreatePreferenceRequest
import ar.com.numguard.data.api.NumGuardApi
import ar.com.numguard.data.api.SubscriptionMeResponse
import ar.com.numguard.data.local.PremiumCacheManager
import ar.com.numguard.domain.DeviceIdProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

data class PaywallUiState(
    val isLoading: Boolean = false,
    val isPremium: Boolean = false,
    val initPointUrl: String? = null,
    val errorMessage: String? = null,
    val subscriptionStatus: String? = null,
    val expiresAtFormatted: String? = null,
    val premiumAvailableOffline: Boolean = false
)

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val api: NumGuardApi,
    private val premiumCache: PremiumCacheManager,
    private val deviceIdProvider: DeviceIdProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaywallUiState())
    val uiState: StateFlow<PaywallUiState> = _uiState.asStateFlow()

    init {
        checkCachedState()
        refreshSubscriptionStatus()
    }

    private fun checkCachedState() {
        val offlineAvailable = premiumCache.premiumAvailableOffline
        val offlinePremium = if (offlineAvailable) premiumCache.isPremium else false
        _uiState.value = _uiState.value.copy(
            isPremium = offlinePremium,
            subscriptionStatus = if (offlinePremium) "ACTIVE (offline)" else null,
            premiumAvailableOffline = offlineAvailable
        )
    }

    fun refreshSubscriptionStatus() {
        val userId = premiumCache.userId.ifEmpty {
            val deviceId = deviceIdProvider.getDeviceIdHash()
            "usr_${deviceId.takeLast(12)}"
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val response: SubscriptionMeResponse = api.getSubscriptionMe(userId)
                val isPremium = response.isPremium
                val sub = response.subscription

                val expiresAtMillis = if (sub?.expiresAt != null) {
                    try {
                        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                        fmt.timeZone = TimeZone.getTimeZone("UTC")
                        fmt.parse(sub.expiresAt.substringBefore("+").substringBefore("Z"))?.time ?: 0L
                    } catch (_: Exception) {
                        0L
                    }
                } else 0L

                premiumCache.cachePremiumState(
                    isPremium = isPremium,
                    status = sub?.status ?: "INACTIVE",
                    expiresAt = expiresAtMillis,
                    userId = response.userId
                )

                val expiresFormatted = if (sub?.expiresAt != null) {
                    try {
                        val iso = sub.expiresAt.substringBefore("+").substringBefore("Z")
                        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                        val parsed = fmt.parse(iso)
                        val outFmt = SimpleDateFormat("dd/MM/yyyy", Locale.US)
                        parsed?.let { outFmt.format(it) }
                    } catch (_: Exception) {
                        sub.expiresAt
                    }
                } else null

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isPremium = isPremium,
                    subscriptionStatus = sub?.status,
                    expiresAtFormatted = expiresFormatted,
                    premiumAvailableOffline = premiumCache.premiumAvailableOffline
                )
            } catch (e: Exception) {
                val offlineAvailable = premiumCache.premiumAvailableOffline
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = if (offlineAvailable) null else "Sin conexion. Verifica tu internet.",
                    isPremium = offlineAvailable && premiumCache.isPremium,
                    premiumAvailableOffline = offlineAvailable
                )
            }
        }
    }

    fun activatePremium(planId: String) {
        val userId = premiumCache.userId.ifEmpty {
            val deviceId = deviceIdProvider.getDeviceIdHash()
            "usr_${deviceId.takeLast(12)}"
        }
        val email = premiumCache.userEmail.ifEmpty { "user@example.com" }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val response = api.createPreference(
                    CreatePreferenceRequest(
                        planId = planId,
                        email = email,
                        userId = userId
                    )
                )
                premiumCache.cachePremiumState(
                    isPremium = false,
                    status = "PENDING",
                    expiresAt = 0L,
                    userId = response.userId
                )
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    initPointUrl = response.initPoint
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Error al iniciar el pago: ${e.message}"
                )
            }
        }
    }

    fun onCheckoutComplete() {
        _uiState.value = _uiState.value.copy(initPointUrl = null)
        refreshSubscriptionStatus()
    }

    fun setUserEmail(email: String) {
        premiumCache.userEmail = email
    }
}
