@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.smellouk.kamper.gpu.repository.source

import com.smellouk.kamper.gpu.GpuInfo
import com.smellouk.kamper.gpu.cinterop.kamper_gpu_stats
import kotlinx.cinterop.useContents

/**
 * macOS GPU source backed by IOKit IOAccelerator (D-07).
 * Reads overall utilization, renderer utilization, and tiler utilization from
 * PerformanceStatistics. Returns UNSUPPORTED when no IOAccelerator service exists.
 * All exceptions absorbed per CLAUDE.md D-06 safety rule.
 */
internal class MacosGpuInfoSource : GpuInfoSource {
    override fun getInfo(): GpuInfo = try {
        kamper_gpu_stats().useContents {
            when {
                utilization <= UNSUPPORTED_SENTINEL -> GpuInfo.UNSUPPORTED
                utilization < 0.0 -> GpuInfo.INVALID
                else -> GpuInfo(
                    utilization = utilization,
                    usedMemoryMb = -1.0,
                    totalMemoryMb = -1.0,
                    rendererUtilization = if (rendererUtil >= 0.0) rendererUtil else -1.0,
                    tilerUtilization = if (tilerUtil >= 0.0) tilerUtil else -1.0
                )
            }
        }
    } catch (_: Exception) {
        GpuInfo.INVALID
    }

    private companion object {
        const val UNSUPPORTED_SENTINEL = -2.0
    }
}
