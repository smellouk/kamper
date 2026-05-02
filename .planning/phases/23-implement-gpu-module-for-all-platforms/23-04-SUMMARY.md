---
phase: 23
plan: "04"
subsystem: gpu
tags: [kmp, cinterop, iokit, macos, ios, tvos, apple-platforms]
dependency_graph:
  requires: ["23-01 (gpuInfo.def IOKit cinterop)", "23-02 (commonMain contracts: GpuInfo, GpuConfig, GpuWatcher, GpuPerformance, GpuInfoRepository, GpuInfoSource)"]
  provides:
    - "macosMain actual val GpuModule wired to IOKit cinterop via MacosGpuInfoSource"
    - "iosMain actual val GpuModule returning GpuInfo.UNSUPPORTED (App Store safe)"
    - "tvosMain actual val GpuModule returning GpuInfo.UNSUPPORTED (App Store safe)"
  affects:
    - "libs/modules/gpu/src/macosMain/"
    - "libs/modules/gpu/src/iosMain/"
    - "libs/modules/gpu/src/tvosMain/"
tech_stack:
  added: []
  patterns:
    - "IOKit cinterop call via kamper_gpu_utilization() (macOS only)"
    - "UNSUPPORTED sentinel unconditionally on iOS/tvOS (no cinterop — D-07)"
    - "try/catch exception absorption per CLAUDE.md D-06 safety rule"
    - "Dispatchers.Default for both default and main dispatcher on all Apple targets (mirrors CpuModule pattern)"
key_files:
  created:
    - libs/modules/gpu/src/macosMain/kotlin/com/smellouk/kamper/gpu/Module.kt
    - libs/modules/gpu/src/macosMain/kotlin/com/smellouk/kamper/gpu/repository/GpuInfoRepositoryImpl.kt
    - libs/modules/gpu/src/macosMain/kotlin/com/smellouk/kamper/gpu/repository/source/MacosGpuInfoSource.kt
    - libs/modules/gpu/src/iosMain/kotlin/com/smellouk/kamper/gpu/Module.kt
    - libs/modules/gpu/src/iosMain/kotlin/com/smellouk/kamper/gpu/repository/GpuInfoRepositoryImpl.kt
    - libs/modules/gpu/src/tvosMain/kotlin/com/smellouk/kamper/gpu/Module.kt
    - libs/modules/gpu/src/tvosMain/kotlin/com/smellouk/kamper/gpu/repository/GpuInfoRepositoryImpl.kt
  modified: []
decisions:
  - "mainDispatcher=Dispatchers.Default on all three Apple platforms — mirrors CpuModule macosMain/iosMain/tvosMain pattern (no Main dispatcher on KN Apple targets)"
  - "@KamperDslMarker removed from fun GpuModule — Kotlin warns this annotation has no effect on top-level functions (KT-81567); mirrors CPU macosMain which imports but does not annotate"
  - "Memory fields set to -1.0 on macOS — IOAccelerator PerformanceStatistics does not expose VRAM totals via this path; deferred enhancement per CONTEXT.md"
  - "iOS/tvOS cinterop excluded at build.gradle.kts level (Plan 01) — this plan adds grep gate verification to confirm no source leakage"
metrics:
  duration: "~4 minutes"
  completed: "2026-05-01T23:03:09Z"
  tasks_completed: 2
  files_created: 7
  files_modified: 0
---

# Phase 23 Plan 04: Apple Platform GPU Actuals Summary

macOS GPU actual backed by IOKit IOAccelerator cinterop; iOS and tvOS return UNSUPPORTED unconditionally — D-07 (App Store safety) enforced at both build-graph and source-grep levels.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | macOS actual — Module.kt + GpuInfoRepositoryImpl + MacosGpuInfoSource | 51c6399 | Module.kt, GpuInfoRepositoryImpl.kt, MacosGpuInfoSource.kt |
| 2 | iOS + tvOS actuals — UNSUPPORTED stubs (4 files) | 973759b | iosMain/Module.kt, iosMain/GpuInfoRepositoryImpl.kt, tvosMain/Module.kt, tvosMain/GpuInfoRepositoryImpl.kt |

## What Was Built

### Task 1 — macOS actual (3 files)

**MacosGpuInfoSource.kt** — Internal class implementing `GpuInfoSource`. Calls `kamper_gpu_utilization()` from the IOKit cinterop binding created in Plan 01. Sentinel routing:
- `util <= -2.0` → `GpuInfo.UNSUPPORTED` (no IOAccelerator service — sandboxed app or no GPU)
- `util < 0.0` → `GpuInfo.INVALID` (service found but PerformanceStatistics unreadable)
- `util in [0.0, 100.0]` → `GpuInfo(utilization=util, usedMemoryMb=-1.0, totalMemoryMb=-1.0)`

Memory fields are `-1.0` because IOAccelerator's PerformanceStatistics dictionary does not expose VRAM totals (partial data per D-02; Metal/IORegistry query is a deferred enhancement).

The cinterop handles AGX ("Device Utilization %") and Intel/AMD ("GPU Activity(%)"). The C body in gpuInfo.def clamps to 100.0 (T-23-14 Tampering mitigated). All exceptions absorbed via `try { ... } catch (_: Exception) { GpuInfo.INVALID }` per D-06.

`@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)` required — cinterop uses experimental KN FFI API.

**GpuInfoRepositoryImpl.kt** (macosMain) — One-line delegate: `override fun getInfo(): GpuInfo = source.getInfo()`.

