package com.sargaverse.corecontrol.domain.usecase

import com.sargaverse.corecontrol.domain.model.CpuSummary
import com.sargaverse.corecontrol.domain.repository.CpuRepository

class GetCpuSummaryUseCase(
    private val cpuRepository: CpuRepository
) {
    suspend operator fun invoke(): CpuSummary {
        return cpuRepository.getSummary()
    }
}
