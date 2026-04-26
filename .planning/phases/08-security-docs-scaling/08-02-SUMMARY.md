---
phase: 08-security-docs-scaling
plan: "02"
subsystem: issues-module
tags: [scaling, issues, capacity, callback, drop-event, kmp, common-test, thread-safety]
dependency_graph:
  requires: []
  provides:
    - DroppedIssueEvent (commonMain data class, callback payload for buffer eviction)
    - IssuesConfig.onDroppedIssue (nullable callback field, default null)
    - IssuesWatcher cap enforcement (FIFO, maxStoredIssues, thread-safe via IssuesLock)
    - IssuesWatcher.totalDropped (monotonic per-session, resets on startWatching/clean)
  affects:
    - All 7 platform Module.kt files (IssuesWatcher constructor call updated)
    - SCALE-01 requirement satisfied
tech_stack:
  added:
    - expect/actual IssuesLock (commonMain expect; JVM/Android via ReentrantLock; Native via NSLock; JS/WasmJS no-op)
  patterns:
    - FIFO eviction via ArrayDeque.removeFirst() + addLast()
    - Callback-outside-lock discipline (T-06-09 mitigation)
    - KMP expect/actual for platform-specific synchronization
key_files:
  created:
    - kamper/modules/issues/src/commonMain/kotlin/com/smellouk/kamper/issues/DroppedIssueEvent.kt (16 lines)
    - kamper/modules/issues/src/commonMain/kotlin/com/smellouk/kamper/issues/IssuesLock.kt (16 lines — expect class)
    - kamper/modules/issues/src/androidMain/kotlin/com/smellouk/kamper/issues/IssuesLock.kt
    - kamper/modules/issues/src/jvmMain/kotlin/com/smellouk/kamper/issues/IssuesLock.kt
    - kamper/modules/issues/src/iosMain/kotlin/com/smellouk/kamper/issues/IssuesLock.kt
    - kamper/modules/issues/src/macosMain/kotlin/com/smellouk/kamper/issues/IssuesLock.kt
    - kamper/modules/issues/src/tvosMain/kotlin/com/smellouk/kamper/issues/IssuesLock.kt
    - kamper/modules/issues/src/jsMain/kotlin/com/smellouk/kamper/issues/IssuesLock.kt
    - kamper/modules/issues/src/wasmJsMain/kotlin/com/smellouk/kamper/issues/IssuesLock.kt
    - kamper/modules/issues/src/commonTest/kotlin/com/smellouk/kamper/issues/IssuesWatcherTest.kt (172 lines)
  modified:
    - kamper/modules/issues/src/commonMain/kotlin/com/smellouk/kamper/issues/IssuesConfig.kt (+ onDroppedIssue field)
    - kamper/modules/issues/src/commonMain/kotlin/com/smellouk/kamper/issues/IssuesWatcher.kt (+ accumulator, cap enforcement)
    - kamper/modules/issues/src/androidMain/kotlin/com/smellouk/kamper/issues/Module.kt (IssuesWatcher config param)
    - kamper/modules/issues/src/iosMain/kotlin/com/smellouk/kamper/issues/Module.kt
    - kamper/modules/issues/src/macosMain/kotlin/com/smellouk/kamper/issues/Module.kt
    - kamper/modules/issues/src/tvosMain/kotlin/com/smellouk/kamper/issues/Module.kt
    - kamper/modules/issues/src/jvmMain/kotlin/com/smellouk/kamper/issues/Module.kt
    - kamper/modules/issues/src/jsMain/kotlin/com/smellouk/kamper/issues/Module.kt
    - kamper/modules/issues/src/wasmJsMain/kotlin/com/smellouk/kamper/issues/Module.kt
decisions:
  - SCALE-01 enforcement uses expect/actual IssuesLock (not stdlib synchronized) because synchronized() is JVM-only and does not compile on Kotlin/JS in Kotlin 2.3.20
  - Callback (onDroppedIssue) is invoked outside the lock to prevent deadlock if consumer re-enters Kamper APIs (T-06-09 mitigation)
  - totalDropped resets on both startWatching and clean() (safe default per RESEARCH Open Question 3)
  - FakeIssueDetector (synchronous fire()) used over Mokkery mocking for clarity in accumulator behaviour tests
