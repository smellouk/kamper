package com.smellouk.kamper

import com.smellouk.kamper.api.Cleanable
import com.smellouk.kamper.api.Config
import com.smellouk.kamper.api.EMPTY
import com.smellouk.kamper.api.Info
import com.smellouk.kamper.api.InfoListener
import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.Performance
import com.smellouk.kamper.api.PerformanceModule
import kotlin.reflect.KClass

open class Engine {

    // Visible for testing
    @PublishedApi
    internal val performanceList: MutableList<Performance<*, *, *>> = mutableListOf()

    // Visible for testing
    @PublishedApi
    internal val mapListeners: MutableMap<KClass<*>, MutableList<InfoListener<*>>> = mutableMapOf()

    // Visible for testing
    @PublishedApi
    internal var config: KamperConfig = KamperConfig.DEFAULT
        set(value) {
            logger = value.logger
            field = value
        }

    // Visible for testing
    @PublishedApi
    internal var logger: Logger = Logger.EMPTY

    open fun start() {
        performanceList.forEach { performance ->
            performance.start()
        }
    }

    open fun stop() {
        performanceList.forEach { performance ->
            performance.stop()
        }
    }

    open fun clear() {
        performanceList.filterIsInstance<Cleanable>()
            .forEach { cleanable ->
                cleanable.clean()
            }
        performanceList.clear()
        mapListeners.clear()
    }

    inline fun <reified I : Info> addInfoListener(noinline listener: InfoListener<I>): Engine {
        mapListeners[I::class]?.add(listener)
            ?: logger.log(
                "Can't add listener, maybe you should try to install a module before adding a listener"
            )
        return this
    }

    inline fun <reified I : Info> removeInfoListener() {
        if (!mapListeners.containsKey(I::class)) {
            return
        }
        mapListeners.remove(I::class)
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <C : Config, reified I : Info> install(module: PerformanceModule<C, I>) {
        with(module) {
            if (performanceList.any { it::class == performance::class }) {
                logger.log("${performance::class.simpleName} performance is already installed!")
                return
            }
            if (!mapListeners.containsKey(I::class)) {
                mapListeners[I::class] = mutableListOf()
            }
            val isInitialized = config.isEnabled && performance.initialize(
                config,
                mapListeners[I::class] as List<InfoListener<I>>
            )
            if (isInitialized) {
                performanceList.add(performance)
            } else {
                logger.log(
                    "${performance::class.simpleName} performance is not initialized! " +
                            "could be disabled or failed to initialize."
                )
            }
        }
    }
}
