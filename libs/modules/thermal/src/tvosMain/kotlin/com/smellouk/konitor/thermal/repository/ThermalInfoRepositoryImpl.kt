@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.smellouk.konitor.thermal.repository

import com.smellouk.konitor.thermal.ThermalInfo
import com.smellouk.konitor.thermal.ThermalState
import com.smellouk.konitor.thermal.cinterop.konitor_thermal_state

internal class ThermalInfoRepositoryImpl : ThermalInfoRepository {
    override fun getInfo(): ThermalInfo = try {
        // NSProcessInfoThermalState: 0=Nominal, 1=Fair, 2=Serious, 3=Critical
        val state = when (konitor_thermal_state()) {
            THERMAL_NOMINAL  -> ThermalState.NONE
            THERMAL_FAIR     -> ThermalState.LIGHT
            THERMAL_SERIOUS  -> ThermalState.MODERATE
            THERMAL_CRITICAL -> ThermalState.CRITICAL
            else             -> ThermalState.UNKNOWN
        }
        ThermalInfo(
            state = state,
            isThrottling = state != ThermalState.NONE && state != ThermalState.UNKNOWN
        )
    } catch (_: Exception) {
        ThermalInfo.INVALID
    }

    private companion object {
        const val THERMAL_NOMINAL = 0
        const val THERMAL_FAIR = 1
        const val THERMAL_SERIOUS = 2
        const val THERMAL_CRITICAL = 3
    }
}
