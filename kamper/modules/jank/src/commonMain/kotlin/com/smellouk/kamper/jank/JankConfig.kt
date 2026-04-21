package com.smellouk.kamper.jank

import com.smellouk.kamper.api.Config
import com.smellouk.kamper.api.EMPTY
import com.smellouk.kamper.api.Logger

data class JankConfig(
    override val isEnabled: Boolean,
    override val intervalInMs: Long,
    val jankThresholdMs: Long,
    val logger: Logger
) : Config {
    companion object {
        val DEFAULT = JankConfig(
            isEnabled = true,
            intervalInMs = 1_000L,
            jankThresholdMs = 16L,
            logger = Logger.EMPTY
        )
    }

    object Builder {
        var isEnabled: Boolean = DEFAULT.isEnabled
        var intervalInMs: Long = DEFAULT.intervalInMs
        var jankThresholdMs: Long = DEFAULT.jankThresholdMs
        var logger: Logger = DEFAULT.logger

        fun build(): JankConfig = JankConfig(isEnabled, intervalInMs, jankThresholdMs, logger)
    }
}
