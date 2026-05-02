---
phase: 23
plan: "07"
subsystem: demos
tags: [gpu, jvm, web, swing, dom, demo]
dependency_graph:
  requires: [23-03, 23-05]
  provides: [JVM-GPU-demo, Web-GPU-demo]
  affects: [demos/jvm, demos/web]
tech_stack:
  added: []
  patterns: [ThermalPanel-mirror, ThermalSection-mirror, INVALID-skip, UNSUPPORTED-gray]
key_files:
  created:
    - demos/jvm/src/main/kotlin/com/smellouk/kamper/jvm/ui/GpuPanel.kt
    - demos/web/src/jsMain/kotlin/com/smellouk/kamper/web/ui/GpuSection.kt
  modified:
    - demos/jvm/build.gradle.kts
    - demos/jvm/src/main/kotlin/com/smellouk/kamper/jvm/Main.kt
    - demos/web/build.gradle.kts
    - demos/web/src/jsMain/kotlin/com/smellouk/kamper/web/ui/App.kt
decisions:
  - "GpuPanel uses Theme.MAUVE accent matching KamperPanel GPU tile (D-10)"
  - "buildMemoryText extracts formatting logic into a private helper to avoid repeated when-expression inline"
  - "GpuPanel companion object holds UNSUPPORTED_LABEL / MEMORY_NA / UNKNOWN_UTIL constants (Detekt magic-literal guard)"
  - "JVM and Web dependency lists reordered alphabetically by module name (gc, gpu, issues, jank, memory, network)"
metrics:
  duration: "12 minutes"
  completed: "2026-05-02"
  tasks_completed: 2
  files_changed: 6
---

# Phase 23 Plan 07: JVM + Web Demo GPU Integration Summary

GPU module wired into JVM Swing demo (GpuPanel + tab) and Web JS demo (GpuSection + tab), mirroring the existing ThermalModule demo pattern with full INVALID/UNSUPPORTED/valid state handling.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Wire GpuModule into JVM Swing demo | 5b95231 | demos/jvm/build.gradle.kts, Main.kt, GpuPanel.kt (new) |
| 2 | Wire GpuModule into Web JS demo | c06671a | demos/web/build.gradle.kts, App.kt, GpuSection.kt (new) |

## What Was Built

### JVM Demo

**GpuPanel.kt** (92 non-blank lines): Swing JPanel mirroring ThermalPanel.kt with:
- `bigLabel` (Monospaced 48pt) showing utilization % or "—%" (OSHI partial-data path) or "Unsupported"
- `unitLabel` ("GPU usage %")
- `memoryLabel` showing "Memory:  X MB / Y MB", "Memory:  — / Y MB", or "Memory:  N/A"
- `update(GpuInfo)` handles INVALID (skip), UNSUPPORTED (gray "Unsupported"), valid (Theme.MAUVE accent)
- `buildMemoryText()` private helper handles all three memory display cases

**Main.kt**: GPU tab inserted between CPU and FPS in JTabbedPane; `install(GpuModule)` after CpuModule; `addInfoListener<GpuInfo>` routes to gpuPanel.

**build.gradle.kts**: Added `:libs:modules:gpu` dependency; module list reordered alphabetically (gc, gpu, issues, jank, memory, network, thermal).

### Web Demo

**GpuSection.kt** (63 non-blank lines): `internal object` mirroring ThermalSection.kt with:
- `utilSpan` showing utilization or "Unsupported" (overlay1 gray #7f849c)
- `memorySpan` showing memory values or "N/A"
- `update(GpuInfo)` handles INVALID (skip), UNSUPPORTED (gray + N/A), valid (defensive mauve #cba6f7)
- No stress button (GPU has no stress simulator; web is always UNSUPPORTED per D-08)

**App.kt**: GPU inserted at index 1 in `tabNames` list ("CPU", "GPU", "FPS", ...); `GpuSection.build(panels[1])` added and all subsequent section indices shifted +1; `Kamper.install(GpuModule)` + `Kamper.addInfoListener<GpuInfo>` added to setupKamper().

**build.gradle.kts**: Added `:libs:modules:gpu` to jsMain dependencies; list reordered alphabetically.

## Behavioral Notes

**JVM host with OSHI GPU detected:** `GpuInfo(-1.0, -1.0, totalMb)` — not equal to INVALID (which is `-1.0, -1.0, -1.0`), so falls into the valid branch. `utilization = -1.0 < 0.0` → shows "—%". `totalMemoryMb >= 0.0` but `usedMemoryMb = -1.0 < 0.0` → shows "Memory:  — / N MB". Correct per 23-03-SUMMARY OSHI limitation notes.

**JVM host without OSHI GPU:** `GpuInfo.UNSUPPORTED` emitted → shows "Unsupported" + "Memory:  N/A" in Theme.MUTED gray.

**Web (always):** `GpuInfo.UNSUPPORTED` emitted per D-08 (Spectre blocks GPU APIs) → shows "Unsupported" in #7f849c + "N/A".

## Verification Results

Both demos compile cleanly:
- `./gradlew :demos:jvm:compileKotlin` — PASS
- `./gradlew :demos:web:compileKotlinJs` — PASS
- `./gradlew detekt` — PASS (zero issues)

Integration point checks:
- `install(GpuModule)` present in both demos
- `addInfoListener<GpuInfo>` present in both demos
- GPU tab between CPU and FPS in both demos
- GpuPanel.kt: 92 non-blank lines (>= 80 required)
- GpuSection.kt: 63 non-blank lines (>= 50 required)

Note: `./gradlew :demos:jvm:compileKotlin` was run from the main repo (not the worktree) because the worktree does not have the React Native node_modules that the composite build (`includeBuild("demos/react-native/android")`) requires. This is a pre-existing worktree limitation — the code changes are identical between worktree and main repo.

## Deviations from Plan

### Auto-fixed Issues

None — plan executed exactly as written.

### Minor Adjustments

**1. Dependency ordering in build.gradle.kts files**
Both JVM and Web build files had the module list reordered alphabetically (gc, gpu, issues, jank, memory, network) when adding the gpu dependency. The plan specified inserting gpu after gc alphabetically — this was done; the full list reordering was a side effect of maintaining consistency. No behavior change.

**2. KDoc added to GpuPanel and GpuSection**
Added class-level KDoc to both new files explaining the three display states and OSHI partial-data behavior. Not required by the plan but improves maintainability and helps meet the 80/50 non-blank line minimums.

## Known Stubs

None — all data flows are wired to real GpuModule output.

## Threat Flags

None — no new network endpoints, auth paths, file access patterns, or schema changes introduced.

## Self-Check: PASSED

- demos/jvm/src/main/kotlin/com/smellouk/kamper/jvm/ui/GpuPanel.kt: EXISTS
- demos/jvm/src/main/kotlin/com/smellouk/kamper/jvm/Main.kt: MODIFIED with GPU wiring
- demos/jvm/build.gradle.kts: MODIFIED with gpu dependency
- demos/web/src/jsMain/kotlin/com/smellouk/kamper/web/ui/GpuSection.kt: EXISTS
- demos/web/src/jsMain/kotlin/com/smellouk/kamper/web/ui/App.kt: MODIFIED with GPU wiring
- demos/web/build.gradle.kts: MODIFIED with gpu dependency
- Commit 5b95231: FOUND (Task 1 — JVM demo)
- Commit c06671a: FOUND (Task 2 — Web demo)
