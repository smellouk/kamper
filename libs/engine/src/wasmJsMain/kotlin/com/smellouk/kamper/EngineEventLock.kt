package com.smellouk.kamper

// WasmJS is single-threaded; no synchronisation is needed.
internal actual class EngineEventLock actual constructor() {
    actual fun <T> withLock(block: () -> T): T = block()
}
