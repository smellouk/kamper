---
phase: 18-kamper-can-integrate-with-services-like-sentry-and-others
plan: "01"
subsystem: kamper-api
tags: [integration, event-driven, expect-actual, kmp, api-contracts]
dependency_graph:
  requires: []
  provides:
    - KamperEvent (canonical event wrapper)
    - IntegrationModule (observer interface)
    - currentPlatform (expect/actual platform tag)
  affects:
    - Plans 18-02 through 18-05 (consume KamperEvent + IntegrationModule)
tech_stack:
  added: []
  patterns:
    - expect/actual for platform tags (mirrors Logger.Companion.DEFAULT precedent)
    - base Info type on KamperEvent to avoid circular deps on metric modules
key_files:
  created:
    - kamper/api/src/commonMain/kotlin/com/smellouk/kamper/api/KamperEvent.kt
    - kamper/api/src/commonMain/kotlin/com/smellouk/kamper/api/IntegrationModule.kt
    - kamper/api/src/commonMain/kotlin/com/smellouk/kamper/api/CurrentPlatform.kt
    - kamper/api/src/androidMain/kotlin/com/smellouk/kamper/api/CurrentPlatform.kt
    - kamper/api/src/iosMain/kotlin/com/smellouk/kamper/api/CurrentPlatform.kt
    - kamper/api/src/jvmMain/kotlin/com/smellouk/kamper/api/CurrentPlatform.kt
    - kamper/api/src/macosMain/kotlin/com/smellouk/kamper/api/CurrentPlatform.kt
    - kamper/api/src/jsMain/kotlin/com/smellouk/kamper/api/CurrentPlatform.kt
    - kamper/api/src/wasmJsMain/kotlin/com/smellouk/kamper/api/CurrentPlatform.kt
    - kamper/api/src/tvosMain/kotlin/com/smellouk/kamper/api/CurrentPlatform.kt
    - kamper/api/src/commonTest/kotlin/com/smellouk/kamper/api/KamperEventTest.kt
  modified: []
decisions:
  - "KamperEvent uses base Info (not typed subtypes) to prevent circular dependency from kamper-api into metric modules — per RESEARCH Open Question 1"
  - "currentPlatform follows Logger.Companion.DEFAULT expect/actual precedent with one actual per source set"
  - "IntegrationModule extends Cleanable (not PerformanceModule) — passive fan-out observer with no interval or isEnabled"
metrics:
  duration: "~15 minutes"
  completed: "2026-04-28"
  tasks_completed: 2
  tasks_total: 2
  files_created: 11
  files_modified: 0
---

# Phase 18 Plan 01: KamperEvent + IntegrationModule + currentPlatform API Contracts Summary

**One-liner:** KamperEvent data class + IntegrationModule observer interface + expect/actual currentPlatform across 7 KMP targets — the stable integration extension point for Phase 18 service connectors.

## What Was Built

Three new contracts added to `kamper-api` commonMain, establishing the event plumbing that all Phase 18 integration modules (Sentry, Firebase, OpenTelemetry, generic webhook) consume.

### 1. KamperEvent (canonical event wrapper)

`data class KamperEvent(moduleName: String, timestampMs: Long, platform: String, info: Info)`

Carries the source module name, epoch timestamp, platform tag, and the base `Info` payload. Uses the base `Info` interface (not typed subtypes like `CpuInfo`) to avoid creating a circular dependency from `kamper-api` into metric modules.

### 2. IntegrationModule (observer interface)

`interface IntegrationModule : Cleanable { fun onEvent(event: KamperEvent) }`

A passive fan-out observer. NOT a PerformanceModule — has no interval or isEnabled flag. Extends `Cleanable` so `Engine.removeIntegration()` and `Engine.clear()` can invoke `clean()`. KDoc mandates implementations wrap SDK calls in try/catch and filter `Info.INVALID`.

### 3. currentPlatform expect/actual (platform tag stamping)

`expect val currentPlatform: String` in commonMain. Seven actuals:

