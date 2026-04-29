package com.smellouk.kamper.network

import com.smellouk.kamper.api.Config
import com.smellouk.kamper.api.EMPTY
import com.smellouk.kamper.api.Logger

/**
 * Configuration for the network monitoring module. Samples per-process network throughput
 * (bytes received and transmitted, where the platform exposes those counters) at the configured
 * interval.
 *
 * @property logger Logger used for module-internal diagnostic output. Defaults to [Logger.EMPTY].
 */
class NetworkConfig(
    override val isEnabled: Boolean,
    override val intervalInMs: Long,
    val logger: Logger
) : Config {
    companion object {
        val DEFAULT = NetworkConfig(true, 1000, Logger.EMPTY)
    }

    class Builder {
        var isEnabled: Boolean = DEFAULT.isEnabled
        var intervalInMs: Long = DEFAULT.intervalInMs
        var logger: Logger = DEFAULT.logger

        fun build(): NetworkConfig = NetworkConfig(isEnabled, intervalInMs, logger)
    }
}
