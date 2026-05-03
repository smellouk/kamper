package com.smellouk.kamper.thermal.repository

import com.smellouk.kamper.thermal.ThermalInfo
import com.smellouk.kamper.thermal.ThermalState
import oshi.SystemInfo

internal class ThermalInfoRepositoryImpl : ThermalInfoRepository {
    private val sensors by lazy { SystemInfo().hardware.sensors }

    override fun getInfo(): ThermalInfo = try {
        val tempC = sensors.cpuTemperature
        if (tempC < TEMP_MIN) return ThermalInfo.INVALID
        val state = when {
            tempC < TEMP_NONE -> ThermalState.NONE
            tempC < TEMP_LIGHT -> ThermalState.LIGHT
            tempC < TEMP_MODERATE -> ThermalState.MODERATE
            tempC < TEMP_SEVERE -> ThermalState.SEVERE
            tempC < TEMP_CRITICAL -> ThermalState.CRITICAL
            else -> ThermalState.EMERGENCY
        }
        ThermalInfo(state = state, isThrottling = tempC >= TEMP_LIGHT, temperatureC = tempC)
    } catch (_: Exception) {
        ThermalInfo.INVALID
    }

    private companion object {
        const val TEMP_MIN = 20.0 // below this → bogus OSHI reading
        const val TEMP_NONE = 60.0
        const val TEMP_LIGHT = 75.0
        const val TEMP_MODERATE = 85.0
        const val TEMP_SEVERE = 95.0
        const val TEMP_CRITICAL = 103.0
    }
}
