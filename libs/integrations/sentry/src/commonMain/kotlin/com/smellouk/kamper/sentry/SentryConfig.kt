package com.smellouk.kamper.sentry

import com.smellouk.kamper.api.KamperDslMarker

/**
 * DSL config for [SentryIntegrationModule]. Per Phase 16 D-04 + D-10 + D-11.
 *
 * @property dsn Sentry DSN (e.g., "https://abc@sentry.io/123"). REQUIRED.
 * @property forwardIssues When true, every IssueInfo event becomes `Sentry.captureException`.
 * @property forwardCpuAbove When non-null, CpuInfo events with `totalUseRatio * 100 > forwardCpuAbove`
 *                          become a Sentry breadcrumb. `null` disables CPU forwarding entirely.
 * @property forwardMemoryAbove When non-null, MemoryInfo events where
 *                              `(allocatedInMb / maxMemoryInMb) * 100 > forwardMemoryAbove`
 *                              become a Sentry breadcrumb. Value is in percent (0–100).
 *                              `null` disables memory forwarding.
 * @property forwardFps When true, every FpsInfo event becomes a breadcrumb regardless of value.
 *
 * Mirrors the `CpuConfig.Builder` `object Builder` pattern. Note that this config
 * does NOT implement `com.smellouk.kamper.api.Config` — IntegrationModule has its
 * own config shape and is not driven by Engine's interval/isEnabled machinery.
 */
public data class SentryConfig(
    val dsn: String,
    val forwardIssues: Boolean,
    val forwardCpuAbove: Float?,
    val forwardMemoryAbove: Float?,
    val forwardFps: Boolean,
    val forwardEvents: Boolean
) {
    // Override to prevent the Sentry DSN (which embeds credentials) from appearing in
    // logs, crash reports, or test failure output via the data class auto-generated toString().
    override fun toString(): String =
        "SentryConfig(dsn=<redacted>, forwardIssues=$forwardIssues, " +
        "forwardCpuAbove=$forwardCpuAbove, forwardMemoryAbove=$forwardMemoryAbove, " +
        "forwardFps=$forwardFps, forwardEvents=$forwardEvents)"

    public companion object {
        public val DEFAULT_FORWARD_ISSUES: Boolean = false
        public val DEFAULT_FORWARD_FPS: Boolean = false
        public const val DEFAULT_FORWARD_EVENTS: Boolean = true
    }

    @KamperDslMarker
    public class Builder internal constructor() {
        public var forwardIssues: Boolean = DEFAULT_FORWARD_ISSUES
        public var forwardCpuAbove: Float? = null
        public var forwardMemoryAbove: Float? = null
        public var forwardFps: Boolean = DEFAULT_FORWARD_FPS
        public var forwardEvents: Boolean = DEFAULT_FORWARD_EVENTS

        internal fun build(dsn: String): SentryConfig =
            SentryConfig(
                dsn = dsn,
                forwardIssues = forwardIssues,
                forwardCpuAbove = forwardCpuAbove,
                forwardMemoryAbove = forwardMemoryAbove,
                forwardFps = forwardFps,
                forwardEvents = forwardEvents
            )
    }
}
