package com.sargaverse.corecontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.sargaverse.corecontrol.data.cpu.CpuRepositoryImpl
import com.sargaverse.corecontrol.data.cpu.SysfsCpuReader
import com.sargaverse.corecontrol.data.history.OperationLogRepositoryImpl
import com.sargaverse.corecontrol.data.root.RootEngine
import com.sargaverse.corecontrol.data.root.RootRepositoryImpl
import com.sargaverse.corecontrol.data.settings.SettingsRepositoryImpl
import com.sargaverse.corecontrol.domain.usecase.BulkCpuControlUseCase
import com.sargaverse.corecontrol.domain.usecase.CheckRootUseCase
import com.sargaverse.corecontrol.domain.usecase.GetCpuCoresUseCase
import com.sargaverse.corecontrol.domain.usecase.GetCpuSummaryUseCase
import com.sargaverse.corecontrol.domain.usecase.GetSystemInfoUseCase
import com.sargaverse.corecontrol.domain.usecase.ToggleCpuCoreUseCase
import com.sargaverse.corecontrol.presentation.dashboard.DashboardScreen
import com.sargaverse.corecontrol.presentation.dashboard.DashboardViewModel
import com.sargaverse.corecontrol.presentation.settings.SettingsScreen
import com.sargaverse.corecontrol.presentation.settings.SettingsViewModel
import com.sargaverse.corecontrol.presentation.theme.CoreControlTheme

enum class Screen {
    DASHBOARD,
    SETTINGS
}

class MainActivity : ComponentActivity() {

    private val rootEngine by lazy { RootEngine() }
    private val sysfsCpuReader by lazy { SysfsCpuReader(rootEngine) }
    private val cpuRepository by lazy { CpuRepositoryImpl(sysfsCpuReader, rootEngine) }
    private val rootRepository by lazy { RootRepositoryImpl(rootEngine) }
    private val settingsRepository by lazy { SettingsRepositoryImpl(applicationContext) }
    private val logRepository by lazy { OperationLogRepositoryImpl() }

    private val getCpuCoresUseCase by lazy { GetCpuCoresUseCase(cpuRepository) }
    private val toggleCpuCoreUseCase by lazy { ToggleCpuCoreUseCase(cpuRepository, logRepository) }
    private val bulkCpuControlUseCase by lazy { BulkCpuControlUseCase(cpuRepository, logRepository) }
    private val checkRootUseCase by lazy { CheckRootUseCase(rootRepository) }
    private val getSystemInfoUseCase by lazy { GetSystemInfoUseCase(cpuRepository) }
    private val getCpuSummaryUseCase by lazy { GetCpuSummaryUseCase(cpuRepository) }

    private val dashboardViewModel: DashboardViewModel by viewModels {
        DashboardViewModel.Factory(
            getCpuCoresUseCase = getCpuCoresUseCase,
            toggleCpuCoreUseCase = toggleCpuCoreUseCase,
            bulkCpuControlUseCase = bulkCpuControlUseCase,
            checkRootUseCase = checkRootUseCase,
            getSystemInfoUseCase = getSystemInfoUseCase,
            getCpuSummaryUseCase = getCpuSummaryUseCase,
            settingsRepository = settingsRepository,
            logRepository = logRepository
        )
    }

    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModel.Factory(
            settingsRepository = settingsRepository,
            checkRootUseCase = checkRootUseCase
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            CoreControlTheme {
                var currentScreen by remember { mutableStateOf(Screen.DASHBOARD) }

                Surface(modifier = Modifier.fillMaxSize()) {
                    when (currentScreen) {
                        Screen.DASHBOARD -> {
                            DashboardScreen(
                                viewModel = dashboardViewModel,
                                onNavigateToSettings = { currentScreen = Screen.SETTINGS }
                            )
                        }
                        Screen.SETTINGS -> {
                            BackHandler {
                                currentScreen = Screen.DASHBOARD
                                dashboardViewModel.manualRefresh()
                            }
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onBack = {
                                    currentScreen = Screen.DASHBOARD
                                    dashboardViewModel.manualRefresh()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        dashboardViewModel.manualRefresh()
    }
}
