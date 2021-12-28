package com.smellouk.kamper

import com.smellouk.kamper.api.Config
import com.smellouk.kamper.api.EMPTY
import com.smellouk.kamper.api.Info
import com.smellouk.kamper.api.InfoListener
import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.api.Performance
import com.smellouk.kamper.api.PerformanceModule
import kotlin.reflect.KClass

open class Engine {

    @PublishedApi
    internal val performanceList: MutableList<Performance<*, *, *>> = mutableListOf()

    @PublishedApi
    internal val mapListeners: MutableMap<KClass<*>, MutableList<InfoListener<*>>> = mutableMapOf()

    @PublishedApi
    internal var config: KamperConfig = KamperConfig.DEFAULT
        set(value) {
            logger = value.logger
            field = value
        }

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
        performanceList.clear()
        mapListeners.clear()
    }

    inline fun <reified I : Info> addInfoListener(noinline listener: InfoListener<I>): Engine {
        println(mapListeners)
        mapListeners[I::class]?.add(listener)
        return this
    }

    inline fun <reified I : Info> removeInfoListener(noinline listener: InfoListener<I>) {
        if (!mapListeners.containsKey(I::class)) {
            return
        }
        mapListeners[I::class]?.remove(listener)
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
                    "${performance::class.simpleName} performance not initialized! " +
                            "could be disabled or failed to initialize."
                )
            }
        }
    }
}
