package com.smellouk.konitor.fps.repository.source

import android.util.Log
import android.view.Choreographer
import java.util.concurrent.atomic.AtomicBoolean

internal object FpsChoreographer {
    private var choreographer: Choreographer? = null
    private val fpsActive = AtomicBoolean(false)

    // Visible only for testing
    internal val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            try {
                frameListener?.invoke(frameTimeNanos)
            } catch (e: Throwable) {
                Log.w(TAG, "FpsChoreographer: doFrame listener threw, re-registering", e)
            }
            if (fpsActive.get()) {
                choreographer?.postFrameCallback(this)
            }
        }
    }
    private var frameListener: FpsChoreographerFrameListener? = null

    fun setFrameListener(listener: FpsChoreographerFrameListener) {
        frameListener = listener
    }

    fun start() {
        if (!fpsActive.compareAndSet(false, true)) return
        if (choreographer == null) {
            choreographer = Choreographer.getInstance()
        }
        choreographer?.postFrameCallback(frameCallback)
    }

    fun stop() {
        fpsActive.set(false)
        choreographer?.removeFrameCallback(frameCallback)
    }

    fun clean() {
        fpsActive.set(false)
        choreographer = null
        frameListener = null
    }

    private const val TAG = "FpsChoreographer"
}

typealias FpsChoreographerFrameListener = (Long) -> Unit
