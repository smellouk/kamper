package com.smellouk.konitor.firebase

internal actual fun recordLog(message: String) {
    // No-op: Firebase Crashlytics has no WasmJS SDK. Per D-07.
}
