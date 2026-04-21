package com.smellouk.kamper.jank

import com.smellouk.kamper.api.Info

data class JankInfo(
    val droppedFrames: Int,
    val jankyFrameRatio: Float,
    val worstFrameMs: Long
) : Info {
    companion object {
        val INVALID = JankInfo(-1, -1f, -1L)
    }
}
