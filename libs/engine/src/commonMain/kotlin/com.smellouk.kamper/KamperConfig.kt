package com.smellouk.kamper

import com.smellouk.kamper.api.EMPTY
import com.smellouk.kamper.api.Logger

/**
 * Top-level configuration for the Kamper engine. Wraps the engine-wide [Logger] used by all
 * installed modules unless overridden in their respective `XxxConfig.logger` field.
 *
 * Construct via the [Builder] DSL: `KamperConfig.Builder().apply { logger = ... }.build()`.
 *
 * @property logger Engine-wide logger. Defaults to [Logger.EMPTY].
 */
class KamperConfig internal constructor(
    val logger: Logger
) {
    companion object {
        val DEFAULT = KamperConfig(Logger.EMPTY)
    }

    class Builder {
        var logger: Logger = DEFAULT.logger
        fun build(): KamperConfig = KamperConfig(logger)
    }
}
