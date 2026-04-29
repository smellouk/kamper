package com.smellouk.kamper.fps

import com.smellouk.kamper.api.Config
import com.smellouk.kamper.api.EMPTY
import com.smellouk.kamper.api.Logger

/**
 * Configuration for the FPS monitoring module. Subscribes to the platform's frame callback
 * mechanism and reports the moving FPS average each second.
 *
 * Note: [intervalInMs] is fixed at 1 second for this module — frame callbacks are aggregated over
 * a one-second window for stable readings.
 *
 * @property logger Logger used for module-internal diagnostic output. Defaults to [Logger.EMPTY].
 */
data class FpsConfig(
    override val isEnabled: Boolean,
    val logger: Logger
) : Config {
    override val intervalInMs: Long = ONE_SECOND_IN_MILLIS

    companion object {
        val DEFAULT = FpsConfig(true, Logger.EMPTY)
    }

    class Builder {
        var isEnabled: Boolean = DEFAULT.isEnabled
        var logger: Logger = DEFAULT.logger

        fun build(): FpsConfig = FpsConfig(isEnabled, logger)
    }
}

private const val ONE_SECOND_IN_MILLIS = 1000L
