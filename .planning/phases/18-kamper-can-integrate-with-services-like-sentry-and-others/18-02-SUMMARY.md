---
phase: 18-kamper-can-integrate-with-services-like-sentry-and-others
plan: "02"
subsystem: kamper-engine
tags: [integration, event-driven, fan-out, kmp, engine, try-catch, isolation]

dependency_graph:
  requires:
    - phase: 18-01
      provides: KamperEvent data class + IntegrationModule interface + currentPlatform expect/actual
  provides:
    - Engine.addIntegration() public fluent method
    - Engine.removeIntegration() public fluent method with clean() lifecycle
    - Engine.dispatchToIntegrations() @PublishedApi internal with try/catch isolation
    - Integration fan-out InfoListener registered per install() call
    - Engine.clear() extended to clean integrations before performanceList teardown
  affects:
    - Plans 18-03 through 18-05 (SentryModule, FirebaseModule, OtelModule implementations)

tech_stack:
  added: []
  patterns:
    - Integration fan-out via internal InfoListener<I> registered inside install() — keeps Performance/Watcher classes untouched
    - Try/catch isolation per integration in dispatchToIntegrations() — buggy third-party SDK can never crash Kamper (T-16-02)
    - Snapshot-before-iterate (integrationList.toList()) in dispatchToIntegrations — prevents ConcurrentModificationException on re-entrant removeIntegration (T-16-06)
    - "@PublishedApi internal" on engineCurrentTimeMs — required for inline install() lambda to call it

key-files:
  created:
    - kamper/engine/src/commonTest/kotlin/com/smellouk/kamper/EngineIntegrationTest.kt
  modified:
    - kamper/engine/src/commonMain/kotlin/com.smellouk.kamper/Engine.kt
    - kamper/engine/src/commonMain/kotlin/com.smellouk.kamper/EnginePlatformTime.kt
    - kamper/engine/src/commonTest/kotlin/com/smellouk/kamper/EngineTest.kt

key-decisions:
  - "engineCurrentTimeMs already existed as EnginePlatformTime.kt — Task 1 skipped per plan pre-existence check"
  - "engineCurrentTimeMs marked @PublishedApi to allow inline install() lambda to call it — required by Kotlin inline visibility rules"
  - "moduleName derived via I::class.simpleName?.removeSuffix(\"Info\")?.lowercase() — returns 'issue' (singular) not 'issues'; Plans 18-03/04/05 must handle this"
  - "EngineTest assertions for mapListeners[Info::class]?.size updated from 0 to 1 to account for internal fan-out listener"

patterns-established:
  - "Fan-out pattern: Engine.install() registers one internal InfoListener<I> per installed module type that wraps Info -> KamperEvent and calls dispatchToIntegrations()"
  - "Integration isolation: every onEvent() and clean() call wrapped in try/catch; exceptions logged via Logger, never propagated"

requirements-completed:
  - D-02
  - D-10

duration: ~20min
completed: "2026-04-28"
---

# Phase 18 Plan 02: Engine Integration Fan-Out Summary

**Engine gains addIntegration/removeIntegration/dispatchToIntegrations + per-module fan-out InfoListener that wraps every Info update into a KamperEvent dispatched to all registered IntegrationModules with try/catch isolation.**

## Performance

- **Duration:** ~20 min
- **Started:** 2026-04-28
- **Completed:** 2026-04-28
- **Tasks:** 3 (Task 1 skipped — engineCurrentTimeMs already present; Tasks 2 and 3 executed)
- **Files modified:** 4

## Accomplishments

- Engine extended with `integrationList: MutableList<IntegrationModule>` alongside existing `performanceList`
- Public fluent API: `addIntegration(integration): Engine` and `removeIntegration(integration): Engine` with `clean()` lifecycle
- Internal `dispatchToIntegrations(event)` with snapshot-before-iterate and per-integration try/catch — satisfies threat T-16-02 and T-16-06
- `install()` now registers an internal `InfoListener<I>` that creates `KamperEvent(moduleName, timestampMs, platform, info)` and dispatches it — Performance/Watcher classes untouched
- `clear()` cleans all integrations first (before performanceList), per D-02 shutdown contract
- 7 new tests in `EngineIntegrationTest` covering D-02, D-10, and T-16-02 isolation contract
- All 29 existing engine tests continue to pass

## Engine Addition Summary

### New public surface

| Symbol | Type | Returns |
|--------|------|---------|
| `addIntegration(integration: IntegrationModule)` | `fun` | `Engine` (fluent) |
| `removeIntegration(integration: IntegrationModule)` | `fun` | `Engine` (fluent) |

### New internal surface

| Symbol | Visibility |
|--------|-----------|
| `integrationList: MutableList<IntegrationModule>` | `@PublishedApi internal` |
| `dispatchToIntegrations(event: KamperEvent)` | `@PublishedApi internal` |

### Line count delta

`Engine.kt`: ~95 lines added (addIntegration, removeIntegration, dispatchToIntegrations bodies + clear() additions + install() fan-out lambda)

## moduleName Derivation Rule

`I::class.simpleName?.removeSuffix("Info")?.lowercase() ?: "unknown"`

| Info class | moduleName |
|------------|------------|
| CpuInfo | "cpu" |
| FpsInfo | "fps" |
| MemoryInfo | "memory" |
| NetworkInfo | "network" |
| IssueInfo | **"issue"** (singular — not "issues") |
| JankInfo | "jank" |
| GcInfo | "gc" |
| ThermalInfo | "thermal" |

