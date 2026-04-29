package com.smellouk.kamper.issues.detector

@JsFun("() => Date.now()")
private external fun jsDateNow(): Double

internal actual fun currentPlatformTimeMs(): Long = jsDateNow().toLong()

internal actual fun captureCurrentStackTrace(): String = ""
