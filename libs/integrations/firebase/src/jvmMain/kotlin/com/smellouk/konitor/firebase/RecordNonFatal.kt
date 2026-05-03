package com.smellouk.konitor.firebase

internal actual fun recordNonFatal(
    throwable: Throwable,
    keysAndValues: Map<String, String>
) {
    // No-op: Firebase Crashlytics is not supported on the JVM target. Per D-07,
    // consumers do not need platform guards — calling FirebaseModule on JVM is safe.
}
