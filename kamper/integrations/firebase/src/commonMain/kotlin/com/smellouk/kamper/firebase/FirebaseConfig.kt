package com.smellouk.kamper.firebase

import com.smellouk.kamper.api.KamperDslMarker

/**
 * DSL config for [FirebaseIntegrationModule]. Per Phase 18 D-04 + D-10.
 *
 * Firebase Crashlytics is for non-fatal exceptions only — performance metrics (CPU,
 * FPS, memory) are NOT a Crashlytics use case (per RESEARCH `Don't Hand-Roll`). The
 * config therefore only exposes a single forward toggle.
 *
 * @property forwardIssues When true, IssueInfo events become Firebase Crashlytics
 *                         non-fatal records via `recordNonFatal`.
 */
public data class FirebaseConfig(
    val forwardIssues: Boolean
) {
    public companion object {
        public val DEFAULT_FORWARD_ISSUES: Boolean = false
    }

    @KamperDslMarker
    public class Builder internal constructor() {
        public var forwardIssues: Boolean = DEFAULT_FORWARD_ISSUES

        internal fun build(): FirebaseConfig = FirebaseConfig(forwardIssues = forwardIssues)
    }
}
