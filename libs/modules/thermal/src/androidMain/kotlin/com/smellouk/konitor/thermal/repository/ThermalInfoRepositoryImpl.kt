package com.smellouk.konitor.thermal.repository

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import com.smellouk.konitor.thermal.ThermalInfo
import com.smellouk.konitor.thermal.ThermalState

internal class ThermalInfoRepositoryImpl(private val context: Context) : ThermalInfoRepository {

    // Emulators have no thermal HAL — currentThermalStatus always returns NONE.
    private val isEmulator: Boolean = Build.FINGERPRINT.startsWith("generic") ||
        Build.FINGERPRINT.startsWith("unknown") ||
        Build.HARDWARE.contains("goldfish") ||
        Build.HARDWARE.contains("ranchu") ||
        Build.PRODUCT.contains("sdk_gphone", ignoreCase = true)

    override fun getInfo(): ThermalInfo {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || isEmulator) return ThermalInfo.UNSUPPORTED

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
            isThrottling = state >= ThermalState.MODERATE,
            temperatureC = batteryTemperatureC()
        )
    }

    private fun batteryTemperatureC(): Double {
        val intent = runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull() ?: return -1.0
        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        return if (tempTenths < 0) -1.0 else tempTenths / 10.0
    }
}
