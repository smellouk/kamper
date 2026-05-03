package com.smellouk.konitor.jank

import com.smellouk.konitor.api.Info

data class JankInfo(
    val droppedFrames: Int,
    val jankyFrameRatio: Float,
    val worstFrameMs: Long
) : Info {
    companion object {
        val INVALID = JankInfo(-1, -1f, -1L)
        val UNSUPPORTED = JankInfo(-2, -2f, -2L)
    }
}
