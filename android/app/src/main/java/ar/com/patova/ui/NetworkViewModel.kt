package ar.com.patova.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.com.patova.data.api.PatovaApi
import ar.com.patova.data.api.TopReportedNumber
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NetworkUiState(
    val isLoading: Boolean = true,
    val isRealData: Boolean = false,
    val totalUsers: String = "14,592",
    val lastUpdate: String = "24m",
    val totalReports: String = "+8.5k",
    val newToday: String = "1.2k",
    val userReportsCount: String = "12",
    val userRank: String = "Centinela",
    val topReported: List<TopReportedNumber> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class NetworkViewModel @Inject constructor(
    private val patovaApi: PatovaApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(NetworkUiState())
    val uiState: StateFlow<NetworkUiState> = _uiState.asStateFlow()

    init {
        fetchNetworkStats()
    }

    fun fetchNetworkStats() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val stats = patovaApi.getStats()
                // Umbral de 15 reportes reales para activar la visualización real
                val threshold = 15
                if (stats.totalReports >= threshold) {
                    _uiState.value = NetworkUiState(
                        isLoading = false,
                        isRealData = true,
                        totalUsers = String.format(java.util.Locale.US, "%,d", stats.totalNumbers),
                        lastUpdate = "1m",
                        totalReports = String.format(java.util.Locale.US, "%,d", stats.totalReports),
                        newToday = String.format(java.util.Locale.US, "+%,d", stats.blockedToday),
                        userReportsCount = "12", // Sync local en M09
                        userRank = "Centinela",
                        topReported = stats.topReported,
                        errorMessage = null
                    )
                } else {
                    // Umbral no alcanzado: mostrar mock social proof + top denuncias reales si las hay
                    _uiState.value = NetworkUiState(
                        isLoading = false,
                        isRealData = false,
                        totalUsers = "14,592",
                        lastUpdate = "24m",
                        totalReports = "+8.5k",
                        newToday = "1.2k",
                        userReportsCount = "12",
                        userRank = "Centinela",
                        topReported = stats.topReported,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                // Fallback robusto ante errores de red (modo offline)
                _uiState.value = NetworkUiState(
                    isLoading = false,
                    isRealData = false,
                    totalUsers = "14,592",
                    lastUpdate = "Modo Offline",
                    totalReports = "+8.5k",
                    newToday = "1.2k",
                    userReportsCount = "12",
                    userRank = "Centinela",
                    errorMessage = "Usando datos locales pre-cargados"
                )
            }
        }
    }
}
