---
phase: 18-kamper-can-integrate-with-services-like-sentry-and-others
plan: "05"
subsystem: integrations/opentelemetry
tags:
  - opentelemetry
  - otlp
  - kmp
  - observability
  - gauge
dependency_graph:
  requires:
    - 18-01 (KamperEvent + IntegrationModule API)
    - 18-02 (Engine fan-out via dispatchToIntegrations)
  provides:
    - kamper-opentelemetry integration module
    - OpenTelemetryModule() DSL factory
    - OTLP gauge export via opentelemetry-java 1.51.0 on JVM+Android
  affects:
    - settings.gradle.kts (new :kamper:integrations:opentelemetry subproject)
tech_stack:
  added:
    - "io.opentelemetry:opentelemetry-sdk-metrics:1.51.0 (JVM+Android only)"
    - "io.opentelemetry:opentelemetry-exporter-otlp:1.51.0 (JVM+Android only)"
  patterns:
    - "expect/actual platform bridge for recordGauge"
    - "ConcurrentHashMap keyed SdkMeterProvider cache"
    - "buildWithCallback + AtomicReference async gauge idiom"
    - "toString-based numeric extraction (no compile dep on metric modules)"
key_files:
  created:
    - kamper/integrations/opentelemetry/build.gradle.kts
    - kamper/integrations/opentelemetry/src/commonMain/kotlin/com/smellouk/kamper/opentelemetry/OtelConfig.kt
    - kamper/integrations/opentelemetry/src/commonMain/kotlin/com/smellouk/kamper/opentelemetry/OtelIntegrationModule.kt
    - kamper/integrations/opentelemetry/src/commonMain/kotlin/com/smellouk/kamper/opentelemetry/Module.kt
    - kamper/integrations/opentelemetry/src/commonMain/kotlin/com/smellouk/kamper/opentelemetry/RecordGauge.kt
    - kamper/integrations/opentelemetry/src/jvmMain/kotlin/com/smellouk/kamper/opentelemetry/RecordGauge.kt
    - kamper/integrations/opentelemetry/src/androidMain/kotlin/com/smellouk/kamper/opentelemetry/RecordGauge.kt
    - kamper/integrations/opentelemetry/src/iosMain/kotlin/com/smellouk/kamper/opentelemetry/RecordGauge.kt
    - kamper/integrations/opentelemetry/src/macosMain/kotlin/com/smellouk/kamper/opentelemetry/RecordGauge.kt
    - kamper/integrations/opentelemetry/src/jsMain/kotlin/com/smellouk/kamper/opentelemetry/RecordGauge.kt
    - kamper/integrations/opentelemetry/src/wasmJsMain/kotlin/com/smellouk/kamper/opentelemetry/RecordGauge.kt
    - kamper/integrations/opentelemetry/src/commonTest/kotlin/com/smellouk/kamper/opentelemetry/OtelConfigBuilderTest.kt
    - kamper/integrations/opentelemetry/src/commonTest/kotlin/com/smellouk/kamper/opentelemetry/OtelIntegrationModuleTest.kt
  modified:
    - settings.gradle.kts
decisions:
  - "Use opentelemetry-java 1.51.0 Java SDK (not opentelemetry-kotlin 0.3.0) because kotlin SDK has no Metrics API"
  - "JVM+Android only for real export; iOS/macOS/JS/WasmJS get no-op actuals due to no KMP OTel Metrics"
  - "ConcurrentHashMap keyed by (endpoint, authToken) to share one SdkMeterProvider per exporter target"
  - "async gauge via buildWithCallback + AtomicReference — canonical OTel Java idiom for current-value gauges"
  - "build-logic convention plugins used (kamper.android.config + kamper.publish) instead of apply(from=...) per project conventions"
metrics:
  duration: "~15 minutes"
  completed: "2026-04-28"
  tasks_completed: 5
  tasks_total: 5
  files_created: 13
  files_modified: 1
  tests_passing: 11
---

# Phase 18 Plan 05: OpenTelemetry OTLP Integration Summary

**One-liner:** OTLP gauge export for Kamper CPU/memory/FPS metrics via opentelemetry-java 1.51.0 with JVM+Android real export and no-op actuals on iOS/macOS/JS/WasmJS.

## What Was Built

