package com.sargaverse.corecontrol.data.history

import com.sargaverse.corecontrol.domain.model.OperationLog
import com.sargaverse.corecontrol.domain.repository.OperationLogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class OperationLogRepositoryImpl : OperationLogRepository {

    private val _logs = MutableStateFlow<List<OperationLog>>(emptyList())
    override val logs: StateFlow<List<OperationLog>> = _logs.asStateFlow()

    override suspend fun addLog(log: OperationLog) {
        _logs.update { current ->
            // Keep recent 100 entries, newest first
            listOf(log) + current.take(99)
        }
    }

    override suspend fun clearLogs() {
        _logs.value = emptyList()
    }
}
