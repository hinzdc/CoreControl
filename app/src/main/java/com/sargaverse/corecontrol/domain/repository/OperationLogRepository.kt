package com.sargaverse.corecontrol.domain.repository

import com.sargaverse.corecontrol.domain.model.OperationLog
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for maintaining an in-memory audit log of CPU operations.
 */
interface OperationLogRepository {
    val logs: StateFlow<List<OperationLog>>

    suspend fun addLog(log: OperationLog)

    suspend fun clearLogs()
}
