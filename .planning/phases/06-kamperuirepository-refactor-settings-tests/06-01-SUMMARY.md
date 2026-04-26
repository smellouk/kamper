---
phase: 06-kamperuirepository-refactor-settings-tests
plan: 01
subsystem: testing
tags: [kotlin-multiplatform, mokkery, commonTest, gradle, test-infrastructure]

# Dependency graph
requires:
  - phase: 05-cpu-performance-recording-buffer
    provides: "stable codebase baseline — ring buffer, CPU recording implementation"
provides:
  - "commonTest source set in ui/android module with kotlinx-coroutines-test"
  - "dev.mokkery plugin enabled in ui/android module"
  - "android.testOptions.unitTests.isReturnDefaultValues = true"
  - "FakePreferencesStore in-memory test double for all subsequent test plans"
affects:
  - 06-02-PLAN
  - 06-03-PLAN
  - 06-04-PLAN
  - 06-05-PLAN
  - 06-06-PLAN

# Tech tracking
tech-stack:
  added:
    - "dev.mokkery 3.3.0 — KMP mocking plugin"
    - "kotlinx-coroutines-test 1.10.1 — commonTest dependency"
  patterns:
    - "commonTest source set declared explicitly in multiplatform build.gradle.kts"
    - "android.testOptions.unitTests.isReturnDefaultValues for JVM unit test Android framework mocking"
    - "In-memory test double via MutableMap with optional shared-map constructor"

key-files:
  created:
    - "kamper/ui/android/src/commonTest/kotlin/com/smellouk/kamper/ui/FakePreferencesStore.kt"
  modified:
    - "kamper/ui/android/build.gradle.kts"

key-decisions:
  - "Placed dev.mokkery after id(\"com.android.library\") in plugins block, matching engine module pattern"
  - "Did not add androidUnitTest source set — it is available implicitly via androidTarget()"
  - "FakePreferencesStore accepts optional MutableMap parameter to allow round-trip testing across instances"
  - "isReturnDefaultValues = true prevents RuntimeException when RecordingManagerTest calls android.os.SystemClock.elapsedRealtimeNanos() in JVM context"

patterns-established:
  - "Pattern: KMP commonTest source set must be declared explicitly to receive coroutines-test dependency"
  - "Pattern: FakeXxx test doubles in commonTest implement internal interfaces from commonMain"

requirements-completed:
  - DEBT-04
  - TEST-01

# Metrics
duration: 15min
completed: 2026-04-26
---

# Phase 06 Plan 01: Build Scaffold — mokkery plugin, commonTest, FakePreferencesStore

**dev.mokkery plugin + commonTest source set with coroutines-test wired into ui/android module, plus in-memory FakePreferencesStore backing all 10 typed get/put methods via MutableMap**

## Performance

- **Duration:** ~15 min
- **Started:** 2026-04-26T01:45:00Z
- **Completed:** 2026-04-26T01:57:28Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- Enabled dev.mokkery 3.3.0 plugin in the ui/android module — required for all KMP mock generation in subsequent test plans
- Declared commonTest source set with kotlin("test") and kotlinx-coroutines-test 1.10.1 — previously absent, blocking all test file compilation
- Added android.testOptions.unitTests.isReturnDefaultValues = true — prevents RuntimeException from unmocked Android framework calls in JVM unit tests
- Created FakePreferencesStore with all 10 typed get/put override methods backed by an injectable MutableMap for both isolated and shared-state test scenarios

## Task Commits

Each task was committed atomically:

1. **Task 1: Add mokkery plugin, commonTest source set, and unitTests returnDefaultValues** - `4bf9d45` (chore)
2. **Task 2: Create FakePreferencesStore in commonTest** - `ad09120` (feat)

## Files Created/Modified

- `kamper/ui/android/build.gradle.kts` - Added dev.mokkery plugin, commonTest block with coroutines-test, and android.testOptions.unitTests.isReturnDefaultValues = true
- `kamper/ui/android/src/commonTest/kotlin/com/smellouk/kamper/ui/FakePreferencesStore.kt` - In-memory PreferencesStore test double with optional shared MutableMap constructor

## Decisions Made

- Placed `id("dev.mokkery")` after `id("com.android.library")` to match the established engine module pattern
- Did not add an `androidUnitTest` source set — it is implicitly available through `androidTarget()`, only `commonTest` was missing
- FakePreferencesStore constructor accepts an optional `MutableMap<String, Any>` parameter so two instances can share state for round-trip testing (used in Plan 06 SettingsRepositoryTest)
- `isReturnDefaultValues = true` placed inside `android { testOptions { unitTests { } } }` to prevent `RuntimeException("Method not mocked")` when JVM unit tests call Android framework methods

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Wave 1 scaffold is complete — both the commonTest source set and FakePreferencesStore are in place
- Plans 02–06 (PreferencesStore interface, SettingsRepository, tests) can now proceed with compilation support
- FakePreferencesStore is ready to be referenced by all subsequent test files in this phase

---
*Phase: 06-kamperuirepository-refactor-settings-tests*
*Completed: 2026-04-26*
