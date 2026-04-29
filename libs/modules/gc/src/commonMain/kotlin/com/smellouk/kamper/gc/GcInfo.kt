package com.smellouk.kamper.gc

import com.smellouk.kamper.api.Info

data class GcInfo(
    val gcCount: Long,
    val gcPauseMs: Long,
    val gcCountDelta: Long,
    val gcPauseMsDelta: Long
) : Info {
    companion object {
        val INVALID = GcInfo(-1L, -1L, -1L, -1L)
        val UNSUPPORTED = GcInfo(-2L, -2L, -2L, -2L)
    }
}
