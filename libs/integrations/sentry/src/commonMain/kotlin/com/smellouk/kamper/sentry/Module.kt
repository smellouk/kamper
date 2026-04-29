package com.smellouk.kamper.sentry

import com.smellouk.kamper.api.KamperDslMarker

/**
 * Public DSL factory for the Sentry integration module.
 *
 * Usage:
 * ```
 * Kamper
 *   .install(CpuModule)
 *   .addIntegration(
 *     SentryModule(dsn = "https://...") {
 *       forwardIssues = true
 *       forwardCpuAbove = 80f
 *       forwardMemoryAbove = 85f
 *       forwardFps = false
 *     }
 *   )
 * ```
 *
 * Per Phase 16 D-04 + CONTEXT.md decisions D-05.
 *
 * @param dsn Sentry DSN. Mandatory. If invalid, init throws inside SentryIntegrationModule's
 *            constructor — caught silently per threat T-16-01.
 * @param builder DSL block configuring which Kamper events are forwarded.
 *
 * NOTE: If the host app has already initialized Sentry (e.g., for crash reporting),
 * this integration will NOT re-initialize Sentry — it forwards events to the already-active
 * SDK instance. The [dsn] parameter is only used if Sentry is not yet enabled.
 */
@Suppress("FunctionNaming")
public fun SentryModule(
    dsn: String,
    builder: SentryConfig.Builder.() -> Unit = {}
): SentryIntegrationModule =
    SentryIntegrationModule(SentryConfig.Builder().apply(builder).build(dsn))
