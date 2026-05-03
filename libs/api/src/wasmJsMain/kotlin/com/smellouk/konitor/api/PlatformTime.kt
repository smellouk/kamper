package com.smellouk.konitor.api

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@JsFun("() => Date.now()")
private external fun jsDateNow(): Double

internal actual fun currentApiTimeMs(): Long = jsDateNow().toLong()
