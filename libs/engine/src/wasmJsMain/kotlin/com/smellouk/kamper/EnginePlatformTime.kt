package com.smellouk.kamper

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun("() => Date.now()")
private external fun jsDateNow(): Double

internal actual fun engineCurrentTimeMs(): Long = jsDateNow().toLong()
