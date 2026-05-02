@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.smellouk.kamper.thermal.repository

import com.smellouk.kamper.thermal.ThermalInfo
import com.smellouk.kamper.thermal.ThermalState
import com.smellouk.kamper.thermal.cinterop.kamper_thermal_state

internal class ThermalInfoRepositoryImpl : ThermalInfoRepository {
    override fun getInfo(): ThermalInfo = try {
        // 0=Nominal, 1=Fair, 2=Serious, 3=Critical (NSProcessInfoThermalState)
        val state = when (kamper_thermal_state()) {
            0    -> ThermalState.NONE
            1    -> ThermalState.LIGHT
            2    -> ThermalState.MODERATE
            3    -> ThermalState.CRITICAL
            else -> ThermalState.UNKNOWN
        }
        ThermalInfo(
            state = state,
            isThrottling = state != ThermalState.NONE && state != ThermalState.UNKNOWN
        )
    } catch (_: Exception) {
        ThermalInfo.INVALID
    }
}
