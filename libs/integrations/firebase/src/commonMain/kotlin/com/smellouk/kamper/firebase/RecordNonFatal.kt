package com.smellouk.kamper.firebase

/**
 * Platform bridge to Firebase Crashlytics. Records [throwable] as a non-fatal exception
 * with the given [keysAndValues] attached as Crashlytics custom keys.
 *
 * Implementations:
 *   - androidMain: `FirebaseCrashlytics.getInstance().recordException` + `setCustomKey`
 *   - iosMain:     wraps Throwable in NSError, calls `Crashlytics.crashlytics().recordError`
 *   - jvmMain, macosMain, jsMain, wasmJsMain: NO-OP (per D-07)
 *
 * The implementation MUST swallow any thrown exception (Firebase not initialized,
 * etc.) — the calling site already wraps in try/catch but defense-in-depth is cheap.
 */
internal expect fun recordNonFatal(
    throwable: Throwable,
    keysAndValues: Map<String, String>
)
