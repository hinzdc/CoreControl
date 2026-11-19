package com.sargaverse.corecontrol.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sargaverse.corecontrol.domain.model.CpuCore
import com.sargaverse.corecontrol.domain.model.OperationResult
import com.sargaverse.corecontrol.domain.repository.OperationLogRepository
import com.sargaverse.corecontrol.domain.repository.SettingsRepository
import com.sargaverse.corecontrol.domain.usecase.BulkCpuControlUseCase
import com.sargaverse.corecontrol.domain.usecase.CheckRootUseCase
import com.sargaverse.corecontrol.domain.usecase.GetCpuCoresUseCase
import com.sargaverse.corecontrol.domain.usecase.GetCpuSummaryUseCase
import com.sargaverse.corecontrol.domain.usecase.GetSystemInfoUseCase
import com.sargaverse.corecontrol.domain.usecase.ToggleCpuCoreUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val getCpuCoresUseCase: GetCpuCoresUseCase,
    private val toggleCpuCoreUseCase: ToggleCpuCoreUseCase,
    private val bulkCpuControlUseCase: BulkCpuControlUseCase,
    private val checkRootUseCase: CheckRootUseCase,
    private val getSystemInfoUseCase: GetSystemInfoUseCase,
    private val getCpuSummaryUseCase: GetCpuSummaryUseCase,
    private val settingsRepository: SettingsRepository,
    private val logRepository: OperationLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null

    init {
        observeLogs()
        initialLoad()
        startPeriodicRefresh()
    }

    private fun observeLogs() {
        viewModelScope.launch {
            logRepository.logs.collect { logs ->
                _uiState.update { it.copy(operationLogs = logs) }
            }
        }
    }

    fun initialLoad() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val root = checkRootUseCase()
            val cores = getCpuCoresUseCase()
            val summary = getCpuSummaryUseCase()
            val sysInfo = getSystemInfoUseCase()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    rootStatus = root,
                    cores = cores,
                    summary = summary,
                    systemInfo = sysInfo
                )
            }
        }
    }

    fun manualRefresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val root = checkRootUseCase()
            val cores = getCpuCoresUseCase()
            val summary = getCpuSummaryUseCase()
            val sysInfo = getSystemInfoUseCase()

            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    rootStatus = root,
                    cores = cores,
                    summary = summary,
                    systemInfo = sysInfo
                )
            }
        }
    }

    private fun startPeriodicRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (isActive) {
                val interval = settingsRepository.refreshIntervalMs.first()
                delay(interval)
                // Background refresh only core stats if not currently executing commands
                if (_uiState.value.activeTogglingCpuIds.isEmpty() && !_uiState.value.isBulkOperating) {
                    val cores = getCpuCoresUseCase()
                    val summary = getCpuSummaryUseCase()
                    _uiState.update {
                        it.copy(cores = cores, summary = summary)
                    }
                }
            }
        }
    }

    fun onCpuCardToggleClicked(core: CpuCore) {
        viewModelScope.launch {
            val requireConfirm = settingsRepository.confirmIndividualAction.first()
            if (requireConfirm) {
                _uiState.update {
                    it.copy(
                        showIndividualConfirmDialog = true,
                        individualConfirmTargetCpu = core
                    )
                }
            } else {
                executeToggle(core.cpuId)
            }
        }
    }

    fun confirmIndividualToggle() {
        val target = _uiState.value.individualConfirmTargetCpu ?: return
        _uiState.update {
            it.copy(
                showIndividualConfirmDialog = false,
                individualConfirmTargetCpu = null
            )
        }
        executeToggle(target.cpuId)
    }

    fun dismissIndividualConfirmDialog() {
        _uiState.update {
            it.copy(
                showIndividualConfirmDialog = false,
                individualConfirmTargetCpu = null
            )
        }
    }

    private fun executeToggle(cpuId: Int) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(activeTogglingCpuIds = it.activeTogglingCpuIds + cpuId)
            }

            val result = toggleCpuCoreUseCase(cpuId)

            val updatedCores = getCpuCoresUseCase()
            val updatedSummary = getCpuSummaryUseCase()

            val msg = when (result) {
                is OperationResult.Success -> result.message
                is OperationResult.Failure -> "Failed on CPU$cpuId: ${result.errorMessage}"
            }

            _uiState.update {
                it.copy(
                    activeTogglingCpuIds = it.activeTogglingCpuIds - cpuId,
                    cores = updatedCores,
                    summary = updatedSummary,
                    userMessage = msg
                )
            }
        }
    }

    fun requestBulkOperation(targetOnline: Boolean) {
        viewModelScope.launch {
            val requireConfirm = settingsRepository.confirmBulkActions.first()
            if (requireConfirm) {
                _uiState.update {
                    it.copy(
                        showBulkConfirmDialog = true,
                        bulkConfirmTargetOnline = targetOnline
                    )
                }
            } else {
                executeBulkOperation(targetOnline)
            }
        }
    }

    fun confirmBulkOperation() {
        val targetOnline = _uiState.value.bulkConfirmTargetOnline
        _uiState.update { it.copy(showBulkConfirmDialog = false) }
        executeBulkOperation(targetOnline)
    }

    fun dismissBulkConfirmDialog() {
        _uiState.update { it.copy(showBulkConfirmDialog = false) }
    }

    private fun executeBulkOperation(targetOnline: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBulkOperating = true) }

            val results = if (targetOnline) {
                bulkCpuControlUseCase.turnAllOn()
            } else {
                bulkCpuControlUseCase.turnAllOff()
            }

            val updatedCores = getCpuCoresUseCase()
            val updatedSummary = getCpuSummaryUseCase()

            val successCount = results.count { it is OperationResult.Success }
            val totalCount = results.size
            val msg = "Bulk operation finished: $successCount of $totalCount cores updated."

            _uiState.update {
                it.copy(
                    isBulkOperating = false,
                    cores = updatedCores,
                    summary = updatedSummary,
                    userMessage = msg
                )
            }
        }
    }

    fun setTopologySheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showTopologySheet = visible) }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
    }

    class Factory(
        private val getCpuCoresUseCase: GetCpuCoresUseCase,
        private val toggleCpuCoreUseCase: ToggleCpuCoreUseCase,
        private val bulkCpuControlUseCase: BulkCpuControlUseCase,
        private val checkRootUseCase: CheckRootUseCase,
        private val getSystemInfoUseCase: GetSystemInfoUseCase,
        private val getCpuSummaryUseCase: GetCpuSummaryUseCase,
        private val settingsRepository: SettingsRepository,
        private val logRepository: OperationLogRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(
                getCpuCoresUseCase = getCpuCoresUseCase,
                toggleCpuCoreUseCase = toggleCpuCoreUseCase,
                bulkCpuControlUseCase = bulkCpuControlUseCase,
                checkRootUseCase = checkRootUseCase,
                getSystemInfoUseCase = getSystemInfoUseCase,
                getCpuSummaryUseCase = getCpuSummaryUseCase,
                settingsRepository = settingsRepository,
                logRepository = logRepository
            ) as T
        }
    }
}
