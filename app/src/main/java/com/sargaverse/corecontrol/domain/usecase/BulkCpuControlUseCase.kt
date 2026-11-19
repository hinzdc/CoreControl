package com.sargaverse.corecontrol.domain.usecase

import com.sargaverse.corecontrol.domain.model.OperationAction
import com.sargaverse.corecontrol.domain.model.OperationLog
import com.sargaverse.corecontrol.domain.model.OperationResult
import com.sargaverse.corecontrol.domain.repository.CpuRepository
import com.sargaverse.corecontrol.domain.repository.OperationLogRepository

/**
 * Use case to perform bulk online or bulk offline operations on eligible CPU cores.
 */
class BulkCpuControlUseCase(
    private val cpuRepository: CpuRepository,
    private val logRepository: OperationLogRepository
) {
    suspend fun turnAllOn(): List<OperationResult> {
        val startTime = System.currentTimeMillis()
        val results = cpuRepository.bulkSetCpus(targetOnline = true)
        val duration = System.currentTimeMillis() - startTime

        for (res in results) {
            val cpuId = when (res) {
                is OperationResult.Success -> res.cpuId
                is OperationResult.Failure -> res.cpuId
            }
            logRepository.addLog(
                OperationLog(
                    cpuId = cpuId,
                    action = OperationAction.BULK_ALL_ON,
                    result = res,
                    durationMs = duration
                )
            )
        }
        return results
    }

    suspend fun turnAllOff(): List<OperationResult> {
        val startTime = System.currentTimeMillis()
        val results = cpuRepository.bulkSetCpus(targetOnline = false)
        val duration = System.currentTimeMillis() - startTime

        for (res in results) {
            val cpuId = when (res) {
                is OperationResult.Success -> res.cpuId
                is OperationResult.Failure -> res.cpuId
            }
            logRepository.addLog(
                OperationLog(
                    cpuId = cpuId,
                    action = OperationAction.BULK_ALL_OFF,
                    result = res,
                    durationMs = duration
                )
            )
        }
        return results
    }
}
