package com.smellouk.konitor

internal actual fun engineCurrentTimeMs(): Long = kotlin.js.Date().getTime().toLong()

@Suppress("MagicNumber")
@PublishedApi
internal actual fun engineCurrentTimeNs(): Long =
    kotlin.js.Date().getTime().toLong() * 1_000_000L
