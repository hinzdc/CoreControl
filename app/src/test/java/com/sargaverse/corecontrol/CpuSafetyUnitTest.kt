package com.sargaverse.corecontrol

import com.sargaverse.corecontrol.data.cpu.CpuRepositoryImpl
import com.sargaverse.corecontrol.data.cpu.SysfsCpuReader
import com.sargaverse.corecontrol.data.history.OperationLogRepositoryImpl
import com.sargaverse.corecontrol.data.root.RootEngine
import com.sargaverse.corecontrol.data.root.ShellResult
import com.sargaverse.corecontrol.domain.model.CpuCapabilityState
import com.sargaverse.corecontrol.domain.model.CpuCore
import com.sargaverse.corecontrol.domain.model.OperationResult
import com.sargaverse.corecontrol.domain.repository.CpuRepository
import com.sargaverse.corecontrol.domain.usecase.BulkCpuControlUseCase
import com.sargaverse.corecontrol.domain.usecase.ToggleCpuCoreUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying CPU safety policies, CPU0 protection,
 * and the Absolute Core Control Rule.
 */
class CpuSafetyUnitTest {

    /**
     * In-memory simulated hardware sysfs state for 8 CPU cores.
     */
    class FakeHardwareSysfs(
        val coreCount: Int = 8
    ) {
        val onlineState = mutableMapOf<Int, Boolean>()
        val commandLog = mutableListOf<String>()

        init {
            // Initially all cores are online
            for (i in 0 until coreCount) {
                onlineState[i] = true
            }
        }

        fun executeCommand(command: String): ShellResult {
            commandLog.add(command)
            // Parse: echo <val> > /sys/devices/system/cpu/cpu<N>/online
            val regex = Regex("""echo\s+([01])\s+>\s+/sys/devices/system/cpu/cpu(\d+)/online""")
            val match = regex.matchEntire(command.trim())
            if (match != null) {
                val value = match.groupValues[1] == "1"
                val targetCpu = match.groupValues[2].toInt()

                // Hardware simulation: CPU0 is physically protected by kernel
                if (targetCpu == 0 && !value) {
                    return ShellResult(1, "", "Permission denied: CPU0 cannot be disabled")
                }

                onlineState[targetCpu] = value
                return ShellResult(0, "", "")
            }
            return ShellResult(1, "", "Invalid sysfs command")
        }

        fun getCores(): List<CpuCore> {
            return (0 until coreCount).map { i ->
                val isOnline = onlineState[i] ?: false
                val cluster = when (i) {
                    in 0..3 -> 0 // Little
                    in 4..6 -> 1 // Mid
                    else -> 2    // Prime
                }
                CpuCore(
                    cpuId = i,
                    state = if (i == 0) CpuCapabilityState.PROTECTED else if (isOnline) CpuCapabilityState.ONLINE else CpuCapabilityState.OFFLINE,
                    isOnline = isOnline,
                    curFreqKHz = if (isOnline) 1000000L else null,
                    clusterId = cluster,
                    hotplugSupported = true,
                    isProtected = (i == 0)
                )
            }
        }
    }

    /**
     * Test repository wrapping the fake hardware state.
     */
    class TestCpuRepository(
        private val hardware: FakeHardwareSysfs
    ) : CpuRepository {
        private val rootEngine = RootEngine()

        override suspend fun detectCpus(): List<CpuCore> = hardware.getCores()

        override suspend fun getCpuCore(cpuId: Int): CpuCore? = hardware.getCores().find { it.cpuId == cpuId }

        override suspend fun toggleCpuCore(cpuId: Int, targetOnline: Boolean): OperationResult {
            if (!rootEngine.validateCpuId(cpuId)) {
                return OperationResult.Failure(cpuId, targetOnline, "Invalid CPU ID: $cpuId")
            }
            if (cpuId == 0 && !targetOnline) {
                return OperationResult.Failure(0, false, "CPU0 is protected from being disabled.")
            }
            val current = getCpuCore(cpuId) ?: return OperationResult.Failure(cpuId, targetOnline, "Not found")
            if (current.isOnline == targetOnline) {
                return OperationResult.Success(cpuId, targetOnline, current.state, "Already in state")
            }

            val cmd = rootEngine.buildCpuToggleCommand(cpuId, targetOnline)
            val res = hardware.executeCommand(cmd)

            val verified = getCpuCore(cpuId)
            return if (verified?.isOnline == targetOnline) {
                OperationResult.Success(cpuId, targetOnline, verified.state, "Success")
            } else {
                OperationResult.Failure(cpuId, targetOnline, "Verification failed: ${res.stderr}", res.exitCode, verified?.state)
            }
        }

        override suspend fun bulkSetCpus(targetOnline: Boolean): List<OperationResult> {
            val results = mutableListOf<OperationResult>()
            val cores = detectCpus()
            if (targetOnline) {
                cores.filter { !it.isOnline && it.hotplugSupported }.forEach {
                    results.add(toggleCpuCore(it.cpuId, true))
                }
            } else {
                // Protect CPU0 and preserve at least 1 core per cluster
                val preserved = setOf(0, 4, 7)
                cores.filter { it.cpuId !in preserved && it.isOnline }.forEach {
                    results.add(toggleCpuCore(it.cpuId, false))
                }
            }
            return results
        }

        override suspend fun getSystemInfo() = throw NotImplementedError()
        override suspend fun getSummary() = throw NotImplementedError()
    }

