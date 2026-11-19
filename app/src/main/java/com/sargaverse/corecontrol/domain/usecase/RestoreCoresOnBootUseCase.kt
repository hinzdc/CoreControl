package com.sargaverse.corecontrol.domain.usecase

import android.util.Log
import com.sargaverse.corecontrol.domain.model.OperationAction
import com.sargaverse.corecontrol.domain.model.OperationLog
import com.sargaverse.corecontrol.domain.model.OperationResult
import com.sargaverse.corecontrol.domain.repository.CpuRepository
import com.sargaverse.corecontrol.domain.repository.OperationLogRepository
import com.sargaverse.corecontrol.domain.repository.RootRepository
import com.sargaverse.corecontrol.domain.repository.SettingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/**
 * UseCase executed on system boot to restore all eligible CPU cores to ONLINE.
 *
 * Ensures CPU0 is protected, handles KernelSU readiness, verifies each core's
 * real hardware status in sysfs, and uses controlled retries without blocking or looping.
 */
class RestoreCoresOnBootUseCase(
    private val cpuRepository: CpuRepository,
    private val rootRepository: RootRepository,
    private val settingsRepository: SettingsRepository,
    private val logRepository: OperationLogRepository
) {
    companion object {
        private const val TAG = "BootRestore"
        private const val INITIAL_STARTUP_DELAY_MS = 2500L
        private const val RETRY_DELAY_MS = 1500L
        private const val MAX_RETRIES = 2

        private fun logI(tag: String, msg: String) {
            try {
                Log.i(tag, msg)
            } catch (_: Throwable) {
                println("[$tag] $msg")
            }
        }

        private fun logW(tag: String, msg: String) {
            try {
                Log.w(tag, msg)
            } catch (_: Throwable) {
                println("[$tag] WARN: $msg")
            }
        }
    }

    suspend operator fun invoke(): List<OperationResult> {
        val startTime = System.currentTimeMillis()

        // 1. Check if user enabled boot restore (Default: Enabled)
        val isEnabled = settingsRepository.restoreOnBoot.first()
        if (!isEnabled) {
            logI(TAG, "Boot restore skipped: Feature is disabled in settings.")
            return emptyList()
        }

        logI(TAG, "Starting Boot Restore: Waiting for system stabilization (${INITIAL_STARTUP_DELAY_MS}ms)...")
        delay(INITIAL_STARTUP_DELAY_MS)

        // 2. Check root access (KernelSU / SU)
        val rootStatus = rootRepository.checkRootStatus()
        if (!rootStatus.isGranted) {
            logW(TAG, "Boot restore aborted: Root access not granted.")
            return emptyList()
        }

        // 3. Scan available CPU cores
        val detectedCores = cpuRepository.detectCpus()
        // Eligible cores: must be currently offline, support hotplug, and NOT be CPU0
        val eligibleOfflineCores = detectedCores.filter { core ->
            !core.isOnline && core.hotplugSupported && core.cpuId != 0
        }

        if (eligibleOfflineCores.isEmpty()) {
            logI(TAG, "Boot restore: All eligible cores are already ONLINE.")
            return emptyList()
        }

        logI(TAG, "Boot restore: Found ${eligibleOfflineCores.size} offline cores to activate: ${eligibleOfflineCores.map { it.cpuId }}")
        val results = mutableListOf<OperationResult>()

        for (core in eligibleOfflineCores) {
            var attempt = 0
            var result: OperationResult? = null

            while (attempt < MAX_RETRIES) {
                attempt++
                result = cpuRepository.toggleCpuCore(core.cpuId, targetOnline = true)
                if (result is OperationResult.Success) {
                    break
                }
                if (attempt < MAX_RETRIES) {
                    delay(RETRY_DELAY_MS)
                }
            }

            val finalResult = result ?: OperationResult.Failure(
                cpuId = core.cpuId,
                targetOnline = true,
                errorMessage = "Boot restore failed after $MAX_RETRIES attempts."
            )
            results.add(finalResult)

            // Audit log
            logRepository.addLog(
                OperationLog(
                    cpuId = core.cpuId,
                    action = OperationAction.BOOT_RESTORE,
                    result = finalResult,
                    durationMs = System.currentTimeMillis() - startTime
                )
            )
        }

        logI(TAG, "Boot restore completed. Activated ${results.count { it is OperationResult.Success }} of ${eligibleOfflineCores.size} cores.")
        return results
    }
}
