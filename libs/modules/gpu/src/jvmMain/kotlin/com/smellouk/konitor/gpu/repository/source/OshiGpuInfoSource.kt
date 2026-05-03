package com.smellouk.konitor.gpu.repository.source

import com.smellouk.konitor.gpu.GpuInfo
import oshi.SystemInfo

/**
 * JVM GPU source backed by OSHI 7.0.0. OSHI exposes only static GPU info — there is
 * NO utilization API on `GraphicsCard` (verified: 23-RESEARCH.md, OSHI issues #1248, #2112).
 *
 * Strategy per D-02 partial-data and D-06:
 *   - No graphics cards detected   → GpuInfo.UNSUPPORTED
 *   - Card found, VRAM bytes > 0   → GpuInfo with totalMemoryMb=vramMb; on macOS the IOKit
 *                                    stats overlay provides utilization + renderer/tiler breakdown
 *   - Any exception                → GpuInfo.INVALID (CLAUDE.md D-06 safety rule)
 */
internal class OshiGpuInfoSource : GpuInfoSource {
    private val hardware = runCatching { SystemInfo().hardware }.getOrNull()

    override fun getInfo(): GpuInfo = try {
        val cards = hardware?.graphicsCards ?: return GpuInfo.UNSUPPORTED
        if (cards.isEmpty()) return GpuInfo.UNSUPPORTED
        val card = cards[0]
        val vramBytes = card.vRam
        if (vramBytes <= 0L) {
            return GpuInfo.UNSUPPORTED
        }
        val vramMb = vramBytes.toDouble() / BYTES_PER_MB
        if (!IS_MACOS) {
            return GpuInfo(utilization = -1.0, usedMemoryMb = -1.0, totalMemoryMb = vramMb)
        }
        val stats = MacOsIoKitGpuSource.getStats()
        GpuInfo(
            utilization = stats.utilization,
            usedMemoryMb = -1.0,
            totalMemoryMb = vramMb,
            rendererUtilization = stats.rendererUtilization,
            tilerUtilization = stats.tilerUtilization
        )
    } catch (_: Exception) {
        GpuInfo.INVALID
    }

    private companion object {
        const val BYTES_PER_MB: Double = 1024.0 * 1024.0
        val IS_MACOS = System.getProperty("os.name").orEmpty().lowercase().contains("mac")
    }
}