**IMPORTANT for Plans 18-03/04/05:** `IssueInfo` produces `"issue"` not `"issues"`. When routing on `event.moduleName`, handle `"issue"`.

## engineCurrentTimeMs Status

Pre-existing — shipped by Phase 9 (FEAT-03 validate() work) as `EnginePlatformTime.kt`. Task 1 skipped per pre-existence check. The function was updated from `internal` to `@PublishedApi internal` to allow the `inline fun install()` lambda to call it.

## Task Commits

| Task | Hash | Description |
|------|------|-------------|
| Task 1 | (skipped) | engineCurrentTimeMs already present as EnginePlatformTime.kt |
| Task 2 | `0bf353c` | feat(18-02): wire integrationList, addIntegration, removeIntegration, dispatchToIntegrations, and install fan-out into Engine |
| Task 3 | `c0cc08f` | test(18-02): add EngineIntegrationTest covering D-02, D-10, and threat T-16-02 |

## Files Created/Modified

- `kamper/engine/src/commonMain/kotlin/com.smellouk.kamper/Engine.kt` — Added integrationList, addIntegration, removeIntegration, dispatchToIntegrations, install fan-out, extended clear()
- `kamper/engine/src/commonMain/kotlin/com.smellouk.kamper/EnginePlatformTime.kt` — Added @PublishedApi to engineCurrentTimeMs expect declaration
- `kamper/engine/src/commonTest/kotlin/com/smellouk/kamper/EngineIntegrationTest.kt` — New: 7 tests for D-02 + D-10 + T-16-02
- `kamper/engine/src/commonTest/kotlin/com/smellouk/kamper/EngineTest.kt` — Updated 2 assertions on mapListeners[Info::class]?.size (0→1) to account for internal fan-out listener

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Updated EngineTest assertions for inner listener list size**
- **Found during:** Task 2 (Engine.kt modification)
- **Issue:** Two existing tests in `EngineTest.kt` asserted `mapListeners[Info::class]?.size == 0` after a removeInfoListener/no-add-listener scenario. After install() registers the internal fan-out listener, the inner list size is 1 (fan-out) not 0. The plan stated "existing tests pass unchanged" but the fan-out listener change made those assertions stale.
- **Fix:** Updated the two assertions from `== 0` to `== 1` with explanatory comments documenting the internal fan-out listener
- **Files modified:** `kamper/engine/src/commonTest/kotlin/com/smellouk/kamper/EngineTest.kt`
- **Verification:** All 13 EngineTest tests pass post-fix
- **Committed in:** `0bf353c` (Task 2 commit)

**2. [Rule 3 - Blocking] Added @PublishedApi to engineCurrentTimeMs**
- **Found during:** Task 2 compilation
- **Issue:** Kotlin compiler rejected the call to `engineCurrentTimeMs()` inside the inline `install()` function's lambda: "Public-API inline function cannot access non-public-API function". The function was `internal` without `@PublishedApi`, which inline functions cannot reference.
- **Fix:** Added `@PublishedApi` annotation to `internal expect fun engineCurrentTimeMs(): Long` in `EnginePlatformTime.kt`
- **Files modified:** `kamper/engine/src/commonMain/kotlin/com.smellouk.kamper/EnginePlatformTime.kt`
- **Verification:** `./gradlew :kamper:engine:compileKotlinMetadata` exits 0
- **Committed in:** `0bf353c` (Task 2 commit)

---

**Total deviations:** 2 auto-fixed (1 bug fix, 1 blocking fix)
**Impact on plan:** Both fixes required for correctness. No scope creep. The EngineTest assertion update accurately reflects the new Engine.install() behavior.

## Issues Encountered

- `demos/react-native/node_modules` symlink missing in worktree — created symlink to main repo's node_modules to allow Gradle build to resolve the RN composite build.

## Known Stubs

None — all dispatching wired end-to-end. Integration modules consume real `KamperEvent` objects with real timestamps and platform tags.

## Threat Surface Scan

No new network endpoints, auth paths, file access patterns, or schema changes. The integration fan-out is in-process only. Threats T-16-02 (SDK throws) and T-16-06 (re-entrant removeIntegration) are both mitigated:

- T-16-02: each `integration.onEvent()` wrapped in `try { ... } catch (t: Throwable)` in `dispatchToIntegrations`
- T-16-06: `dispatchToIntegrations` snapshots `integrationList.toList()` before iterating

## Next Phase Readiness

- Plans 18-03 / 18-04 / 18-05 can implement `SentryModule`, `FirebaseModule`, `OtelModule` by implementing `IntegrationModule` and routing on `event.moduleName` (use `"issue"` not `"issues"`)
- Engine integration lifecycle fully covered by tests

## Self-Check: PASSED

Files verified:
- `kamper/engine/src/commonMain/kotlin/com.smellouk.kamper/Engine.kt` — FOUND
- `kamper/engine/src/commonMain/kotlin/com.smellouk.kamper/EnginePlatformTime.kt` — FOUND
- `kamper/engine/src/commonTest/kotlin/com/smellouk/kamper/EngineIntegrationTest.kt` — FOUND
- `kamper/engine/src/commonTest/kotlin/com/smellouk/kamper/EngineTest.kt` — FOUND

Commits verified:
- `0bf353c` — Task 2 feat commit
- `c0cc08f` — Task 3 test commit

---
*Phase: 18-kamper-can-integrate-with-services-like-sentry-and-others*
*Completed: 2026-04-28*
