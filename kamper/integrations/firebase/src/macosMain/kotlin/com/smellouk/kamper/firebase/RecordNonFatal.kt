package com.smellouk.kamper.firebase

internal actual fun recordNonFatal(
    throwable: Throwable,
    keysAndValues: Map<String, String>
) {
    // No-op: Firebase Crashlytics is not supported on macOS. Per D-07.
}
