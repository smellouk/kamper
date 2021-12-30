package com.smellouk.kamper.fps.repository.source

import android.view.Choreographer

internal object FpsChoreographer {
    private var choreographer: Choreographer? = null

    // Visible only for testing
    internal val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            frameListener?.invoke(frameTimeNanos)
            choreographer?.postFrameCallback(this)
        }
    }
    private var frameListener: FpsChoreographerFrameListener? = null

    fun setFrameListener(listener: FpsChoreographerFrameListener) {
        frameListener = listener
    }

    fun start() {
        if (choreographer == null) {
            choreographer = Choreographer.getInstance()
        }
        choreographer?.postFrameCallback(frameCallback)
    }

    fun stop() {
        choreographer?.removeFrameCallback(frameCallback)
    }

    fun clean() {
        choreographer = null
        frameListener = null
    }
}

typealias FpsChoreographerFrameListener = (Long) -> Unit
