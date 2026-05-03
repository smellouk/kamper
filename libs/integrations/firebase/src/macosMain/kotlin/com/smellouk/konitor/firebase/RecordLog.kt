package com.smellouk.konitor.firebase

internal actual fun recordLog(message: String) {
    // No-op: Firebase Crashlytics is not supported on macOS. Per D-07.
}
