---
phase: 09
plan: "05"
subsystem: api-watcher-plumbing
tags: [feat-03, watcher, performance, expect-actual, time-source, api, callback]
dependency_graph:
  requires:
    - 09-01 (Performance.lastValidSampleAt @Volatile field)
  provides:
    - currentApiTimeMs() internal expect/actual — 7 platform actuals in kamper/api module
    - IWatcher.startWatching optional onSampleDelivered callback parameter
    - Watcher invokes onSampleDelivered after listener dispatch on mainDispatcher
    - Performance.installedAt @Volatile field (first-start anchor for Engine.validate())
    - Performance.start() wires onSampleDelivered to update lastValidSampleAt = currentApiTimeMs()
  affects:
    - kamper/api (IWatcher, Watcher, Performance, PlatformTime)
    - kamper/modules/issues (IssuesWatcher signature updated)
tech_stack:
  added: []
  patterns:
    - expect/actual Platform Time (mirrors kamper/modules/issues PlatformUtils.kt pattern)
    - Callback threading via Watcher — onSampleDelivered fires on mainDispatcher after listener dispatch
    - First-start guard: installedAt written only when installedAt == 0L (stop()/start() cycles safe)
key_files:
  created:
    - kamper/api/src/commonMain/kotlin/com/smellouk/kamper/api/PlatformTime.kt
    - kamper/api/src/androidMain/kotlin/com/smellouk/kamper/api/PlatformTime.kt
    - kamper/api/src/jvmMain/kotlin/com/smellouk/kamper/api/PlatformTime.kt
    - kamper/api/src/iosMain/kotlin/com/smellouk/kamper/api/PlatformTime.kt
    - kamper/api/src/macosMain/kotlin/com/smellouk/kamper/api/PlatformTime.kt
    - kamper/api/src/tvosMain/kotlin/com/smellouk/kamper/api/PlatformTime.kt
    - kamper/api/src/jsMain/kotlin/com/smellouk/kamper/api/PlatformTime.kt
    - kamper/api/src/wasmJsMain/kotlin/com/smellouk/kamper/api/PlatformTime.kt
  modified:
    - kamper/api/src/commonMain/kotlin/com/smellouk/kamper/api/IWatcher.kt
    - kamper/api/src/commonMain/kotlin/com/smellouk/kamper/api/Watcher.kt
    - kamper/api/src/commonMain/kotlin/com/smellouk/kamper/api/Performance.kt
    - kamper/api/src/commonTest/kotlin/com/smellouk/kamper/api/WatcherTest.kt
    - kamper/api/src/commonTest/kotlin/com/smellouk/kamper/api/PerformanceTest.kt
    - kamper/modules/issues/src/commonMain/kotlin/com/smellouk/kamper/issues/IssuesWatcher.kt
decisions:
  - "api module uses separate iosMain/macosMain/tvosMain source sets (no shared appleMain) — matched issues module pattern with 3 separate PlatformTime.kt files"
  - "wasmJs actual uses @OptIn(ExperimentalWasmJsInterop) on @JsFun declaration to suppress compiler warning; issues module has same pattern without OptIn but warning is acceptable there too"
  - "onSampleDelivered callback placed inside withContext(mainDispatcher) block AFTER listeners.forEach — fires on main thread, fires only for non-null delivery, fires after all listeners"
  - "No custom IWatcher subclass besides Watcher.kt and IssuesWatcher.kt discovered in codebase grep; IssuesWatcher updated with new 3-arg signature"
metrics:
  duration: "7m"
  completed_date: "2026-04-26"
  tasks_completed: 3
  tasks_total: 3
  files_created: 8
  files_modified: 6
---

# Phase 09 Plan 05: Watcher → Performance Timestamp Plumbing Summary

Thread `currentApiTimeMs()` expect/actual through the Watcher callback API into `Performance.lastValidSampleAt` and add a new `installedAt` anchor field, completing the FEAT-03 prerequisite for `Engine.validate()`.

## What Was Built

Three coordinated changes to wire per-sample timestamps through the existing internal machinery:

