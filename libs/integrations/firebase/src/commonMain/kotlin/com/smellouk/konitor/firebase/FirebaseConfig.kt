package com.smellouk.konitor.firebase

import com.smellouk.konitor.api.KonitorDslMarker

/**
 * DSL config for [FirebaseIntegrationModule]. Per Phase 18 D-04 + D-10; Phase 24 D-27.
 *
 * Firebase Crashlytics is for non-fatal exceptions and breadcrumb logs. The config exposes
 * per-signal forwarding toggles.
 *
 * @property forwardIssues When true, IssueInfo events become Firebase Crashlytics
 *                         non-fatal records via `recordNonFatal`.
 * @property forwardEvents When true, custom UserEventInfo events are written to the
 *                         Crashlytics log buffer via `recordLog` (D-27). Defaults to
 *                         `true` so events appear in crash-context breadcrumbs by default.
 */
public data class FirebaseConfig(
    val forwardIssues: Boolean,
    val forwardEvents: Boolean
) {
    override fun toString(): String =
        "FirebaseConfig(forwardIssues=$forwardIssues, forwardEvents=$forwardEvents)"

    public companion object {
        public val DEFAULT_FORWARD_ISSUES: Boolean = false
        public const val DEFAULT_FORWARD_EVENTS: Boolean = true
    }

    @KonitorDslMarker
    public class Builder internal constructor() {
        public var forwardIssues: Boolean = DEFAULT_FORWARD_ISSUES
        public var forwardEvents: Boolean = DEFAULT_FORWARD_EVENTS

        internal fun build(): FirebaseConfig = FirebaseConfig(
            forwardIssues = forwardIssues,
            forwardEvents = forwardEvents
        )
    }
}
