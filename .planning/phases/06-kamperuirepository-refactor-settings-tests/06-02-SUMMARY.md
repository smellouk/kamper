---
phase: 06-kamperuirepository-refactor-settings-tests
plan: 02
subsystem: ui-settings
tags: [kotlin-multiplatform, commonMain, coroutines, settings, repository-pattern, tdd]

# Dependency graph
requires:
  - plan: "06-01"
    provides: "FakePreferencesStore in commonTest, Mokkery plugin, test infrastructure"
provides:
  - "PreferencesStore interface — typed storage abstraction"
  - "SettingsRepository — settings StateFlow + async persistence"
affects:
  - "kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/"
  - "kamper/ui/android/src/commonTest/kotlin/com/smellouk/kamper/ui/"

# Tech stack
added:
  - "kotlinx-coroutines (CoroutineScope, SupervisorJob, MutableStateFlow)"
patterns:
  - "Repository + StateFlow pattern with synchronous in-memory update + async persistence"
  - "Dispatcher injection for virtual-time testability"
  - "TDD RED/GREEN cycle — failing tests committed before implementation"

# Key files
created:
  - path: "kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/PreferencesStore.kt"
    purpose: "Internal interface with 10 typed get/put methods (Boolean, Long, Float, Int, String)"
  - path: "kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/SettingsRepository.kt"
    purpose: "Settings state management: StateFlow + async persistence via PreferencesStore"
  - path: "kamper/ui/android/src/commonTest/kotlin/com/smellouk/kamper/ui/SettingsRepositoryTest.kt"
    purpose: "Unit tests for SettingsRepository using FakePreferencesStore + StandardTestDispatcher"
modified: []

# Key decisions
decisions:
  - "PreferencesStore includes getString/putString even though SettingsRepository does not use them — keeps interface complete for future implementations (issues persistence, etc.)"
  - "_settings.value = s before scope.launch in updateSettings() ensures in-memory state is immediately consistent before background write completes"
  - "scope.launch (not scope.launch(dispatcher)) because scope was constructed with dispatcher — avoids double-passing dispatcher"
  - "Dispatchers.IO default retained in constructor — tests override with StandardTestDispatcher, production uses real IO dispatcher"

# Metrics
duration_seconds: 143
completed_date: "2026-04-26"
tasks_completed: 2
files_created: 3
files_modified: 0
---

# Phase 06 Plan 02: PreferencesStore interface and SettingsRepository Summary

**One-liner:** PreferencesStore interface (10 typed methods) + SettingsRepository (StateFlow + dispatcher-injected async persistence) in commonMain — independently testable without platform APIs.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Create PreferencesStore interface in commonMain | d472722 | PreferencesStore.kt (created) |
| 2 (RED) | Add failing tests for SettingsRepository | 28bc4da | SettingsRepositoryTest.kt (created) |
| 2 (GREEN) | Implement SettingsRepository in commonMain | e1fe4d7 | SettingsRepository.kt (created) |

## What Was Built

### PreferencesStore.kt (commonMain)

Internal interface providing typed storage abstraction with 10 methods:
- `getBoolean`/`putBoolean` — 23 Boolean preference fields
- `getLong`/`putLong` — 9 Long preference fields
- `getFloat`/`putFloat` — 2 Float preference fields
- `getInt`/`putInt` — 1 Int preference field
- `getString`/`putString` — included for interface completeness (issues persistence, future use)

### SettingsRepository.kt (commonMain)

Platform-independent settings state management:
- Constructor: `SettingsRepository(store: PreferencesStore, dispatcher: CoroutineDispatcher = Dispatchers.IO)`
- Owned `CoroutineScope(dispatcher + SupervisorJob())` — cancelled in `clear()`
- `MutableStateFlow<KamperUiSettings>` initialised synchronously from store on construction
- `updateSettings(s)` — updates `_settings.value` immediately (in-memory), then `scope.launch { saveSettingsSync(s) }` for async persistence
- `suspend loadSettings()` — re-reads from store using `withContext(dispatcher)`, updates StateFlow
- `saveSettingsSync()` — writes all 35 preference keys (23 putBoolean + 9 putLong + 2 putFloat + 1 putInt)
- Zero platform imports — pure Kotlin/coroutines

### SettingsRepositoryTest.kt (commonTest)

5 tests covering:
1. Default settings match KamperUiSettings() when store is empty
2. updateSettings() immediately updates in-memory StateFlow value
3. updateSettings() persists value to store after advanceUntilIdle()
4. loadSettings() re-reads from store and updates the StateFlow
5. clear() cancels scope so subsequent saves do not execute

## TDD Gate Compliance

- RED gate: `test(06-02)` commit at `28bc4da` — failing tests committed before implementation
- GREEN gate: `feat(06-02)` commit at `e1fe4d7` — implementation makes tests pass
- No REFACTOR gate needed — implementation was clean on first pass

## Verification

```bash
./gradlew :kamper:ui:android:compileDebugSources
# BUILD SUCCESSFUL - no errors

./gradlew :kamper:ui:android:compileKotlinIosArm64
# BUILD SUCCESSFUL - no errors
```

No "Unresolved reference: PreferencesStore" or "Unresolved reference: SettingsRepository" errors.

## Threat Model Compliance

| Threat ID | Status |
|-----------|--------|
| T-02-03 (scope not cancelled) | Mitigated — `scope.cancel()` in `clear()` confirmed at line 109 |
| T-02-04 (preference key disclosure) | Accepted — keys are non-sensitive config strings |
| T-02-05 (DoS from uncancelled scope) | Mitigated — `clear()` cancels SupervisorJob |

## Deviations from Plan

None — plan executed exactly as written.

## Known Stubs

None — all fields fully wired with correct preference keys and defaults.

## Threat Flags

None — no new network endpoints, auth paths, or trust boundary changes introduced.

## Self-Check: PASSED

- PreferencesStore.kt exists: FOUND
- SettingsRepository.kt exists: FOUND
- SettingsRepositoryTest.kt exists: FOUND
- Commit d472722: FOUND (Task 1)
- Commit 28bc4da: FOUND (Task 2 RED)
- Commit e1fe4d7: FOUND (Task 2 GREEN)
