package com.smellouk.kamper.gpu.repository.source

import com.smellouk.kamper.gpu.GpuInfo

/**
 * Android GPU breakdown source backed by /proc/self/fdinfo DRM engine counters.
 * Reads cumulative drm-engine-* busy-time nanoseconds and converts to
 * per-engine utilization% by comparing consecutive samples.
 *
 * Fields populated: appUtilization, rendererUtilization, tilerUtilization, computeUtilization.
 * Returns GpuInfo.INVALID on the first call (no prior sample) or if the JNI call fails.
 * All exceptions absorbed per CLAUDE.md D-06 safety rule.
 */
internal class FdinfoGpuSource : GpuInfoSource {
    private var prevNs: DoubleArray? = null
    private var prevTimeNs: Long = 0L

    override fun getInfo(): GpuInfo = try {
        val nowNs = System.nanoTime()
        val current = FdinfoJni.readEngineNs() ?: return GpuInfo.INVALID

        val prev = prevNs
        val prevTime = prevTimeNs
        prevNs = current
        prevTimeNs = nowNs

        if (prev == null || prevTime == 0L || nowNs <= prevTime) return GpuInfo.INVALID

        val elapsedNs = (nowNs - prevTime).toDouble()

        fun pct(idx: Int) = ((current[idx] - prev[idx]) / elapsedNs * PCT_SCALE).coerceIn(MIN_PCT, MAX_PCT)

        GpuInfo(
            utilization = -1.0,
            usedMemoryMb = -1.0,
            totalMemoryMb = -1.0,
            appUtilization = pct(IDX_TOTAL),
            rendererUtilization = pct(IDX_RENDER),
            tilerUtilization = pct(IDX_TILER),
            computeUtilization = pct(IDX_COMPUTE)
        )
    } catch (_: Exception) {
        GpuInfo.INVALID
    }

    private companion object {
        const val IDX_RENDER = 0
        const val IDX_TILER = 1
        const val IDX_COMPUTE = 2
        const val IDX_TOTAL = 3
        const val MIN_PCT = 0.0
        const val MAX_PCT = 100.0
        const val PCT_SCALE = 100.0
    }
}
