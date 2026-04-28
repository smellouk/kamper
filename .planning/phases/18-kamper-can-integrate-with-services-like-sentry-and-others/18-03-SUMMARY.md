---
phase: 18-kamper-can-integrate-with-services-like-sentry-and-others
plan: "03"
subsystem: kamper-sentry
tags: [integration, sentry, kmp, try-catch, isolation, breadcrumb, captureException, dsl]

dependency_graph:
  requires:
    - phase: 18-01
      provides: KamperEvent data class + IntegrationModule interface + currentPlatform expect/actual
    - phase: 18-02
      provides: Engine.addIntegration() + dispatchToIntegrations() fan-out
  provides:
    - SentryIntegrationModule implementing IntegrationModule — routes KamperEvent to Sentry SDK
    - SentryConfig DSL builder (forwardIssues, forwardCpuAbove, forwardMemoryAbove, forwardFps)
    - SentryModule(dsn) public DSL factory
    - :kamper:integrations:sentry Gradle subproject registered in settings.gradle.kts
    - KamperPublishPlugin updated to produce "sentry-integration" artifact ID for integrations/ modules
  affects:
    - Plans 18-04, 18-05 (FirebaseModule, OtelModule) benefit from the same publish artifact naming fix

tech_stack:
  added:
    - io.sentry:sentry-kotlin-multiplatform:0.13.0 (Android, iOS arm64+simulatorArm64, JVM, macOS x64+arm64)
  patterns:
    - toString()-based numeric extraction for threshold gating — avoids compile-time dep on metric modules
    - Info.INVALID sentinel guard as first line of onEvent (T-16-04)
    - Double try/catch: init{} and onEvent() both wrap all SDK calls (T-16-01, T-16-02)
    - object Builder with reset() for re-entrant DSL factory calls (SentryConfig pattern)

key-files:
  created:
    - kamper/integrations/sentry/build.gradle.kts
    - kamper/integrations/sentry/src/commonMain/kotlin/com/smellouk/kamper/sentry/SentryConfig.kt
    - kamper/integrations/sentry/src/commonMain/kotlin/com/smellouk/kamper/sentry/SentryIntegrationModule.kt
    - kamper/integrations/sentry/src/commonMain/kotlin/com/smellouk/kamper/sentry/Module.kt
    - kamper/integrations/sentry/src/commonTest/kotlin/com/smellouk/kamper/sentry/SentryConfigBuilderTest.kt
    - kamper/integrations/sentry/src/commonTest/kotlin/com/smellouk/kamper/sentry/SentryIntegrationModuleTest.kt
  modified:
    - settings.gradle.kts — added include(":kamper:integrations:sentry")
    - build-logic/src/main/kotlin/KamperPublishPlugin.kt — artifact naming fix for integrations/ path

key-decisions:
  - "buildSrc/Modules.kt does not exist (Phase 11 migrated to build-logic convention plugins); used string literals directly per all other modules"
  - "kamper/publish.gradle.kts does not exist (Phase 11 migration); adapted artifact naming fix to KamperPublishPlugin.kt instead"
  - "js(IR) and wasmJs targets removed from build.gradle.kts: sentry-kotlin-multiplatform:0.13.0 does not publish JS/WasmJS artifacts; including them causes KMP dependency resolution failure (Rule 3 fix)"
  - "kamper.kmp.library convention plugin NOT used: it adds tvOS targets which are excluded per phase scope; used individual plugin IDs instead"

requirements-completed:
  - D-04
  - D-05
  - D-08
  - D-10
  - D-11

duration: ~30min
completed: "2026-04-28"
---

# Phase 18 Plan 03: kamper-sentry Integration Module Summary

**New `:kamper:integrations:sentry` KMP subproject implementing IntegrationModule to route KamperEvents to Sentry: IssueInfo -> captureException, CPU/Memory/FPS -> breadcrumb with threshold gating, all wrapped in try/catch isolation.**

## Performance

- **Duration:** ~30 min
- **Started:** 2026-04-28
- **Completed:** 2026-04-28
- **Tasks:** 4 (all executed)
- **Files modified/created:** 8

## Accomplishments

- New Gradle subproject `:kamper:integrations:sentry` registered and building
- `KamperPublishPlugin.kt` updated with `when { parentDir.contains("integrations") -> "${project.name}-integration" ... }` — future Plans 18-04/05 get this for free
- `SentryConfig` data class with `object Builder` DSL (forwardIssues off, forwardCpuAbove null, forwardMemoryAbove null, forwardFps off by default — all opt-in per D-10)
- `SentryIntegrationModule` routing table: `"issue"/"issues"` -> `captureException`, `"cpu"` -> breadcrumb (threshold), `"memory"` -> breadcrumb (threshold), `"fps"` -> breadcrumb, `else` -> drop
- Info.INVALID guard as first line (T-16-04), double try/catch (init + onEvent) per T-16-01/T-16-02
- toString()-based ratio extractor for threshold gating — no compile-time dep on metric modules
- `SentryModule(dsn) { ... }` public DSL factory with `@KamperDslMarker` + `@Suppress("FunctionNaming")`
- 9 tests (3 Builder + 6 Module) all passing on `:kamper:integrations:sentry:jvmTest`
- `:kamper:integrations:sentry:assemble` exits 0

