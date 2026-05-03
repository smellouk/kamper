package com.smellouk.kamper

import com.smellouk.kamper.api.EMPTY
import com.smellouk.kamper.api.Logger

/**
 * Top-level configuration for the Kamper engine.
 *
 * @property logger Engine-wide logger. Defaults to [Logger.EMPTY].
 * @property eventsEnabled Phase 24 D-06. When `false`, all four event APIs
 *   ([Engine.logEvent], [Engine.startEvent], [Engine.endEvent], [Engine.measureEvent])
 *   return immediately without allocating. Defaults to `true`. Set to `BuildConfig.DEBUG`
 *   for zero overhead in release builds.
 */
class KamperConfig internal constructor(
    val logger: Logger,
    val eventsEnabled: Boolean = true
) {
    companion object {
        val DEFAULT = KamperConfig(logger = Logger.EMPTY, eventsEnabled = true)
    }

    class Builder {
        var logger: Logger = DEFAULT.logger
        var eventsEnabled: Boolean = DEFAULT.eventsEnabled
        fun build(): KamperConfig = KamperConfig(
            logger = logger,
            eventsEnabled = eventsEnabled
        )
    }
}
