package com.sargaverse.corecontrol.data.cpu

import com.sargaverse.corecontrol.data.root.RootEngine
import com.sargaverse.corecontrol.domain.model.CpuCapabilityState
import com.sargaverse.corecontrol.domain.model.CpuCore
import com.sargaverse.corecontrol.domain.model.CpuSummary
import com.sargaverse.corecontrol.domain.model.OperationResult
import com.sargaverse.corecontrol.domain.model.SystemInfo
import com.sargaverse.corecontrol.domain.repository.CpuRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Concrete implementation of [CpuRepository].
 *
 * Implements strict serialization of hardware operations, protection of CPU0,
 * and post-execution sysfs state verification.
 */
class CpuRepositoryImpl(
    private val sysfsReader: SysfsCpuReader = SysfsCpuReader(),
    private val rootEngine: RootEngine = RootEngine()
) : CpuRepository {

    // Serializes hardware control operations to prevent race conditions
    private val operationMutex = Mutex()

    override suspend fun detectCpus(): List<CpuCore> = withContext(Dispatchers.IO) {
        sysfsReader.readAllCores(useRoot = true)
    }

    override suspend fun getCpuCore(cpuId: Int): CpuCore? = withContext(Dispatchers.IO) {
        val detected = detectCpus()
        detected.find { it.cpuId == cpuId }
    }

    override suspend fun toggleCpuCore(cpuId: Int, targetOnline: Boolean): OperationResult = operationMutex.withLock {
        withContext(Dispatchers.IO) {
            // Rule 1: Validate CPU ID
            if (!rootEngine.validateCpuId(cpuId)) {
                return@withContext OperationResult.Failure(
                    cpuId = cpuId,
                    targetOnline = targetOnline,
                    errorMessage = "Invalid CPU ID: $cpuId"
                )
            }

            // Rule 2: Absolute Protection of CPU 0
            if (cpuId == 0 && !targetOnline) {
                return@withContext OperationResult.Failure(
                    cpuId = 0,
                    targetOnline = false,
                    errorMessage = "CPU0 is protected from being disabled to maintain system stability."
                )
            }

            // Rule 3: Check capability and hotplug support
            val currentCore = getCpuCore(cpuId)
            if (currentCore == null) {
                return@withContext OperationResult.Failure(
                    cpuId = cpuId,
                    targetOnline = targetOnline,
                    errorMessage = "CPU$cpuId not found in system sysfs."
                )
            }

            if (!currentCore.hotplugSupported && !targetOnline) {
                return@withContext OperationResult.Failure(
                    cpuId = cpuId,
                    targetOnline = targetOnline,
                    errorMessage = "CPU$cpuId does not support sysfs hotplugging.",
                    verifiedState = currentCore.state
                )
            }

            // If already in target state, return early
            if (currentCore.isOnline == targetOnline) {
                return@withContext OperationResult.Success(
                    cpuId = cpuId,
                    targetOnline = targetOnline,
                    verifiedState = currentCore.state,
                    message = "CPU$cpuId is already ${if (targetOnline) "ONLINE" else "OFFLINE"}."
                )
            }

            // Rule 4: Build command strictly for ONLY this specific cpuId
            val command = rootEngine.buildCpuToggleCommand(cpuId, targetOnline)
            val shellResult = rootEngine.execute(command, timeoutMs = 5000L)

            // Rule 5: State verification - Never rely purely on shell exit code!
            // Re-read sysfs directly to verify the real hardware state
            val verifiedCore = getCpuCore(cpuId)
            val isNowOnline = verifiedCore?.isOnline ?: false

            if (isNowOnline == targetOnline) {
                val stateDesc = if (targetOnline) "ONLINE" else "OFFLINE"
                OperationResult.Success(
                    cpuId = cpuId,
                    targetOnline = targetOnline,
                    verifiedState = verifiedCore?.state ?: if (targetOnline) CpuCapabilityState.ONLINE else CpuCapabilityState.OFFLINE,
                    message = "CPU$cpuId successfully transitioned to $stateDesc."
                )
            } else {
                val errReason = when {
                    !shellResult.isSuccess -> shellResult.stderr.ifBlank { "Exit code ${shellResult.exitCode}" }
                    else -> "Kernel or SELinux prevented hotplug change (hardware lock)."
                }
                OperationResult.Failure(
                    cpuId = cpuId,
                    targetOnline = targetOnline,
                    errorMessage = "Verification failed: CPU$cpuId remained ${if (isNowOnline) "ONLINE" else "OFFLINE"}. $errReason",
                    exitCode = shellResult.exitCode,
                    verifiedState = verifiedCore?.state
                )
            }
        }
    }

    override suspend fun bulkSetCpus(targetOnline: Boolean): List<OperationResult> = operationMutex.withLock {
        withContext(Dispatchers.IO) {
            val results = mutableListOf<OperationResult>()
            val allCores = detectCpus()

            if (targetOnline) {
                // ALL ON: Activate eligible offline cores individually
                val eligible = allCores.filter { !it.isOnline && it.hotplugSupported }
                for (core in eligible) {
                    val command = rootEngine.buildCpuToggleCommand(core.cpuId, true)
                    val shellResult = rootEngine.execute(command, timeoutMs = 4000L)
                    val verified = getCpuCore(core.cpuId)
                    if (verified?.isOnline == true) {
                        results.add(
                            OperationResult.Success(
                                cpuId = core.cpuId,
                                targetOnline = true,
                                verifiedState = verified.state,
                                message = "CPU${core.cpuId} brought ONLINE."
                            )
                        )
                    } else {
                        results.add(
                            OperationResult.Failure(
                                cpuId = core.cpuId,
                                targetOnline = true,
                                errorMessage = "Failed to activate CPU${core.cpuId}: ${shellResult.stderr}",
                                exitCode = shellResult.exitCode,
                                verifiedState = verified?.state
                            )
                        )
                    }
                }
            } else {
                // ALL OFF: Disable eligible secondary cores individually
                // STRICT SAFETY:
                // 1. CPU0 is always protected.
                // 2. Preserve at least one online CPU per detected cluster where permitted.
                val clusters = sysfsReader.groupClusters(allCores)
                val preservedCpuIds = mutableSetOf(0) // CPU 0 must always be preserved

                clusters.forEach { cluster ->
                    // Find first available controllable core or core 0 in this cluster to preserve
                    val toPreserve = cluster.coreIds.firstOrNull { it == 0 }
                        ?: cluster.coreIds.firstOrNull()
                    if (toPreserve != null) {
                        preservedCpuIds.add(toPreserve)
                    }
                }

                val eligibleToDisable = allCores.filter {
                    it.cpuId !in preservedCpuIds && it.isOnline && it.isControllable
                }

                for (core in eligibleToDisable) {
                    val command = rootEngine.buildCpuToggleCommand(core.cpuId, false)
                    val shellResult = rootEngine.execute(command, timeoutMs = 4000L)
                    val verified = getCpuCore(core.cpuId)
                    if (verified?.isOnline == false) {
                        results.add(
                            OperationResult.Success(
                                cpuId = core.cpuId,
                                targetOnline = false,
                                verifiedState = verified.state,
                                message = "CPU${core.cpuId} brought OFFLINE."
                            )
                        )
                    } else {
                        results.add(
                            OperationResult.Failure(
                                cpuId = core.cpuId,
                                targetOnline = false,
                                errorMessage = "Failed to deactivate CPU${core.cpuId}: ${shellResult.stderr}",
                                exitCode = shellResult.exitCode,
                                verifiedState = verified?.state
                            )
                        )
                    }
                }
            }
            results
        }
    }

    override suspend fun getSystemInfo(): SystemInfo = withContext(Dispatchers.IO) {
        val cores = detectCpus()
        sysfsReader.getSystemInfo(cores)
    }

    override suspend fun getSummary(): CpuSummary = withContext(Dispatchers.IO) {
        val cores = detectCpus()
        var onlineCount = 0
        var offlineCount = 0
        var unsupportedCount = 0
        var protectedCount = 0

        for (core in cores) {
            when (core.state) {
                CpuCapabilityState.ONLINE -> onlineCount++
                CpuCapabilityState.OFFLINE -> offlineCount++
                CpuCapabilityState.UNSUPPORTED -> unsupportedCount++
                CpuCapabilityState.PROTECTED -> {
                    protectedCount++
                    if (core.isOnline) onlineCount++ else offlineCount++
                }
                CpuCapabilityState.UNKNOWN, CpuCapabilityState.ERROR -> unsupportedCount++
            }
        }

        CpuSummary(
            totalCores = cores.size,
            onlineCores = onlineCount,
            offlineCores = offlineCount,
            unsupportedCores = unsupportedCount,
            protectedCores = protectedCount
        )
    }
}