A new `kamper/integrations/opentelemetry/` KMP subproject that exports Kamper metric events as OpenTelemetry gauge measurements over OTLP HTTP to any compatible backend (Grafana Cloud, Datadog OTLP, New Relic OTLP, Honeycomb, self-hosted OTel Collector). This eliminates the need for separate `kamper-grafana` / `kamper-datadog` artifacts.

### New Subproject: `:kamper:integrations:opentelemetry`

**Target list:**
| Target | Behavior |
|--------|----------|
| `androidTarget` | Real OTLP gauge export via opentelemetry-java 1.51.0 |
| `jvm` | Real OTLP gauge export via opentelemetry-java 1.51.0 |
| `iosArm64` + `iosSimulatorArm64` | No-op `actual` |
| `macosX64` + `macosArm64` | No-op `actual` |
| `js(IR)` | No-op `actual` |
| `wasmJs` | No-op `actual` |

**Why iOS/macOS/JS/WasmJS are no-op:**
- opentelemetry-kotlin 0.3.0 (the KMP SDK) has **no Metrics API** (RESEARCH Pitfall 1)
- opentelemetry-kotlin does **not target macOS or wasmJs** (RESEARCH Pitfall 2)
- The OTel Java SDK is a JVM-only artifact and cannot resolve on Native/JS targets

### DSL Usage Example

```kotlin
Kamper
    .install(CpuModule)
    .install(MemoryModule)
    .addIntegration(
        OpenTelemetryModule(otlpEndpointUrl = "https://otlp-gateway-prod-us-central-0.grafana.net/otlp/v1/metrics") {
            otlpAuthToken = "Bearer glc_eyJ..."
            forwardCpu = true
            forwardMemory = true
            forwardFps = false
            exportIntervalSeconds = 30L
        }
    )
```

### Shared MeterProvider Cache

The JVM+Android `recordGauge` actual maintains a `ConcurrentHashMap<ProviderKey, GaugeRegistration>` keyed by `(endpoint, authToken)`. The first event for a given key builds a `SdkMeterProvider` + `PeriodicMetricReader` and registers an async gauge callback once; subsequent events with the same key only update an `AtomicReference<Double?>` that the callback closure captures. This avoids creating a new HTTP exporter per metric event.

### URL Validation Rule

`OtelIntegrationModule` validates the endpoint URL at construction time by checking for an `http://` or `https://` prefix. If the URL is invalid, `onEvent()` returns early and never calls `recordGauge`. This satisfies OWASP V5 ASVS L1 input validation (T-16-12).

### Security Mitigations

| Threat | Mitigation |
|--------|------------|
| T-16-01 (token disclosure) | Token stored only in private `config: OtelConfig`. Never logged. |
| T-16-02 (SDK exceptions) | Double try/catch: outer in `onEvent`, inner in `recordGauge` actual |
| T-16-03 (consent violation) | All `forwardXxx` flags default to `false` (opt-in only) |
| T-16-04 (INVALID forwarding) | First branch in `onEvent`: `if (event.info === Info.INVALID) return` |
| T-16-04b (wrong actual on Native) | No `io.opentelemetry` imports in iOS/macOS/JS/WasmJS actuals |
| T-16-12 (bad URL) | `urlIsValid` check rejects non-http(s) endpoints silently |

### Test Results

11 tests pass via `./gradlew :kamper:integrations:opentelemetry:jvmTest`:

**OtelConfigBuilderTest (4 tests):**
- Builder defaults all forwarding off per D-10 and 30s interval
- Builder honors all opt-in toggles per D-10/D-11
- OpenTelemetryModule factory invokes Builder DSL and returns OtelIntegrationModule
- OpenTelemetryModule factory with empty DSL builds with defaults

**OtelIntegrationModuleTest (7 tests):**
- onEvent silently drops Info.INVALID even when all forwarding enabled (T-16-04)
- onEvent rejects non-http endpoints silently (V5 input validation)
- onEvent does not forward CPU when forwardCpu is false (D-10)
- onEvent ignores moduleNames outside cpu/memory/fps route table (D-09)
- onEvent on JVM with valid endpoint does not throw even when network unreachable (T-16-02)
- onEvent does not propagate exceptions from pathological Info (T-16-02)
- clean is a no-op and does not throw

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] buildSrc/Modules.kt does not exist**
- **Found during:** Task 1
- **Issue:** Plan references `buildSrc/src/main/kotlin/Modules.kt` for a `const val OPENTELEMETRY` constant, but the project migrated to `build-logic` convention plugins with no `buildSrc` directory. No `Modules.kt` exists anywhere in the project.
- **Fix:** Skipped `Modules.kt` update entirely. The opentelemetry integration references its project path directly as a string in `build.gradle.kts` (same pattern as `kamper/integrations/sentry/build.gradle.kts`).
- **Impact:** None — no consuming module references `Modules.OPENTELEMETRY` at this stage.

