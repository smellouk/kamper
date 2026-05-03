package com.smellouk.kamper.firebase

import com.google.firebase.crashlytics.FirebaseCrashlytics

internal actual fun recordLog(message: String) {
    try {
        FirebaseCrashlytics.getInstance().log(message)
    } catch (_: Throwable) {
        // Crashlytics not initialized or unavailable; swallow per integration contract.
    }
}
