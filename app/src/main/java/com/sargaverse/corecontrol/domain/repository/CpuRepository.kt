package com.sargaverse.corecontrol.domain.repository

import com.sargaverse.corecontrol.domain.model.CpuCore
import com.sargaverse.corecontrol.domain.model.CpuSummary
import com.sargaverse.corecontrol.domain.model.OperationResult
import com.sargaverse.corecontrol.domain.model.SystemInfo
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for interacting with hardware CPU topology,
 * sysfs parameters, and core state toggling.
 */
interface CpuRepository {
    /**
     * Scans and returns the current state of all detected CPU cores.
     */
    suspend fun detectCpus(): List<CpuCore>

    /**
     * Retrieves the current real-time state of a specific CPU core.
     */
    suspend fun getCpuCore(cpuId: Int): CpuCore?

    /**
     * Toggles an individual CPU core to the desired target state.
     *
     * MUST strictly target ONLY the provided [cpuId].
     * Never affects any other core.
     */
    suspend fun toggleCpuCore(cpuId: Int, targetOnline: Boolean): OperationResult

    /**
     * Executes bulk state activation or deactivation of eligible secondary cores.
     * Core 0 is strictly protected and at least one execution core per cluster is preserved.
     */
    suspend fun bulkSetCpus(targetOnline: Boolean): List<OperationResult>

    /**
     * Reads hardware and system platform information.
     */
    suspend fun getSystemInfo(): SystemInfo

    /**
     * Computes the current summary metrics across all detected cores.
     */
    suspend fun getSummary(): CpuSummary
}
