package com.sargaverse.corecontrol.domain.usecase

import com.sargaverse.corecontrol.domain.model.OperationAction
import com.sargaverse.corecontrol.domain.model.OperationLog
import com.sargaverse.corecontrol.domain.model.OperationResult
import com.sargaverse.corecontrol.domain.repository.CpuRepository
import com.sargaverse.corecontrol.domain.repository.OperationLogRepository

/**
 * Use case to toggle an individual CPU core ON or OFF.
 *
 * Implements the Absolute Core Control Rule:
 * - Operates strictly on the exact [cpuId] provided.
 * - Protects CPU 0 unconditionally.
 * - Records an audit log entry with execution duration and verification status.
 */
class ToggleCpuCoreUseCase(
    private val cpuRepository: CpuRepository,
    private val logRepository: OperationLogRepository
) {
    suspend operator fun invoke(cpuId: Int): OperationResult {
        val startTime = System.currentTimeMillis()

        // 1. Read current core state
        val currentCore = cpuRepository.getCpuCore(cpuId)
            ?: return OperationResult.Failure(
                cpuId = cpuId,
                targetOnline = null,
                errorMessage = "Core CPU$cpuId is not accessible in sysfs."
            ).also { failResult ->
                logRepository.addLog(
                    OperationLog(
                        cpuId = cpuId,
                        action = OperationAction.REFRESH,
                        result = failResult,
                        durationMs = System.currentTimeMillis() - startTime
                    )
                )
            }

        // 2. Strict protection of CPU0
        if (cpuId == 0) {
            val failResult = OperationResult.Failure(
                cpuId = 0,
                targetOnline = false,
                errorMessage = "CPU0 is protected by kernel safety policy and cannot be disabled.",
                verifiedState = currentCore.state
            )
            logRepository.addLog(
                OperationLog(
                    cpuId = 0,
                    action = OperationAction.TOGGLE_OFF,
                    result = failResult,
                    durationMs = System.currentTimeMillis() - startTime
                )
            )
            return failResult
        }

        // 3. Determine requested target opposite state
        val targetOnline = !currentCore.isOnline
        val action = if (targetOnline) OperationAction.TOGGLE_ON else OperationAction.TOGGLE_OFF

        // 4. Validate controllability
        if (!currentCore.hotplugSupported && !targetOnline) {
            val failResult = OperationResult.Failure(
                cpuId = cpuId,
                targetOnline = targetOnline,
                errorMessage = "CPU$cpuId does not support hotplug operations.",
                verifiedState = currentCore.state
            )
            logRepository.addLog(
                OperationLog(
                    cpuId = cpuId,
                    action = action,
                    result = failResult,
                    durationMs = System.currentTimeMillis() - startTime
                )
            )
            return failResult
        }

        // 5. Execute toggle strictly on this CPU core
        val result = cpuRepository.toggleCpuCore(cpuId, targetOnline)
        val duration = System.currentTimeMillis() - startTime

        // 6. Record operation log
        logRepository.addLog(
            OperationLog(
                cpuId = cpuId,
                action = action,
                result = result,
                durationMs = duration
            )
        )

        return result
    }
}
