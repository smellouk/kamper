package com.smellouk.konitor.fps.repository.source

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

internal object JvmFpsTimer {
    private var executor: ScheduledExecutorService? = null
    private var frameListener: ((Long) -> Unit)? = null

    fun setFrameListener(listener: (Long) -> Unit) {
        frameListener = listener
    }

    fun start() {
        if (executor?.isShutdown == false) return
        executor = Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "konitor-fps-timer").apply { isDaemon = true }
        }.also { ex ->
            ex.scheduleAtFixedRate(
                { frameListener?.invoke(System.nanoTime()) },
                0L,
                FRAME_INTERVAL_MS,
                TimeUnit.MILLISECONDS
            )
        }
    }

    fun stop() {
        executor?.shutdown()
        executor = null
    }

    fun clean() {
        stop()
        frameListener = null
    }

    private const val FRAME_INTERVAL_MS = 1000L / 60L
}
