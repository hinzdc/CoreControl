package com.sargaverse.corecontrol.domain.model

/**
 * Represents a cluster of CPU cores sharing similar microarchitecture / frequencies.
 */
data class CpuCluster(
    val clusterId: Int,
    val name: String,
    val coreIds: List<Int>,
    val maxCapacity: Int? = null
)
