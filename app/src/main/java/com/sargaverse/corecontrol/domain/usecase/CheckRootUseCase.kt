package com.sargaverse.corecontrol.domain.usecase

import com.sargaverse.corecontrol.domain.model.RootStatus
import com.sargaverse.corecontrol.domain.repository.RootRepository

class CheckRootUseCase(
    private val rootRepository: RootRepository
) {
    suspend operator fun invoke(): RootStatus {
        return rootRepository.checkRootStatus()
    }
}