**Module.kt** (macosMain) — `actual val GpuModule` wires `MacosGpuInfoSource` through `GpuInfoRepositoryImpl` → `GpuWatcher` → `GpuPerformance`. Uses `mainDispatcher = Dispatchers.Default` (no Main dispatcher available on Kotlin/Native Apple targets — mirrors CpuModule).

### Task 2 — iOS + tvOS UNSUPPORTED stubs (4 files)

**iosMain/GpuInfoRepositoryImpl.kt** — Returns `GpuInfo.UNSUPPORTED` unconditionally. Comment explains: IOAccelerator is a private framework on iOS; using IOKit GPU APIs risks App Store rejection (D-07 / 23-RESEARCH Pitfall 3).

**tvosMain/GpuInfoRepositoryImpl.kt** — Identical pattern with "tvOS" in the comment.

**iosMain/Module.kt** and **tvosMain/Module.kt** — Identical `actual val GpuModule` implementations wiring the UNSUPPORTED repository. `mainDispatcher = Dispatchers.Default` (mirrors CpuModule iOS/tvOS pattern).

Zero cinterop imports on both platforms — confirmed by grep gate (`grep -r 'com.smellouk.kamper.gpu.cinterop' iosMain/ tvosMain/` → 0 results).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Compiler warning] Removed @KamperDslMarker from fun GpuModule**
- **Found during:** Task 1, compileKotlinMacosArm64
- **Issue:** Plan template included `@KamperDslMarker` on the top-level `fun GpuModule`. Kotlin compiler warns this annotation has no effect on top-level functions (KT-81567). Applying DSL marker annotations to non-type targets is a no-op.
- **Fix:** Removed `@KamperDslMarker` from the function and the now-unused import. Both macosMain and iOS/tvOS Module.kt files omit this annotation, matching CPU's iosMain/tvosMain pattern.
- **Impact:** None — behavior unchanged. Eliminates compiler warning.

**2. [Rule 3 - Blocking] node_modules symlink recreated for worktree**
- **Found during:** Pre-task setup
- **Issue:** The worktree did not carry the `demos/react-native/node_modules` symlink required for Gradle composite build resolution (same issue documented in 23-01-SUMMARY.md).
- **Fix:** `ln -s /Users/smellouk/Developer/git/kamper/demos/react-native/node_modules demos/react-native/node_modules` — worktree-local runtime fix, not committed.

**3. [Rule 2 - Canonical reference] mainDispatcher=Dispatchers.Default instead of Dispatchers.Main**
- **Found during:** Task 2 implementation
- **Issue:** Plan template showed `mainDispatcher = Dispatchers.Main` for iosMain. The canonical CpuModule iosMain/tvosMain uses `Dispatchers.Default` for both dispatchers.
- **Fix:** Used `Dispatchers.Default` for `mainDispatcher` on all three Apple platforms, matching CpuModule — the authoritative reference per CLAUDE.md.

## Known Stubs

None. All seven files are production-ready implementations (macOS with real cinterop data, iOS/tvOS with intentional UNSUPPORTED sentinel — not stubs but correct platform behavior).

## Threat Flags

No new network endpoints, auth paths, or schema changes introduced.

Threat register mitigations from plan verified as implemented:

| Threat ID | Mitigation Status |
|-----------|------------------|
| T-23-13 | MITIGATED — cinterop registered only for macosArm64/macosX64 in build.gradle.kts (Plan 01); grep gate confirms zero cinterop imports in iosMain/tvosMain |
| T-23-14 | MITIGATED — C body clamps to 100.0; MacosGpuInfoSource maps any negative to UNSUPPORTED/INVALID |
| T-23-15 | MITIGATED — `try { ... } catch (_: Exception) { GpuInfo.INVALID }` absorbs all exceptions |

## Verification Results

```
./gradlew :libs:modules:gpu:compileKotlinMacosArm64 -q   # PASS (no output)
./gradlew :libs:modules:gpu:compileKotlinIosArm64 -q     # PASS (no output)
./gradlew :libs:modules:gpu:compileKotlinTvosArm64 -q    # PASS (no output)
grep -r 'com.smellouk.kamper.gpu.cinterop' iosMain/ tvosMain/ | wc -l  # 0 (D-07 gate PASS)
```

## Self-Check: PASSED

Files created:
- libs/modules/gpu/src/macosMain/kotlin/com/smellouk/kamper/gpu/Module.kt: FOUND
- libs/modules/gpu/src/macosMain/kotlin/com/smellouk/kamper/gpu/repository/GpuInfoRepositoryImpl.kt: FOUND
- libs/modules/gpu/src/macosMain/kotlin/com/smellouk/kamper/gpu/repository/source/MacosGpuInfoSource.kt: FOUND
- libs/modules/gpu/src/iosMain/kotlin/com/smellouk/kamper/gpu/Module.kt: FOUND
- libs/modules/gpu/src/iosMain/kotlin/com/smellouk/kamper/gpu/repository/GpuInfoRepositoryImpl.kt: FOUND
- libs/modules/gpu/src/tvosMain/kotlin/com/smellouk/kamper/gpu/Module.kt: FOUND
- libs/modules/gpu/src/tvosMain/kotlin/com/smellouk/kamper/gpu/repository/GpuInfoRepositoryImpl.kt: FOUND

Commits verified:
- 51c6399: feat(gpu): add macosMain actual — IOKit cinterop via MacosGpuInfoSource
- 973759b: feat(gpu): add iosMain + tvosMain UNSUPPORTED stubs (D-07 / App Store safety)
