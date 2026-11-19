package com.sargaverse.corecontrol.domain.model

/**
 * Model representing an individual CPU core on the system.
 *
 * @property cpuId The physical index of the CPU (0-indexed).
 * @property state The current operational or capability state of the core.
 * @property isOnline True if the core is currently online and running instructions.
 * @property curFreqKHz Current operating frequency in kHz, or null if offline/unavailable.
 * @property minFreqKHz Minimum scaling frequency in kHz, or null if unavailable.
 * @property maxFreqKHz Maximum scaling frequency in kHz, or null if unavailable.
 * @property clusterId Topology cluster ID (e.g. 0 for Little, 1 for Mid, 2 for Prime).
 * @property coreCapacity Raw capacity score (e.g. 698 for Efficiency, 1024 for Prime).
 * @property hotplugSupported True if `/sys/devices/system/cpu/cpuN/online` exists and supports hotplug.
 * @property isProtected True if the core must never be disabled (e.g. CPU0).
 * @property governor Current CPU scaling governor if readable, or null.
 */
data class CpuCore(
    val cpuId: Int,
    val state: CpuCapabilityState,
    val isOnline: Boolean,
    val curFreqKHz: Long? = null,
    val minFreqKHz: Long? = null,
    val maxFreqKHz: Long? = null,
    val clusterId: Int? = null,
    val coreCapacity: Int? = null,
    val hotplugSupported: Boolean = true,
    val isProtected: Boolean = (cpuId == 0),
    val governor: String? = null
) {
    /**
     * Determines whether the core can be safely toggled by the user.
     * Core 0 is strictly protected and non-hotpluggable cores cannot be controlled.
     */
    val isControllable: Boolean
        get() = !isProtected && hotplugSupported && (state == CpuCapabilityState.ONLINE || state == CpuCapabilityState.OFFLINE)

    /**
     * Formats frequency into human-readable string (MHz or GHz).
     */
    fun formattedFrequency(): String {
        val freq = curFreqKHz ?: return "Offline"
        return when {
            freq >= 1_000_000 -> String.format(java.util.Locale.US, "%.2f GHz", freq / 1_000_000.0)
            freq > 0 -> "${freq / 1000} MHz"
            else -> "0 MHz"
        }
    }
}
