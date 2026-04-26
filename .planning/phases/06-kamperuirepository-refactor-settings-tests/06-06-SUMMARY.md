---
phase: 06
plan: 06
subsystem: kamper-ui
tags: [test, settings, recording, SettingsRepository, RecordingManager, DEBT-04, TEST-01]
dependency_graph:
  requires:
    - "06-01"  # FakePreferencesStore in commonTest
    - "06-02"  # SettingsRepository (commonMain)
    - "06-03"  # RecordingManager (androidMain)
  provides:
    - "SettingsRepositoryTest: 12 tests covering Boolean/Long/Float/Int CRUD + round-trip"
    - "RecordingManagerTest: 10 tests covering buffer, state transitions, and export"
  affects:
    - "kamper/ui/android/build.gradle.kts — androidUnitTest source set added"
tech_stack:
  added:
    - "androidUnitTest source set (KMP androidTarget) for Android JVM unit tests"
  patterns:
    - "TestCoroutineScheduler + StandardTestDispatcher for deterministic async control"
    - "Shared FakePreferencesStore map for round-trip persistence simulation"
    - "androidUnitTest source set with isReturnDefaultValues=true for SystemClock stub"
key_files:
  created:
    - "kamper/ui/android/src/commonTest/kotlin/com/smellouk/kamper/ui/SettingsRepositoryTest.kt"
    - "kamper/ui/android/src/androidUnitTest/kotlin/com/smellouk/kamper/ui/RecordingManagerTest.kt"
  modified:
    - "kamper/ui/android/build.gradle.kts"
decisions:
  - "Used TestCoroutineScheduler + runTest(testScheduler) instead of TestScope() to match WatcherTest.kt pattern and plan specification"
  - "Placed RecordingManagerTest in androidUnitTest (not commonTest) because RecordingManager uses android.os.SystemClock.elapsedRealtimeNanos(); isReturnDefaultValues=true handles the stub"
  - "Declared MAX_RECORDING_SAMPLES = 4_200 as private test-file constant because production constant is file-level private and not visible to tests"
  - "Added androidUnitTest source set to build.gradle.kts with kotlin-test dependency only (coroutines-test not needed for synchronous RecordingManager tests)"
  - "SettingsRepositoryTest rewritten from 5-test initial version to 12-test full version per DEBT-04 requirements"
metrics:
  duration_minutes: 8
  completed_date: "2026-04-26"
  tasks_completed: 2
  tasks_total: 2
  files_modified: 3
---

# Phase 6 Plan 6: Settings and Recording Tests Summary

First test coverage for the ui module: 12 SettingsRepositoryTest tests (Boolean/Long/Float/Int CRUD, in-memory StateFlow, clear scope cancellation, 2 round-trip persistence tests via shared FakePreferencesStore) + 10 RecordingManagerTest tests (record guard, start/stop state, buffer cap at 4200, clear resets, exportTrace non-null). All 22 tests pass with BUILD SUCCESSFUL.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Write SettingsRepositoryTest in commonTest | 4f0ecef | SettingsRepositoryTest.kt |
| 2 | Write RecordingManagerTest in androidUnitTest | 97a09e2 | RecordingManagerTest.kt, build.gradle.kts |

## Verification

Full suite run: `./gradlew :kamper:ui:android:cleanAllTests :kamper:ui:android:test`

```
22 passing (370ms)
BUILD SUCCESSFUL
```

- SettingsRepositoryTest: 12 tests (Boolean CRUD: showCpu, isDarkTheme, showJank; Long CRUD: cpuIntervalMs, anrThresholdMs; Float CRUD: memPressureWarningPct; Int CRUD: droppedFrameConsecutiveThreshold; StateFlow synchronous update; clear() scope cancellation; 2 round-trip persistence tests)
- RecordingManagerTest: 10 tests (record guard, startRecording state, sample count increment, buffer cleared on restart, buffer cap at 4200, stopRecording state, record ignored after stop, clearRecording resets isRecording, clearRecording resets sampleCount, exportTrace non-null)

## Deviations from Plan

None — plan executed exactly as written. The SettingsRepositoryTest was expanded from the 5-test version committed in plan 02 (TDD RED phase) to the full 12-test version specified in this plan. The androidUnitTest source set was added to build.gradle.kts as required by the task.

## Threat Model Compliance

| Threat | Disposition | Status |
|--------|-------------|--------|
| T-02-13: SettingsRepositoryTest round-trip | accept (no disk I/O) | Tests use FakePreferencesStore only |
| T-02-14: RecordingManagerTest buffer cap | accept (test env) | Buffer cap verified to enforce at exactly 4200 |

## Known Stubs

None — both test files wire real production classes via constructor injection.

## Self-Check: PASSED

- SettingsRepositoryTest.kt exists: FOUND
- RecordingManagerTest.kt exists: FOUND
- Task 1 commit 4f0ecef: FOUND
- Task 2 commit 97a09e2: FOUND
- Build verification: `./gradlew :kamper:ui:android:cleanAllTests :kamper:ui:android:test` BUILD SUCCESSFUL, 22 tests passing
