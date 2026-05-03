package com.smellouk.konitor.api

/**
 * Canonical event format dispatched to every [IntegrationModule] when a Konitor
 * metric module emits an [Info] update. Carries enough context for an integration
 * to route the event to its SDK without depending on concrete Info subtypes.
 *
 * @property moduleName Short identifier of the source module ("cpu", "fps", "memory",
 *                      "network", "issues", "jank", "gc", "thermal").
 * @property timestampMs Epoch milliseconds when the Engine wrapped this Info.
 * @property platform   Platform tag from [currentPlatform] — "android", "ios", "jvm",
 *                      "macos", "js", "wasmjs", or "tvos".
 * @property info       The base [Info] payload. Integration modules cast to a concrete
 *                      subtype via `info as? CpuInfo` and guard against [Info.INVALID].
 *
 * Per Phase 16 D-03 + D-12; uses base Info to avoid circular dependency on metric modules
 * (see Phase 16 RESEARCH.md Open Question 1).
 */
public data class KonitorEvent(
    val moduleName: String,
    val timestampMs: Long,
    val platform: String,
    val info: Info
)
