package com.smellouk.konitor.firebase

internal actual fun recordNonFatal(
    throwable: Throwable,
    keysAndValues: Map<String, String>
) {
    // No-op: Firebase Crashlytics has no WasmJS SDK. Per D-07.
}
