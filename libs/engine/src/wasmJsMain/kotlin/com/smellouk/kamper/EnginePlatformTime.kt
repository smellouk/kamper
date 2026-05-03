package com.smellouk.kamper

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun("() => Date.now()")
private external fun jsDateNow(): Double

internal actual fun engineCurrentTimeMs(): Long = jsDateNow().toLong()

@Suppress("MagicNumber")
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@PublishedApi
internal actual fun engineCurrentTimeNs(): Long = jsDateNow().toLong() * 1_000_000L
