package com.smellouk.kamper.gc.repository

import android.os.Build
import android.os.Debug
import androidx.annotation.RequiresApi
import com.smellouk.kamper.gc.GcInfo

internal class GcInfoRepositoryImpl : GcInfoRepository {
    // ART stats path
    private var lastArtGcCount = -1L
    private var lastArtGcPauseMs = -1L

    // Heap-heuristic fallback (used when ART stats are unavailable)
    private var inferredGcCount = 0L
    private var lastUsedHeapBytes = -1L
    private var fallbackLastReported = -1L

    override fun getInfo(): GcInfo {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return heapHeuristicInfo()

        val gcCount = runCatching { getRuntimeStat("art.gc.gc-count") }.getOrNull()
            ?: return heapHeuristicInfo()
        val gcPauseMs = runCatching { getRuntimeStat("art.gc.gc-time") }.getOrNull()
            ?: return heapHeuristicInfo()

        val countDelta = if (lastArtGcCount < 0) 0L else gcCount - lastArtGcCount
        val pauseDelta = if (lastArtGcPauseMs < 0) 0L else gcPauseMs - lastArtGcPauseMs

        lastArtGcCount = gcCount
        lastArtGcPauseMs = gcPauseMs

        return GcInfo(
            gcCount = gcCount,
            gcPauseMs = gcPauseMs,
            gcCountDelta = countDelta,
            gcPauseMsDelta = pauseDelta
        )
    }

    // Detects GC cycles via heap size drops when ART runtime stats are not available.
    // gcPauseMs is 0 because Runtime provides no pause-time information.
    private fun heapHeuristicInfo(): GcInfo {
        val rt = Runtime.getRuntime()
        val usedNow = rt.totalMemory() - rt.freeMemory()
        if (lastUsedHeapBytes >= 0 && usedNow < lastUsedHeapBytes - GC_HEAP_DROP_BYTES) {
            inferredGcCount++
        }
        lastUsedHeapBytes = usedNow
        val delta = if (fallbackLastReported < 0) 0L else inferredGcCount - fallbackLastReported
        fallbackLastReported = inferredGcCount
        return GcInfo(
            gcCount = inferredGcCount,
            gcPauseMs = 0L,
            gcCountDelta = delta,
            gcPauseMsDelta = 0L
        )
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getRuntimeStat(stat: String): Long? =
        Debug.getRuntimeStat(stat).toLongOrNull()

    private companion object {
        const val GC_HEAP_DROP_BYTES = 512 * 1024L
    }
}
