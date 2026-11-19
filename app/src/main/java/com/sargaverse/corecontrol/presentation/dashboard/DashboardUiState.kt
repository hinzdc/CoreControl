package com.sargaverse.corecontrol.presentation.dashboard

import com.sargaverse.corecontrol.domain.model.CpuCore
import com.sargaverse.corecontrol.domain.model.CpuSummary
import com.sargaverse.corecontrol.domain.model.OperationLog
import com.sargaverse.corecontrol.domain.model.RootStatus
import com.sargaverse.corecontrol.domain.model.SystemInfo

data class DashboardUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val rootStatus: RootStatus = RootStatus.Checking,
    val cores: List<CpuCore> = emptyList(),
    val summary: CpuSummary = CpuSummary(),
    val systemInfo: SystemInfo? = null,
    val operationLogs: List<OperationLog> = emptyList(),
    val activeTogglingCpuIds: Set<Int> = emptySet(),
    val isBulkOperating: Boolean = false,
    val showBulkConfirmDialog: Boolean = false,
    val bulkConfirmTargetOnline: Boolean = true,
    val showIndividualConfirmDialog: Boolean = false,
    val individualConfirmTargetCpu: CpuCore? = null,
    val showTopologySheet: Boolean = false,
    val userMessage: String? = null
)
