package com.smellouk.kamper.fps

import com.smellouk.kamper.api.Config
import com.smellouk.kamper.api.EMPTY
import com.smellouk.kamper.api.Logger

data class FpsConfig(
    override val isEnabled: Boolean,
    val logger: Logger
) : Config {
    override val intervalInMs: Long = ONE_SECOND_IN_MILLIS

    companion object {
        val DEFAULT = FpsConfig(true, Logger.EMPTY)
    }

    object Builder {
        var isEnabled: Boolean = DEFAULT.isEnabled
        var logger: Logger = DEFAULT.logger

        fun build(): FpsConfig = FpsConfig(isEnabled, logger)
    }
}

private const val ONE_SECOND_IN_MILLIS = 1000L
