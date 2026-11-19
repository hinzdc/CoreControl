package com.sargaverse.corecontrol.domain.model

/**
 * Explicit capability states for each CPU core.
 */
enum class CpuCapabilityState {
    /** Core is active and executing tasks. */
    ONLINE,

    /** Core is in low-power offline state (hotplugged out). */
    OFFLINE,

    /** Core does not support hotplugging via sysfs. */
    UNSUPPORTED,

    /** Primary system execution core (CPU0) protected from hotplugging. */
    PROTECTED,

    /** Core status could not be determined. */
    UNKNOWN,

    /** Core encountered a kernel or sysfs I/O error. */
    ERROR
}
