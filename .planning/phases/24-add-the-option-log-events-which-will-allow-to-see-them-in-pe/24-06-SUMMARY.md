---
phase: 24
plan: "06"
subsystem: firebase-integration
tags: [firebase, crashlytics, events, expect-actual, kmp]
dependency_graph:
  requires: [24-02]
  provides: [firebase-event-forwarding]
  affects: [libs/integrations/firebase]
tech_stack:
  added: []
  patterns: [expect-actual, logSink-test-seam]
key_files:
  created:
    - libs/integrations/firebase/src/commonMain/kotlin/com/smellouk/kamper/firebase/RecordLog.kt
    - libs/integrations/firebase/src/androidMain/kotlin/com/smellouk/kamper/firebase/RecordLog.kt
    - libs/integrations/firebase/src/iosMain/kotlin/com/smellouk/kamper/firebase/RecordLog.kt
    - libs/integrations/firebase/src/jvmMain/kotlin/com/smellouk/kamper/firebase/RecordLog.kt
    - libs/integrations/firebase/src/macosMain/kotlin/com/smellouk/kamper/firebase/RecordLog.kt
    - libs/integrations/firebase/src/jsMain/kotlin/com/smellouk/kamper/firebase/RecordLog.kt
    - libs/integrations/firebase/src/wasmJsMain/kotlin/com/smellouk/kamper/firebase/RecordLog.kt
  modified:
    - libs/integrations/firebase/src/commonMain/kotlin/com/smellouk/kamper/firebase/FirebaseConfig.kt
    - libs/integrations/firebase/src/commonMain/kotlin/com/smellouk/kamper/firebase/FirebaseIntegrationModule.kt
    - libs/integrations/firebase/src/commonTest/kotlin/com/smellouk/kamper/firebase/FirebaseIntegrationModuleTest.kt
decisions:
  - "Used logSink: (String) -> Unit constructor parameter on FirebaseIntegrationModule (default ::recordLog) as a test seam to capture messages in tests — consistent with pattern from plan description"
  - "Made DEFAULT_FORWARD_EVENTS a const val (not val) to satisfy Detekt MayBeConst rule"
  - "Introduced explicit toString() override on FirebaseConfig for consistency with SentryConfig pattern"
metrics:
  duration: "~12 minutes"
  completed: "2026-05-02"
  tasks_completed: 3
  files_created: 7
  files_modified: 3
---

# Phase 24 Plan 06: Firebase Crashlytics Event Forwarding Summary

Firebase Crashlytics integration now forwards custom `UserEventInfo` events as Crashlytics log breadcrumbs via a new `RecordLog` expect/actual (distinct from `RecordNonFatal` — events are not exceptions, per Pitfall 6).

## What Was Built

### Task 1: FirebaseConfig.forwardEvents (commit a82dffe)

- Added `val forwardEvents: Boolean` to `FirebaseConfig` constructor
- Added `const val DEFAULT_FORWARD_EVENTS: Boolean = true` in companion (D-27 default — opt-in by default)
- Added `var forwardEvents: Boolean = DEFAULT_FORWARD_EVENTS` to Builder
- Updated `build()` to pass the field
- Added explicit `toString()` for log safety consistency

### Task 2: RecordLog expect/actual across 7 source sets (commit 9c65bcc)

7 new files mirroring the `RecordNonFatal` source-set list (no tvosMain — Firebase has no tvOS SDK):

| Source set | Implementation |
|------------|----------------|
| commonMain | `internal expect fun recordLog(message: String)` |
| androidMain | `FirebaseCrashlytics.getInstance().log(message)` wrapped in try/catch |
| iosMain | `Crashlytics.crashlytics().log(message)` wrapped in try/catch |
| jvmMain | No-op |
| macosMain | No-op |
| jsMain | No-op |
| wasmJsMain | No-op |

**Pitfall 6 confirmed:** `RecordLog` calls `.log()` (the Crashlytics breadcrumb buffer), NOT `.recordException()` / `.recordError()`. These are entirely separate code paths from `RecordNonFatal`.

### Task 3: "event" branch in FirebaseIntegrationModule + 4 new tests (commit 370fcd6)

The `onEvent()` `when (event.moduleName)` block now handles `"event"`:
- Guards `if (!config.forwardEvents) return` (D-27)
- Casts `event.info as? UserEventInfo ?: return` (T-24-E-02)
- Instant events (`durationMs == null`): `"kamper.event: <name>"`
- Duration events (`durationMs != null`): `"kamper.event: <name> (<ms> ms)"`
- Calls `logSink(message)` (default `::recordLog`)

4 new tests added to `FirebaseIntegrationModuleTest` using a `logSink` seam:
1. `event_branch_skips_when_forwardEvents_is_false` — guard verified
2. `event_branch_skips_when_info_is_not_UserEventInfo` — cast guard verified
3. `event_branch_emits_log_for_instant_event` — message format `"kamper.event: purchase"`
4. `event_branch_emits_log_with_duration_suffix_for_duration_event` — format `"kamper.event: video (1024 ms)"`

All 14 jvmTests pass (6 pre-existing + 4 new event tests + 4 FirebaseConfigBuilderTest).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical Feature] const val for DEFAULT_FORWARD_EVENTS**
- **Found during:** Task 1 — Detekt `MayBeConst` rule flagged `DEFAULT_FORWARD_EVENTS`
- **Fix:** Changed from `val` to `const val`; `DEFAULT_FORWARD_ISSUES` was already in the detekt baseline, but adding a new non-const caused a build failure
- **Files modified:** `FirebaseConfig.kt`

**2. [Rule 2 - Test Seam] logSink constructor parameter**
- **Found during:** Task 3 — existing tests use no-exception assertions (JVM no-op actuals); message format tests required observable output
- **Fix:** Added `logSink: (String) -> Unit = ::recordLog` constructor parameter; `@PublishedApi internal` visibility allows test access; production default is `::recordLog`

### Pre-existing Issues (Deferred, Out of Scope)

- `libs/modules/thermal/src/jvmMain/.../ThermalInfoRepositoryImpl.kt` — `NoMultipleSpaces` detekt violations (pre-existing before this plan)
- `libs/modules/thermal/src/iosMain/...` and `tvosMain/...` — `MagicNumber` detekt violations (pre-existing)
- `compileIosMainKotlinMetadata` fails in this environment — CocoaPods not installed in worktree; same failure existed before any changes in this plan

## Threat Surface Scan

No new network endpoints, auth paths, or schema changes introduced. All new surface is consistent with the threat model declared in the plan:
- T-24-E-02 (Tampering via non-UserEventInfo): mitigated by `as? UserEventInfo ?: return`
- T-24-E-03 (Wrong API call): mitigated — `RecordLog` calls `.log()` not `.recordException()`
- T-24-E-04 (DoS via Crashlytics SDK throw): mitigated — each platform actual wraps in `try/catch`

## Known Stubs

None.

## Self-Check: PASSED

All 10 task files found on disk. All 3 task commits found in git log (a82dffe, 9c65bcc, 370fcd6). 14 jvmTests passing.
