package com.smellouk.kamper.gc.repository

import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.gc.GcInfo
import java.lang.management.ManagementFactory

internal class GcInfoRepositoryImpl(private val logger: Logger) : GcInfoRepository {
    private val gcBeans = ManagementFactory.getGarbageCollectorMXBeans()

    private var lastCount: Long = 0L
    private var lastPauseMs: Long = 0L

    override fun getInfo(): GcInfo = try {
        var totalCount = 0L
        var totalPauseMs = 0L
        for (bean in gcBeans) {
            val c = bean.collectionCount
            val t = bean.collectionTime
            if (c >= 0) totalCount += c
            if (t >= 0) totalPauseMs += t
        }
        val countDelta = totalCount - lastCount
        val pauseMsDelta = totalPauseMs - lastPauseMs
        lastCount = totalCount
        lastPauseMs = totalPauseMs
        GcInfo(
            gcCount = totalCount,
            gcPauseMs = totalPauseMs,
            gcCountDelta = countDelta,
            gcPauseMsDelta = pauseMsDelta
        )
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        logger.log(e.stackTraceToString())
        GcInfo.INVALID
    }
}
