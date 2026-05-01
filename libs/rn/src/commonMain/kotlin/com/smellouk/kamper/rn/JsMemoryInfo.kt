package com.smellouk.kamper.rn

import com.smellouk.kamper.api.Info

data class JsMemoryInfo(val usedMb: Double, val totalMb: Double) : Info {
    companion object {
        val INVALID = JsMemoryInfo(-1.0, -1.0)
    }
}
