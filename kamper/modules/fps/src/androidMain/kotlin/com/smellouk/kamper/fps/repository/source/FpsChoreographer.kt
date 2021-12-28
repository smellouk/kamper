package com.smellouk.kamper.fps.repository.source

import android.view.Choreographer

internal object FpsChoreographer {
    private var choreographer: Choreographer? = null
    private val callback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            doFrameListener?.invoke(frameTimeNanos)
            choreographer?.postFrameCallback(this)
        }
    }
    private var doFrameListener: ((Long) -> Unit)? = null

    fun setDoFrameListener(listener: ((Long) -> Unit)?) {
        doFrameListener = listener
    }

    fun start() {
        choreographer = Choreographer.getInstance()
        choreographer?.postFrameCallback(callback)
    }

    fun stop() {
        choreographer?.removeFrameCallback(callback)
        choreographer = null
    }
}
