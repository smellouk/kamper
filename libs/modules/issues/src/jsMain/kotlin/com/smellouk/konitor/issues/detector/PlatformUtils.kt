package com.smellouk.konitor.issues.detector

internal actual fun currentPlatformTimeMs(): Long = kotlin.js.Date().getTime().toLong()

internal actual fun captureCurrentStackTrace(): String = ""
