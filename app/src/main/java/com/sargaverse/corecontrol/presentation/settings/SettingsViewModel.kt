package com.sargaverse.corecontrol.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sargaverse.corecontrol.domain.model.RootStatus
import com.sargaverse.corecontrol.domain.repository.SettingsRepository
import com.sargaverse.corecontrol.domain.usecase.CheckRootUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val refreshIntervalMs: Long = 2000L,
    val confirmBulkActions: Boolean = true,
    val confirmIndividualAction: Boolean = false,
    val rootStatus: RootStatus = RootStatus.Checking
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val checkRootUseCase: CheckRootUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
        checkRoot()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.refreshIntervalMs.collect { interval ->
                _uiState.update { it.copy(refreshIntervalMs = interval) }
            }
        }
        viewModelScope.launch {
            settingsRepository.confirmBulkActions.collect { confirm ->
                _uiState.update { it.copy(confirmBulkActions = confirm) }
            }
        }
        viewModelScope.launch {
            settingsRepository.confirmIndividualAction.collect { confirm ->
                _uiState.update { it.copy(confirmIndividualAction = confirm) }
            }
        }
    }

    fun checkRoot() {
        viewModelScope.launch {
            _uiState.update { it.copy(rootStatus = RootStatus.Checking) }
            val status = checkRootUseCase()
            _uiState.update { it.copy(rootStatus = status) }
        }
    }

    fun setRefreshInterval(interval: Long) {
        viewModelScope.launch {
            settingsRepository.setRefreshIntervalMs(interval)
        }
    }

    fun setConfirmBulkActions(confirm: Boolean) {
        viewModelScope.launch {
            settingsRepository.setConfirmBulkActions(confirm)
        }
    }

    fun setConfirmIndividualAction(confirm: Boolean) {
        viewModelScope.launch {
            settingsRepository.setConfirmIndividualAction(confirm)
        }
    }

    class Factory(
        private val settingsRepository: SettingsRepository,
        private val checkRootUseCase: CheckRootUseCase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(settingsRepository, checkRootUseCase) as T
        }
    }
}
