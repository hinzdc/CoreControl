package com.sargaverse.corecontrol.data.root

import android.os.SystemClock
import com.sargaverse.corecontrol.domain.model.RootStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Root Engine for safely managing root access and executing commands
 * via KernelSU, Magisk, APatch, or standard su.
 */
class RootEngine {

    companion object {
        private const val DEFAULT_TIMEOUT_MS = 6000L
        private val CPU_ID_PATTERN = Pattern.compile("^[0-9]+$")

        private val SU_BINARY_PATHS = arrayOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/data/adb/ksu/bin/su",
            "/data/adb/ap/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/data/local/su"
        )
    }

    /**
     * Checks if a su binary exists in common locations or in PATH.
     */
    fun isSuBinaryAvailable(): Boolean {
        for (path in SU_BINARY_PATHS) {
            if (File(path).exists()) return true
        }
        val pathEnv = System.getenv("PATH") ?: return false
        for (dir in pathEnv.split(":")) {
            if (File(dir, "su").exists()) return true
        }
        return false
    }

    /**
     * Verifies whether root authorization is actually granted by running `id`.
     */
    suspend fun checkRootAuthorization(): RootStatus = withContext(Dispatchers.IO) {
        if (!isSuBinaryAvailable()) {
            return@withContext RootStatus.NotAvailable
        }

        val result = execute("id", timeoutMs = 4000L)
        if (result.isSuccess && (result.stdout.contains("uid=0") || result.stdout.contains("root"))) {
            RootStatus.Granted
        } else {
            val error = result.stderr.ifBlank { result.stdout }.ifBlank { "Permission denied by superuser manager." }
            RootStatus.Denied(error.trim())
        }
    }

    /**
     * Safely executes a command as root with a timeout.
     * Prevents hanging processes if the root manager dialog waits indefinitely.
     */
    suspend fun execute(command: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS): ShellResult = withContext(Dispatchers.IO) {
        val timedResult = withTimeoutOrNull(timeoutMs) {
            executeInternal(command, timeoutMs)
        }
        timedResult ?: ShellResult(
            exitCode = -1,
            stdout = "",
            stderr = "Command timed out after ${timeoutMs}ms"
        )
    }

    private fun executeInternal(command: String, timeoutMs: Long): ShellResult {
        var process: Process? = null
        return try {
            val pb = ProcessBuilder("su", "-c", command)
            pb.redirectErrorStream(false)
            process = pb.start()

            val stdoutBuilder = StringBuilder()
            val stderrBuilder = StringBuilder()

            val outReader = BufferedReader(InputStreamReader(process.inputStream))
            val errReader = BufferedReader(InputStreamReader(process.errorStream))

            // Thread to drain stdout
            val outThread = Thread {
                try {
                    var line: String?
                    while (outReader.readLine().also { line = it } != null) {
                        stdoutBuilder.append(line).append("\n")
                    }
                } catch (_: Exception) {}
            }

            // Thread to drain stderr
            val errThread = Thread {
                try {
                    var line: String?
                    while (errReader.readLine().also { line = it } != null) {
                        stderrBuilder.append(line).append("\n")
                    }
                } catch (_: Exception) {}
            }

            outThread.start()
            errThread.start()

            val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                return ShellResult(
                    exitCode = -1,
                    stdout = stdoutBuilder.toString().trim(),
                    stderr = "Timeout waiting for su process execution"
                )
            }

            outThread.join(500)
            errThread.join(500)

            ShellResult(
                exitCode = process.exitValue(),
                stdout = stdoutBuilder.toString().trim(),
                stderr = stderrBuilder.toString().trim()
            )
        } catch (e: Exception) {
            ShellResult(
                exitCode = -1,
                stdout = "",
                stderr = e.message ?: "Unknown root execution exception"
            )
        } finally {
            process?.destroy()
        }
    }

    /**
     * Strict validation for CPU ID input to prevent shell injection.
     */
    fun validateCpuId(cpuId: Int): Boolean {
        return cpuId in 0..127 && CPU_ID_PATTERN.matcher(cpuId.toString()).matches()
    }

    /**
     * Generates a safe command to toggle a specific CPU core.
     * Enforces that cpuId is strictly an integer.
     */
    fun buildCpuToggleCommand(cpuId: Int, online: Boolean): String {
        require(validateCpuId(cpuId)) { "Invalid CPU ID: $cpuId" }
        val value = if (online) "1" else "0"
        return "echo $value > /sys/devices/system/cpu/cpu$cpuId/online"
    }
}
