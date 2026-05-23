package ar.com.patova.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ar.com.patova.domain.ConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val configRepository: ConfigRepository
) : ViewModel() {

    private val _blockNonContacts = MutableStateFlow(configRepository.getBlockNonContacts())
    val blockNonContacts: StateFlow<Boolean> = _blockNonContacts.asStateFlow()

    private val _allowedPrefixes = MutableStateFlow(configRepository.getAllowedPrefixes().joinToString(", "))
    val allowedPrefixes: StateFlow<String> = _allowedPrefixes.asStateFlow()

    private val _blockedPrefixes = MutableStateFlow(configRepository.getBlockedPrefixes().joinToString(", "))
    val blockedPrefixes: StateFlow<String> = _blockedPrefixes.asStateFlow()

    init {
        viewModelScope.launch {
            configRepository.syncConfig()
            _blockNonContacts.value = configRepository.getBlockNonContacts()
            _allowedPrefixes.value = configRepository.getAllowedPrefixes().joinToString(", ")
            _blockedPrefixes.value = configRepository.getBlockedPrefixes().joinToString(", ")
        }
    }

    fun updateBlockNonContacts(block: Boolean) {
        _blockNonContacts.value = block
        saveConfig()
    }

    fun updateAllowedPrefixes(prefixes: String) {
        _allowedPrefixes.value = prefixes
        saveConfig()
    }

    fun updateBlockedPrefixes(prefixes: String) {
        _blockedPrefixes.value = prefixes
        saveConfig()
    }

    private fun saveConfig() {
        viewModelScope.launch {
            val allowedList = _allowedPrefixes.value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val blockedList = _blockedPrefixes.value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            
            configRepository.updateConfig(
                blockNonContacts = _blockNonContacts.value,
                allowedPrefixes = allowedList,
                blockedPrefixes = blockedList
            )
        }
    }
}
