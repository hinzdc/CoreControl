package com.sargaverse.corecontrol.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
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
import com.sargaverse.corecontrol.presentation.theme.CoreOnlineGreen
import com.sargaverse.corecontrol.presentation.theme.CoreProtectedAmber
import com.sargaverse.corecontrol.presentation.theme.CyberPrimary

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val configuration = LocalConfiguration.current

    // Target 4 columns on all standard Android phones (width >= 320dp), adapting only if screen is narrower
    val columnCount = if (configuration.screenWidthDp >= 320) 4 else 2

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
                // Header (Branding, Root Status, Actions)
                item {
                    CpuHeaderSection(
                        rootStatus = uiState.rootStatus,
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = viewModel::manualRefresh,
                        onOpenTopology = { viewModel.setTopologySheetVisible(!uiState.showTopologySheet) },
                        onOpenSettings = onNavigateToSettings
                    )
                }

                // Core Availability Summary
                item {
                    CpuSummarySection(summary = uiState.summary)
                }

                // Activity State Status Banner: ALL CORES ACTIVE vs PARTIALLY ACTIVE
                item {
                    val allActive = uiState.summary.totalCores > 0 && uiState.summary.offlineCores == 0
                    val bannerColor = if (allActive) CoreOnlineGreen else CoreProtectedAmber

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = bannerColor.copy(alpha = 0.12f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(bannerColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (allActive) "ALL CORES ACTIVE" else "PARTIALLY ACTIVE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = bannerColor
                                )
                            }
                            Text(
                                text = "${uiState.summary.onlineCores}/${uiState.summary.totalCores} Online",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Bulk Controls (Separate ALL ON and ALL OFF)
                item {
                    BulkControlsSection(
                        isOperating = uiState.isBulkOperating,
                        onAllOnClick = { viewModel.requestBulkOperation(true) },
                        onAllOffClick = { viewModel.requestBulkOperation(false) }
                    )
                }

                // CPU Core Cards Section Title
                item {
                    Column(modifier = Modifier.padding(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 1.dp)) {
                        Text(
                            text = "INDIVIDUAL CPU CORES (${uiState.cores.size})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 0.8.sp,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Compact 4-column Responsive Grid of Clickable Rectangular CPU Cards (4x2 for 8 cores)
                val chunkedCores = uiState.cores.chunked(columnCount)
                items(chunkedCores) { rowCores ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.5.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (core in rowCores) {
                            val isToggling = uiState.activeTogglingCpuIds.contains(core.cpuId) || uiState.isBulkOperating
                            CpuCoreCard(
                                core = core,
                                isExecuting = isToggling,
                                onToggle = viewModel::onCpuCardToggleClicked,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill remainder of row if last row has fewer items
                        if (rowCores.size < columnCount) {
                            repeat(columnCount - rowCores.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
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
