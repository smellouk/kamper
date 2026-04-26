---
phase: 09-missing-features
fixed_at: 2026-04-26T00:00:00Z
review_path: .planning/phases/09-missing-features/09-REVIEW.md
iteration: 1
findings_in_scope: 7
fixed: 7
skipped: 0
status: all_fixed
---

# Phase 09: Code Review Fix Report

**Fixed at:** 2026-04-26T00:00:00Z
**Source review:** .planning/phases/09-missing-features/09-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 7 (2 Critical + 5 Warning)
- Fixed: 7
- Skipped: 0

## Fixed Issues

### CR-01: Operator-precedence bug makes /proc/stat non-zero check always false

**Files modified:** `kamper/modules/cpu/src/androidMain/kotlin/com/smellouk/kamper/cpu/repository/CpuInfoRepositoryImpl.kt`
**Commit:** 81a1284
**Applied fix:** Added parentheses around `(it.toLongOrNull() ?: 0L)` so the elvis operator is evaluated before the `> 0L` comparison. Without parens, `>` had higher precedence than `?:`, making the fallback always evaluate `0L > 0L` = false and causing the accessibility check to be unreliable.

---

### CR-02: FPS mutable fields written across two threads without synchronization

**Files modified:** `kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/ModuleLifecycleManager.kt`
**Commit:** 4526284
**Applied fix:** Added `@Volatile` to `fpsActive`, `fpsFrameCount`, and `fpsWindowStartNanos`. Also moved `fpsFrameCount = 0` and `fpsWindowStartNanos = 0L` resets inside the `Handler.post` block in `stopFps()` so they execute on the Choreographer/main thread, eliminating the race with an in-flight `doFrame` call.

---

### WR-01: ValidationInfo.EMPTY and ValidationInfo.INVALID are identical — misleading API

**Files modified:** `kamper/engine/src/commonMain/kotlin/com.smellouk.kamper/ValidationInfo.kt`
**Commit:** 1b21954
**Applied fix:** Removed `ValidationInfo.INVALID` entirely. It was structurally identical to `EMPTY` (both `data class` instances with `emptyList()`), making `EMPTY == INVALID` always true. A grep confirmed `INVALID` is not referenced anywhere in the codebase.

---

### WR-02: KamperUiState.EMPTY defaults engineRunning=true — state mismatch before initialise()

**Files modified:** `kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/KamperUiState.kt`
**Commit:** f5a0110
**Applied fix:** Changed `engineRunning = true` to `engineRunning = false` in `KamperUiState.EMPTY`. The engine is not started at construction time; `initialise()` already calls `state.update { it.copy(engineRunning = true) }` after starting the engine.

---

### WR-03: applySettings() reads state.value.engineRunning outside atomic update — TOCTOU

**Files modified:** `kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/ModuleLifecycleManager.kt`
**Commit:** 8a76aa0
**Applied fix:** Added a `@Volatile private var engineRunning = false` field to `ModuleLifecycleManager`. Updated `startEngine()`, `stopEngine()`, and `initialise()` to maintain this flag alongside each `engine.start()`/`engine.stop()` call. Changed `applySettings()` to read `engineRunning` (the authoritative field) instead of `state.value.engineRunning` (a StateFlow snapshot subject to TOCTOU races).

---

### WR-04: Android Log.d calls left unconditionally in production CPU code

**Files modified:** `kamper/modules/cpu/src/androidMain/kotlin/com/smellouk/kamper/cpu/repository/CpuInfoRepositoryImpl.kt`
**Commit:** 73964c6
**Applied fix:** Wrapped all seven `Log.d` calls with `if (BuildConfig.DEBUG)` guards. Added the import for `io.mellouk.kamper.cpu.BuildConfig` (matching the module's namespace declared in `build.gradle.kts`). Logs now only fire in debug builds and are stripped from production, consistent with the zero-logcat-noise expectation for library releases.

---

### WR-05: Issue serialization newline delimiter collides with per-record separator

**Files modified:** `kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/ModuleLifecycleManager.kt`
**Commit:** af806e7
**Applied fix:** Replaced the `"\n"` inter-record join separator with `""` (Group Separator, U+001D, code 0x1D). Updated `loadPersistedIssues()` to split on `''` instead of `raw.lines()`. The Group Separator has code < 0x20, so `pctEncode()` will encode it if it ever appears inside a field value, making the format self-consistent and robust against future refactoring that adds non-encoded fields.

---

_Fixed: 2026-04-26T00:00:00Z_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
