---
phase: 10-test-coverage
plan: 02
subsystem: engine
tags: [testing, mokkery, lifecycle, engine, TEST-03]
dependency_graph:
  requires: []
  provides: [EngineLifecycleTest]
  affects: [kamper/engine]
tech_stack:
  added: []
  patterns: [Mokkery mocks, fresh-mock-per-iteration, synchronous lifecycle testing]
key_files:
  created:
    - kamper/engine/src/commonTest/kotlin/com/smellouk/kamper/EngineLifecycleTest.kt
  modified: []
decisions:
  - "Added every { it.start() } returns Unit to createPerformance — Engine.start() calls performance.start() on each installed module, which requires the mock to stub this call (Rule 1 auto-fix)"
  - "D-06 (StandardTestDispatcher) formally inapplicable — Engine install/start/stop/uninstall/clear are all synchronous (Engine.kt lines 43-65); no internal coroutines exist in Engine itself"
  - "Fresh-mock-per-iteration design: each mock<Performance<...>>() call yields a distinct anonymous-subclass ::class identity, defeating Engine.install duplicate-type guard"
metrics:
  duration: "~5 minutes"
  completed_date: "2026-04-26"
  tasks_completed: 1
  tasks_total: 1
---

# Phase 10 Plan 02: Engine Lifecycle Tests (TEST-03) Summary

Engine lifecycle state machine tests ensuring zero dangling entries in `performanceList` or `mapListeners` across full Install → Enable → Disable → Uninstall cycles and 50-iteration rapid cycling.

## What Was Built

A single new test file `EngineLifecycleTest.kt` (82 lines) covering TEST-03 with two test methods and two factory functions mirroring the existing `EngineTest.kt` pattern exactly.

## Test Results

`./gradlew :kamper:engine:test` exits 0. All 22 tests pass across debug and release variants:

```
com.smellouk.kamper.EngineLifecycleTest

  ✔ full lifecycle Install Enable Disable Uninstall should complete without error
  ✔ rapid cycling 50 times should leave no dangling state

com.smellouk.kamper.EngineTest

  ✔ install should init performance and should not add it to list when module fail to init
  ✔ uninstall should allow reinstall after removal
  ✔ removeInfoListener should remove the specific listener while keeping the map entry
  ✔ stop should stop all performance list
  ✔ addInfoListener should add new listener to the current map listeners when performance module is installed
  ✔ uninstall should do nothing when module is not installed
  ✔ install should init performance and should not add it to list when config is disabled
  ✔ removeInfoListener should do nothing when listener was not added
  ✔ uninstall should stop and remove the performance and its listeners
  ✔ install should initialize performance and add it to list of performances
  ✔ clear should clear all performance list
  ✔ addInfoListener should not add listener when target performance module is not installed
  ✔ start should start all performance list

com.smellouk.kamper.EngineValidateTest

  ✔ validate should report a problem when lastValidSampleAt is 0L AND installedAt is older than 10s
  ✔ validate should return empty when no modules installed
  ✔ validate should emit ValidationInfo to registered listeners
  ✔ addInfoListener of ValidationInfo should work without installing any module
  ✔ clear should re-seed ValidationInfo listener slot so addInfoListener works after clear
  ✔ validate should NOT report a problem when lastValidSampleAt is 0L AND installedAt is recent

com.smellouk.kamper.KamperConfigBuilderTest

  ✔ build should build correct config

22 passing (288ms)
```

## Fresh-Mock-Per-Iteration Design

The `repeat(50)` body calls `createPerformance(isInitialized = true)` and `createPerformanceModule(...)` inside the loop — not hoisted outside. This is the load-bearing design choice:

- Each `mock<Performance<Config, IWatcher<Info>, Info>>()` call produces a new anonymous subclass, giving each mock a distinct `::class` identity.
- `Engine.install` guards against duplicate types: `performanceList.any { it::class == performance::class }`. Without fresh mocks per iteration, iterations 2..50 would silently no-op.
- Verified: `awk '/repeat\(50\)/,/^    }/' EngineLifecycleTest.kt | grep -c 'createPerformance'` returns 2 (both factory calls inside the loop body).

## D-06 Applicability

StandardTestDispatcher + advanceTimeBy()/runCurrent() are formally inapplicable to TEST-03:

- Engine.kt lines 43-65 (`start()`, `stop()`, `clear()`) are fully synchronous — no internal coroutines.
- Engine.kt lines 89-111 (`install()`) and lines 80-86 (`uninstall()`) are fully synchronous.
- Coroutine usage lives inside concrete Performance implementations (e.g., CpuPerformance), which are mocked here via Mokkery — no async path exists.
- D-06's underlying intent (no Thread.sleep, no real-time delays, fully deterministic) is satisfied without invoking StandardTestDispatcher.

The test file contains no `Thread.sleep`, `runBlocking`, `delay(`, `runTest`, or `import kotlinx.coroutines` — confirmed by grep.

## Production Code

Zero production code modified. `git diff --stat kamper/engine/src/commonMain/` reports no changes.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Missing start() stub in createPerformance factory**

- **Found during:** Task 1 — first test run
- **Issue:** `Engine.start()` iterates performanceList and calls `performance.start()` on each entry. The initial `createPerformance` factory only stubbed `initialize` and `stop`, causing `CallNotMockedException: Call Performance(1).start() not mocked!`
- **Fix:** Added `every { it.start() } returns Unit` to the `createPerformance` factory function
- **Files modified:** `kamper/engine/src/commonTest/kotlin/com/smellouk/kamper/EngineLifecycleTest.kt`
- **Commit:** 8d79ecd (same task commit)

## Known Stubs

None.

## Threat Flags

None — plan authors a single test-only file with no new production surface.

## Self-Check: PASSED

- File exists: `kamper/engine/src/commonTest/kotlin/com/smellouk/kamper/EngineLifecycleTest.kt` - FOUND
- Task commit 8d79ecd - FOUND
- 22 passing tests - CONFIRMED
- No production code modified - CONFIRMED
