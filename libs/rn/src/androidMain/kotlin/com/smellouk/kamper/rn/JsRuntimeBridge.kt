package com.smellouk.kamper.rn

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

actual object JsRuntimeBridge {
    private val jsHeapUsed = AtomicReference(-1.0)
    private val jsHeapTotal = AtomicReference(-1.0)
    private val jsGcCount = AtomicLong(-1L)
    private val jsGcPause = AtomicReference(-1.0)
    private val crashQueue = ConcurrentLinkedQueue<Triple<String, String, Boolean>>()

    actual fun updateMemory(usedMb: Double, totalMb: Double) {
        jsHeapUsed.set(usedMb)
        jsHeapTotal.set(totalMb)
    }

    actual fun updateGc(count: Long, pauseMs: Double) {
        jsGcCount.set(count)
        jsGcPause.set(pauseMs)
    }

    actual fun enqueueCrash(message: String, stack: String, isFatal: Boolean) {
        crashQueue.offer(Triple(message, stack, isFatal))
    }

    actual fun readMemory(): Pair<Double, Double> = Pair(jsHeapUsed.get(), jsHeapTotal.get())

    actual fun readGc(): Pair<Long, Double> = Pair(jsGcCount.get(), jsGcPause.get())

    actual fun drainCrash(): Triple<String, String, Boolean>? = crashQueue.poll()
}
