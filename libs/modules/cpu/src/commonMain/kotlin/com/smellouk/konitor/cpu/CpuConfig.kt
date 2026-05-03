package com.smellouk.konitor.cpu

import com.smellouk.konitor.api.Config
import com.smellouk.konitor.api.EMPTY
import com.smellouk.konitor.api.Logger

/**
 * Configuration for the CPU monitoring module. Samples per-process CPU utilisation at the configured
 * interval and emits a [CpuInfo] update to listeners.
 *
 * @property logger Logger used for module-internal diagnostic output. Defaults to [Logger.EMPTY].
 */
data class CpuConfig(
    override val isEnabled: Boolean,
    override val intervalInMs: Long,
    val logger: Logger
) : Config {
    companion object {
        val DEFAULT = CpuConfig(true, 1000, Logger.EMPTY)
    }

    class Builder {
        var isEnabled: Boolean = DEFAULT.isEnabled
        var intervalInMs: Long = DEFAULT.intervalInMs
        var logger: Logger = DEFAULT.logger

        fun build(): CpuConfig = CpuConfig(isEnabled, intervalInMs, logger)
    }
}
