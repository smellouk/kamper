package com.smellouk.kamper.firebase

internal actual fun recordLog(message: String) {
    // No-op: Firebase Crashlytics has no Kotlin/JS SDK in this Kamper integration. Per D-07.
}
