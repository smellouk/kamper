package com.smellouk.kamper.thermal.repository

import android.content.Context
import android.os.Build
import android.os.PowerManager
import com.smellouk.kamper.thermal.ThermalInfo
import com.smellouk.kamper.thermal.ThermalState

internal class ThermalInfoRepositoryImpl(private val context: Context) : ThermalInfoRepository {

    override fun getInfo(): ThermalInfo {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return ThermalInfo.INVALID

        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return ThermalInfo.INVALID

        val state = when (pm.currentThermalStatus) {
            PowerManager.THERMAL_STATUS_NONE      -> ThermalState.NONE
            PowerManager.THERMAL_STATUS_LIGHT     -> ThermalState.LIGHT
            PowerManager.THERMAL_STATUS_MODERATE  -> ThermalState.MODERATE
            PowerManager.THERMAL_STATUS_SEVERE    -> ThermalState.SEVERE
            PowerManager.THERMAL_STATUS_CRITICAL  -> ThermalState.CRITICAL
            PowerManager.THERMAL_STATUS_EMERGENCY -> ThermalState.EMERGENCY
            PowerManager.THERMAL_STATUS_SHUTDOWN  -> ThermalState.SHUTDOWN
            else                                  -> ThermalState.UNKNOWN
        }

        return ThermalInfo(
            state = state,
            isThrottling = state >= ThermalState.MODERATE
        )
    }
}
