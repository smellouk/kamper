package com.smellouk.konitor.gpu

import com.smellouk.konitor.api.Config
import com.smellouk.konitor.api.EMPTY
import com.smellouk.konitor.api.Logger

/**
 * Configuration for the GPU monitoring module. Polls a [GpuInfo] sample at the
 * configured interval and emits to listeners.
 *
 * @property logger Logger for module-internal diagnostics. Defaults to [Logger.EMPTY].
 */
data class GpuConfig(
    override val isEnabled: Boolean,
    override val intervalInMs: Long,
    val logger: Logger
) : Config {
    companion object {
        val DEFAULT = GpuConfig(true, 1000, Logger.EMPTY)
    }

    class Builder {
        var isEnabled: Boolean = DEFAULT.isEnabled
        var intervalInMs: Long = DEFAULT.intervalInMs
        var logger: Logger = DEFAULT.logger

        fun build(): GpuConfig = GpuConfig(isEnabled, intervalInMs, logger)
    }
}
