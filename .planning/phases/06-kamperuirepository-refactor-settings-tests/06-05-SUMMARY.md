---
phase: 06
plan: 05
subsystem: kamper-ui
tags: [refactor, facade, KamperUiRepository, android, apple, delegation, DEBT-03]
dependency_graph:
  requires:
    - "06-02"  # SettingsRepository (commonMain)
    - "06-03"  # Android platform classes (AndroidPreferencesStore, RecordingManager, ModuleLifecycleManager)
    - "06-04"  # Apple platform classes
  provides:
    - "Thin KamperUiRepository facades for androidMain and appleMain"
    - "Configurable maxSamples in Android RecordingManager"
  affects:
    - "KamperUi.kt (android + apple) — unchanged callers"
    - "KamperPanel.kt — unchanged; maxRecordingSamples still accessible via facade"
tech_stack:
  added: []
  patterns:
    - "Facade / Ports-and-Adapters delegation"
    - "Thin actual-class wrapping three inner collaborators"
key_files:
  created: []
  modified:
    - "kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/KamperUiRepository.kt"
    - "kamper/ui/android/src/appleMain/kotlin/com/smellouk/kamper/ui/KamperUiRepository.kt"
    - "kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/RecordingManager.kt"
decisions:
  - "Preserved `actual val maxRecordingSamples: Int` on both facades to satisfy the expect class contract and KamperPanel call site (repo.maxRecordingSamples)"
  - "Updated RecordingManager to accept configurable maxSamples constructor param (Rule 2: silently hardcoded constant was ignoring user-configured value)"
  - "Apple facade uses `actual val maxRecordingSamples: Int = 4_200` default; Android uses constructor param forwarded from KamperUi.kt"
metrics:
  duration_minutes: 2
  completed_date: "2026-04-26"
  tasks_completed: 2
  tasks_total: 2
  files_modified: 3
---

# Phase 6 Plan 5: KamperUiRepository Thin Facade Summary

Both actual `KamperUiRepository` classes replaced with thin delegation facades (~52-58 lines each), completing DEBT-03 by moving all settings/recording/module logic into `SettingsRepository`, `RecordingManager`, and `ModuleLifecycleManager`.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Refactor Android actual KamperUiRepository into thin facade | f7bbd96 | KamperUiRepository.kt (androidMain), RecordingManager.kt |
| 2 | Refactor Apple actual KamperUiRepository into thin facade | 0b18e2a | KamperUiRepository.kt (appleMain) |

## Verification

- Android `compileDebugSources`: BUILD SUCCESSFUL (no `error:` lines)
- Both facades satisfy all acceptance criteria (verified with grep checks)
- Android facade: 58 lines; Apple facade: 52 lines

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical Functionality] RecordingManager accepts configurable maxSamples**

- **Found during:** Task 1
- **Issue:** The plan's `RecordingManager()` no-arg constructor template would silently hardcode `MAX_RECORDING_SAMPLES = 4_200`, discarding the `maxRecordingSamples` value passed by `KamperUi.kt` (`config.maxRecordingSamples.coerceAtLeast(100)`). The expect class declares `val maxRecordingSamples: Int`, and `KamperPanel` reads `repo.maxRecordingSamples`.
- **Fix:** Added `maxSamples: Int = DEFAULT_MAX_RECORDING_SAMPLES` constructor param to `RecordingManager`. Facade passes `RecordingManager(maxSamples = maxRecordingSamples)` so the user's config value is honoured.
- **Files modified:** `kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/RecordingManager.kt`
- **Commit:** f7bbd96

**2. [Rule 1 - Bug] Preserved `actual val maxRecordingSamples: Int` on both facades**

- **Found during:** Task 1
- **Issue:** The plan's thin-facade template omitted `maxRecordingSamples` entirely. The expect class at `commonMain/KamperUiRepository.kt` declares `val maxRecordingSamples: Int` and `KamperUi.kt` (both platforms) passes it as a constructor arg. Omitting it would cause a compile error.
- **Fix:** Added `actual val maxRecordingSamples: Int = 4_200` constructor param to both facades. Android version mirrors the original constructor signature `(context: Context, maxRecordingSamples: Int = 4_200)`.
- **Files modified:** Both `KamperUiRepository.kt` files
- **Commit:** f7bbd96, 0b18e2a

## Threat Model Compliance

| Threat | Mitigation | Status |
|--------|-----------|--------|
| T-02-11: updateSettings() old/new ordering | `old` captured from `settingsRepository.settings.value` BEFORE `settingsRepository.updateSettings(normalized)` | Satisfied in both facades |
| T-02-12: clear() scope leak | `clear()` calls both `lifecycleManager.clear()` and `settingsRepository.clear()` | Satisfied in both facades |

## Self-Check: PASSED

- Android KamperUiRepository.kt exists: FOUND
- Apple KamperUiRepository.kt exists: FOUND
- Task 1 commit f7bbd96: FOUND
- Task 2 commit 0b18e2a: FOUND
- Build verification: `compileDebugSources` BUILD SUCCESSFUL with zero `error:` lines
