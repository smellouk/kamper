---
phase: 24
plan: 03
subsystem: engine
tags: [engine, kmp, expect-actual, lock, timestamp, perfetto]
dependency_graph:
  requires: []
  provides:
    - EngineEventLock (expect class, all 8 KMP targets)
    - engineCurrentTimeNs (expect fun, all 8 KMP targets)
  affects:
    - libs/engine (new primitives)
    - plan 04 (consumers of EngineEventLock and engineCurrentTimeNs)
tech_stack:
  added:
    - ReentrantLock (JVM/Android locking)
    - NSLock (Apple platform locking)
    - SystemClock.elapsedRealtimeNanos() (CLOCK_BOOTTIME Android nanos)
    - System.nanoTime() (JVM monotonic nanos)
    - clock_gettime(CLOCK_REALTIME) (Apple nanos via cinterop)
  patterns:
    - expect/actual class for KMP mutual exclusion (mirrors IssuesLock)
    - expect/actual fun for platform timestamps (appends to EnginePlatformTime)
key_files:
  created:
    - libs/engine/src/commonMain/kotlin/com.smellouk.kamper/EngineEventLock.kt
    - libs/engine/src/androidMain/kotlin/com/smellouk/kamper/EngineEventLock.kt
    - libs/engine/src/jvmMain/kotlin/com/smellouk/kamper/EngineEventLock.kt
    - libs/engine/src/iosMain/kotlin/com/smellouk/kamper/EngineEventLock.kt
    - libs/engine/src/macosMain/kotlin/com/smellouk/kamper/EngineEventLock.kt
    - libs/engine/src/tvosMain/kotlin/com/smellouk/kamper/EngineEventLock.kt
    - libs/engine/src/jsMain/kotlin/com/smellouk/kamper/EngineEventLock.kt
    - libs/engine/src/wasmJsMain/kotlin/com/smellouk/kamper/EngineEventLock.kt
  modified:
    - libs/engine/src/commonMain/kotlin/com.smellouk.kamper/EnginePlatformTime.kt
    - libs/engine/src/androidMain/kotlin/com/smellouk/kamper/EnginePlatformTime.kt
    - libs/engine/src/jvmMain/kotlin/com/smellouk/kamper/EnginePlatformTime.kt
    - libs/engine/src/iosMain/kotlin/com/smellouk/kamper/EnginePlatformTime.kt
    - libs/engine/src/macosMain/kotlin/com/smellouk/kamper/EnginePlatformTime.kt
    - libs/engine/src/tvosMain/kotlin/com/smellouk/kamper/EnginePlatformTime.kt
    - libs/engine/src/jsMain/kotlin/com/smellouk/kamper/EnginePlatformTime.kt
    - libs/engine/src/wasmJsMain/kotlin/com/smellouk/kamper/EnginePlatformTime.kt
decisions:
  - D-08: EngineEventLock provides thread-safe mutual exclusion for Engine.eventBuffer without coroutines
  - D-11: IssuesLock structural pattern reused for EngineEventLock; JS/WasmJS are no-op (single-threaded)
  - Android nanos uses SystemClock.elapsedRealtimeNanos() (CLOCK_BOOTTIME) to align with RecordingManager.nowNs()
metrics:
  duration: ~20 minutes
  completed: 2026-05-02
  tasks_completed: 2
  tasks_total: 2
  files_created: 8
  files_modified: 8
---

# Phase 24 Plan 03: Engine Platform Primitives (EngineEventLock + engineCurrentTimeNs) Summary

EngineEventLock expect/actual (8 files, NSLock/ReentrantLock/no-op) and engineCurrentTimeNs() expect/actual (8 files, CLOCK_BOOTTIME on Android) added as engine prerequisites for Plan 04's event buffer API.

---

## Tasks Completed

| Task | Description | Commit |
|------|-------------|--------|
| 1 | EngineEventLock expect + 7 platform actuals | 77e7823 |
| 2 | engineCurrentTimeNs() expect + 7 platform actuals | 5ade508 |

---

## What Was Built

### Task 1: EngineEventLock (8 files)

A platform-specific mutual exclusion lock for `Engine.eventBuffer`, mirroring the `IssuesLock` pattern exactly:

