package com.smellouk.konitor.firebase

import cocoapods.FirebaseCrashlytics.Crashlytics

internal actual fun recordLog(message: String) {
    try {
        Crashlytics.crashlytics().log(message)
    } catch (_: Throwable) {
        // Crashlytics not initialized or unavailable; swallow per integration contract.
    }
}
