package com.sargaverse.corecontrol.domain.model

/**
 * System hardware and platform topology overview.
 */
data class SystemInfo(
    val socModel: String,
    val hardwareBoard: String,
    val architecture: String,
    val kernelRelease: String,
    val androidVersion: String,
    val hyperOsVersion: String?,
    val clusters: List<CpuCluster>
)
