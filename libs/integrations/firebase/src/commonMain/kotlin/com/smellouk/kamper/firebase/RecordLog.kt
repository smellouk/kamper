package com.smellouk.kamper.firebase

/**
 * Phase 24 D-28. Routes a Kamper custom-event message into Firebase Crashlytics' log
 * buffer (NOT recordError — events are not exceptions; see RESEARCH Pitfall 6).
 *
 * On Android: `FirebaseCrashlytics.getInstance().log(message)`.
 * On iOS: `Crashlytics.crashlytics().log(message)`.
 * On other targets: no-op (Firebase Crashlytics SDK is unavailable).
 */
internal expect fun recordLog(message: String)
