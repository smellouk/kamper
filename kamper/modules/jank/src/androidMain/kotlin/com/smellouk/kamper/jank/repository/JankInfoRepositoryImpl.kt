package com.smellouk.kamper.jank.repository

import android.os.Build
import android.view.FrameMetrics
import com.smellouk.kamper.jank.JankInfo

internal class JankInfoRepositoryImpl(
    private val frameTracker: JankFrameTracker,
    private val jankThresholdMs: Long
) : JankInfoRepository {

    override fun getInfo(): JankInfo {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return JankInfo.INVALID
        val snapshot = frameTracker.snapshot()
        if (snapshot.totalFrames == 0) return JankInfo.INVALID

        val droppedFrames = snapshot.frameDurationsMs.count { it > jankThresholdMs }
        val jankyRatio = droppedFrames.toFloat() / snapshot.totalFrames
        val worstMs = snapshot.frameDurationsMs.maxOrNull() ?: 0L

        return JankInfo(
            droppedFrames = droppedFrames,
            jankyFrameRatio = jankyRatio,
            worstFrameMs = worstMs
        )
    }
}

internal data class FrameSnapshot(
    val totalFrames: Int,
    val frameDurationsMs: List<Long>
)

internal class JankFrameTracker {
    private val durations = mutableListOf<Long>()

    // onFrame is only called from Window.OnFrameMetricsAvailableListener which is API 24+;
    // the caller (JankPerformance) only registers the listener on API >= N.
    @Suppress("NewApi")
    fun onFrame(metrics: FrameMetrics) {
        val totalNs = metrics.getMetric(FrameMetrics.TOTAL_DURATION)
        if (totalNs > 0) {
            synchronized(durations) {
                durations.add(totalNs / 1_000_000L)
                if (durations.size > MAX_FRAMES) durations.removeAt(0)
            }
        }
    }

    fun snapshot(): FrameSnapshot = synchronized(durations) {
        FrameSnapshot(durations.size, durations.toList())
    }

    private companion object {
        const val MAX_FRAMES = 120
    }
}
