package com.sargaverse.corecontrol.data.cpu

import android.os.Build
import com.sargaverse.corecontrol.data.root.RootEngine
import com.sargaverse.corecontrol.domain.model.CpuCapabilityState
import com.sargaverse.corecontrol.domain.model.CpuCluster
import com.sargaverse.corecontrol.domain.model.CpuCore
import com.sargaverse.corecontrol.domain.model.SystemInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Low-level Linux Sysfs parser for reading CPU topology, frequencies,
 * core online status, and platform architecture.
 */
class SysfsCpuReader(
    private val rootEngine: RootEngine = RootEngine()
) {

    companion object {
        private const val SYS_CPU_DIR = "/sys/devices/system/cpu"
    }

    /**
     * Parses Linux sysfs CPU range string (e.g., "0-3,5-7" or "0-7" or "0").
     */
    fun parseCpuRange(rangeString: String?): Set<Int> {
        if (rangeString.isNullOrBlank()) return emptySet()
        val result = mutableSetOf<Int>()
        val segments = rangeString.trim().split(",")
        for (segment in segments) {
            val trimmed = segment.trim()
            if (trimmed.isEmpty()) continue
            if (trimmed.contains("-")) {
                val parts = trimmed.split("-")
                if (parts.size == 2) {
                    val start = parts[0].toIntOrNull()
                    val end = parts[1].toIntOrNull()
                    if (start != null && end != null && start <= end) {
                        for (i in start..end) {
                            result.add(i)
                        }
                    }
                }
            } else {
                trimmed.toIntOrNull()?.let { result.add(it) }
            }
        }
        return result
    }

    /**
     * Discovers all available CPU core IDs on the system.
     */
    suspend fun discoverCpuIds(): List<Int> = withContext(Dispatchers.IO) {
        val cpuDir = File(SYS_CPU_DIR)
        val discovered = mutableListOf<Int>()

        if (cpuDir.exists() && cpuDir.isDirectory) {
            val cpuFolders = cpuDir.listFiles { file ->
                file.isDirectory && file.name.matches(Regex("^cpu[0-9]+$"))
            }
            cpuFolders?.forEach { folder ->
                val id = folder.name.substring(3).toIntOrNull()
                if (id != null) discovered.add(id)
            }
        }

        if (discovered.isEmpty()) {
            val presentRange = readSysfsFile("$SYS_CPU_DIR/present")
            val parsed = parseCpuRange(presentRange)
            if (parsed.isNotEmpty()) {
                discovered.addAll(parsed)
            } else {
                val possibleRange = readSysfsFile("$SYS_CPU_DIR/possible")
                discovered.addAll(parseCpuRange(possibleRange))
            }
        }

        if (discovered.isEmpty()) {
            // Fallback to runtime available processors
            val count = Runtime.getRuntime().availableProcessors()
            for (i in 0 until count) discovered.add(i)
        }

        discovered.sorted()
    }

    /**
     * Reads a sysfs file as regular user, or falls back to root if permission is denied.
     */
    suspend fun readSysfsFile(path: String, useRootFallback: Boolean = false): String? = withContext(Dispatchers.IO) {
        val file = File(path)
        if (file.exists() && file.canRead()) {
            try {
                return@withContext file.readText().trim()
            } catch (_: Exception) {}
        }

        if (useRootFallback) {
            try {
                val res = rootEngine.execute("cat $path", timeoutMs = 2000L)
                if (res.isSuccess && res.stdout.isNotBlank()) {
                    return@withContext res.stdout.trim()
                }
            } catch (_: Exception) {}
        }
        null
    }

    /**
     * Reads the online status and capability of a specific CPU.
     */
    suspend fun readCpuCore(
        cpuId: Int,
        globalOnlineSet: Set<Int>,
        useRootForOnline: Boolean = true
    ): CpuCore = withContext(Dispatchers.IO) {
        val onlineFile = File("$SYS_CPU_DIR/cpu$cpuId/online")
        val hotplugSupported = onlineFile.exists()

        // CPU0 is strictly protected from being turned off
        val isProtected = (cpuId == 0)

        // Read specific online state
        var isOnline: Boolean
        var state: CpuCapabilityState

        val rawOnlineValue = readSysfsFile(onlineFile.absolutePath, useRootFallback = useRootForOnline)

        if (rawOnlineValue != null) {
            isOnline = (rawOnlineValue == "1")
            state = when {
                isProtected -> CpuCapabilityState.PROTECTED
                isOnline -> CpuCapabilityState.ONLINE
                else -> CpuCapabilityState.OFFLINE
            }
        } else {
            // Online file either not accessible or doesn't exist
            if (globalOnlineSet.contains(cpuId)) {
                isOnline = true
                state = if (isProtected) CpuCapabilityState.PROTECTED else {
                    if (hotplugSupported) CpuCapabilityState.ONLINE else CpuCapabilityState.UNSUPPORTED
                }
            } else if (!hotplugSupported && !isProtected) {
                isOnline = true
                state = CpuCapabilityState.UNSUPPORTED
            } else {
                isOnline = false
                state = if (isProtected) CpuCapabilityState.PROTECTED else CpuCapabilityState.OFFLINE
            }
        }

        // Frequencies
        var curFreq: Long? = null
        var minFreq: Long? = null
        var maxFreq: Long? = null
        var governor: String? = null

        if (isOnline) {
            curFreq = readSysfsFile("$SYS_CPU_DIR/cpu$cpuId/cpufreq/scaling_cur_freq", useRootFallback = useRootForOnline)?.toLongOrNull()
                ?: readSysfsFile("$SYS_CPU_DIR/cpu$cpuId/cpufreq/cpuinfo_cur_freq", useRootFallback = useRootForOnline)?.toLongOrNull()
            minFreq = readSysfsFile("$SYS_CPU_DIR/cpu$cpuId/cpufreq/scaling_min_freq", useRootFallback = false)?.toLongOrNull()
            maxFreq = readSysfsFile("$SYS_CPU_DIR/cpu$cpuId/cpufreq/scaling_max_freq", useRootFallback = false)?.toLongOrNull()
            governor = readSysfsFile("$SYS_CPU_DIR/cpu$cpuId/cpufreq/scaling_governor", useRootFallback = false)
        }

        // Topology & capacity
        val clusterId = readSysfsFile("$SYS_CPU_DIR/cpu$cpuId/topology/cluster_id", useRootFallback = false)?.toIntOrNull()
            ?: readSysfsFile("$SYS_CPU_DIR/cpu$cpuId/topology/physical_package_id", useRootFallback = false)?.toIntOrNull()
        val capacity = readSysfsFile("$SYS_CPU_DIR/cpu$cpuId/cpu_capacity", useRootFallback = false)?.toIntOrNull()

        CpuCore(
            cpuId = cpuId,
            state = state,
            isOnline = isOnline,
            curFreqKHz = curFreq,
            minFreqKHz = minFreq,
            maxFreqKHz = maxFreq,
            clusterId = clusterId,
            coreCapacity = capacity,
            hotplugSupported = hotplugSupported,
            isProtected = isProtected,
            governor = governor
        )
    }

    /**
     * Reads all detected CPU cores and returns full list.
     */
    suspend fun readAllCores(useRoot: Boolean = true): List<CpuCore> = withContext(Dispatchers.IO) {
        val cpuIds = discoverCpuIds()
        val globalOnlineRaw = readSysfsFile("$SYS_CPU_DIR/online", useRootFallback = false)
        val globalOnlineSet = parseCpuRange(globalOnlineRaw)

        cpuIds.map { id ->
            readCpuCore(id, globalOnlineSet, useRootForOnline = useRoot)
        }
    }

    /**
     * Resolves clusters based on topology and capacities.
     */
    fun groupClusters(cores: List<CpuCore>): List<CpuCluster> {
        val clustersMap = mutableMapOf<Int, MutableList<CpuCore>>()

        cores.forEach { core ->
            val clusterKey = core.clusterId ?: 0
            clustersMap.getOrPut(clusterKey) { mutableListOf() }.add(core)
        }

        return clustersMap.entries.sortedBy { it.key }.map { (clusterId, clusterCores) ->
            val maxCap = clusterCores.mapNotNull { it.coreCapacity }.maxOrNull()
            val name = when {
                maxCap != null && maxCap >= 1000 -> "Prime / Ultra Core"
                maxCap != null && maxCap >= 800 -> "Performance / Mid Core"
                clusterId == 0 -> "Efficiency / Little Core"
                clusterId == 1 -> "Performance Core"
                clusterId == 2 -> "Prime Core"
                else -> "Cluster $clusterId"
            }
            CpuCluster(
                clusterId = clusterId,
                name = name,
                coreIds = clusterCores.map { it.cpuId },
                maxCapacity = maxCap
            )
        }
    }

    /**
     * Gathers device hardware metadata.
     */
    suspend fun getSystemInfo(cores: List<CpuCore>): SystemInfo = withContext(Dispatchers.IO) {
        val socModel = readSysfsFile("/proc/device-tree/model")
            ?: Build.HARDWARE.ifBlank { Build.BOARD }
        val kernelRelease = System.getProperty("os.version") ?: "Unknown"
        val arch = System.getProperty("os.arch") ?: "arm64"
        val androidVer = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

        var hyperOs: String? = null
        try {
            val p = Runtime.getRuntime().exec(arrayOf("getprop", "ro.build.version.incremental"))
            val output = p.inputStream.bufferedReader().readText().trim()
            if (output.isNotBlank()) hyperOs = output
        } catch (_: Exception) {}

        val clusters = groupClusters(cores)

        SystemInfo(
            socModel = socModel,
            hardwareBoard = Build.BOARD,
            architecture = arch,
            kernelRelease = kernelRelease,
            androidVersion = androidVer,
            hyperOsVersion = hyperOs,
            clusters = clusters
        )
    }
}