- **commonMain** (`com.smellouk.kamper`): `internal expect class EngineEventLock()` with `withLock<T>`
- **androidMain / jvmMain**: `ReentrantLock`-backed, using a private local extension function (matches IssuesLock's exact pattern — NOT `kotlin.concurrent.withLock`)
- **iosMain / macosMain / tvosMain**: `NSLock`-backed with explicit `lock()`/`unlock()` in `try/finally`
- **jsMain / wasmJsMain**: No-op (single-threaded environments — no synchronization possible or needed)

Note: The commonMain source set uses the dot-named directory `com.smellouk.kamper` (not slash-separated), consistent with all other engine commonMain files.

### Task 2: engineCurrentTimeNs() (8 files, appended to existing EnginePlatformTime.kt)

Nanosecond monotonic timestamps appended to each existing `EnginePlatformTime.kt` without removing `engineCurrentTimeMs()`:

- **commonMain**: `@PublishedApi internal expect fun engineCurrentTimeNs(): Long`
- **androidMain**: `SystemClock.elapsedRealtimeNanos()` — CLOCK_BOOTTIME, identical clock source as `RecordingManager.nowNs()` for Perfetto trace alignment
- **jvmMain**: `System.nanoTime()` (monotonic)
- **iosMain / macosMain / tvosMain**: `clock_gettime(CLOCK_REALTIME)` returning `tv_sec * 1_000_000_000L + tv_nsec` (consistent with existing `engineCurrentTimeMs` clock source)
- **jsMain**: `Date().getTime().toLong() * 1_000_000L` (ms-precision scaled to ns)
- **wasmJsMain**: `jsDateNow().toLong() * 1_000_000L` (existing external helper, ms-precision)

---

## Decisions Made

**D-08 / D-11 fulfilled:** `EngineEventLock` provides the KMP-safe synchronization primitive required by `Engine.eventBuffer` without adding a coroutines dependency to the engine module.

**CLOCK_BOOTTIME alignment (D-04 threat mitigation):** Android's `engineCurrentTimeNs()` uses `SystemClock.elapsedRealtimeNanos()` — the same clock as `RecordingManager.nowNs()` — ensuring `EventRecord.timestampNs` values align with Perfetto counter-track timestamps in exported traces.

**IssuesLock pattern match:** The androidMain/jvmMain implementation uses a private local `ReentrantLock.withLock()` extension (not `kotlin.concurrent.withLock` from stdlib) to exactly mirror the IssuesLock implementation, maintaining codebase consistency.

---

## Verification

- `./gradlew :libs:engine:build` — BUILD SUCCESSFUL (all 8 KMP targets, no missing-actual errors)
- `./gradlew :libs:engine:test` — BUILD SUCCESSFUL (no regressions)
- Detekt: no issues in the 16 files created/modified by this plan (pre-existing thermal module failures are out-of-scope)

---

## Deviations from Plan

**1. [Rule 3 - Blocking Issue] kotlinWasmStoreYarnLock task failure**

- **Found during:** Task 1 verification build
- **Issue:** Gradle's `kotlinWasmStoreYarnLock` task ran and modified the tracked `kotlin-js-store/wasm/yarn.lock` file, causing subsequent builds to fail with "Lock file was changed"
- **Fix:** Ran `./gradlew :kotlinWasmUpgradeYarnLock` to sync, then restored `yarn.lock` via `git checkout -- kotlin-js-store/wasm/yarn.lock` (the task deleted the file but the file is tracked in git). Used `-x :kotlinWasmStoreYarnLock` flag on subsequent verification builds.
- **Files modified:** None (yarn.lock left as tracked state)

**2. Pre-existing detekt failures (out-of-scope)**

- `libs/modules/thermal/src/jvmMain/.../ThermalInfoRepositoryImpl.kt` — `NoMultipleSpaces` and `MagicNumber` warnings exist in the main repo and in parallel worktrees. These are not caused by this plan and are logged to deferred items.

---

## Self-Check: PASSED

- [x] `libs/engine/src/commonMain/kotlin/com.smellouk.kamper/EngineEventLock.kt` — FOUND
- [x] `libs/engine/src/androidMain/kotlin/com/smellouk/kamper/EngineEventLock.kt` — FOUND
- [x] `libs/engine/src/jvmMain/kotlin/com/smellouk/kamper/EngineEventLock.kt` — FOUND
- [x] `libs/engine/src/iosMain/kotlin/com/smellouk/kamper/EngineEventLock.kt` — FOUND
- [x] `libs/engine/src/macosMain/kotlin/com/smellouk/kamper/EngineEventLock.kt` — FOUND
- [x] `libs/engine/src/tvosMain/kotlin/com/smellouk/kamper/EngineEventLock.kt` — FOUND
- [x] `libs/engine/src/jsMain/kotlin/com/smellouk/kamper/EngineEventLock.kt` — FOUND
- [x] `libs/engine/src/wasmJsMain/kotlin/com/smellouk/kamper/EngineEventLock.kt` — FOUND
- [x] All 8 EnginePlatformTime.kt files contain both `engineCurrentTimeMs` and `engineCurrentTimeNs` — VERIFIED
- [x] Commit 77e7823 exists — VERIFIED
- [x] Commit 5ade508 exists — VERIFIED
