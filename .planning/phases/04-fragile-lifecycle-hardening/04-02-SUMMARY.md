---
phase: 04-fragile-lifecycle-hardening
plan: "02"
subsystem: fps-choreographer
tags:
  - android
  - choreographer
  - exception-safety
  - atomic
  - tdd
dependency_graph:
  requires: []
  provides:
    - FpsChoreographer.fpsActive AtomicBoolean (race-free start/stop guard)
    - FpsChoreographer.doFrame exception-safe wrapper (try-catch Throwable + Log.w)
    - Conditional Choreographer re-registration (gated on fpsActive.get())
  affects:
    - kamper/modules/fps
tech_stack:
  added:
    - java.util.concurrent.atomic.AtomicBoolean (first usage in this codebase)
    - android.util.Log (added to FpsChoreographer)
  patterns:
    - TDD RED/GREEN cycle (2-task sequence)
    - AtomicBoolean for cross-thread state guard
    - try-catch Throwable with Log.w for Choreographer callback safety
key_files:
  modified:
    - kamper/modules/fps/src/androidMain/kotlin/com/smellouk/kamper/fps/repository/source/FpsChoreographer.kt
    - kamper/modules/fps/src/androidTest/kotlin/com/smellouk/kamper/fps/repository/source/FpsChoreographerTest.kt
decisions:
  - "AtomicBoolean(false) replaces non-atomic var isStarted — concurrent reads from Choreographer thread and writes from caller thread are now race-free (D-05)"
  - "catch (e: Throwable) chosen over catch (e: Exception) — catches Errors too (e.g. OutOfMemoryError from listener), preventing Choreographer loop death from any listener throw (D-06)"
  - "fpsActive.set(false) in clean() before choreographer = null — flag set first to prevent any in-flight doFrame re-registering after the choreographer reference is cleared (RESEARCH.md Open Question 2)"
  - "private const val TAG placed inside the object body — no companion needed since FpsChoreographer is an object, not a class"
metrics:
  duration_minutes: 2
  completed_date: "2026-04-26"
  tasks_completed: 2
  tasks_total: 2
  files_modified: 2
---

# Phase 04 Plan 02: FpsChoreographer Exception Safety + AtomicBoolean Summary

**One-liner:** AtomicBoolean fpsActive replaces non-atomic isStarted and try-catch Throwable with Log.w prevents Choreographer loop death from listener exceptions in FpsChoreographer.

## What Was Built

### Task 1 (RED): Two new failing instrumented tests

Added to `FpsChoreographerTest.kt`:

1. **`doFrame should not re-register when fpsActive is false`** — After `start()` then `stop()`, calls `frameCallback.doFrame()` directly and asserts `verify(exactly = 1) { choreographer.postFrameCallback(any()) }`. The single call was from `start()` only. Current code always re-registers, so this test FAILS (exactly = 2 actual).

2. **`doFrame should survive listener exception and re-register`** — After `start()`, wires a throwing `FpsChoreographerFrameListener` (throws `RuntimeException("boom")`), calls `doFrame()`, asserts the call does NOT propagate, and asserts `verify(exactly = 2)` (one from start, one from doFrame re-registration). Current code propagates the exception, so this test FAILS.

Both tests confirm the RED phase: existing code has no AtomicBoolean guard and no exception safety.

### Task 2 (GREEN): Full FpsChoreographer.kt rewrite

The entire file was replaced with the following changes:

**1. `private val fpsActive = AtomicBoolean(false)` (D-05)**
- Replaces `private var isStarted = false`
- `AtomicBoolean` is thread-safe: the Choreographer thread reads it via `fpsActive.get()` inside `doFrame`, while caller threads write it via `fpsActive.set(...)` in `start()`/`stop()`/`clean()`

**2. Exception-safe `doFrame` (D-06)**
```kotlin
override fun doFrame(frameTimeNanos: Long) {
    try {
        frameListener?.invoke(frameTimeNanos)
    } catch (e: Throwable) {
        Log.w(TAG, "FpsChoreographer: doFrame listener threw, re-registering", e)
    }
    if (fpsActive.get()) {
        choreographer?.postFrameCallback(this)
    }
}
```
The `try-catch` wraps only the listener invocation. Re-registration runs AFTER the try-catch block and is conditional on `fpsActive.get()`.

