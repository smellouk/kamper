package com.smellouk.kamper.gc

import com.smellouk.kamper.api.Config
import com.smellouk.kamper.api.EMPTY
import com.smellouk.kamper.api.Logger

/**
 * Configuration for the garbage collection (GC) monitoring module. Samples GC counters
 * (collection counts, pause times, where the platform exposes them) at the configured interval.
 *
 * @property logger Logger used for module-internal diagnostic output. Defaults to [Logger.EMPTY].
 */
data class GcConfig(
    override val isEnabled: Boolean,
    override val intervalInMs: Long,
    val logger: Logger
) : Config {
    companion object {
        val DEFAULT = GcConfig(isEnabled = true, intervalInMs = 1_000L, logger = Logger.EMPTY)
    }

    class Builder {
        var isEnabled: Boolean = DEFAULT.isEnabled
        var intervalInMs: Long = DEFAULT.intervalInMs
        var logger: Logger = DEFAULT.logger

        fun build(): GcConfig = GcConfig(isEnabled, intervalInMs, logger)
    }
}
