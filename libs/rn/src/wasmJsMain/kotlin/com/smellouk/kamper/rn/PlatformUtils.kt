package com.smellouk.kamper.rn

internal actual fun generateId(): String = js("Math.random().toString(36).substr(2, 9)")

internal actual fun currentTimeMs(): Long = js("Date.now()").toLong()