| Source set | Value |
|------------|-------|
| androidMain | `"android"` |
| iosMain | `"ios"` |
| jvmMain | `"jvm"` |
| macosMain | `"macos"` |
| jsMain | `"js"` |
| wasmJsMain | `"wasmjs"` |
| tvosMain | `"tvos"` |

## Design Rationale

**Base Info on KamperEvent (RESEARCH Open Question 1):** Using typed subtypes (e.g., `KamperEvent<CpuInfo>`) would require `kamper-api` to import from every metric module, creating a circular dependency graph. The chosen design keeps `kamper-api` dependency-free; integration modules cast `event.info as? CpuInfo` and guard against INVALID.

**Extends Cleanable, not PerformanceModule (D-01):** Integration modules are observers, not metric collectors. They have no collection interval. Reusing `Cleanable` gives the Engine a clean shutdown hook without pulling in the metric module lifecycle.

## Test Results

| Test | Result |
|------|--------|
| `KamperEvent stores moduleName timestampMs platform and base Info` | PASS |
| `KamperEvent supports Info INVALID sentinel as info payload` | PASS |
| `KamperEvent equality is value-based for data class semantics` | PASS |
| `IntegrationModule is Cleanable so Engine can call clean on removal` | PASS |
| `currentPlatform returns one of the seven supported platform tags` | PASS |

Command: `./gradlew :kamper:api:jvmTest --tests "com.smellouk.kamper.api.KamperEventTest" --no-configuration-cache`
Result: 5/5 passing (BUILD SUCCESSFUL)

## Commits

| Task | Hash | Description |
|------|------|-------------|
| Task 1 | `ef7fda3` | feat(18-01): add KamperEvent, IntegrationModule, and currentPlatform expect/actuals |
| Task 2 | `8041506` | test(18-01): add KamperEventTest covering D-03, D-12, and IntegrationModule contract |

## Deviations from Plan

None — plan executed exactly as written.

## Threat Surface Scan

No new network endpoints, auth paths, file access patterns, or schema changes introduced. New symbols (`KamperEvent`, `IntegrationModule`, `currentPlatform`) are pure in-process Kotlin contracts.

Threat T-16-01 (no credentials on KamperEvent): KamperEvent has exactly four fields (`moduleName`, `timestampMs`, `platform`, `info`), none of which carry secrets.

Threat T-16-02 (SDK throws crash Kamper): Mitigated in KDoc — `IntegrationModule` KDoc mandates try/catch in `onEvent`; enforcement happens in Plans 18-03/04/05 implementations.

## Self-Check: PASSED

Files verified to exist:
- kamper/api/src/commonMain/kotlin/com/smellouk/kamper/api/KamperEvent.kt — FOUND
- kamper/api/src/commonMain/kotlin/com/smellouk/kamper/api/IntegrationModule.kt — FOUND
- kamper/api/src/commonMain/kotlin/com/smellouk/kamper/api/CurrentPlatform.kt — FOUND
- kamper/api/src/androidMain/kotlin/com/smellouk/kamper/api/CurrentPlatform.kt — FOUND
- kamper/api/src/iosMain/kotlin/com/smellouk/kamper/api/CurrentPlatform.kt — FOUND
- kamper/api/src/jvmMain/kotlin/com/smellouk/kamper/api/CurrentPlatform.kt — FOUND
- kamper/api/src/macosMain/kotlin/com/smellouk/kamper/api/CurrentPlatform.kt — FOUND
- kamper/api/src/jsMain/kotlin/com/smellouk/kamper/api/CurrentPlatform.kt — FOUND
- kamper/api/src/wasmJsMain/kotlin/com/smellouk/kamper/api/CurrentPlatform.kt — FOUND
- kamper/api/src/tvosMain/kotlin/com/smellouk/kamper/api/CurrentPlatform.kt — FOUND
- kamper/api/src/commonTest/kotlin/com/smellouk/kamper/api/KamperEventTest.kt — FOUND

Commits verified:
- ef7fda3 — FOUND
- 8041506 — FOUND
