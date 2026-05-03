package com.smellouk.konitor.issues

/**
 * Platform-specific mutual exclusion lock for IssuesWatcher's capped accumulator.
 *
 * On JVM/Android: backed by ReentrantLock — required because AnrDetector fires
 * onIssue() from a background Thread on both platforms.
 *
 * On iOS/macOS/tvOS: backed by NSLock — required because AnrDetector uses
 * Dispatchers.Default (multi-threaded) for its watchdog coroutine.
 *
 * On JS/WasmJS: no-op — both targets are single-threaded; no races are possible.
 */
internal expect class IssuesLock() {
    fun <T> withLock(block: () -> T): T
}
