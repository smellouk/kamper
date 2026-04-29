package com.smellouk.kamper.firebase

internal actual fun recordNonFatal(
    throwable: Throwable,
    keysAndValues: Map<String, String>
) {
    // No-op: Firebase Crashlytics has no Kotlin/JS SDK in this Kamper integration. Per D-07.
}
