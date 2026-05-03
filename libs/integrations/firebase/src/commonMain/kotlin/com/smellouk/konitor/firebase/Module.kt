package com.smellouk.konitor.firebase

import com.smellouk.konitor.api.KonitorDslMarker

/**
 * Public DSL factory for the Firebase Crashlytics integration module.
 *
 * Usage:
 * ```
 * Konitor
 *   .install(IssuesModule)
 *   .addIntegration(
 *     FirebaseModule {
 *       forwardIssues = true
 *     }
 *   )
 * ```
 *
 * Per Phase 18 D-04 + D-05 + D-07.
 *
 * NOTE: Firebase MUST already be initialized in the host app (google-services.json on
 * Android, GoogleService-Info.plist on iOS). On JVM, macOS, JS, and WasmJS, the
 * underlying `recordNonFatal` is a no-op — consumers do NOT need platform guards.
 */
@Suppress("FunctionNaming")
public fun FirebaseModule(
    builder: FirebaseConfig.Builder.() -> Unit = {}
): FirebaseIntegrationModule =
    FirebaseIntegrationModule(FirebaseConfig.Builder().apply(builder).build())
