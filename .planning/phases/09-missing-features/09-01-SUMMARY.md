---
phase: 09
plan: "01"
subsystem: api-sentinels
tags: [feat-01, sentinels, info, performance, kotlin-multiplatform, common-main]
dependency_graph:
  requires: []
  provides:
    - Info.UNSUPPORTED companion constant
    - CpuInfo.UNSUPPORTED / FpsInfo.UNSUPPORTED / MemoryInfo.UNSUPPORTED / NetworkInfo.UNSUPPORTED / GcInfo.UNSUPPORTED / JankInfo.UNSUPPORTED / ThermalInfo.UNSUPPORTED / IssueInfo.UNSUPPORTED
    - ThermalState.UNSUPPORTED enum entry
    - Issue.UNSUPPORTED companion constant
    - Performance.lastValidSampleAt @Volatile field
  affects:
    - kamper/api (Info, Performance)
    - kamper/modules/* (8 Info subclasses + ThermalState)
    - kamper/ui/android (ActivityTab exhaustive when branch)
tech_stack:
  added: []
  patterns:
    - Sentinel companion constant (UNSUPPORTED at -2 vs INVALID at -1)
    - kotlin.concurrent.Volatile for KMP commonMain
key_files:
  created:
    - kamper/modules/cpu/src/commonTest/kotlin/com/smellouk/kamper/cpu/CpuInfoUnsupportedTest.kt
  modified:
    - kamper/api/src/commonMain/kotlin/com/smellouk/kamper/api/Info.kt
    - kamper/api/src/commonMain/kotlin/com/smellouk/kamper/api/Performance.kt
    - kamper/modules/cpu/src/commonMain/kotlin/com/smellouk/kamper/cpu/CpuInfo.kt
    - kamper/modules/fps/src/commonMain/kotlin/com/smellouk/kamper/fps/FpsInfo.kt
    - kamper/modules/memory/src/commonMain/kotlin/com/smellouk/kamper/memory/MemoryInfo.kt
    - kamper/modules/network/src/commonMain/kotlin/com/smellouk/kamper/network/NetworkInfo.kt
    - kamper/modules/gc/src/commonMain/kotlin/com/smellouk/kamper/gc/GcInfo.kt
    - kamper/modules/jank/src/commonMain/kotlin/com/smellouk/kamper/jank/JankInfo.kt
    - kamper/modules/thermal/src/commonMain/kotlin/com/smellouk/kamper/thermal/ThermalState.kt
    - kamper/modules/thermal/src/commonMain/kotlin/com/smellouk/kamper/thermal/ThermalInfo.kt
    - kamper/modules/issues/src/commonMain/kotlin/com/smellouk/kamper/issues/Issue.kt
    - kamper/modules/issues/src/commonMain/kotlin/com/smellouk/kamper/issues/IssueInfo.kt
    - kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/ActivityTab.kt
decisions:
  - "-2 sentinel for numeric UNSUPPORTED fields (vs -1 for INVALID); Boolean fields use false; enum field uses new UNSUPPORTED enum entry"
  - "kotlin.concurrent.Volatile import used for Performance.lastValidSampleAt in commonMain (Kotlin 2.3.20 supports it)"
  - "NetworkInfo.NOT_SUPPORTED retained unchanged alongside new UNSUPPORTED to preserve KamperUiRepository compatibility"
  - "ActivityTab.kt exhaustive when extended with ThermalState.UNSUPPORTED -> 0f (Rule 1 auto-fix)"
metrics:
  duration: "6m"
  completed_date: "2026-04-26"
  tasks_completed: 3
  tasks_total: 3
  files_created: 1
  files_modified: 13
---

# Phase 09 Plan 01: UNSUPPORTED Sentinel Constants — Foundation Summary

Add `UNSUPPORTED` companion constants to every `Info` subclass, the `Info` base interface, `ThermalState` enum, and `Issue` class. Add `@Volatile internal var lastValidSampleAt: Long = 0L` to `Performance` base class. Pure additive scaffolding required by all downstream plans in Phase 09.

## What Was Built

Twelve source-file modifications and one test file body replacement adding sentinel constants and a base-class field:

**Sentinel values chosen per module:**

| Module | UNSUPPORTED sentinel | INVALID sentinel | Rationale |
|--------|---------------------|-----------------|-----------|
| CpuInfo | `-2.0` (each Double field) | `-1.0` | Numeric — standard -2 convention |
| FpsInfo | `-2` (Int fps) | `-1` | Numeric — standard -2 convention |
| HeapMemoryInfo | `-2F, -2F` (Float) | `-1F, -1F` | Numeric |
| PssInfo | `-2F, -2F, -2F, -2F` (Float) | `-1F, ...` | Numeric |
| RamInfo | `-2F, -2F, -2F, false` | `-1F, ..., false` | Boolean isLowMemory stays false (no numeric sentinel) |
| NetworkInfo | `-2F, -2F, -2F, -2F` | `-1F, ...` | Numeric; NOT_SUPPORTED (-100F) retained |
| GcInfo | `-2L, -2L, -2L, -2L` (Long) | `-1L, ...` | Numeric |
| JankInfo | `-2, -2f, -2L` | `-1, -1f, -1L` | Numeric |
| ThermalInfo | `ThermalState.UNSUPPORTED, false` | `ThermalState.UNKNOWN, false` | Enum field — new UNSUPPORTED entry added to ThermalState |
| IssueInfo | `Issue.UNSUPPORTED` | `Issue.INVALID` | Data-class field — see Issue.UNSUPPORTED |
| Issue | `id="unsupported", timestampMs=-2L` | `id="", timestampMs=-1L` | Both id and timestampMs are sentinel-distinct |

**Info base interface:** `val UNSUPPORTED = object : Info {}` added alongside `INVALID` — follows identical anonymous-object pattern.

**kotlin.concurrent.Volatile:** Used for `Performance.lastValidSampleAt` in commonMain. Kotlin 2.3.20 supports `kotlin.concurrent.Volatile` natively in KMP — the import path works cleanly. No fallback comment needed.

**NetworkInfo.NOT_SUPPORTED retained:** The existing `NOT_SUPPORTED = NetworkInfo(-100F, ...)` constant is unchanged and still present at line 15-17. KamperUiRepository continues to compile without modification.

## RED→GREEN Transition

**Task 1 (RED):** `CpuInfoUnsupportedTest.kt` replaced the Wave-0 TODO stub with 3 real `@Test` methods. Running `compileTestKotlinJvm` confirmed RED state with `Unresolved reference 'UNSUPPORTED'` — expected.

**Task 2 (GREEN):** `CpuInfo.UNSUPPORTED = CpuInfo(-2.0, -2.0, -2.0, -2.0, -2.0)` added. Running `:kamper:modules:cpu:jvmTest --tests "com.smellouk.kamper.cpu.CpuInfoUnsupportedTest"` reported:
- `UNSUPPORTED should be structurally distinct from INVALID` — PASSED
- `UNSUPPORTED should use -2_0 sentinel for every numeric field` — PASSED
- `INVALID should still use -1_0 sentinel after the change` — PASSED

**Task 3:** Thermal, Issue, and Performance changes added. All jvmTests pass with no regression.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] ThermalState exhaustive `when` in ActivityTab.kt**

- **Found during:** Task 3
- **Issue:** Adding `UNSUPPORTED` to the `ThermalState` enum makes the exhaustive `when (thermalState)` expression in `ActivityTab.kt` non-exhaustive, causing a compile error.
- **Fix:** Added `com.smellouk.kamper.thermal.ThermalState.UNSUPPORTED -> 0f` branch to the thermal fraction `when` block, matching the pattern described in the PATTERNS.md plan.
- **Files modified:** `kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/ActivityTab.kt`
- **Commit:** 9cc7041

## Known Stubs

None — all sentinel constants are real compile-time values wired to the existing companion object pattern. No stub data flows to UI rendering from this plan alone.

## Threat Flags

None — this plan introduces only inert compile-time constants and a monotonic timestamp field (`internal` visibility). No new trust boundaries, network endpoints, or data input paths.

## Self-Check: PASSED

Files verified to exist:
- kamper/api/src/commonMain/kotlin/com/smellouk/kamper/api/Info.kt: FOUND
- kamper/api/src/commonMain/kotlin/com/smellouk/kamper/api/Performance.kt: FOUND
- kamper/modules/cpu/src/commonMain/kotlin/com/smellouk/kamper/cpu/CpuInfo.kt: FOUND
- kamper/modules/fps/src/commonMain/kotlin/com/smellouk/kamper/fps/FpsInfo.kt: FOUND
- kamper/modules/memory/src/commonMain/kotlin/com/smellouk/kamper/memory/MemoryInfo.kt: FOUND
- kamper/modules/network/src/commonMain/kotlin/com/smellouk/kamper/network/NetworkInfo.kt: FOUND
- kamper/modules/gc/src/commonMain/kotlin/com/smellouk/kamper/gc/GcInfo.kt: FOUND
- kamper/modules/jank/src/commonMain/kotlin/com/smellouk/kamper/jank/JankInfo.kt: FOUND
- kamper/modules/thermal/src/commonMain/kotlin/com/smellouk/kamper/thermal/ThermalState.kt: FOUND
- kamper/modules/thermal/src/commonMain/kotlin/com/smellouk/kamper/thermal/ThermalInfo.kt: FOUND
- kamper/modules/issues/src/commonMain/kotlin/com/smellouk/kamper/issues/Issue.kt: FOUND
- kamper/modules/issues/src/commonMain/kotlin/com/smellouk/kamper/issues/IssueInfo.kt: FOUND
- kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/ActivityTab.kt: FOUND (auto-fix)
- kamper/modules/cpu/src/commonTest/kotlin/com/smellouk/kamper/cpu/CpuInfoUnsupportedTest.kt: FOUND

Commits verified:
- 7561f2d: test(09-01) Task 1 RED — FOUND
- 14933d9: feat(09-01) Task 2 GREEN — FOUND
- 9cc7041: feat(09-01) Task 3 — FOUND
