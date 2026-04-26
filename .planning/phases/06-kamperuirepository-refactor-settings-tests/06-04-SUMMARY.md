---
phase: 06-kamperuirepository-refactor-settings-tests
plan: 04
subsystem: apple-platform
tags: [kotlin-multiplatform, apple, ios, nsuserdefaults, recording-stub, module-lifecycle]

# Dependency graph
requires:
  - phase: 06-kamperuirepository-refactor-settings-tests
    plan: 02
    provides: "PreferencesStore interface in commonMain"
provides:
  - "ApplePreferencesStore: NSUserDefaults-backed PreferencesStore implementation in appleMain"
  - "Apple RecordingManager: no-op stub exposing isRecording and recordingSampleCount StateFlows"
  - "Apple ModuleLifecycleManager: Engine + 8 module install/uninstall pairs using FpsModule (not Choreographer)"
affects:
  - 06-05-PLAN

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Apple PreferencesStore: objectForKey null-check before typed read (boolForKey/integerForKey/floatForKey/stringForKey)"
    - "Apple putInt uses setInteger(value.toLong(), key) for NativeLong mapping"
    - "Apple RecordingManager stub: MutableStateFlow(false).asStateFlow() for isRecording, all methods no-ops"
    - "Apple ModuleLifecycleManager: FpsModule object instead of Choreographer for FPS"
    - "PREF_ISSUES key 'kamper_issues_list' matches existing Apple NSUserDefaults data on disk"

key-files:
  created:
    - "kamper/ui/android/src/appleMain/kotlin/com/smellouk/kamper/ui/ApplePreferencesStore.kt"
    - "kamper/ui/android/src/appleMain/kotlin/com/smellouk/kamper/ui/RecordingManager.kt"
    - "kamper/ui/android/src/appleMain/kotlin/com/smellouk/kamper/ui/ModuleLifecycleManager.kt"
  modified: []

key-decisions:
  - "Apple RecordingManager is a full stub (recording not yet implemented on Apple) — clean drop-in when Apple recording support is added"
  - "ModuleLifecycleManager has no Application/context parameter — Apple modules need none"
  - "ModuleLifecycleManager has no recordingManager parameter — Apple recording is stub-only, no record() calls needed in listeners"
  - "PREF_ISSUES = 'kamper_issues_list' preserved from original appleMain to avoid data loss on upgrade"
  - "Issues persistence via PreferencesStore.getString/putString (not NSUserDefaults.stringForKey/setObject directly)"

requirements-completed:
  - DEBT-03

# Metrics
duration: 15min
completed: 2026-04-26
---

# Phase 06 Plan 04: Apple Platform Counterparts — ApplePreferencesStore, RecordingManager stub, ModuleLifecycleManager

**NSUserDefaults-backed ApplePreferencesStore with 10 typed get/put methods, no-op Apple RecordingManager stub with StateFlow parity, and Apple ModuleLifecycleManager using FpsModule (not Choreographer) with no Android context dependency**

## Performance

- **Duration:** ~15 min
- **Started:** 2026-04-26T01:48:00Z
- **Completed:** 2026-04-26T02:07:00Z
- **Tasks:** 2
- **Files modified:** 3

## Accomplishments

- Created `ApplePreferencesStore` wrapping `NSUserDefaults.standardUserDefaults` with correct Kotlin/Native type conversions — `putInt` uses `setInteger(value.toLong(), key)` matching the NativeLong type expected by the Foundation API
- Created Apple `RecordingManager` stub exposing the same `isRecording: StateFlow<Boolean>` and `recordingSampleCount: StateFlow<Int>` StateFlow properties as the Android version — all methods are no-ops, `exportTrace()` returns `ByteArray(0)`
- Created Apple `ModuleLifecycleManager` mirroring the Android version with Apple-specific adaptations: uses `FpsModule` (not Choreographer), no `Application`/`context` constructor parameter, no `recordingManager` dependency, all 8 modules (CPU, FPS, Memory, Network, Issues, Jank, GC, Thermal) with proper install/uninstall pairs

