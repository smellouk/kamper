package com.smellouk.konitor.gpu.repository.source

import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.gpu.GpuInfo
import com.smellouk.konitor.gpu.repository.DevfreqAccessibilityProvider
import java.io.File

/**
 * Mali devfreq fallback (D-05). Reads `cur_freq` / max for utilization (frequency-based proxy)
 * and `dma_buf_gpu_mem` / `total_gpu_mem` for memory (both world-readable on stock Pixel/Tensor).
 * Returns partial data (D-02) when utilization is unavailable but memory is readable — callers
 * see utilization=-1.0 with real memory values rather than a full INVALID.
 *
 * [dirProvider] is overridable for unit tests; production code uses the default.
 */
internal class DevfreqGpuInfoSource(
    private val logger: Logger,
    private val dirProvider: () -> File? = { DevfreqAccessibilityProvider.findMaliDir() }
) : GpuInfoSource {
    private var diagnosed = false

    override fun getInfo(): GpuInfo = try {
        val dir = dirProvider()
        if (dir == null) {
            if (!diagnosed) {
                diagnosed = true
                logger.log("[GPU/devfreq] INVALID: no mali sysfs dir found")
            }
            return GpuInfo.INVALID
        }
        val curFreq = readLong(dir, "cur_freq")
        val maxFreq = readMaxFreq(dir)
        val utilization = if (curFreq != null && maxFreq != null) {
            computeUtilization(curFreq, maxFreq)
        } else {
            UNKNOWN
        }
        val usedMemoryMb = readMemoryMb(dir, FILE_DMA_BUF_MEM)
        val totalMemoryMb = readMemoryMb(dir, FILE_TOTAL_MEM)

        return when {
            utilization >= MIN_PCT || usedMemoryMb >= 0.0 || totalMemoryMb >= 0.0 -> {
                if (!diagnosed && utilization < MIN_PCT) {
                    diagnosed = true
                    logger.log(
                        "[GPU/devfreq] partial: dir=$dir — utilization unavailable " +
                            "(no max_freq/available_frequencies); " +
                            "usedMb=$usedMemoryMb totalMb=$totalMemoryMb"
                    )
                }
                GpuInfo(utilization, usedMemoryMb, totalMemoryMb,
                    curFreqKhz = curFreq ?: UNKNOWN_FREQ,
                    maxFreqKhz = maxFreq ?: UNKNOWN_FREQ)
            }
            else -> {
                if (!diagnosed) {
                    diagnosed = true
                    logger.log("[GPU/devfreq] INVALID: no usable data at $dir")
                }
                GpuInfo.INVALID
            }
        }
    } catch (e: Exception) {
        logger.log("[GPU/devfreq] exception: ${e.message}")
        GpuInfo.INVALID
    }

    private fun computeUtilization(curFreq: Long, maxFreq: Long): Double =
        ((curFreq.toDouble() / maxFreq.toDouble()) * PCT_MULTIPLIER).coerceIn(MIN_PCT, MAX_PCT)

    private fun readMaxFreq(dir: File): Long? =
        readLong(dir, "max_freq")?.takeIf { it > 0L }
            ?: readMaxFromAvailableFrequencies(dir)

    private fun readMaxFromAvailableFrequencies(dir: File): Long? = try {
        File(dir, "available_frequencies").readText()
            .trim()
            .splitToSequence("\\s+".toRegex())
            .mapNotNull { it.toLongOrNull() }
            .filter { it > 0L }
            .maxOrNull()
    } catch (_: Exception) {
        null
    }

    private fun readMemoryMb(dir: File, filename: String): Double = try {
        (readLong(dir, filename) ?: return UNKNOWN_MEMORY) / BYTES_PER_MB
    } catch (_: Exception) {
        UNKNOWN_MEMORY
    }

    private fun readLong(dir: File, filename: String): Long? = try {
        File(dir, filename).readText().trim().toLongOrNull()
    } catch (_: Exception) {
        null
    }

    private companion object {
        const val FILE_DMA_BUF_MEM = "dma_buf_gpu_mem"
        const val FILE_TOTAL_MEM = "total_gpu_mem"
        const val PCT_MULTIPLIER = 100.0
        const val MIN_PCT = 0.0
        const val MAX_PCT = 100.0
        const val UNKNOWN = -1.0
        const val UNKNOWN_MEMORY = -1.0
        const val UNKNOWN_FREQ = -1L
        const val BYTES_PER_MB = 1024.0 * 1024.0
    }
}
