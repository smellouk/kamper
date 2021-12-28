package com.smellouk.kamper.fps

import com.smellouk.kamper.api.Info

data class FpsInfo(val fps: Int) : Info {
    companion object {
        val INVALID = FpsInfo(-1)
    }
}