## Task Commits

Each task was committed atomically:

1. **Task 1: Create ApplePreferencesStore and Apple RecordingManager stub** - `71068cf` (feat)
2. **Task 2: Create Apple ModuleLifecycleManager** - `b74a3f4` (feat)

## Files Created/Modified

- `kamper/ui/android/src/appleMain/kotlin/com/smellouk/kamper/ui/ApplePreferencesStore.kt` - NSUserDefaults-backed PreferencesStore with 10 override methods; `putInt` correctly calls `setInteger(value.toLong(), key)`
- `kamper/ui/android/src/appleMain/kotlin/com/smellouk/kamper/ui/RecordingManager.kt` - No-op stub with `isRecording: StateFlow<Boolean>` (init false) and `recordingSampleCount: StateFlow<Int>` (init 0); `exportTrace()` returns `ByteArray(0)`
- `kamper/ui/android/src/appleMain/kotlin/com/smellouk/kamper/ui/ModuleLifecycleManager.kt` - Engine + 8 module install/uninstall + listener wiring; `PREF_ISSUES = "kamper_issues_list"`; `applySettings`, `initialise`, `clearIssues`, `startEngine`, `stopEngine`, `restartEngine`, `clear` methods

## Decisions Made

- Apple `RecordingManager` has identical StateFlow property names as Android's version (`isRecording`, `recordingSampleCount`) so the facade can delegate without platform-specific branching
- `ModuleLifecycleManager` does not hold a reference to `RecordingManager` — Apple recording stubs live in the repository facade itself (Plan 05)
- `PREF_ISSUES = "kamper_issues_list"` preserved from original `appleMain/KamperUiRepository.kt` (Android uses `"issues_list"`) to avoid data loss when migrating existing Apple users
- Issues persistence routes through `PreferencesStore.getString/putString` rather than directly calling `NSUserDefaults` — consistent with the abstraction boundary

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

- `RecordingManager.record(trackId, value)` - returns Unit; Apple recording not yet implemented
- `RecordingManager.startRecording()` - returns Unit; Apple recording not yet implemented
- `RecordingManager.stopRecording()` - returns Unit; Apple recording not yet implemented
- `RecordingManager.exportTrace()` - returns `ByteArray(0)`; Apple recording not yet implemented
- `RecordingManager.clearRecording()` - returns Unit; Apple recording not yet implemented

These stubs are intentional per the plan — the class design is complete, ready for Apple recording implementation in a future phase.

## Compilation Note

The Apple files reference `PreferencesStore` (created by plan 06-02 in a parallel wave-2 worktree). Standalone compilation in this worktree produces 4 unresolved reference errors (`PreferencesStore`, `getString`, `putString`). This is expected parallel wave behavior — all errors resolve after the orchestrator merges all wave-2 worktrees together. The code structure and type usage are correct.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Apple counterpart classes are ready for Plan 05 which refactors `appleMain/KamperUiRepository.kt` to use them
- `ApplePreferencesStore` is ready to replace direct `NSUserDefaults` calls in the facade
- `RecordingManager` stub is a clean drop-in when Apple recording support is added
- `ModuleLifecycleManager` eliminates duplicate module-wiring code from the Apple facade

## Self-Check: PASSED

- `kamper/ui/android/src/appleMain/kotlin/com/smellouk/kamper/ui/ApplePreferencesStore.kt` - FOUND
- `kamper/ui/android/src/appleMain/kotlin/com/smellouk/kamper/ui/RecordingManager.kt` - FOUND
- `kamper/ui/android/src/appleMain/kotlin/com/smellouk/kamper/ui/ModuleLifecycleManager.kt` - FOUND
- Commit `71068cf` - FOUND
- Commit `b74a3f4` - FOUND

---
*Phase: 06-kamperuirepository-refactor-settings-tests*
*Completed: 2026-04-26*
