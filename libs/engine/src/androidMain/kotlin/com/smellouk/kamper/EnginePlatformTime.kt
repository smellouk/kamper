package com.smellouk.kamper

@PublishedApi
internal actual fun engineCurrentTimeMs(): Long = System.currentTimeMillis()

@PublishedApi
internal actual fun engineCurrentTimeNs(): Long =
    android.os.SystemClock.elapsedRealtimeNanos()
