package com.smellouk.kamper.gc.repository

import android.os.Build
import android.os.Debug
import androidx.annotation.RequiresApi
import com.smellouk.kamper.gc.GcInfo

internal class GcInfoRepositoryImpl : GcInfoRepository {
    private var lastGcCount = -1L
    private var lastGcPauseMs = -1L

    override fun getInfo(): GcInfo {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return GcInfo.INVALID
        val gcCount = runCatching {
            getRuntimeStat("art.gc.gc-count")
        }.getOrElse { return GcInfo.INVALID }

        val gcPauseMs = runCatching {
            getRuntimeStat("art.gc.gc-time")
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

    @RequiresApi(Build.VERSION_CODES.M)
    private fun getRuntimeStat(stat: String): Long =
        Debug.getRuntimeStat(stat).toLong()
}