**Task 1 — `currentApiTimeMs()` expect/actual (8 new files):**

| Platform | File | Implementation |
|----------|------|----------------|
| commonMain | PlatformTime.kt | `internal expect fun currentApiTimeMs(): Long` |
| android | PlatformTime.kt | `System.currentTimeMillis()` |
| jvm | PlatformTime.kt | `System.currentTimeMillis()` |
| ios | PlatformTime.kt | `clock_gettime(CLOCK_REALTIME)` via cinterop |
| macos | PlatformTime.kt | `clock_gettime(CLOCK_REALTIME)` via cinterop (identical to ios) |
| tvos | PlatformTime.kt | `clock_gettime(CLOCK_REALTIME)` via cinterop (identical to ios) |
| js | PlatformTime.kt | `kotlin.js.Date().getTime().toLong()` |
| wasmJs | PlatformTime.kt | `@JsFun("() => Date.now()")` with `@OptIn(ExperimentalWasmJsInterop)` |

The api module uses separate platform source sets (no shared `appleMain`) — three separate iosMain/macosMain/tvosMain files mirror the issues module's PlatformUtils.kt pattern.

**Task 2 — IWatcher/Watcher/Performance callback wiring (3 files modified + 2 auto-fixed):**

- `IWatcher.startWatching` gains optional `onSampleDelivered: (() -> Unit)? = null` — fully backwards-compatible
- `Watcher.startWatching` accepts and invokes the callback inside `withContext(mainDispatcher)`, after `listeners.forEach`, only when `info != null`
- `Performance` gains `@Volatile internal var installedAt: Long = 0L` — first-start anchor for `Engine.validate()`
- `Performance.start()` sets `installedAt = currentApiTimeMs()` on first call only (guard: `if (installedAt == 0L)`) and wires `onSampleDelivered = { lastValidSampleAt = currentApiTimeMs() }`

**Task 3 — WatcherTest new tests (1 file modified):**

WatcherTest gains 2 new tests (total: 5 tests):
- `startWatching should invoke onSampleDelivered once per non-null info delivery` — asserts `callbackInvocations == 2` after 2 loop iterations
- `startWatching should NOT invoke onSampleDelivered when infoRepository throws` — asserts `callbackInvocations == 0`

Uses a real counter lambda (`callbackInvocations++`) instead of Mokkery mock (Mokkery cannot natively mock `() -> Unit` lambdas). A `@BeforeTest resetCounter()` resets the counter for isolation.

## Apple Platform Note

The api module uses **separate** `iosMain`, `macosMain`, `tvosMain` source sets (confirmed from `ls kamper/api/src/`), not a shared `appleMain`. Three identical `PlatformTime.kt` files were created (one per platform) mirroring the issues module pattern. If a future plan introduces `appleMain`, the three files can be consolidated.

## Custom IWatcher Subclass Discovery

The grep `grep -rn "override fun startWatching" --include="*.kt" kamper/` found **one custom subclass** outside of `Watcher.kt`:

- **`kamper/modules/issues/src/commonMain/kotlin/com/smellouk/kamper/issues/IssuesWatcher.kt`** — updated as a Rule 3 fix (would not compile with new IWatcher interface). The `onSampleDelivered: (() -> Unit)?` parameter was added to the override signature; the parameter is not used inside `IssuesWatcher` (issue detection uses `config.onDroppedIssue` callback, not the Watcher callback). This is correct — `IssuesWatcher` accumulates and dispatches its own way.

## Backwards Compatibility

The default value `null` for `onSampleDelivered` is declared on the `IWatcher` interface. `Watcher.startWatching` accepts the parameter without repeating the default (Kotlin interface-default override rules). All existing callers that pass only `(intervalInMs, listeners)` compile and behave identically. All 3 original WatcherTest tests pass unchanged. All 14 api jvmTests pass.

## Confirmation: installedAt First-Start Guard

