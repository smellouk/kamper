---
phase: 09-missing-features
plan: "03"
subsystem: cpu
tags: [feat-01, cpu, capability-probe, unsupported, android, repository, instrumented-test]

dependency_graph:
  requires:
    - phase: 09-01
      provides: CpuInfo.UNSUPPORTED companion constant (CpuInfo(-2.0, ...))
  provides:
    - CpuInfoRepositoryImpl.platformSupported one-time capability probe
    - CpuInfo.UNSUPPORTED return path in getInfo() when both proc/stat and shell return INVALID
    - CpuInfoRepositoryImplUnsupportedTest with 3 tests covering UNSUPPORTED branch + cache + Pitfall 6
  affects:
    - 09-04 (FEAT-01 UI — UNSUPPORTED tile rendering uses this probe result)

tech-stack:
  added: []
  patterns:
    - "One-time capability probe: private var Boolean? = null — null=not-probed, true=supported, false=UNSUPPORTED"
    - "Non-nullable CpuInfoSource contract: sources return CpuInfoDto.INVALID (not null) when unavailable"
    - "Early-return guard: if (platformSupported == false) return CpuInfo.UNSUPPORTED before any OS call"

key-files:
  created:
    - kamper/modules/cpu/src/androidTest/kotlin/com/smellouk/kamper/cpu/repository/CpuInfoRepositoryImplUnsupportedTest.kt
  modified:
    - kamper/modules/cpu/src/androidMain/kotlin/com/smellouk/kamper/cpu/repository/CpuInfoRepositoryImpl.kt

key-decisions:
  - "CpuInfoDto.INVALID (not null) is the sentinel for source unavailability — CpuInfoSource.getCpuInfoDto() is non-nullable; sources return CpuInfoDto.INVALID when they cannot produce data"
  - "UNSUPPORTED probe condition: dto == CpuInfoDto.INVALID && apiLevel >= 26 && !procStatOk — only triggers when shell fallback was the active source and also failed"
  - "platformSupported = true set only when dto != CpuInfoDto.INVALID — Pitfall 6 guard: transient CpuInfo.INVALID from mapper does not flip a working device to UNSUPPORTED"
  - "No @Volatile on platformSupported — getInfo() runs on the single Watcher coroutine dispatcher; concurrent access is not a concern in this code path"
  - "connectedDebugAndroidTest not run (no device in executor environment) — compileDebugAndroidTestKotlin exits 0; connected test is pre-merge verification step"

patterns-established:
  - "UNSUPPORTED probe pattern: capability check at top of getInfo() with private var Boolean? cache"

requirements-completed: [FEAT-01]

duration: 15min
completed: 2026-04-26
---

# Phase 09 Plan 03: CpuInfoRepositoryImpl UNSUPPORTED Probe Summary

**One-time `/proc/stat` capability probe in CpuInfoRepositoryImpl — caches `platformSupported = false` when both proc and shell sources return `CpuInfoDto.INVALID` on API 26+, returning `CpuInfo.UNSUPPORTED` permanently without retrying OS calls**

## Performance

- **Duration:** ~15 min
- **Completed:** 2026-04-26
- **Tasks:** 2
- **Files modified:** 1
- **Files created:** 1

## Accomplishments

- Added `private var platformSupported: Boolean?` field to `CpuInfoRepositoryImpl` for one-time capability caching (FEAT-01 D-03)
- Implemented early-return guard: `if (platformSupported == false) return CpuInfo.UNSUPPORTED` prevents any OS calls after the cache is set
- Added UNSUPPORTED probe branch: when shell returns `CpuInfoDto.INVALID` on an API 26+ device without `/proc/stat`, the cache is set and `CpuInfo.UNSUPPORTED` returned
- Created `CpuInfoRepositoryImplUnsupportedTest` with 3 instrumented tests covering UNSUPPORTED branch, cache short-circuit, and Pitfall 6 self-correcting guard
- All existing `Log.d("Kamper/CPU", ...)` lines preserved; one new diagnostic line added for the UNSUPPORTED branch

## Task Commits

Each task was committed atomically:

1. **Task 1: Add platformSupported cache + UNSUPPORTED return path** - `d13309b` (feat)
2. **Rule 1 fix: CpuInfoDto.INVALID instead of null** - `cba5666` (fix — deviation auto-fix)
3. **Task 2: CpuInfoRepositoryImplUnsupportedTest** - `c03308d` (test)

## Files Created/Modified

