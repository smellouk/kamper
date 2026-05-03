package com.smellouk.konitor.api

import kotlin.concurrent.Volatile

open class Performance<C : Config, W : IWatcher<I>, I : Info>(
    private val watcher: W,
    private val logger: Logger
) {
    private lateinit var config: C

    private var listeners: List<InfoListener<I>> = emptyList()

    // Visible to Engine module for Engine.validate() (FEAT-03 / Plan 09-06):
    // 'open' so Mokkery can generate mocks; non-internal so the cross-module
    // access from Engine.validate() compiles cleanly (internal does not cross
    // Kotlin module boundaries). Both fields are @Volatile for safe cross-thread
    // reads inside validate().
    @Volatile open var lastValidSampleAt: Long = 0L

    // Set in start() the first time the Performance is started. Engine.validate()
    // (Plan 09-06) uses this as the elapsed-time anchor for modules that have
    // never delivered a sample (lastValidSampleAt == 0L) — without this anchor
    // a 1-second-old module would be falsely reported as a 10-second problem.
    @Volatile open var installedAt: Long = 0L

    open fun initialize(config: C, listeners: List<InfoListener<I>>): Boolean {
        if (config.intervalInMs <= 0) {
            return false
        }
        if (isInitialized()) {
            return true
        }
        this.config = config
        this.listeners = listeners
        return true
    }

    open fun start() {
        if (isInitialized()) {
            if (installedAt == 0L) {
                installedAt = currentApiTimeMs()
            }
            watcher.startWatching(
                intervalInMs = config.intervalInMs,
                listeners = listeners,
                onSampleDelivered = { lastValidSampleAt = currentApiTimeMs() }
            )
        } else {
            logger.log("Performance[${this::class.simpleName}] is not initialized yet!")
        }
    }

    open fun stop() {
        watcher.stopWatching()
    }

    private fun isInitialized(): Boolean = this::config.isInitialized
}

class PerformanceModule<C : Config, I : Info>(
    val config: C,
    val performance: Performance<C, IWatcher<I>, I>
)
