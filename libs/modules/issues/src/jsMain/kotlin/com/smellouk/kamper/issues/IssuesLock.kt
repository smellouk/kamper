package com.smellouk.kamper.issues

// JS is single-threaded; no synchronisation is needed.
internal actual class IssuesLock actual constructor() {
    actual fun <T> withLock(block: () -> T): T = block()
}
