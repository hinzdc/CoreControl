package com.sargaverse.corecontrol

import com.sargaverse.corecontrol.data.cpu.SysfsCpuReader
import com.sargaverse.corecontrol.domain.model.CpuCapabilityState
import com.sargaverse.corecontrol.domain.model.CpuCore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SysfsParserUnitTest {

    private val reader = SysfsCpuReader()

    @Test
    fun testParseCpuRange() {
        // Continuous range
        assertEquals(setOf(0, 1, 2, 3, 4, 5, 6, 7), reader.parseCpuRange("0-7"))

        // Segmented range
        assertEquals(setOf(0, 1, 2, 3, 5, 6, 7), reader.parseCpuRange("0-3,5-7"))

        // Single core
        assertEquals(setOf(0), reader.parseCpuRange("0"))

        // Mixed singles and ranges
        assertEquals(setOf(0, 2, 4, 5, 6), reader.parseCpuRange("0,2,4-6"))

        // Empty / Null
        assertTrue(reader.parseCpuRange("").isEmpty())
        assertTrue(reader.parseCpuRange(null).isEmpty())
    }

    @Test
    fun testCoreControllability() {
        val cpu0 = CpuCore(cpuId = 0, state = CpuCapabilityState.PROTECTED, isOnline = true, hotplugSupported = true, isProtected = true)
        assertFalse("CPU0 must never be controllable", cpu0.isControllable)

        val cpu1Online = CpuCore(cpuId = 1, state = CpuCapabilityState.ONLINE, isOnline = true, hotplugSupported = true, isProtected = false)
        assertTrue("CPU1 online with hotplug must be controllable", cpu1Online.isControllable)

        val cpu1Offline = CpuCore(cpuId = 1, state = CpuCapabilityState.OFFLINE, isOnline = false, hotplugSupported = true, isProtected = false)
        assertTrue("CPU1 offline with hotplug must be controllable", cpu1Offline.isControllable)

        val cpu2Unsupported = CpuCore(cpuId = 2, state = CpuCapabilityState.UNSUPPORTED, isOnline = true, hotplugSupported = false, isProtected = false)
        assertFalse("Core without hotplug must not be controllable", cpu2Unsupported.isControllable)
    }

    @Test
    fun testClusterGrouping() {
        val cores = listOf(
            CpuCore(cpuId = 0, state = CpuCapabilityState.PROTECTED, isOnline = true, clusterId = 0, coreCapacity = 698),
            CpuCore(cpuId = 1, state = CpuCapabilityState.ONLINE, isOnline = true, clusterId = 0, coreCapacity = 698),
            CpuCore(cpuId = 4, state = CpuCapabilityState.ONLINE, isOnline = true, clusterId = 1, coreCapacity = 947),
            CpuCore(cpuId = 7, state = CpuCapabilityState.ONLINE, isOnline = true, clusterId = 2, coreCapacity = 1024)
        )

        val clusters = reader.groupClusters(cores)
        assertEquals(3, clusters.size)
        assertEquals(listOf(0, 1), clusters[0].coreIds)
        assertEquals(listOf(4), clusters[1].coreIds)
        assertEquals(listOf(7), clusters[2].coreIds)
        assertTrue(clusters[2].name.contains("Prime"))
    }
}
