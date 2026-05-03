package com.smellouk.konitor.issues

import platform.Foundation.NSLock

internal actual class IssuesLock actual constructor() {
    private val lock = NSLock()
    actual fun <T> withLock(block: () -> T): T {
        lock.lock()
        return try {
            block()
        } finally {
            lock.unlock()
        }
    }
}
