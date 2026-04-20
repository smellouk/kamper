package com.smellouk.kamper.cpu.repository.source

import kotlinx.browser.window

internal object JsCpuSampler {
    private const val SAMPLE_INTERVAL_MS = 50
    private const val SMOOTHING = 0.25

    var loadEstimate = 0.0
        private set

    private var started = false
    private var handle = 0
    private var expectedNextMs = 0.0

    fun ensureStarted() {
        if (started) return
        started = true
        expectedNextMs = window.performance.now() + SAMPLE_INTERVAL_MS
        scheduleNext()
    }

    private fun scheduleNext() {
        handle = window.setTimeout({
            val actual = window.performance.now()
            val drift = maxOf(0.0, actual - expectedNextMs)
            val instant = minOf(1.0, drift / SAMPLE_INTERVAL_MS)
            loadEstimate = loadEstimate * (1.0 - SMOOTHING) + instant * SMOOTHING
            expectedNextMs = actual + SAMPLE_INTERVAL_MS
            scheduleNext()
        }, SAMPLE_INTERVAL_MS)
    }

    fun stop() {
        window.clearTimeout(handle)
        started = false
        loadEstimate = 0.0
    }
}
