package com.sargaverse.corecontrol.domain.model

/**
 * State of device root authorization.
 */
sealed class RootStatus {
    object Granted : RootStatus()
    data class Denied(val reason: String) : RootStatus()
    object NotAvailable : RootStatus()
    object Checking : RootStatus()

    val isGranted: Boolean
        get() = this is Granted
}
