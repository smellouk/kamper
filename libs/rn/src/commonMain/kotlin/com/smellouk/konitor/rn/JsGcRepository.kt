package com.smellouk.konitor.rn

import com.smellouk.konitor.api.InfoRepository

internal interface JsGcRepository : InfoRepository<JsGcInfo>

internal class JsGcRepositoryImpl : JsGcRepository {
    private var lastCount = -1L
    private var lastPauseMs = -1.0

    override fun getInfo(): JsGcInfo {
        val (count, pauseMs) = JsRuntimeBridge.readGc()
        if (count < 0) return JsGcInfo.INVALID

        val countDelta = if (lastCount < 0) 0L else count - lastCount
        val pauseDelta = if (lastPauseMs < 0) 0.0 else pauseMs - lastPauseMs

        lastCount = count
        lastPauseMs = pauseMs

        return JsGcInfo(
            gcCount = count,
            gcPauseMs = pauseMs,
            gcCountDelta = countDelta,
            gcPauseMsDelta = pauseDelta
        )
    }
}
