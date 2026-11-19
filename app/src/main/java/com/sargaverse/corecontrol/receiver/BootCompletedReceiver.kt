package com.sargaverse.corecontrol.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.sargaverse.corecontrol.data.cpu.CpuRepositoryImpl
import com.sargaverse.corecontrol.data.cpu.SysfsCpuReader
import com.sargaverse.corecontrol.data.history.OperationLogRepositoryImpl
import com.sargaverse.corecontrol.data.root.RootEngine
import com.sargaverse.corecontrol.data.root.RootRepositoryImpl
import com.sargaverse.corecontrol.data.settings.SettingsRepositoryImpl
import com.sargaverse.corecontrol.domain.usecase.RestoreCoresOnBootUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver triggered when Android finishes booting.
 * Restores all eligible CPU cores to ONLINE if enabled by user preferences.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "CoreControlBoot"
    }

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.i(TAG, "Received boot event action: $action")

        val validActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON"
        )

        if (action !in validActions) {
            return
        }

        val pendingResult = goAsync()

        receiverScope.launch {
            try {
                Log.i(TAG, "Initializing boot restoration services...")
                val appContext = context.applicationContext
                val rootEngine = RootEngine()
                val sysfsReader = SysfsCpuReader(rootEngine)
                val cpuRepo = CpuRepositoryImpl(sysfsReader, rootEngine)
                val rootRepo = RootRepositoryImpl(rootEngine)
                val settingsRepo = SettingsRepositoryImpl(appContext)
                val logRepo = OperationLogRepositoryImpl()

                val useCase = RestoreCoresOnBootUseCase(
                    cpuRepository = cpuRepo,
                    rootRepository = rootRepo,
                    settingsRepository = settingsRepo,
                    logRepository = logRepo
                )

                useCase()
            } catch (e: Exception) {
                Log.e(TAG, "Exception during boot CPU restoration: ${e.message}", e)
            } finally {
                Log.i(TAG, "BootCompletedReceiver async execution finished.")
                pendingResult.finish()
            }
        }
    }
}
