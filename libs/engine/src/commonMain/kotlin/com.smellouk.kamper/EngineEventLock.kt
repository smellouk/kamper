package com.smellouk.kamper

/**
 * Platform-specific mutual exclusion lock for [Engine.eventBuffer] (Phase 24, D-08/D-11).
 *
 * On JVM/Android: backed by ReentrantLock — host apps may call logEvent/startEvent/endEvent
 * from any thread.
 *
 * On iOS/macOS/tvOS: backed by NSLock.
 *
 * On JS/WasmJS: no-op — both targets are single-threaded; no races are possible.
 */
internal expect class EngineEventLock() {
    fun <T> withLock(block: () -> T): T
}
