package com.smellouk.konitor.rn

import platform.Foundation.NSLock

actual object JsRuntimeBridge {
    @kotlin.concurrent.Volatile
    private var jsHeapUsed = 0.0
    @kotlin.concurrent.Volatile
    private var jsHeapTotal = 0.0
    @kotlin.concurrent.Volatile
    private var jsGcCount = 0L
    @kotlin.concurrent.Volatile
    private var jsGcPause = 0.0

    private val crashLock = NSLock()
    private val crashes = mutableListOf<Triple<String, String, Boolean>>()

    actual fun updateMemory(usedMb: Double, totalMb: Double) {
        jsHeapUsed = usedMb
        jsHeapTotal = totalMb
    }

    actual fun updateGc(count: Long, pauseMs: Double) {
        jsGcCount = count
        jsGcPause = pauseMs
    }

    actual fun enqueueCrash(message: String, stack: String, isFatal: Boolean) {
        crashLock.lock()
        crashes.add(Triple(message, stack, isFatal))
        crashLock.unlock()
    }

    actual fun readMemory(): Pair<Double, Double> = Pair(jsHeapUsed, jsHeapTotal)

    actual fun readGc(): Pair<Long, Double> = Pair(jsGcCount, jsGcPause)

    actual fun drainCrash(): Triple<String, String, Boolean>? {
        crashLock.lock()
        val item = if (crashes.isNotEmpty()) crashes.removeAt(0) else null
        crashLock.unlock()
        return item
    }
}
