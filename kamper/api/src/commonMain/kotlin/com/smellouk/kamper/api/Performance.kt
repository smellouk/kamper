package com.smellouk.kamper.api

open class Performance<C : Config, W : Watcher<I>, I : Info>(
    private val watcher: W,
    private val logger: Logger
) {
    private lateinit var config: C

    private var listeners: List<InfoListener<I>> = emptyList()

    fun initialize(config: C, listeners: List<InfoListener<I>>): Boolean {
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
            watcher.startWatching(config.intervalInMs, listeners)
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
    val performance: Performance<C, Watcher<I>, I>
)
