package com.smellouk.kamper.thermal

import com.smellouk.kamper.api.Info

data class ThermalInfo(
    val state: ThermalState,
    val isThrottling: Boolean
) : Info {
    companion object {
        val INVALID = ThermalInfo(ThermalState.UNKNOWN, false)
        val UNSUPPORTED = ThermalInfo(ThermalState.UNSUPPORTED, false)
    }
}
