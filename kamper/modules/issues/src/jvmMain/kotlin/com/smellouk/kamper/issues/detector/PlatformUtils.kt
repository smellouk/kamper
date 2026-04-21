package com.smellouk.kamper.issues.detector

internal actual fun currentPlatformTimeMs(): Long = System.currentTimeMillis()

internal actual fun captureCurrentStackTrace(): String =
    Thread.currentThread().stackTrace.drop(3).joinToString("\n") { "\tat $it" }
