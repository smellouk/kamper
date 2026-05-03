package com.smellouk.konitor.issues

// WasmJS is single-threaded; no synchronisation is needed.
internal actual class IssuesLock actual constructor() {
    actual fun <T> withLock(block: () -> T): T = block()
}
