package com.smellouk.kamper.firebase

import com.smellouk.kamper.api.Info
import com.smellouk.kamper.api.IntegrationModule
import com.smellouk.kamper.api.KamperEvent
import com.smellouk.kamper.api.UserEventInfo

/**
 * Forwards Kamper IssueInfo and UserEventInfo events to Firebase Crashlytics.
 *
 * Routing by [KamperEvent.moduleName]:
 *   - "issue" -> Crashlytics non-fatal (when [FirebaseConfig.forwardIssues])
 *   - "event" -> Crashlytics log breadcrumb (when [FirebaseConfig.forwardEvents]) (D-28)
 *   - any other moduleName -> ignored (per D-07, performance metrics are not Crashlytics input)
 *
 * Platform behavior (per D-07):
 *   - androidMain -> `FirebaseCrashlytics.getInstance().recordException(...)` / `.log(...)`
 *   - iosMain     -> `Crashlytics.crashlytics().recordError(NSError)` / `.log(...)`
 *   - jvmMain, macosMain, jsMain, wasmJsMain -> NO-OP
 *
 * Firebase initialization is the host app's responsibility (per RESEARCH anti-pattern:
 * "Initializing Firebase SDK inside the IntegrationModule constructor").
 *
 * Per Phase 18 D-05 + D-07 + D-10; Phase 24 D-27 + D-28; threats T-16-02, T-16-04,
 * T-24-E-01..E-04 mitigated by try/catch, INVALID guard, and as? cast.
 */
public class FirebaseIntegrationModule internal constructor(
    private val config: FirebaseConfig,
    @PublishedApi internal val logSink: (String) -> Unit = ::recordLog
) : IntegrationModule {

    override fun onEvent(event: KamperEvent) {
        try {
            // T-16-04 / T-24-E-02 — Info.INVALID sentinel must NEVER reach Firebase.
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
                "event" -> {
                    if (!config.forwardEvents) return
                    val info = event.info as? UserEventInfo ?: return
                    val message = if (info.durationMs != null) {
                        "kamper.event: ${info.name} (${info.durationMs} ms)"
                    } else {
                        "kamper.event: ${info.name}"
                    }
                    logSink(message)
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
