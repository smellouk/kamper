package com.smellouk.konitor.api

/**
 * Cross-cutting observer that receives every [KonitorEvent] dispatched by the Engine.
 *
 * IMPORTANT: [IntegrationModule] is NOT a [com.smellouk.konitor.api.PerformanceModule].
 * It does not have an interval or an isEnabled flag — it is a passive listener that
 * the Engine fans out to alongside (not as part of) the metric InfoListener pipeline.
 *
 * Implementations MUST:
 *   - Wrap all SDK calls inside `onEvent` in `try { ... } catch (e: Throwable) { ... }`
 *     so SDK failures never propagate to Konitor core (Phase 16 threat T-16-02).
 *   - Filter `event.info == Info.INVALID` (and module-specific INVALID sentinels)
 *     before forwarding (Phase 16 RESEARCH Pitfall 6).
 *   - Forward only metric types explicitly enabled by their DSL config (Phase 16 D-10).
 *
 * `clean()` (from [Cleanable]) is invoked by `Engine.removeIntegration(...)` and
 * `Engine.clear()` so implementations can shut down their SDK if needed.
 *
 * Per Phase 16 D-01.
 */
public interface IntegrationModule : Cleanable {
    public fun onEvent(event: KonitorEvent)
}