`Performance.start()` checks `if (installedAt == 0L) { installedAt = currentApiTimeMs() }` before calling `watcher.startWatching(...)`. This ensures:
- First `start()` call: `installedAt` is set to the current wall-clock millisecond.
- Subsequent `start()` calls after `stop()`: `installedAt` is NOT overwritten — the original installation time is preserved. `Engine.validate()` can correctly compute how long the module has been installed even across stop/start cycles.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] IssuesWatcher.startWatching signature mismatch**

- **Found during:** Task 2 (grep step in plan action)
- **Issue:** `IssuesWatcher` implements `IWatcher<IssueInfo>` and overrides `startWatching(intervalInMs, listeners)`. After adding the third `onSampleDelivered` parameter to `IWatcher`, `IssuesWatcher` would fail to compile.
- **Fix:** Added `onSampleDelivered: (() -> Unit)?` as the third parameter to `IssuesWatcher.startWatching`. The parameter is ignored inside the method (IssuesWatcher manages its own callback via `config.onDroppedIssue`).
- **Files modified:** `kamper/modules/issues/src/commonMain/kotlin/com/smellouk/kamper/issues/IssuesWatcher.kt`
- **Commit:** 1eb573f

**2. [Rule 1 - Bug] PerformanceTest verify calls use 2-arg startWatching**

- **Found during:** Task 2 jvmTest run
- **Issue:** `PerformanceTest` verifies `watcher.startWatching(any(), any())` — 2 arguments. After adding the third `onSampleDelivered` parameter, Mokkery's verify found no matching calls (the actual call had 3 args).
- **Fix:** Updated 3 verify calls in `PerformanceTest` from `startWatching(any(), any())` to `startWatching(any(), any(), any())`.
- **Files modified:** `kamper/api/src/commonTest/kotlin/com/smellouk/kamper/api/PerformanceTest.kt`
- **Commit:** 1eb573f

## Known Stubs

None — all timestamp fields are real values from `currentApiTimeMs()` actuals. No UI stub data.

## Threat Flags

None — `currentApiTimeMs()` is `internal` and not exposed by any public API. `lastValidSampleAt` and `installedAt` are `@Volatile internal` fields. No new trust boundaries or network endpoints introduced.

## Self-Check: PASSED

Files verified to exist:
- kamper/api/src/commonMain/kotlin/com/smellouk/kamper/api/PlatformTime.kt: FOUND
- kamper/api/src/androidMain/kotlin/com/smellouk/kamper/api/PlatformTime.kt: FOUND
- kamper/api/src/jvmMain/kotlin/com/smellouk/kamper/api/PlatformTime.kt: FOUND
- kamper/api/src/iosMain/kotlin/com/smellouk/kamper/api/PlatformTime.kt: FOUND
- kamper/api/src/macosMain/kotlin/com/smellouk/kamper/api/PlatformTime.kt: FOUND
- kamper/api/src/tvosMain/kotlin/com/smellouk/kamper/api/PlatformTime.kt: FOUND
- kamper/api/src/jsMain/kotlin/com/smellouk/kamper/api/PlatformTime.kt: FOUND
- kamper/api/src/wasmJsMain/kotlin/com/smellouk/kamper/api/PlatformTime.kt: FOUND
- kamper/api/src/commonMain/kotlin/com/smellouk/kamper/api/IWatcher.kt: FOUND
- kamper/api/src/commonMain/kotlin/com/smellouk/kamper/api/Watcher.kt: FOUND
- kamper/api/src/commonMain/kotlin/com/smellouk/kamper/api/Performance.kt: FOUND
- kamper/api/src/commonTest/kotlin/com/smellouk/kamper/api/WatcherTest.kt: FOUND
- kamper/api/src/commonTest/kotlin/com/smellouk/kamper/api/PerformanceTest.kt: FOUND
- kamper/modules/issues/src/commonMain/kotlin/com/smellouk/kamper/issues/IssuesWatcher.kt: FOUND

Commits verified:
- 1ee20f8: feat(09-05) Task 1 PlatformTime expect/actual — FOUND
- 1eb573f: feat(09-05) Task 2 IWatcher/Watcher/Performance callback wiring — FOUND
- 0aace46: test(09-05) Task 3 WatcherTest new tests — FOUND
