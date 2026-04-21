package com.smellouk.kamper.gc

import com.smellouk.kamper.api.Config
import com.smellouk.kamper.api.EMPTY
import com.smellouk.kamper.api.Logger

data class GcConfig(
    override val isEnabled: Boolean,
    override val intervalInMs: Long,
    val logger: Logger
) : Config {
    companion object {
        val DEFAULT = GcConfig(isEnabled = true, intervalInMs = 1_000L, logger = Logger.EMPTY)
    }

    object Builder {
        var isEnabled: Boolean = DEFAULT.isEnabled
        var intervalInMs: Long = DEFAULT.intervalInMs
        var logger: Logger = DEFAULT.logger

        fun build(): GcConfig = GcConfig(isEnabled, intervalInMs, logger)
    }
}
