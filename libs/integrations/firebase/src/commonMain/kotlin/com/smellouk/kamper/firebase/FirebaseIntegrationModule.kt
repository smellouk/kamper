package com.smellouk.kamper.firebase

import com.smellouk.kamper.api.Info
import com.smellouk.kamper.api.IntegrationModule
import com.smellouk.kamper.api.KamperEvent

/**
 * Forwards Kamper IssueInfo events to Firebase Crashlytics as non-fatal exceptions.
 *
 * Routing by [KamperEvent.moduleName]:
 *   - "issue" -> Crashlytics non-fatal (when [FirebaseConfig.forwardIssues])
 *   - any other moduleName -> ignored (per D-07, performance metrics are not Crashlytics input)
 *
 * Platform behavior (per D-07):
 *   - androidMain -> `FirebaseCrashlytics.getInstance().recordException(...)`
 *   - iosMain     -> `Crashlytics.crashlytics().recordError(NSError)` with NSError wrapping
 *   - jvmMain, macosMain, jsMain, wasmJsMain -> NO-OP
 *
 * Firebase initialization is the host app's responsibility (per RESEARCH anti-pattern:
 * "Initializing Firebase SDK inside the IntegrationModule constructor").
 *
 * Per Phase 18 D-05 + D-07 + D-10; threats T-16-02, T-16-04 mitigated by try/catch and
 * INVALID guard.
 */
public class FirebaseIntegrationModule internal constructor(
    private val config: FirebaseConfig
) : IntegrationModule {

    override fun onEvent(event: KamperEvent) {
        try {
            // T-16-04 — Info.INVALID sentinel must NEVER reach Firebase.
            if (event.info === Info.INVALID) return

            when (event.moduleName) {
                "issue" -> {
                    if (!config.forwardIssues) return
                    val message = "Kamper issue on ${event.platform} from ${event.moduleName}: ${event.info}"
                    val keys = mapOf(
                        "kamper.module" to event.moduleName,
                        "kamper.platform" to event.platform,
                        "kamper.timestampMs" to event.timestampMs.toString()
                    )
                    recordNonFatal(RuntimeException(message), keys)
                }
                else -> Unit
            }
        } catch (t: Throwable) {
            // T-16-02 — never propagate SDK errors to Kamper core.
        }
    }

    override fun clean() {
        // Firebase Crashlytics has no per-app teardown — flush is automatic on app exit.
    }
}