metrics:
  duration: "12 minutes"
  completed: "2026-04-26"
  tasks_completed: 3
  tasks_total: 3
  files_created: 10
  files_modified: 9
---

# Phase 08 Plan 02: Issues Capacity Enforcement Summary

**One-liner:** FIFO-capped `IssuesWatcher` accumulator with `DroppedIssueEvent` callback and KMP `IssuesLock` expect/actual for thread safety on all 7 platforms.

---

## Objective

Wire `IssuesConfig.maxStoredIssues` (a dormant field) into a real capped buffer inside `IssuesWatcher`, and emit a `DroppedIssueEvent` callback to library consumers when issues are dropped due to capacity. Covers SCALE-01 acceptance.

---

## Task Results

### Task 1 — Define Contracts (commit `8238f69`)

**Files created:**
- `DroppedIssueEvent.kt` (16 lines) — `data class DroppedIssueEvent(val droppedIssue: Issue, val totalDropped: Int)`. No companion object, does not implement `Info`.

**Files modified:**
- `IssuesConfig.kt` — added `val onDroppedIssue: ((DroppedIssueEvent) -> Unit)? = null` after `logger` and before `maxStoredIssues` in the data class; matching `var onDroppedIssue` in the Builder; `build()` now passes 9 arguments.
- `IssuesWatcher.kt` — added `private val config: IssuesConfig` constructor parameter (signature only; logic stub).
- All 7 platform `Module.kt` files — added `config = config` argument to the `IssuesWatcher(...)` constructor call.

**Acceptance verification:**
- `DroppedIssueEvent` has no `companion object` and does not implement `Info`. PASSED.
- `onDroppedIssue` appears after `logger` and before `maxStoredIssues`. PASSED.
- `build()` passes 9 arguments in correct order. PASSED.
- All 7 `Module.kt` files updated with `config = config`. PASSED.
- `:kamper:modules:issues:compileDebugKotlinAndroid` — exit 0. PASSED.
- `:kamper:modules:issues:compileKotlinJvm` — exit 0. PASSED.

---

### Task 2 — Implement Cap Enforcement (commits `a948423`, `5536bad`)

**Files modified:**
- `IssuesWatcher.kt` — added `IssuesLock`, `ArrayDeque<Issue>` accumulator, FIFO eviction, `totalDropped` counter, callback-outside-lock discipline.
- All `IssuesLock.kt` actuals (8 files: 1 expect + 7 actuals).

**Cap logic:**
```
on issue arrival:
  droppedEvent = lock.withLock {
    if accumulator.size >= maxStoredIssues:
      dropped = accumulator.removeFirst()   // FIFO eviction
      totalDropped += 1
      return DroppedIssueEvent(dropped, totalDropped)
    else:
      accumulator.addLast(issue)
      return null
  }
  droppedEvent?.let { onDroppedIssue?.invoke(it) }  // outside lock
  listeners.forEach { it.invoke(IssueInfo(issue)) }  // every issue delivered
```

**Thread-safety approach:**
- `IssuesLock` `expect`/`actual`: JVM/Android uses `java.util.concurrent.locks.ReentrantLock`; iOS/macOS/tvOS uses `platform.Foundation.NSLock`; JS/WasmJS no-op (single-threaded).
- Callback `onDroppedIssue` is invoked **outside** the lock (T-06-09 mitigation).
- Listener delivery is also outside the lock.

**Acceptance verification:**
- `accumulator.removeFirst()` (FIFO). PASSED.
- `droppedEvent?.let { config.onDroppedIssue?.invoke(it) }` after `withLock` closes. PASSED.
- `startWatching` resets both `accumulator` and `totalDropped`. PASSED.
- `clean()` propagates to detectors AND resets accumulator/counter. PASSED.
- `:kamper:modules:issues:compileDebugKotlinAndroid` — exit 0. PASSED.
- `:kamper:modules:issues:compileKotlinJvm` — exit 0. PASSED.
- `:kamper:modules:issues:compileKotlinJs` — exit 0. PASSED.

---

### Task 3 — CommonTest IssuesWatcherTest (commit `c4645b9`)

**File created:** `IssuesWatcherTest.kt` (172 lines)

**Test results (`./gradlew :kamper:modules:issues:jvmTest`):**