**2. [Rule 3 - Blocking] Plan's build.gradle.kts used apply(from=...) instead of convention plugins**
- **Found during:** Task 2
- **Issue:** Plan template uses `apply(from = projectDir.resolve("../../../publish.gradle.kts"))` but the project uses `id("kamper.publish")` + `id("kamper.android.config")` convention plugins from `build-logic`.
- **Fix:** Used `id("kamper.android.config")` and `id("kamper.publish")` matching the sentry/firebase integration pattern.
- **Files modified:** `kamper/integrations/opentelemetry/build.gradle.kts`

**3. [Rule 3 - Blocking] React Native node_modules missing in worktree**
- **Found during:** Task 3 verification
- **Issue:** The `demos/react-native/node_modules/@react-native/gradle-plugin` directory is missing in the worktree, causing ALL Gradle tasks to fail at settings evaluation.
- **Fix:** Created a symlink from `demos/react-native/node_modules` in the worktree to the main repo's populated `node_modules`. All subsequent Gradle builds succeeded.

**4. [Observation] Test count is 11 not 10**
- **Found during:** Task 5
- **Issue:** The plan's acceptance criteria says `jvmTest` runs 10 tests (4+6), but the plan's actual test source has 7 tests in `OtelIntegrationModuleTest`, producing 11 total.
- **Fix:** All 7 tests in `OtelIntegrationModuleTest` as written in the plan template are included; the criteria count was a typo in the plan.

## Phase 18 Completion Note

With Plan 05 complete, `settings.gradle.kts` now registers all three integration subprojects:
- `:kamper:integrations:sentry` (Plan 18-03)
- `:kamper:integrations:firebase` (Plan 18-04)
- `:kamper:integrations:opentelemetry` (Plan 18-05)

The `kamper.publish` convention plugin's artifact-name logic already covers `integrations` parent dir (implemented in the `KamperPublishPlugin`), so `opentelemetry-integration` resolves automatically. Phase 18 is now feature-complete for the integration layer.

## Self-Check: PASSED

- [x] `settings.gradle.kts` contains `include(":kamper:integrations:opentelemetry")` — FOUND
- [x] `kamper/integrations/opentelemetry/build.gradle.kts` — FOUND
- [x] `kamper/integrations/opentelemetry/src/commonMain/.../OtelConfig.kt` — FOUND
- [x] `kamper/integrations/opentelemetry/src/commonMain/.../OtelIntegrationModule.kt` — FOUND
- [x] `kamper/integrations/opentelemetry/src/commonMain/.../Module.kt` — FOUND
- [x] `kamper/integrations/opentelemetry/src/commonMain/.../RecordGauge.kt` — FOUND
- [x] `kamper/integrations/opentelemetry/src/jvmMain/.../RecordGauge.kt` — FOUND (contains OtlpHttpMetricExporter)
- [x] `kamper/integrations/opentelemetry/src/androidMain/.../RecordGauge.kt` — FOUND (contains OtlpHttpMetricExporter)
- [x] No-op actuals for iosMain, macosMain, jsMain, wasmJsMain — FOUND
- [x] `kamper/integrations/opentelemetry/src/commonTest/.../OtelConfigBuilderTest.kt` — FOUND (4 tests)
- [x] `kamper/integrations/opentelemetry/src/commonTest/.../OtelIntegrationModuleTest.kt` — FOUND (7 tests)
- [x] Commits: 87fb552, 23655cb, 2627c5e, 7232625, 74d1844 — all present
- [x] `./gradlew :kamper:integrations:opentelemetry:jvmTest` — 11 tests PASSING
- [x] Zero OTel imports in no-op platform files — VERIFIED
- [x] Zero forbidden module imports in commonMain — VERIFIED
