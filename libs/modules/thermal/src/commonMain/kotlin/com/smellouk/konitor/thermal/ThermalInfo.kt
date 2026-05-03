package com.smellouk.konitor.thermal

import com.smellouk.konitor.api.Info

data class ThermalInfo(
    val state: ThermalState,
    val isThrottling: Boolean,
    val temperatureC: Double = -1.0
) : Info {
    companion object {
        val INVALID = ThermalInfo(ThermalState.UNKNOWN, false)
        val UNSUPPORTED = ThermalInfo(ThermalState.UNSUPPORTED, false)
    }
}