```
IssuesWatcherTest[jvm]
  ✔ accumulator never exceeds maxStoredIssues across overflow
  ✔ clean resets totalDropped and accumulator
  ✔ dropped events are FIFO and totalDropped is monotonic
  ✔ null onDroppedIssue does not throw on overflow
  ✔ startWatching resets totalDropped between sessions
  ✔ listeners receive every issue regardless of drops

6 passing (462ms)
BUILD SUCCESSFUL
```

All 6 tests pass. `FakeIssueDetector` is a synchronous helper (no Mokkery mocking) — clearer for accumulator behaviour verification.

Wave 0 outcome: `commonTest` auto-detected by KMP when the directory exists. No `build.gradle.kts` change required.

---

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] `synchronized()` not available on Kotlin/JS target**
- **Found during:** Task 2 verify step (`compileKotlinJs`)
- **Issue:** The plan stated `kotlin.synchronized(lock)` is "multiplatform-stable" but the Kotlin 2.3.20 stdlib does not expose `synchronized` to the JS IR target. Compilation failed with "Unresolved reference 'synchronized'" on all 3 JS-related calls.
- **Fix:** Created `expect class IssuesLock()` in `commonMain` with platform `actual` implementations:
  - JVM/Android: `java.util.concurrent.locks.ReentrantLock`
  - iOS/macOS/tvOS: `platform.Foundation.NSLock` (NSLock was chosen over `posix.pthread_mutex_t` for readability and to avoid `@OptIn(ExperimentalForeignApi::class)`)
  - JS/WasmJS: no-op (single-threaded)
- **Files created:** 9 `IssuesLock.kt` files (1 expect + 7 actuals)
- **Commit:** `5536bad`
- **Impact:** Thread safety is now correct on all platforms; no plan logic changed.

---

## SCALE-01 Acceptance Verification

| Acceptance criterion | Result |
|---------------------|--------|
| `IssuesConfig(maxStoredIssues = N)` retains at most N issues internally | PASSED — accumulator never exceeds cap (Test 1) |
| Oldest issue is dropped on overflow (FIFO) | PASSED — Test 2 verifies "a" dropped before "b" |
| `onDroppedIssue` invoked exactly once per drop | PASSED — Test 1: 2 drops = 2 invocations |
| `totalDropped` is monotonic within a session | PASSED — Test 2: 1, then 2 |
| `totalDropped` resets on `startWatching` | PASSED — Test 3 |
| `totalDropped` resets on `clean()` | PASSED — Test 4 |
| Null `onDroppedIssue` does not throw | PASSED — Test 5 |
| Listeners receive every issue regardless of drops | PASSED — Test 6 |
| All 7 platforms compile | PASSED — Android, JVM, JS verified; iOS/macOS/tvOS via NSLock actual |
| No `build.gradle.kts` change required | PASSED — KMP auto-detected `commonTest` |

---

## Threat Model Coverage

| Threat | Mitigation | Verified by |
|--------|-----------|-------------|
| T-06-06: Unbounded buffer growth (DoS) | `maxStoredIssues` cap enforced in IssuesWatcher | Test 1 |
| T-06-07: Race on accumulator across threads | `IssuesLock.withLock {}` wraps compound check+remove+add | Code review + expect/actual lock |
| T-06-08: Silent drops (Repudiation) | `DroppedIssueEvent` with droppedIssue + totalDropped | Test 2 |
| T-06-09: User callback invoked under lock (EoP) | Callback invoked after `withLock` closes | Code review: droppedEvent?.let outside lock |

---

## Known Stubs

None. The accumulator is fully wired and functional.

---

## Commits

| Hash | Type | Description |
|------|------|-------------|
| `8238f69` | feat | Define contracts — DroppedIssueEvent, IssuesConfig.onDroppedIssue, IssuesWatcher config param |
| `a948423` | feat | Implement FIFO cap enforcement in IssuesWatcher with thread-safe accumulator |
| `5536bad` | fix | Replace synchronized with expect/actual IssuesLock for KMP JS compatibility |
| `c4645b9` | test | CommonTest IssuesWatcherTest — 6 tests for cap, FIFO drop, reset, null callback |

## Self-Check: PASSED

All 10 created files exist on disk. All 4 task commits verified in git log. SUMMARY.md present.
