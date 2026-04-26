package com.smellouk.kamper.issues

import java.util.concurrent.locks.ReentrantLock

internal actual class IssuesLock actual constructor() {
    private val lock = ReentrantLock()
    actual fun <T> withLock(block: () -> T): T = lock.withLock(block)
}

private fun <T> ReentrantLock.withLock(block: () -> T): T {
    lock()
    return try {
        block()
    } finally {
        unlock()
    }
}
