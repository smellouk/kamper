package com.smellouk.konitor.rn

expect object JsRuntimeBridge {
    // Write side — called by TurboModule
    fun updateMemory(usedMb: Double, totalMb: Double)
    fun updateGc(count: Long, pauseMs: Double)
    fun enqueueCrash(message: String, stack: String, isFatal: Boolean)

    // Read side — called by Watchers
    fun readMemory(): Pair<Double, Double>
    fun readGc(): Pair<Long, Double>
    fun drainCrash(): Triple<String, String, Boolean>?
}
