package com.smellouk.kamper

import java.util.concurrent.locks.ReentrantLock

internal actual class EngineEventLock actual constructor() {
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
