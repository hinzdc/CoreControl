package com.sargaverse.corecontrol.presentation.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sargaverse.corecontrol.presentation.components.BulkControlsSection
import com.sargaverse.corecontrol.presentation.components.ConfirmationDialog
import com.sargaverse.corecontrol.presentation.components.CpuCoreCard
import com.sargaverse.corecontrol.presentation.components.CpuHeaderSection
import com.sargaverse.corecontrol.presentation.components.CpuSummarySection
import com.sargaverse.corecontrol.presentation.components.OperationHistorySection
import com.sargaverse.corecontrol.presentation.components.SystemInfoSection
import com.sargaverse.corecontrol.presentation.theme.CyberPrimary

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userMessage) {
        val msg = uiState.userMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = CyberPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Detecting CPU Topology & Sysfs...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Header
                item {
                    CpuHeaderSection(
                        rootStatus = uiState.rootStatus,
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = viewModel::manualRefresh,
                        onOpenTopology = { viewModel.setTopologySheetVisible(!uiState.showTopologySheet) },
                        onOpenSettings = onNavigateToSettings
                    )
                }

                // Summary Cards
                item {
                    CpuSummarySection(summary = uiState.summary)
                }

                // Bulk Controls
                item {
                    BulkControlsSection(
                        isOperating = uiState.isBulkOperating,
                        onAllOnClick = { viewModel.requestBulkOperation(true) },
                        onAllOffClick = { viewModel.requestBulkOperation(false) }
                    )
                }

                // CPU Core Cards Section Title
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Text(
                            text = "INDIVIDUAL CPU CORES (${uiState.cores.size})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Dynamically Generated Individual Core Cards
                items(uiState.cores, key = { it.cpuId }) { core ->
                    val isToggling = uiState.activeTogglingCpuIds.contains(core.cpuId) || uiState.isBulkOperating
                    CpuCoreCard(
                        core = core,
                        isExecuting = isToggling,
                        onToggle = viewModel::onCpuCardToggleClicked,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                // System Info & Topology
                item {
                    SystemInfoSection(systemInfo = uiState.systemInfo)
                }

                // Operation History
                item {
                    OperationHistorySection(logs = uiState.operationLogs)
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // Bulk Confirmation Dialog
        if (uiState.showBulkConfirmDialog) {
            val target = uiState.bulkConfirmTargetOnline
            ConfirmationDialog(
                title = if (target) "Activate All Cores?" else "Deactivate Secondary Cores?",
                message = if (target) {
                    "This will attempt to bring all eligible offline secondary CPU cores ONLINE."
                } else {
                    "This will attempt to turn OFF eligible secondary CPU cores to conserve power. CPU0 and at least one execution core per cluster will remain protected and active."
                },
                confirmLabel = if (target) "ALL ON" else "ALL OFF",
                isDestructive = !target,
                onConfirm = viewModel::confirmBulkOperation,
                onDismiss = viewModel::dismissBulkConfirmDialog
            )
        }

        // Individual Core Confirmation Dialog
        if (uiState.showIndividualConfirmDialog) {
            val target = uiState.individualConfirmTargetCpu
            if (target != null) {
                val nextState = !target.isOnline
                ConfirmationDialog(
                    title = "Confirm CPU ${target.cpuId} Change",
                    message = "Are you sure you want to turn CPU ${target.cpuId} ${if (nextState) "ONLINE" else "OFFLINE"}?",
                    confirmLabel = if (nextState) "Turn ON" else "Turn OFF",
                    isDestructive = !nextState,
                    onConfirm = viewModel::confirmIndividualToggle,
                    onDismiss = viewModel::dismissIndividualConfirmDialog
                )
            }
        }
    }
}
