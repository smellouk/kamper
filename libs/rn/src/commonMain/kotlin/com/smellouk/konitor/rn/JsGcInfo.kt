package com.smellouk.konitor.rn

import com.smellouk.konitor.api.Info

data class JsGcInfo(
    val gcCount: Long,
    val gcPauseMs: Double,
    val gcCountDelta: Long,
    val gcPauseMsDelta: Double
) : Info {
    companion object {
        val INVALID = JsGcInfo(-1L, -1.0, 0L, 0.0)
    }
}