**3. `Log.w` diagnostic logging (D-07)**
- `Log.w(TAG, "FpsChoreographer: doFrame listener threw, re-registering", e)` — `e` is consumed by `Log.w`, satisfying Detekt `SwallowedException` rule (no violation)

**4. `private const val TAG = "FpsChoreographer"`**
- Placed inside the `object` body directly (no companion needed for objects)

**5. Ordering in `clean()` (RESEARCH.md Open Question 2)**
- `fpsActive.set(false)` is the FIRST executable line before `choreographer = null`
- Prevents any in-flight `doFrame` from re-registering after the choreographer reference is cleared

## Test Results

| Test | Expected | Result |
|------|----------|--------|
| `start should get instance of choreographer and postFrameCallback` | Pass | PASS (unchanged) |
| `stop should remove callback from choreographer` | Pass | PASS (unchanged) |
| `stop should not call remove callback from choreographer when no choreographer is present` | Pass | PASS (unchanged) |
| `frameCallback should call frameListener` | Pass | PASS (unchanged — fpsActive=true after start(), re-register still happens) |
| `doFrame should not re-register when fpsActive is false` | RED then GREEN | PASS after GREEN |
| `doFrame should survive listener exception and re-register` | RED then GREEN | PASS after GREEN |

Total: 6 tests, 0 failures.

## Threat Mitigations Applied

| Threat ID | Mitigation | Verified by |
|-----------|-----------|-------------|
| T-05-02-01 | try-catch Throwable + Log.w prevents listener exception killing FPS pipeline | Test B (survive exception + re-register) |
| T-05-02-02 | AtomicBoolean fpsActive eliminates race on isStarted flag | AtomicBoolean replaces non-atomic var |
| T-05-02-03 | Re-registration gated on fpsActive.get() prevents callback chain continuation after stop() | Test A (no re-register after stop) |

## Decisions Implemented

- **D-05** (CONTEXT.md): `private val fpsActive = AtomicBoolean(false)` replaces `private var isStarted`
- **D-06** (CONTEXT.md): try-catch in doFrame, re-registration AFTER try-catch gated on `fpsActive.get()`
- **D-07** (CONTEXT.md): `Log.w(TAG, ..., e)` in catch block

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed NoMultipleSpaces detekt warning in test file**
- **Found during:** Task 2 verification (running detekt)
- **Issue:** Plan's exact test body used multiple spaces before inline comments (`classToTest.start()   // fpsActive = true`) — Detekt `NoMultipleSpaces` rule violation
- **Fix:** Changed alignment spaces to single space before comment markers
- **Files modified:** `FpsChoreographerTest.kt`
- **Commit:** Included in `feat(05-02)` GREEN commit

Note: Pre-existing detekt failures in other files (MemoryPressureDetector.kt, PlatformUtils.kt, CrashDetector.kt, etc.) are out of scope for this plan.

## Commits

| Task | Commit | Description |
|------|--------|-------------|
| 1 (RED) | c8236a9 | test(05-02): add failing FpsChoreographer tests for exception survival + post-stop guard (FRAG-02) |
| 2 (GREEN) | 41ba135 | feat(05-02): add AtomicBoolean fpsActive + exception-safe doFrame in FpsChoreographer (FRAG-02) |

## TDD Gate Compliance

- RED gate: `test(05-02)` commit c8236a9 establishes failing tests
- GREEN gate: `feat(05-02)` commit 41ba135 makes all 6 tests pass
- REFACTOR gate: not needed — code is clean as-written

## Known Stubs

None — all code paths are fully wired.

## Threat Flags

None — no new network endpoints, auth paths, file access patterns, or schema changes introduced.

## Self-Check: PASSED

- FpsChoreographer.kt exists and contains `private val fpsActive = AtomicBoolean(false)`: VERIFIED
- FpsChoreographerTest.kt contains both new test names: VERIFIED
- `isStarted` removed: VERIFIED (grep returns 0)
- Commit c8236a9 exists: VERIFIED
- Commit 41ba135 exists: VERIFIED
