package com.smellouk.konitor

import com.smellouk.konitor.api.EMPTY
import com.smellouk.konitor.api.Logger

/**
 * Top-level configuration for the Konitor engine.
 *
 * @property logger Engine-wide logger. Defaults to [Logger.EMPTY].
 * @property eventsEnabled Phase 24 D-06. When `false`, all four event APIs
 *   ([Engine.logEvent], [Engine.startEvent], [Engine.endEvent], [Engine.measureEvent])
 *   return immediately without allocating. Defaults to `true`. Set to `BuildConfig.DEBUG`
 *   for zero overhead in release builds.
 */
class KonitorConfig internal constructor(
    val logger: Logger,
    val eventsEnabled: Boolean = true
) {
    companion object {
        val DEFAULT = KonitorConfig(logger = Logger.EMPTY, eventsEnabled = true)
    }

    class Builder {
        var logger: Logger = DEFAULT.logger
        var eventsEnabled: Boolean = DEFAULT.eventsEnabled
        fun build(): KonitorConfig = KonitorConfig(
            logger = logger,
            eventsEnabled = eventsEnabled
        )
    }
}
