package com.smellouk.konitor.rn

actual object JsRuntimeBridge {
    actual fun updateMemory(usedMb: Double, totalMb: Double) = Unit
    actual fun updateGc(count: Long, pauseMs: Double) = Unit
    actual fun enqueueCrash(message: String, stack: String, isFatal: Boolean) = Unit
    actual fun readMemory(): Pair<Double, Double> = Pair(-1.0, -1.0)
    actual fun readGc(): Pair<Long, Double> = Pair(-1L, -1.0)
    actual fun drainCrash(): Triple<String, String, Boolean>? = null
}
