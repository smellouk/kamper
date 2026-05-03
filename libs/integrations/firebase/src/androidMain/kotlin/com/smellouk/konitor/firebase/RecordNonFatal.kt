package com.smellouk.konitor.firebase

import com.google.firebase.crashlytics.FirebaseCrashlytics

internal actual fun recordNonFatal(
    throwable: Throwable,
    keysAndValues: Map<String, String>
) {
    try {
        val crashlytics = FirebaseCrashlytics.getInstance()
        keysAndValues.forEach { (k, v) -> crashlytics.setCustomKey(k, v) }
        crashlytics.recordException(throwable)
    } catch (t: Throwable) {
        // Firebase not initialized in the host app, or transient SDK error.
        // Defense-in-depth: never propagate. Caller (FirebaseIntegrationModule.onEvent)
        // already wraps this call in try/catch as well.
    }
}
