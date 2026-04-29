package com.smellouk.kamper.memory.repository.source

import com.smellouk.kamper.memory.repository.MemoryInfoDto
import com.sun.management.OperatingSystemMXBean
import java.lang.management.ManagementFactory

internal class JvmMemoryInfoSource {
    private val osBean: OperatingSystemMXBean? = runCatching {
        ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java)
    }.getOrNull()

    fun getMemoryInfoDto(): MemoryInfoDto {
        val runtime = Runtime.getRuntime()
        val maxMemory = runtime.maxMemory()
        val allocatedInBytes = runtime.totalMemory() - runtime.freeMemory()

        val totalRam = osBean?.totalMemorySize ?: 0L
        val availRam = osBean?.freeMemorySize ?: 0L
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

    private companion object {
        const val LOW_MEMORY_THRESHOLD_DIVISOR = 10L
    }
}
