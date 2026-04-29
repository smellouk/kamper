package com.smellouk.kamper

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

object Kamper : Engine(), DefaultLifecycleObserver {
    fun setup(
        builder: KamperConfig.Builder.() -> Unit
    ): Kamper {
        config = KamperConfig.Builder().apply(builder).build()
        return this
    }

    override fun onResume(owner: LifecycleOwner) = start()

    override fun onStop(owner: LifecycleOwner) = stop()

    override fun onDestroy(owner: LifecycleOwner) = clear()
}
