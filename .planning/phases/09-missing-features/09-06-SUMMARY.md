---
phase: 09
plan: "06"
subsystem: engine-validation
tags: [feat-03, engine, validate, validation-info, expect-actual, common-main]
dependency_graph:
  requires:
    - 09-01 (Performance.lastValidSampleAt @Volatile field)
    - 09-05 (Performance.installedAt @Volatile field, Watcher timestamp plumbing)
  provides:
    - ValidationInfo data class (engine commonMain) with EMPTY/INVALID companions
    - Engine.init {} seeding mapListeners[ValidationInfo::class]
    - Engine.clear() re-seeding ValidationInfo slot (Assumption A5)
    - Engine.validate(): List<String> — FEAT-03 health-check API
    - engineCurrentTimeMs() internal expect/actual — 7 platform actuals in engine module
  affects:
    - kamper/engine (Engine, ValidationInfo, EnginePlatformTime)
    - kamper/api (Performance.lastValidSampleAt, installedAt visibility changes)
tech_stack:
  added: []
  patterns:
    - Listener-slot seeding in init {} + re-seeding in clear() (ValidationInfo has no module)
    - installedAt anchor for elapsed computation in Engine.validate()
    - internal expect/actual Platform Time (mirrors kamper/api module pattern)
key_files:
  created:
    - kamper/engine/src/commonMain/kotlin/com.smellouk.kamper/ValidationInfo.kt
    - kamper/engine/src/commonMain/kotlin/com.smellouk.kamper/EnginePlatformTime.kt
    - kamper/engine/src/androidMain/kotlin/com/smellouk/kamper/EnginePlatformTime.kt
    - kamper/engine/src/jvmMain/kotlin/com/smellouk/kamper/EnginePlatformTime.kt
    - kamper/engine/src/iosMain/kotlin/com/smellouk/kamper/EnginePlatformTime.kt
    - kamper/engine/src/macosMain/kotlin/com/smellouk/kamper/EnginePlatformTime.kt
    - kamper/engine/src/tvosMain/kotlin/com/smellouk/kamper/EnginePlatformTime.kt
    - kamper/engine/src/jsMain/kotlin/com/smellouk/kamper/EnginePlatformTime.kt
    - kamper/engine/src/wasmJsMain/kotlin/com/smellouk/kamper/EnginePlatformTime.kt
  modified:
    - kamper/engine/src/commonMain/kotlin/com.smellouk.kamper/Engine.kt
    - kamper/api/src/commonMain/kotlin/com/smellouk/kamper/api/Performance.kt
    - kamper/engine/src/commonTest/kotlin/com/smellouk/kamper/EngineValidateTest.kt
    - kamper/engine/src/commonTest/kotlin/com/smellouk/kamper/EngineTest.kt
decisions:
  - "Performance.lastValidSampleAt and installedAt changed from @Volatile internal var to @Volatile open var — drops internal to allow cross-module access from Engine.validate(), adds open so Mokkery can generate mocks"
  - "Engine commonMain uses single-directory com.smellouk.kamper naming (literal dots, matches existing Engine.kt path); platform actuals use nested com/smellouk/kamper/ (matches existing Kamper.kt actuals)"
  - "engineCurrentTimeMs() is a separate internal expect fun from api.currentApiTimeMs() because Kotlin internal symbols do not cross module boundaries"
  - "POSIX actuals (ios/macos/tvos) use @Suppress(MagicNumber) for the 1_000L/1_000_000L conversion constants"
  - "Engine.kt extracts MS_PER_SECOND = 1_000L companion constant for the elapsed/1000 format in validate() problem strings"
  - "EngineTest mapListeners.size assertions updated from 0/1 to 1/2 (Engine.init now seeds ValidationInfo slot before tests)"
metrics:
  duration: "25m"
  completed_date: "2026-04-26"
  tasks_completed: 3
  tasks_total: 3
  files_created: 9
  files_modified: 4
---

# Phase 09 Plan 06: Engine.validate() — FEAT-03 Health Check Summary

Implement `Engine.validate(): List<String>` with `ValidationInfo` data class, `engineCurrentTimeMs()` expect/actual, listener-slot seeding in `init {}` + re-seeding in `clear()`, and 6 regression tests covering the installedAt false-positive guard.

## What Was Built

**Task 1 — ValidationInfo + engineCurrentTimeMs() (9 new files):**

`ValidationInfo` data class added to engine commonMain with `EMPTY` and `INVALID` companion constants (both equal to `ValidationInfo(emptyList())` per API convention). The class implements `Info` and is in the single-directory `com.smellouk.kamper` path matching `Engine.kt`.