## Subproject Target Set

| Target | Included | Reason |
|--------|----------|--------|
| androidTarget | YES | sentry-kotlin-multiplatform supports Android |
| jvm | YES | supported |
| macosX64 | YES | supported |
| macosArm64 | YES | supported |
| iosArm64 | YES | supported |
| iosSimulatorArm64 | YES | supported |
| tvosArm64 | NO | excluded per CONTEXT.md phase scope |
| tvosSimulatorArm64 | NO | excluded per CONTEXT.md phase scope |
| js(IR) | NO | sentry-kotlin-multiplatform:0.13.0 does not publish JS artifacts |
| wasmJs | NO | sentry-kotlin-multiplatform:0.13.0 does not publish WasmJS artifacts |

## DSL Usage Example

```kotlin
Kamper
    .install(CpuModule)
    .install(MemoryModule)
    .addIntegration(
        SentryModule(dsn = "https://abc123@sentry.io/123456") {
            forwardIssues = true       // IssueInfo -> Sentry.captureException
            forwardCpuAbove = 80f      // CPU > 80% -> Sentry breadcrumb
            forwardMemoryAbove = 85f   // Memory > 85% -> Sentry breadcrumb
            forwardFps = false         // FPS not forwarded
        }
    )
```

## Event Routing Rules

| `event.moduleName` | Config gate | Sentry call |
|--------------------|-------------|-------------|
| `"issue"` or `"issues"` | `forwardIssues = true` | `Sentry.captureException(RuntimeException(msg))` |
| `"cpu"` | `forwardCpuAbove != null AND value > threshold` | `Sentry.addBreadcrumb(WARNING)` |
| `"memory"` | `forwardMemoryAbove != null AND value > threshold` | `Sentry.addBreadcrumb(WARNING)` |
| `"fps"` | `forwardFps = true` | `Sentry.addBreadcrumb(INFO)` |
| anything else | — | silently dropped |

## macOS Strategy

`sentry-kotlin-multiplatform:0.13.0` is used as a HARD implementation dependency for macOS targets. If the Sentry Cocoa pod is missing on the host app's macOS target, `Symbol not found` at first SDK call surfaces as a `Throwable` caught by the `init {}` try/catch block — the host app does not crash. The `user_setup` in the plan documents the optional Cocoa pod for host apps wanting live macOS Sentry capture.

## Task Commits

| Task | Hash | Description |
|------|------|-------------|
| Task 1 | `1b5e513` | feat(18-03): register :kamper:integrations:sentry + fix publish artifact naming for integrations/ |
| Task 2 | (included in Task 1 commit) | build.gradle.kts created with KMP targets |
| Task 3 | `aada39f` | feat(18-03): add SentryConfig, SentryIntegrationModule, and SentryModule DSL factory |
| Task 4 | `dea1f53` | test(18-03): add SentryConfigBuilderTest and SentryIntegrationModuleTest (9 tests) |

## Files Created/Modified

- `settings.gradle.kts` — Added `include(":kamper:integrations:sentry")`
- `build-logic/src/main/kotlin/KamperPublishPlugin.kt` — Artifact naming: integrations/ -> `${project.name}-integration`
- `kamper/integrations/sentry/build.gradle.kts` — KMP targets + sentry-kotlin-multiplatform:0.13.0 dep
- `kamper/integrations/sentry/src/commonMain/kotlin/com/smellouk/kamper/sentry/SentryConfig.kt` — DSL config builder
- `kamper/integrations/sentry/src/commonMain/kotlin/com/smellouk/kamper/sentry/SentryIntegrationModule.kt` — IntegrationModule implementation
- `kamper/integrations/sentry/src/commonMain/kotlin/com/smellouk/kamper/sentry/Module.kt` — Public DSL factory
- `kamper/integrations/sentry/src/commonTest/kotlin/com/smellouk/kamper/sentry/SentryConfigBuilderTest.kt` — 3 Builder tests
- `kamper/integrations/sentry/src/commonTest/kotlin/com/smellouk/kamper/sentry/SentryIntegrationModuleTest.kt` — 6 module routing tests

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] buildSrc/Modules.kt does not exist**
- **Found during:** Task 1
- **Issue:** Plan referenced `buildSrc/src/main/kotlin/Modules.kt` for adding `object Integrations { const val SENTRY = "..." }` — this file does not exist. The project migrated from buildSrc to build-logic convention plugins in Phase 11.
- **Fix:** Skipped Modules.kt step. Used string literal `":kamper:api"` directly in build.gradle.kts, same pattern as all other modules (cpu, engine, etc.). No `Modules.kt` constant needed.
- **Files modified:** None (deviation is a skip, not a code change)

