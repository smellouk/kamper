package com.smellouk.kamper

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent

object Kamper : Engine(), LifecycleObserver {
    fun setup(
        builder: KamperConfig.Builder.() -> Unit
    ): Kamper {
        config = KamperConfig.Builder.apply(builder).build()
        return this
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    override fun start() {
        super.start()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    override fun stop() {
        super.stop()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    override fun clear() {
        super.clear()
    }
}
