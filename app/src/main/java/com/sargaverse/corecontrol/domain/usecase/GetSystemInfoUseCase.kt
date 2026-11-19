package com.sargaverse.corecontrol.domain.usecase

import com.sargaverse.corecontrol.domain.model.SystemInfo
import com.sargaverse.corecontrol.domain.repository.CpuRepository

class GetSystemInfoUseCase(
    private val cpuRepository: CpuRepository
) {
    suspend operator fun invoke(): SystemInfo {
        return cpuRepository.getSystemInfo()
    }
}
