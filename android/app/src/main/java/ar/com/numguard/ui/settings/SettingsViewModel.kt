package ar.com.numguard.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.com.numguard.data.local.entities.BlacklistEntity
import ar.com.numguard.data.local.entities.LocalPreferencesEntity
import ar.com.numguard.data.local.entities.WhitelistEntity
import ar.com.numguard.domain.ConfigRepository
import ar.com.numguard.domain.PhoneHashing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val configRepository: ConfigRepository
) : ViewModel() {

    val preferences: StateFlow<LocalPreferencesEntity?> = configRepository
        .observePreferences()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val whitelistEntries: StateFlow<List<WhitelistEntity>> = configRepository
        .observeWhitelist()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val blacklistEntries: StateFlow<List<BlacklistEntity>> = configRepository
        .observeBlacklist()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _showAddWhitelistDialog = MutableStateFlow(false)
    val showAddWhitelistDialog: StateFlow<Boolean> = _showAddWhitelistDialog.asStateFlow()

    private val _showAddBlacklistDialog = MutableStateFlow(false)
    val showAddBlacklistDialog: StateFlow<Boolean> = _showAddBlacklistDialog.asStateFlow()

    private val _whitelistPhoneInput = MutableStateFlow("")
    val whitelistPhoneInput: StateFlow<String> = _whitelistPhoneInput.asStateFlow()

    private val _whitelistLabelInput = MutableStateFlow("")
    val whitelistLabelInput: StateFlow<String> = _whitelistLabelInput.asStateFlow()

    private val _blacklistPhoneInput = MutableStateFlow("")
    val blacklistPhoneInput: StateFlow<String> = _blacklistPhoneInput.asStateFlow()

    private val _blacklistReasonInput = MutableStateFlow("")
    val blacklistReasonInput: StateFlow<String> = _blacklistReasonInput.asStateFlow()

    fun updateStrictMode(enabled: Boolean) {
        viewModelScope.launch {
            configRepository.updatePreferences(strictMode = enabled)
        }
    }

    fun updateBlockUnknown(enabled: Boolean) {
        viewModelScope.launch {
            configRepository.updatePreferences(blockUnknown = enabled)
        }
    }

    fun updateSpamThreshold(threshold: Float) {
        viewModelScope.launch {
            configRepository.updatePreferences(spamThreshold = threshold)
        }
    }

    fun updateSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            configRepository.updatePreferences(syncEnabled = enabled)
        }
    }

    fun showAddWhitelistDialog() {
        _showAddWhitelistDialog.value = true
    }

    fun dismissAddWhitelistDialog() {
        _showAddWhitelistDialog.value = false
        _whitelistPhoneInput.value = ""
        _whitelistLabelInput.value = ""
    }

    fun updateWhitelistPhoneInput(value: String) {
        _whitelistPhoneInput.value = value
    }

    fun updateWhitelistLabelInput(value: String) {
        _whitelistLabelInput.value = value
    }

    fun addWhitelistEntry() {
        val phone = _whitelistPhoneInput.value.trim()
        val label = _whitelistLabelInput.value.trim().ifBlank { "Sin etiqueta" }
        if (phone.isNotBlank()) {
            viewModelScope.launch {
                val hash = PhoneHashing.sha256(phone)
                configRepository.addToWhitelist(hash, label)
                dismissAddWhitelistDialog()
            }
        }
    }

    fun removeWhitelistEntry(phoneHash: String) {
        viewModelScope.launch {
            configRepository.removeFromWhitelist(phoneHash)
        }
    }

    fun showAddBlacklistDialog() {
        _showAddBlacklistDialog.value = true
    }

    fun dismissAddBlacklistDialog() {
        _showAddBlacklistDialog.value = false
        _blacklistPhoneInput.value = ""
        _blacklistReasonInput.value = ""
    }

    fun updateBlacklistPhoneInput(value: String) {
        _blacklistPhoneInput.value = value
    }

    fun updateBlacklistReasonInput(value: String) {
        _blacklistReasonInput.value = value
    }

    fun addBlacklistEntry() {
        val phone = _blacklistPhoneInput.value.trim()
        val reason = _blacklistReasonInput.value.trim().ifBlank { "Reportado manualmente" }
        if (phone.isNotBlank()) {
            viewModelScope.launch {
                val hash = PhoneHashing.sha256(phone)
                configRepository.addToBlacklist(hash, reason)
                dismissAddBlacklistDialog()
            }
        }
    }

    fun removeBlacklistEntry(phoneHash: String) {
        viewModelScope.launch {
            configRepository.removeFromBlacklist(phoneHash)
        }
    }
}
