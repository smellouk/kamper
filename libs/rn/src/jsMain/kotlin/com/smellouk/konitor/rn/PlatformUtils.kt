package com.smellouk.konitor.rn

internal actual fun generateId(): String = js("Math.random().toString(36).substr(2, 9)").toString()

internal actual fun currentTimeMs(): Long = js("Date.now()").toString().toLong()
