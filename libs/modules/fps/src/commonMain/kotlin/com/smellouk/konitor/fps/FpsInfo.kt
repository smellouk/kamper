package com.smellouk.konitor.fps

import com.smellouk.konitor.api.Info

data class FpsInfo(val fps: Int) : Info {
    companion object {
        val INVALID = FpsInfo(-1)
        val UNSUPPORTED = FpsInfo(-2)
    }
}
