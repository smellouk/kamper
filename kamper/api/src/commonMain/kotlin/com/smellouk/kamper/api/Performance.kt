package com.smellouk.kamper.api

open class Performance<C : Config, W : Watcher<I>, I : Info>(
    private val watcher: W
) {
    var config: C? = null
        private set

    private var listeners: List<InfoListener<I>> = emptyList()

    fun initialize(config: C, listeners: List<InfoListener<I>>): Boolean {
        if (config.intervalInMs < 0) {
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
        config?.intervalInMs?.let { intervalInMs ->
            watcher.startWatching(intervalInMs, listeners)
        }
    }

    open fun stop() {
        watcher.stopWatching()
    }

    private fun isInitialized(): Boolean = config != null
}

class PerformanceModule<C : Config, I : Info>(
    val config: C,
    val performance: Performance<C, Watcher<I>, I>
)