**2. [Rule 3 - Blocking] kamper/publish.gradle.kts does not exist**
- **Found during:** Task 1
- **Issue:** Plan specified `apply(from = projectDir.resolve("../../../publish.gradle.kts"))` in build.gradle.kts and edits to `kamper/publish.gradle.kts` — this file was replaced by `KamperPublishPlugin.kt` in Phase 11 composite build migration.
- **Fix:** Applied `id("kamper.publish")` convention plugin in build.gradle.kts and edited `build-logic/src/main/kotlin/KamperPublishPlugin.kt` to add the `integrations` path branch.
- **Files modified:** `build-logic/src/main/kotlin/KamperPublishPlugin.kt`

**3. [Rule 3 - Blocking] sentry-kotlin-multiplatform:0.13.0 does not support JS/WasmJS targets**
- **Found during:** Task 3 compile verification
- **Issue:** Adding `js(IR)` and `wasmJs` targets to build.gradle.kts caused a KMP dependency resolution failure: "Source set 'webMain' couldn't resolve dependencies for all target platforms. Unresolved platforms: [js, wasmJs]". The plan stated that 0.13.0 "produces no-op stubs for JS/WasmJS" — this was incorrect; the library does not publish JS/WasmJS artifacts.
- **Fix:** Removed `js(IR)` and `wasmJs` target declarations from `kamper/integrations/sentry/build.gradle.kts` with explanatory comment.
- **Files modified:** `kamper/integrations/sentry/build.gradle.kts`

**4. [Rule 3 - Blocking] kamper.kmp.library convention plugin cannot be used**
- **Found during:** Task 2 design
- **Issue:** The `kamper.kmp.library` plugin automatically adds tvOS targets (`tvosArm64`, `tvosSimulatorArm64`) which are intentionally excluded per CONTEXT.md phase scope. Using the convention plugin would require a workaround to remove targets.
- **Fix:** Applied individual plugin IDs (`org.jetbrains.kotlin.multiplatform`, `com.android.library`, `dev.mokkery`, `kamper.android.config`, `kamper.publish`) directly in build.gradle.kts and configured only the required target set.
- **Files modified:** `kamper/integrations/sentry/build.gradle.kts`

---

**Total deviations:** 4 auto-fixed (all Rule 3 blocking fixes)
**Impact on plan:** Build artifacts are equivalent. The JS/WasmJS exclusion is an accurate reflection of sentry-kotlin-multiplatform 0.13.0 capabilities. buildSrc/publish.gradle.kts stubs are replaced by the Phase 11 convention plugin equivalents.

## Note for Plans 18-04 and 18-05

- `settings.gradle.kts` and `KamperPublishPlugin.kt` artifact-name fix are now SHARED. Plans 18-04/05 only need to:
  - Add their own `include(":kamper:integrations:firebase")` / `include(":kamper:integrations:opentelemetry")` line to `settings.gradle.kts`
  - Use `id("kamper.publish")` in their build.gradle.kts — the `parentDir.contains("integrations")` branch is already in place
- `moduleName` for IssueInfo is `"issue"` (singular, NOT "issues") per Plan 18-02 summary

## Known Stubs

None — SentryIntegrationModule fully wired to Sentry SDK. All routes (issue/cpu/memory/fps) are implemented. Default configuration is all-off (opt-in per D-10) which is intentional behavior, not a stub.

## Threat Surface Scan

No new network endpoints or auth paths. Trust boundaries:

- Kamper core ↔ Sentry SDK: All SDK calls inside SentryIntegrationModule behind try/catch. Kamper never exposes Sentry types in public API.
- T-16-01 (DSN disclosure): DSN in private `config` field only. Never logged. init{} catch block does not reference DSN.
- T-16-02 (SDK DoS): init{} and onEvent() both wrap all SDK calls in try/catch(Throwable).
- T-16-03 (Forward without consent): All flags default to false/null. Test "Builder build returns config with provided dsn and defaults" locks this.
- T-16-04 (INVALID forwarded): First line of onEvent() is `if (event.info === Info.INVALID) return`. Test locks it.

## Self-Check: PASSED

Files verified:
- `kamper/integrations/sentry/build.gradle.kts` — FOUND
- `kamper/integrations/sentry/src/commonMain/kotlin/com/smellouk/kamper/sentry/SentryConfig.kt` — FOUND
- `kamper/integrations/sentry/src/commonMain/kotlin/com/smellouk/kamper/sentry/SentryIntegrationModule.kt` — FOUND
- `kamper/integrations/sentry/src/commonMain/kotlin/com/smellouk/kamper/sentry/Module.kt` — FOUND
- `kamper/integrations/sentry/src/commonTest/kotlin/com/smellouk/kamper/sentry/SentryConfigBuilderTest.kt` — FOUND
- `kamper/integrations/sentry/src/commonTest/kotlin/com/smellouk/kamper/sentry/SentryIntegrationModuleTest.kt` — FOUND
- `settings.gradle.kts` — include(":kamper:integrations:sentry") verified
- `build-logic/src/main/kotlin/KamperPublishPlugin.kt` — integrations branch verified

Commits verified:
- `1b5e513` — Task 1 feat commit
- `aada39f` — Task 3 feat commit
- `dea1f53` — Task 4 test commit

Tests: 9/9 passing on `:kamper:integrations:sentry:jvmTest`

---
*Phase: 18-kamper-can-integrate-with-services-like-sentry-and-others*
*Completed: 2026-04-28*