    private lateinit var fakeHardware: FakeHardwareSysfs
    private lateinit var repository: TestCpuRepository
    private lateinit var logRepository: OperationLogRepositoryImpl
    private lateinit var toggleUseCase: ToggleCpuCoreUseCase
    private lateinit var bulkUseCase: BulkCpuControlUseCase

    @Before
    fun setUp() {
        fakeHardware = FakeHardwareSysfs(8)
        repository = TestCpuRepository(fakeHardware)
        logRepository = OperationLogRepositoryImpl()
        toggleUseCase = ToggleCpuCoreUseCase(repository, logRepository)
        bulkUseCase = BulkCpuControlUseCase(repository, logRepository)
    }

    @Test
    fun testCpu0ProtectionNeverAllowsDisabling() = runTest {
        val result = toggleUseCase(0)
        assertTrue("Disabling CPU0 must fail", result is OperationResult.Failure)
        val failure = result as OperationResult.Failure
        assertEquals(0, failure.cpuId)
        assertTrue(failure.errorMessage.contains("CPU0 is protected"))

        // Verify CPU0 is still online
        val core0 = repository.getCpuCore(0)
        assertNotNull(core0)
        assertTrue("CPU0 must remain online", core0!!.isOnline)
    }

    /**
     * CRITICAL REQUIREMENT TEST:
     * When CPU 3 is disabled, verify that CPU 0, 1, 2, 4, 5, 6, and 7
     * have not been modified and remain online.
     */
    @Test
    fun testCriticalRule_DisablingCpu3ModifiesOnlyCpu3() = runTest {
        // Record initial state
        val initialCores = repository.detectCpus()
        for (core in initialCores) {
            assertTrue("Initially all cores should be online", core.isOnline)
        }

        // Action: Disable CPU 3
        val result = toggleUseCase(3)
        assertTrue("Toggling CPU 3 OFF must succeed", result is OperationResult.Success)

        // Verify only 1 shell command was dispatched targeting strictly cpu3
        assertEquals(1, fakeHardware.commandLog.size)
        assertEquals("echo 0 > /sys/devices/system/cpu/cpu3/online", fakeHardware.commandLog.first())

        // Verify CPU 3 is offline
        val cpu3 = repository.getCpuCore(3)
        assertNotNull(cpu3)
        assertFalse("CPU 3 must now be offline", cpu3!!.isOnline)

        // Verify EVERY OTHER CPU (0, 1, 2, 4, 5, 6, 7) is strictly untouched and ONLINE
        val otherCpus = listOf(0, 1, 2, 4, 5, 6, 7)
        for (id in otherCpus) {
            val core = repository.getCpuCore(id)
            assertNotNull("CPU $id must exist", core)
            assertTrue("CPU $id MUST REMAIN ONLINE when CPU 3 was toggled", core!!.isOnline)
        }
    }

    @Test
    fun testTogglingCpu3BackOnlineModifiesOnlyCpu3() = runTest {
        // First disable CPU 3
        toggleUseCase(3)
        assertFalse(repository.getCpuCore(3)!!.isOnline)

        // Action: Turn CPU 3 back ON
        val result = toggleUseCase(3)
        assertTrue("Toggling CPU 3 ON must succeed", result is OperationResult.Success)

        val cpu3 = repository.getCpuCore(3)
        assertTrue("CPU 3 must be online again", cpu3!!.isOnline)
    }

    @Test
    fun testCpuIdValidationPreventsInjection() {
        val rootEngine = RootEngine()
        assertTrue(rootEngine.validateCpuId(0))
        assertTrue(rootEngine.validateCpuId(7))
        assertTrue(rootEngine.validateCpuId(15))

        assertFalse(rootEngine.validateCpuId(-1))
        assertFalse(rootEngine.validateCpuId(9999))
    }

    @Test
    fun testBulkAllOffPreservesCpu0AndClusterHeads() = runTest {
        val results = bulkUseCase.turnAllOff()
        assertTrue(results.isNotEmpty())

        // Core 0 must be online
        assertTrue(repository.getCpuCore(0)!!.isOnline)
        // Core 4 (Cluster 1 head) must be online
        assertTrue(repository.getCpuCore(4)!!.isOnline)
        // Core 7 (Cluster 2 head) must be online
        assertTrue(repository.getCpuCore(7)!!.isOnline)

        // Secondary non-preserved cores should be offline
        assertFalse(repository.getCpuCore(1)!!.isOnline)
        assertFalse(repository.getCpuCore(2)!!.isOnline)
        assertFalse(repository.getCpuCore(3)!!.isOnline)
        assertFalse(repository.getCpuCore(5)!!.isOnline)
        assertFalse(repository.getCpuCore(6)!!.isOnline)
    }

    @Test
    fun testBulkAllOnBringsAllCoresOnline() = runTest {
        // First turn some off
        toggleUseCase(1)
        toggleUseCase(2)
        toggleUseCase(3)

        val results = bulkUseCase.turnAllOn()
        assertTrue(results.isNotEmpty())

        val allCores = repository.detectCpus()
        for (c in allCores) {
            assertTrue("Core ${c.cpuId} should be online after Bulk ALL ON", c.isOnline)
        }
    }
}
