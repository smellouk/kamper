package com.smellouk.konitor.memory.repository.source

import com.smellouk.konitor.memory.repository.MemoryInfoDto
import com.sun.management.OperatingSystemMXBean
import java.lang.management.ManagementFactory

internal class JvmMemoryInfoSource {
    private val osBean: OperatingSystemMXBean? = runCatching {
        ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java)
    }.getOrNull()

    private val isMacOs: Boolean = System.getProperty("os.name")
        ?.lowercase()
        ?.contains("mac") == true

    fun getMemoryInfoDto(): MemoryInfoDto {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val allocatedInBytes = runtime.totalMemory() - runtime.freeMemory()

        val totalRam = osBean?.totalMemorySize ?: 0L
        val availRam = if (isMacOs) macOsAvailableMemory() else osBean?.freeMemorySize ?: 0L
        val threshold = totalRam / LOW_MEMORY_THRESHOLD_DIVISOR
        val isLowMemory = availRam > 0L && availRam < threshold

        return MemoryInfoDto(
            maxMemoryInBytes = maxMemory,
            allocatedInBytes = allocatedInBytes,
            totalPssInKiloBytes = null,
            dalvikPssInKiloBytes = null,
            nativePssInKiloBytes = null,
            otherPssInKiloBytes = null,
            availableRamInBytes = availRam,
            totalRamInBytes = totalRam,
            lowRamThresholdInBytes = threshold,
            isLowMemory = isLowMemory
        )
    }

    // On macOS, OperatingSystemMXBean.freeMemorySize returns only truly-free pages (~100 MB)
    // because macOS uses nearly all RAM for file caching. vm_stat exposes the full picture:
    // available = (free + inactive + speculative) × pageSize, matching Activity Monitor.
    private fun macOsAvailableMemory(): Long = runCatching {
        val pageSize = ProcessBuilder("sysctl", "-n", "hw.pagesize")
            .start().inputStream.bufferedReader().readText().trim().toLong()
        val vmStat = ProcessBuilder("vm_stat")
            .start().inputStream.bufferedReader().readText()
        val free = vmStat.parseVmStatPages("Pages free:")
        val inactive = vmStat.parseVmStatPages("Pages inactive:")
        val speculative = vmStat.parseVmStatPages("Pages speculative:")
        (free + inactive + speculative) * pageSize
    }.getOrElse { osBean?.freeMemorySize ?: 0L }

    private companion object {
        const val LOW_MEMORY_THRESHOLD_DIVISOR = 10L

        fun String.parseVmStatPages(key: String): Long =
            lines().firstOrNull { it.trimStart().startsWith(key) }
                ?.substringAfter(":")?.trim()?.trimEnd('.')?.toLongOrNull() ?: 0L
    }
}
