package com.smellouk.konitor.jank

import com.smellouk.konitor.api.Config
import com.smellouk.konitor.api.EMPTY
import com.smellouk.konitor.api.Logger

/**
 * Configuration for the jank detection module. Reports an issue when a frame's render time exceeds
 * [jankThresholdMs] (default 16 ms ≈ one refresh interval on a 60 Hz display).
 *
 * @property jankThresholdMs Frame render time above which the frame is reported as janky.
 * @property logger Logger used for module-internal diagnostic output. Defaults to [Logger.EMPTY].
 */
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

    class Builder {
        var isEnabled: Boolean = DEFAULT.isEnabled
        var intervalInMs: Long = DEFAULT.intervalInMs
        var jankThresholdMs: Long = DEFAULT.jankThresholdMs
        var logger: Logger = DEFAULT.logger

        fun build(): JankConfig = JankConfig(isEnabled, intervalInMs, jankThresholdMs, logger)
    }
}
