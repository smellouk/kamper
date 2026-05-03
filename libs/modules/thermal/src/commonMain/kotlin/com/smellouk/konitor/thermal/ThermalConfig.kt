package com.smellouk.konitor.thermal

import com.smellouk.konitor.api.Config
import com.smellouk.konitor.api.EMPTY
import com.smellouk.konitor.api.Logger

/**
 * Configuration for the thermal monitoring module. Polls the device thermal state at the configured
 * interval and reports throttling levels (where supported by the underlying platform — Android
 * exposes `PowerManager.getCurrentThermalStatus()` from API 29).
 *
 * @property logger Logger used for module-internal diagnostic output. Defaults to [Logger.EMPTY].
 */
data class ThermalConfig(
    override val isEnabled: Boolean,
    override val intervalInMs: Long,
    val logger: Logger
) : Config {
    companion object {
        val DEFAULT = ThermalConfig(isEnabled = true, intervalInMs = 2_000L, logger = Logger.EMPTY)
    }

    class Builder {
        var isEnabled: Boolean = DEFAULT.isEnabled
        var intervalInMs: Long = DEFAULT.intervalInMs
        var logger: Logger = DEFAULT.logger

        fun build(): ThermalConfig = ThermalConfig(isEnabled, intervalInMs, logger)
    }
}