`engineCurrentTimeMs()` internal expect function created in engine commonMain as a module-scoped time source, separate from `kamper.api.currentApiTimeMs()` because Kotlin `internal` does not cross module boundaries.

| Platform | Implementation |
|----------|----------------|
| android | `System.currentTimeMillis()` |
| jvm | `System.currentTimeMillis()` |
| ios | `clock_gettime(CLOCK_REALTIME)` via cinterop |
| macos | `clock_gettime(CLOCK_REALTIME)` via cinterop |
| tvos | `clock_gettime(CLOCK_REALTIME)` via cinterop |
| js | `kotlin.js.Date().getTime().toLong()` |
| wasmJs | `@JsFun("() => Date.now()")` |

**Task 2 — Engine.kt modifications + Performance visibility fix:**

Three additions to `Engine.kt`:
1. `init {}` block seeding `mapListeners[ValidationInfo::class] = mutableListOf()` before any module is installed (RESEARCH Pitfall 1 mitigation).
2. `clear()` re-seeds the slot after `mapListeners.clear()` (Assumption A5).
3. `validate(): List<String>` — uses `engineCurrentTimeMs()`, reads `performance.lastValidSampleAt` and `performance.installedAt` to compute elapsed. Problem string format: `"<SimpleName>: no valid samples for ${elapsed/MS_PER_SECOND}s (threshold: 10s)"`. Emits `ValidationInfo(problems)` to all registered listeners AND returns the list (D-12).

**Performance field visibility fix (cross-module access):** `lastValidSampleAt` and `installedAt` changed from `@Volatile internal var` to `@Volatile open var`. The `open` modifier is required so Mokkery can generate mocks; dropping `internal` allows Engine (a separate Kotlin module) to read the fields in `validate()`.

**Task 3 — EngineValidateTest + EngineTest updates:**

EngineValidateTest.kt replaced (Wave-0 TODO stub removed) with 6 tests:

| Test | Coverage |
|------|----------|
| A: validate should return empty when no modules installed | Base case |
| B: validate should report a problem when lastValidSampleAt is 0L AND installedAt is older than 10s | SC #3 happy path |
| C: validate should NOT report a problem when lastValidSampleAt is 0L AND installedAt is recent | Revision-1 regression guard |
| D: addInfoListener of ValidationInfo should work without installing any module | Pitfall 1 mitigation |
| E: validate should emit ValidationInfo to registered listeners | D-12 + SC #4 |
| F: clear should re-seed ValidationInfo listener slot so addInfoListener works after clear | Assumption A5 |

EngineTest.setup() updated to mirror Engine.init re-seeding. All `mapListeners.size` assertions updated (+1 throughout to account for the seeded ValidationInfo slot).

## installedAt Anchor — Revision-1 Bug-Fix Verification

**Test C** specifically guards the revision-1 bug. A 1-second-old module (installedAt = now - 1_000L) must NOT be reported as a 10-second problem. Without the `installedAt` anchor, any module with `lastValidSampleAt == 0L` would compute `elapsed = nowMs - 0L = current epoch ms >> 10_000ms`, producing a false positive immediately after installation. Test C PASSED — the fix works correctly.

## Layout Observation

The engine module's `commonMain` uses the unusual **single-directory** path `com.smellouk.kamper` (with literal dots in the directory name) matching the existing `Engine.kt` and `KamperConfig.kt`. `ValidationInfo.kt` and `EnginePlatformTime.kt` (commonMain) were placed in this same directory. Platform actuals use the conventional nested `com/smellouk/kamper/` path matching the existing `Kamper.kt` actuals in each platform source set.

## Test Results

- `EngineValidateTest` jvmTest: **6/6 passing**
- `EngineTest` jvmTest: **13/13 passing** (after updating mapListeners.size assertions)
- `KamperConfigBuilderTest` jvmTest: **1/1 passing**
- Total engine jvmTests: **20/20 passing**
- Cross-module: `:kamper:ui:android:assembleDebug` — BUILD SUCCESSFUL
- Cross-module: `:kamper:api:jvmTest` — **14/14 passing**

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Performance.lastValidSampleAt and installedAt inaccessible from Engine module**

- **Found during:** Task 2 (compile check)
- **Issue:** Fields declared `@Volatile internal var` in `kamper.api`. Kotlin `internal` does not cross Gradle module boundaries — `Engine.validate()` in `kamper.engine` cannot read them.
- **Fix:** Changed both fields to `@Volatile open var` — dropping `internal` enables cross-module access; adding `open` preserves Mokkery mockability (Mokkery cannot mock final members).
- **Files modified:** `kamper/api/src/commonMain/kotlin/com/smellouk/kamper/api/Performance.kt`
- **Commit:** 85dea99

