package com.smellouk.kamper.gpu.repository.source

import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.gpu.GpuInfo
import java.io.File

/**
 * Reads GPU data from Google Pixel's /sys/kernel/gpu/ interface (Tensor G1+).
 * Primary: `gpu_busy` — direct utilization percentage (0-100).
 * Fallback: `gpu_freq` / `gpu_max_freq` — frequency-based proxy.
 * Memory: not available via this interface; fields set to -1.0.
 *
 * [sysDir] is overridable for unit tests; production code uses the default.
 */
internal class PixelGpuInfoSource(
    private val logger: Logger,
    private val sysDir: File = File(SYS_KERNEL_GPU)
) : GpuInfoSource {
    private var diagnosed = false

    override fun getInfo(): GpuInfo = try {
        val busy = readBusy()
        if (busy != null) return GpuInfo(busy, -1.0, -1.0)
        val freq = readFreqProxy()
        if (freq != null) return GpuInfo(freq, -1.0, -1.0)
        if (!diagnosed) {
            diagnosed = true
            logger.log(
                "[GPU/Pixel] INVALID: gpu_busy not readable at $sysDir/gpu_busy; " +
                    "gpu_freq/gpu_max_freq also unavailable"
            )
        }
        GpuInfo.INVALID
    } catch (e: Exception) {
        logger.log("[GPU/Pixel] exception: ${e.message}")
        GpuInfo.INVALID
    }

    private fun readBusy(): Double? = try {
        File(sysDir, FILE_GPU_BUSY).readText().trim()
            .toLongOrNull()?.toDouble()?.takeIf { it in MIN_PCT..MAX_PCT }
    } catch (_: Exception) {
        null
    }

    private fun readFreqProxy(): Double? = try {
        val cur = File(sysDir, FILE_GPU_FREQ).readText().trim().toLongOrNull()
            ?: return null
        val max = File(sysDir, FILE_GPU_MAX_FREQ).readText().trim().toLongOrNull()
            ?.takeIf { it > 0L } ?: return null
        ((cur.toDouble() / max.toDouble()) * PCT_MULTIPLIER).coerceIn(MIN_PCT, MAX_PCT)
    } catch (_: Exception) {
        null
    }

    private companion object {
        const val SYS_KERNEL_GPU = "/sys/kernel/gpu"
        const val FILE_GPU_BUSY = "gpu_busy"
        const val FILE_GPU_FREQ = "gpu_freq"
        const val FILE_GPU_MAX_FREQ = "gpu_max_freq"
        const val MIN_PCT = 0.0
        const val MAX_PCT = 100.0
        const val PCT_MULTIPLIER = 100.0
    }
}
