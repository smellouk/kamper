package com.smellouk.konitor.fps.repository.source

import kotlinx.browser.window

internal object JsFpsTimer {
    private var handle = 0
    private var running = false
    private var frameListener: ((Double) -> Unit)? = null

    fun setFrameListener(listener: (Double) -> Unit) {
        frameListener = listener
    }

    fun start() {
        if (running) return
        running = true
        requestNext()
    }

    private fun requestNext() {
        if (!running) return
        handle = window.requestAnimationFrame { timestamp ->
            frameListener?.invoke(timestamp)
            requestNext()
        }
    }

    fun stop() {
        running = false
        window.cancelAnimationFrame(handle)
        handle = 0
    }

    fun clean() {
        stop()
        frameListener = null
    }
}
