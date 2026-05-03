package com.smellouk.konitor.rn

import com.smellouk.konitor.api.Info

data class JsMemoryInfo(val usedMb: Double, val totalMb: Double) : Info {
    companion object {
        val INVALID = JsMemoryInfo(-1.0, -1.0)
    }
}
