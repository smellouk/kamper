package com.smellouk.kamper.gpu.repository.source

import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.gpu.GpuInfo
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.FileReader

/**
 * Reads Adreno GPU data from kgsl sysfs (D-05).
 * Utilization: tries `gpu_busy_percentage` (pre-computed %) first, falls back to
 * `gpubusy` (raw busy/total ticks ratio — same data, different format).
 * Memory: reads `KgslShmemUsage` from `/proc/meminfo` — world-readable on all Android,
 * gives VRAM shared memory in KB.
 */
internal class KgslGpuInfoSource(private val logger: Logger) : GpuInfoSource {
    private var diagnosed = false

    override fun getInfo(): GpuInfo = try {
        val utilization = readUtilization()
        val usedMemoryMb = readKgslMemoryMb()
        if (utilization != null) {
            GpuInfo(utilization = utilization, usedMemoryMb = usedMemoryMb, totalMemoryMb = -1.0)
        } else {
            if (!diagnosed) {
                diagnosed = true
                logger.log(
                    "[GPU/KGSL] INVALID: $PATH_BUSY_PCT and $PATH_GPUBUSY both not readable"
                )
            }
            GpuInfo.INVALID
        }
    } catch (e: Exception) {
        logger.log("[GPU/KGSL] exception: ${e.message}")
        GpuInfo.INVALID
    }

    private fun readUtilization(): Double? {
        readBusyPercentage()?.let { return it }
        return readGpuBusyRatio()
    }

    private fun readBusyPercentage(): Double? = try {
        val line = FileInputStream(PATH_BUSY_PCT).bufferedReader().use { it.readLine() }?.trim()
        line?.toLongOrNull()?.toDouble()?.takeIf { it in MIN_PCT..MAX_PCT }
    } catch (_: Exception) {
        null
    }

    private fun readGpuBusyRatio(): Double? = try {
        val line = FileInputStream(PATH_GPUBUSY).bufferedReader().use { it.readLine() }?.trim()
        val parts = line?.split("\\s+".toRegex())
        if (parts != null && parts.size == 2) {
            val busy = parts[0].toLongOrNull()
            val total = parts[1].toLongOrNull()
            if (busy != null && total != null && total > 0L) {
                ((busy.toDouble() / total.toDouble()) * PCT_MULTIPLIER).coerceIn(MIN_PCT, MAX_PCT)
            } else {
                null
            }
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }

    private fun readKgslMemoryMb(): Double = try {
        BufferedReader(FileReader(PROC_MEMINFO)).use { reader ->
            reader.lineSequence()
                .firstOrNull { it.startsWith(KGSL_SHMEM_KEY) }
                ?.split("\\s+".toRegex())
                ?.getOrNull(1)
                ?.toLongOrNull()
                ?.let { it.toDouble() / KB_PER_MB }
                ?: -1.0
        }
    } catch (_: Exception) {
        -1.0
    }

    private companion object {
        const val PATH_BUSY_PCT = "/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage"
        const val PATH_GPUBUSY = "/sys/class/kgsl/kgsl-3d0/gpubusy"
        const val PROC_MEMINFO = "/proc/meminfo"
        const val KGSL_SHMEM_KEY = "KgslShmemUsage:"
        const val MIN_PCT = 0.0
        const val MAX_PCT = 100.0
        const val PCT_MULTIPLIER = 100.0
        const val KB_PER_MB = 1024.0
    }
}