- `kamper/modules/cpu/src/androidMain/kotlin/com/smellouk/kamper/cpu/repository/CpuInfoRepositoryImpl.kt` — Added platformSupported field, early-return guard, both-sources-INVALID UNSUPPORTED branch, and platformSupported = true terminal-state assignment
- `kamper/modules/cpu/src/androidTest/kotlin/com/smellouk/kamper/cpu/repository/CpuInfoRepositoryImplUnsupportedTest.kt` — New instrumented test class with 3 tests (UNSUPPORTED return, cache no-retry, Pitfall 6)

## Log.d Diagnostic for UNSUPPORTED Branch

The following diagnostic log line was added (exact format):
```
Log.d("Kamper/CPU", "platformSupported=false (both sources unavailable) — returning CpuInfo.UNSUPPORTED")
```
This fires exactly once per `CpuInfoRepositoryImpl` instance lifetime — the first poll on a device where both `/proc/stat` and the shell fallback are unavailable. On all subsequent polls, the early-return guard fires silently before this line is reached.

## Pitfall 6 (Self-Correcting) — Verified by Test 3

Test 3 (`getInfo should NOT switch to UNSUPPORTED when shell fallback succeeds with INVALID mapper output`) explicitly guards Pitfall 6:
- Setup: shell returns a non-INVALID `CpuInfoDto`, but mapper returns `CpuInfo.INVALID` (transient error)
- Assert: result is `CpuInfo.INVALID` — NOT `CpuInfo.UNSUPPORTED`
- Verify: shell is consulted on BOTH calls (no false-positive cache flip to UNSUPPORTED)

This ensures a device that can produce data (even if mapper returns INVALID transiently) is never permanently marked as UNSUPPORTED.

## Decisions Made

- `CpuInfoDto.INVALID` is the correct "unavailable" signal — `CpuInfoSource.getCpuInfoDto()` is non-nullable; the plan's interface stub showed `CpuInfoDto?` but the actual implementation is non-nullable. Sources use `CpuInfoDto.INVALID` as their sentinel.
- `platformSupported = true` branch: only triggers when `dto != CpuInfoDto.INVALID` — CpuInfoDto.INVALID from the shell source (when it has no delta yet) will not prematurely mark the platform as "supported" either. This ensures the first valid sample sets the terminal true state.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Replace null check with CpuInfoDto.INVALID check**
- **Found during:** Task 2 (compileDebugAndroidTestKotlin)
- **Issue:** Plan's `<interfaces>` section showed `CpuInfoSource.getCpuInfoDto(): CpuInfoDto?` (nullable), but the actual interface and all implementations return non-nullable `CpuInfoDto`. The implementation used `dto == null` which can never be true, and `dto != null` which is always true. The UNSUPPORTED probe would never fire; the platformSupported = true assignment would always execute.
- **Fix:** Changed `dto == null` to `dto == CpuInfoDto.INVALID` and `dto != null` to `dto != CpuInfoDto.INVALID` in `CpuInfoRepositoryImpl`. Updated test to use `CpuInfoDto.INVALID` (not null) as the "unavailable" return value in mock setup.
- **Files modified:** CpuInfoRepositoryImpl.kt, CpuInfoRepositoryImplUnsupportedTest.kt
- **Verification:** compileDebugAndroidTestKotlin exits 0; assembleDebug exits 0; jvmTest exits 0
- **Committed in:** cba5666 (Rule 1 fix commit)

---

**Total deviations:** 1 auto-fixed (Rule 1 — bug: incorrect null check vs non-nullable interface contract)
**Impact on plan:** Auto-fix essential for correctness. Semantic intent of UNSUPPORTED probe preserved — CpuInfoDto.INVALID from the shell source correctly signals "source returned nothing useful", matching the plan's null-returning design intent.

## Issues Encountered

- No connected Android device available in executor environment — `connectedDebugAndroidTest` was not run. `compileDebugAndroidTestKotlin` confirms the test class compiles cleanly. The user should run `./gradlew :kamper:modules:cpu:connectedDebugAndroidTest --tests "com.smellouk.kamper.cpu.repository.CpuInfoRepositoryImplUnsupportedTest"` as part of pre-merge verification.

## Detekt Note

The project-level `detekt` task reports 109 pre-existing weighted issues (including `MagicNumber` at `ProcStatAccessibilityProvider:70` — value `5` in `parts.size >= 5`). This is a pre-existing issue in code that was not modified by this plan. No new detekt issues were introduced.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- `CpuInfoRepositoryImpl` now surfaces `CpuInfo.UNSUPPORTED` on devices with no CPU data access — ready for plan 09-04 (FEAT-01 UI) to render the gray "Unsupported" tile
- Pre-merge: run `./gradlew :kamper:modules:cpu:connectedDebugAndroidTest` on a connected device to verify all 3 instrumented tests pass

---
*Phase: 09-missing-features*
*Completed: 2026-04-26*
