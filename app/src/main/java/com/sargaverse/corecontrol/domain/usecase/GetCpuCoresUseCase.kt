package com.sargaverse.corecontrol.domain.usecase

import com.sargaverse.corecontrol.domain.model.CpuCore
import com.sargaverse.corecontrol.domain.repository.CpuRepository

class GetCpuCoresUseCase(
    private val cpuRepository: CpuRepository
) {
    suspend operator fun invoke(): List<CpuCore> {
        return cpuRepository.detectCpus()
    }
}
