package com.sargaverse.corecontrol.domain.model

/**
 * Summary metrics of all detected CPU cores on the device.
 */
data class CpuSummary(
    val totalCores: Int = 0,
    val onlineCores: Int = 0,
    val offlineCores: Int = 0,
    val unsupportedCores: Int = 0,
    val protectedCores: Int = 1
)
