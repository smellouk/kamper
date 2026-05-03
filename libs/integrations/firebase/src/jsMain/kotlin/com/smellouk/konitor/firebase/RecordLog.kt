package com.smellouk.konitor.firebase

internal actual fun recordLog(message: String) {
    // No-op: Firebase Crashlytics has no Kotlin/JS SDK in this Konitor integration. Per D-07.
}
