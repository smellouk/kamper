package com.smellouk.kamper.rn

import com.smellouk.kamper.api.Info

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
