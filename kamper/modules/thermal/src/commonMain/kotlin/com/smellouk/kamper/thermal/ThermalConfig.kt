package com.smellouk.kamper.thermal

import com.smellouk.kamper.api.Config
import com.smellouk.kamper.api.EMPTY
import com.smellouk.kamper.api.Logger

data class ThermalConfig(
    override val isEnabled: Boolean,
    override val intervalInMs: Long,
    val logger: Logger
) : Config {
    companion object {
        val DEFAULT = ThermalConfig(isEnabled = true, intervalInMs = 2_000L, logger = Logger.EMPTY)
    }

    object Builder {
        var isEnabled: Boolean = DEFAULT.isEnabled
        var intervalInMs: Long = DEFAULT.intervalInMs
        var logger: Logger = DEFAULT.logger

        fun build(): ThermalConfig = ThermalConfig(isEnabled, intervalInMs, logger)
    }
}
