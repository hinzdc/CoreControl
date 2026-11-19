package com.sargaverse.corecontrol.data.root

/**
 * Result of executing a shell command.
 */
data class ShellResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String
) {
    val isSuccess: Boolean
        get() = exitCode == 0
}
