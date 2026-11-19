package com.sargaverse.corecontrol.domain.repository

import com.sargaverse.corecontrol.data.root.ShellResult
import com.sargaverse.corecontrol.domain.model.RootStatus

/**
 * Repository interface for checking root status and running validated shell commands.
 */
interface RootRepository {
    /**
     * Checks if root / su is accessible (KernelSU, Magisk, APatch, etc.).
     */
    suspend fun checkRootStatus(): RootStatus

    /**
     * Executes a command via su with strict timeout and output capture.
     */
    suspend fun executeSu(command: String, timeoutMs: Long = 5000): ShellResult
}
