package com.smellouk.konitor

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

object Konitor : Engine(), DefaultLifecycleObserver {
    fun setup(
        builder: KonitorConfig.Builder.() -> Unit
    ): Konitor {
        config = KonitorConfig.Builder().apply(builder).build()
        return this
    }

    override fun onResume(owner: LifecycleOwner) = start()

    override fun onStop(owner: LifecycleOwner) = stop()

    override fun onDestroy(owner: LifecycleOwner) = clear()
}
