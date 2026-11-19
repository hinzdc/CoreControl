package com.sargaverse.corecontrol.data.root

import com.sargaverse.corecontrol.domain.model.RootStatus
import com.sargaverse.corecontrol.domain.repository.RootRepository

class RootRepositoryImpl(
    private val rootEngine: RootEngine = RootEngine()
) : RootRepository {

    override suspend fun checkRootStatus(): RootStatus {
        return rootEngine.checkRootAuthorization()
    }

    override suspend fun executeSu(command: String, timeoutMs: Long): ShellResult {
        return rootEngine.execute(command, timeoutMs)
    }
}
