package com.smellouk.konitor

@PublishedApi
internal actual fun engineCurrentTimeMs(): Long = System.currentTimeMillis()

@PublishedApi
internal actual fun engineCurrentTimeNs(): Long = System.nanoTime()
