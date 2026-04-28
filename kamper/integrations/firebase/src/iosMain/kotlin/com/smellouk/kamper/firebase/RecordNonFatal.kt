package com.smellouk.kamper.firebase

import cocoapods.FirebaseCrashlytics.Crashlytics
import platform.Foundation.NSError
import platform.Foundation.NSLocalizedDescriptionKey

@Suppress("CAST_NEVER_SUCCEEDS")
internal actual fun recordNonFatal(
    throwable: Throwable,
    keysAndValues: Map<String, String>
) {
    try {
        val userInfo: Map<Any?, Any?> = mutableMapOf<Any?, Any?>(
            NSLocalizedDescriptionKey to (throwable.message ?: "Kamper issue")
        ).apply {
            // Surface custom keys inside the NSError userInfo so they appear in
            // Crashlytics dashboards alongside the error frame.
            keysAndValues.forEach { (k, v) -> put(k, v) }
        }
        val nsError = NSError.errorWithDomain(
            domain = throwable::class.simpleName ?: "KamperFirebase",
            code = 0L,
            userInfo = userInfo
        )
        Crashlytics.crashlytics().recordError(nsError)
    } catch (t: Throwable) {
        // Firebase not initialized in the host app. Silent no-op.
    }
}
