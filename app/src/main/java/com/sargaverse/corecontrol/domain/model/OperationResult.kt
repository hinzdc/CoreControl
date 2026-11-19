package com.sargaverse.corecontrol.domain.model

/**
 * Result of an individual or bulk CPU core operation.
 */
sealed class OperationResult {
    data class Success(
        val cpuId: Int,
        val targetOnline: Boolean,
        val verifiedState: CpuCapabilityState,
        val message: String
    ) : OperationResult()

    data class Failure(
        val cpuId: Int,
        val targetOnline: Boolean?,
        val errorMessage: String,
        val exitCode: Int? = null,
        val verifiedState: CpuCapabilityState? = null
    ) : OperationResult()
}
