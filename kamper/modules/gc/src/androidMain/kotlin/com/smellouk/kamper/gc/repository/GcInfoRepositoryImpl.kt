package com.smellouk.kamper.gc.repository

import android.os.Debug
import com.smellouk.kamper.gc.GcInfo

internal class GcInfoRepositoryImpl : GcInfoRepository {
    private var lastGcCount = -1L
    private var lastGcPauseMs = -1L

    override fun getInfo(): GcInfo {
        val gcCount = runCatching {
            Debug.getRuntimeStat("art.gc.gc-count").toLong()
        }.getOrElse { return GcInfo.INVALID }

        val gcPauseMs = runCatching {
            Debug.getRuntimeStat("art.gc.gc-time").toLong()
        }.getOrElse { return GcInfo.INVALID }

        val countDelta = if (lastGcCount < 0) 0L else gcCount - lastGcCount
        val pauseDelta = if (lastGcPauseMs < 0) 0L else gcPauseMs - lastGcPauseMs

        lastGcCount = gcCount
        lastGcPauseMs = gcPauseMs

        return GcInfo(
            gcCount = gcCount,
            gcPauseMs = gcPauseMs,
            gcCountDelta = countDelta,
            gcPauseMsDelta = pauseDelta
        )
    }
}
