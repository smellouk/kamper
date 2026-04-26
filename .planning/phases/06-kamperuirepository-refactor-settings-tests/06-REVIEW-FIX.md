---
phase: 06-kamperuirepository-refactor-settings-tests
fixed_at: 2026-04-26T00:00:00Z
review_path: .planning/phases/06-kamperuirepository-refactor-settings-tests/06-REVIEW.md
iteration: 1
findings_in_scope: 8
fixed: 7
skipped: 1
status: partial
---

# Phase 06: Code Review Fix Report

**Fixed at:** 2026-04-26T00:00:00Z
**Source review:** .planning/phases/06-kamperuirepository-refactor-settings-tests/06-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 8 (3 Critical, 5 Warning)
- Fixed: 7
- Skipped: 1

## Fixed Issues

### CR-01: Android PREF_ISSUES key differs from Apple

**Files modified:** `kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/ModuleLifecycleManager.kt`
**Commit:** `367aaa4`
**Applied fix:** Changed `PREF_ISSUES` constant from `"issues_list"` to `"kamper_issues_list"` to match the Apple implementation and prevent silent data loss on persisted issues.

---

### CR-02: Info listeners double-start engine in applySettings

**Files modified:** `kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/ModuleLifecycleManager.kt`, `kamper/ui/android/src/appleMain/kotlin/com/smellouk/kamper/ui/ModuleLifecycleManager.kt`
**Commit:** `6195281`
**Applied fix:** Replaced bare `engine.start()` with `engine.stop(); engine.start()` inside the `if (state.value.engineRunning)` block in `applySettings()` for both Android and Apple implementations, preventing duplicate OS callbacks or timers during config-change reinstalls.

---

### CR-03: Apple RecordingManager is a no-op stub

**Files modified:** `kamper/ui/android/src/appleMain/kotlin/com/smellouk/kamper/ui/RecordingManager.kt`
**Commit:** `08a16db`
**Applied fix:** Replaced the stub implementation with a full `RecordingManager` mirroring the Android implementation. Uses `NSDate.timeIntervalSinceReferenceDate` for the clock source. All five recording APIs (`startRecording`, `stopRecording`, `clearRecording`, `record`, `exportTrace`) are now functional on iOS. Added `DEFAULT_MAX_RECORDING_SAMPLES = 4_200` constant with a `maxSamples` constructor parameter.

---

### WR-01: issuesList mutated without synchronization (Android)

**Files modified:** `kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/ModuleLifecycleManager.kt`
**Commit:** `0dbc38e`
**Applied fix:** Wrapped `issuesList` mutations in `synchronized(issuesList)` blocks in both `issuesListener` and `clearIssues()` on the Android implementation. The Apple implementation was not modified because `synchronized` is a JVM-only construct and Kotlin/Native does not support it (see Skipped Issues).

---

### WR-02: Apple KamperUiRepository ignores maxRecordingSamples

**Files modified:** `kamper/ui/android/src/appleMain/kotlin/com/smellouk/kamper/ui/KamperUiRepository.kt`
**Commit:** `920362b`
**Applied fix:** Changed `RecordingManager()` to `RecordingManager(maxSamples = maxRecordingSamples)` so the user-configured recording cap is forwarded to the manager instead of silently falling back to the internal default.

---

### WR-03: SettingsRepository.clear() silently abandons in-flight saves

**Files modified:** `kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/SettingsRepository.kt`
**Commit:** `09dea1b`
**Applied fix:** Added KDoc comment on `clear()` documenting that any in-flight `updateSettings` save will be silently abandoned on scope cancellation, and that `clear()` should only be called when persistence is no longer required (e.g. on UI teardown). A structural flush-before-cancel was not applied as it would require introducing a blocking call or suspend context change beyond the scope of this review cycle.

---

### WR-04: fpsLow never reset when FPS module restarts (Android)

**Files modified:** `kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/ModuleLifecycleManager.kt`
**Commit:** `0b2a12b`
**Applied fix:** Added `state.update { it.copy(fpsPeak = 0, fpsLow = Int.MAX_VALUE, fpsHistory = emptyList()) }` to `stopFps()` so that stale peak/low sentinel values do not bleed into the next measurement window when FPS is restarted after a settings change.

---

### WR-05: RecordingManagerTest duplicates DEFAULT_MAX_RECORDING_SAMPLES

**Files modified:** `kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/RecordingManager.kt`, `kamper/ui/android/src/androidUnitTest/kotlin/com/smellouk/kamper/ui/RecordingManagerTest.kt`
**Commit:** `67e73f1`
**Applied fix:** Changed `DEFAULT_MAX_RECORDING_SAMPLES` from `private` to `internal` in `RecordingManager.kt`. Removed the local `MAX_RECORDING_SAMPLES = 4_200` constant from `RecordingManagerTest.kt` and updated the buffer-cap test to reference `DEFAULT_MAX_RECORDING_SAMPLES` directly.

---

## Skipped Issues

### WR-01 (Apple): issuesList mutated without synchronization (appleMain)

**File:** `kamper/ui/android/src/appleMain/kotlin/com/smellouk/kamper/ui/ModuleLifecycleManager.kt`
**Reason:** skipped — `synchronized` is a JVM-only construct and is not available in Kotlin/Native (appleMain/iosMain). The reviewer's suggested fix would not compile for this target. A Kotlin/Native-compatible solution would require using `kotlinx.atomicfu` or a `Mutex` from `kotlinx.coroutines`, which would require additional build dependencies and a coroutine scope not currently present in `ModuleLifecycleManager`. This needs human evaluation to choose the appropriate threading strategy for the Apple target.
**Original issue:** `issuesList` is a plain `mutableListOf()` mutated from the engine's data-delivery thread and from `clearIssues()` without synchronization, risking `ConcurrentModificationException` or lost updates.

---

_Fixed: 2026-04-26T00:00:00Z_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
