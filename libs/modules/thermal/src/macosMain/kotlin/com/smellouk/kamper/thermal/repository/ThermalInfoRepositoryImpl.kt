@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.smellouk.kamper.thermal.repository

import com.smellouk.kamper.thermal.ThermalInfo
import com.smellouk.kamper.thermal.ThermalState
import com.smellouk.kamper.thermal.cinterop.kamper_get_cpu_temperature

// NSProcessInfo.thermalState only exposes 4 levels and lags real temperature on Apple Silicon.
// State is derived directly from SMC temperature for finer-grained classification.
// Thresholds based on M-series TJ Max (~105°C) and observed throttle onset (~75°C).
internal class ThermalInfoRepositoryImpl : ThermalInfoRepository {
    override fun getInfo(): ThermalInfo = try {
        val tempC = kamper_get_cpu_temperature()
        if (tempC < 0.0) return ThermalInfo.INVALID
        val state = when {
            tempC < TEMP_NONE     -> ThermalState.NONE
            tempC < TEMP_LIGHT    -> ThermalState.LIGHT
            tempC < TEMP_MODERATE -> ThermalState.MODERATE
            tempC < TEMP_SEVERE   -> ThermalState.SEVERE
            tempC < TEMP_CRITICAL -> ThermalState.CRITICAL
            else                  -> ThermalState.EMERGENCY
        }
        ThermalInfo(state = state, isThrottling = tempC >= TEMP_LIGHT, temperatureC = tempC)
    } catch (_: Exception) {
        ThermalInfo.INVALID
    }

    private companion object {
        const val TEMP_NONE = 60.0
        const val TEMP_LIGHT = 75.0
        const val TEMP_MODERATE = 85.0
        const val TEMP_SEVERE = 95.0
        const val TEMP_CRITICAL = 103.0
    }
}