**2. [Rule 1 - Bug] EngineTest mapListeners.size assertions broken by Engine.init {} seeding**

- **Found during:** Task 3 (jvmTest run)
- **Issue:** Engine.init {} seeds the ValidationInfo slot into mapListeners before any test runs. EngineTest.setup() called `mapListeners.clear()` then manually re-seeded — but tests still expected old sizes (0 when no module, 1 when module installed). After seeding, all sizes are +1.
- **Fix:** Updated 9 mapListeners.size assertions in EngineTest.kt (0→1, 1→2) to reflect the Engine.init invariant. Also updated EngineTest.setup() to call `mapListeners[ValidationInfo::class] = mutableListOf()` after clear (mirrors what Engine.init does).
- **Files modified:** `kamper/engine/src/commonTest/kotlin/com/smellouk/kamper/EngineTest.kt`
- **Commit:** 316d50d

**3. [Rule 2 - Missing] Detekt MagicNumber in new engine POSIX actuals**

- **Found during:** Post-Task-3 verification
- **Issue:** The new ios/macos/tvos `EnginePlatformTime.kt` files have the same POSIX formula (`1_000L`, `1_000_000L`) as the api module's `PlatformTime.kt` — both trigger Detekt MagicNumber. The engine's POSIX files added 1 new weighted issue (pre-existing: 121, after Task 1: 123). Also, `elapsed / 1000` in Engine.validate() added 1 magic number.
- **Fix:** Added `@Suppress("MagicNumber")` to the POSIX actual functions; extracted `MS_PER_SECOND = 1_000L` constant in Engine's companion object. Detekt weighted issue count is now 116 (below the 121 pre-existing baseline at commit 659ae47).
- **Files modified:** Engine.kt, ios/macos/tvos `EnginePlatformTime.kt`
- **Commit:** e256ae9

## Plan 07-04 Visual Checkpoint Acknowledgment

The plan frontmatter references `depends_on: [07-04]` for a visual checkpoint approval. This plan file is in phase 09 (renamed from 07 in the frontmatter — the plan was authored during phase 7 planning but executed in phase 9). The orchestrator enforced the dependency via wave ordering. No action required in this executor — the checkpoint was resolved upstream.

## Known Stubs

None — `ValidationInfo` carries real problem strings derived from actual `lastValidSampleAt`/`installedAt` timestamps. No placeholder data flows to UI from this plan alone.

## Threat Flags

None — `ValidationInfo.problems` contains only class simple names (public Kotlin reflection) and elapsed time in seconds (no PII). No new network endpoints, auth paths, or data input surfaces introduced.

## Self-Check: PASSED

Files verified to exist:
- kamper/engine/src/commonMain/kotlin/com.smellouk.kamper/ValidationInfo.kt: FOUND
- kamper/engine/src/commonMain/kotlin/com.smellouk.kamper/EnginePlatformTime.kt: FOUND
- kamper/engine/src/androidMain/kotlin/com/smellouk/kamper/EnginePlatformTime.kt: FOUND
- kamper/engine/src/jvmMain/kotlin/com/smellouk/kamper/EnginePlatformTime.kt: FOUND
- kamper/engine/src/iosMain/kotlin/com/smellouk/kamper/EnginePlatformTime.kt: FOUND
- kamper/engine/src/macosMain/kotlin/com/smellouk/kamper/EnginePlatformTime.kt: FOUND
- kamper/engine/src/tvosMain/kotlin/com/smellouk/kamper/EnginePlatformTime.kt: FOUND
- kamper/engine/src/jsMain/kotlin/com/smellouk/kamper/EnginePlatformTime.kt: FOUND
- kamper/engine/src/wasmJsMain/kotlin/com/smellouk/kamper/EnginePlatformTime.kt: FOUND
- kamper/engine/src/commonMain/kotlin/com.smellouk.kamper/Engine.kt: FOUND (modified)
- kamper/api/src/commonMain/kotlin/com/smellouk/kamper/api/Performance.kt: FOUND (modified)
- kamper/engine/src/commonTest/kotlin/com/smellouk/kamper/EngineValidateTest.kt: FOUND
- kamper/engine/src/commonTest/kotlin/com/smellouk/kamper/EngineTest.kt: FOUND (modified)

Commits verified:
- f010a31: feat(09-06): Task 1 — ValidationInfo + engineCurrentTimeMs() — FOUND
- 85dea99: feat(09-06): Task 2 — Engine init/clear seeding + validate() — FOUND
- 316d50d: test(09-06): Task 3 — EngineValidateTest stub replaced — FOUND
- e256ae9: fix(09-06): suppress MagicNumber + extract MS_PER_SECOND — FOUND
