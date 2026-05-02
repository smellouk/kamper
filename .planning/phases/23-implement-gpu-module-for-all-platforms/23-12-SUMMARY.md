---
phase: 23
plan: 12
subsystem: demos/macos
tags: [gpu, macos, appkit, nsview, demo]
dependency_graph:
  requires: [23-04]
  provides: [macOS-demo-gpu-wired]
  affects: [demos/macos]
tech_stack:
  added: []
  patterns: [ThermalView-mirror, NSView-lifecycle, Kamper-listener-pattern]
key_files:
  created:
    - demos/macos/src/macosMain/kotlin/com/smellouk/kamper/macos/ui/GpuView.kt
  modified:
    - demos/macos/build.gradle.kts
    - demos/macos/src/macosMain/kotlin/com/smellouk/kamper/macos/Main.kt
decisions:
  - GpuView omits stress button (no GPU stress simulator; thermal demo stress is CPU-only)
  - Sep separator pinned to bottomAnchor with -10.0 constant (no footer button to anchor to)
  - GpuModule installed after CpuModule (alphabetical proximity preserved)
metrics:
  duration: "~8 minutes"
  completed: "2026-05-02"
  tasks_completed: 2
  files_modified: 3
---

# Phase 23 Plan 12: macOS Demo GPU Wiring Summary

Wire GpuModule and GpuView (AppKit NSView) into the macOS AppKit demo, inserting GPU between CPU and FPS per D-10 tab order with three-state rendering (valid/UNSUPPORTED/INVALID).

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Create GpuView.kt (AppKit NSView) | 8fbcf2e | demos/macos/src/macosMain/kotlin/com/smellouk/kamper/macos/ui/GpuView.kt |
| 2 | Wire GpuView into KamperDemoWindow + install GpuModule + add :libs:modules:gpu dep | a8dbb0f | demos/macos/build.gradle.kts, demos/macos/src/macosMain/kotlin/com/smellouk/kamper/macos/Main.kt |

## Compile Status

- `./gradlew :demos:macos:compileKotlinMacosArm64` — PASS (warnings only, all pre-existing)
- `./gradlew :demos:macos:compileKotlinMacosX64` — PASS (warnings only, all pre-existing)
- `./gradlew detekt` — PASS (zero issues)

## Runtime Behavior (Expected)

On hosts where `IOAccelerator` service is available (bare-metal macOS with GPU):
- `bigLabel` shows `"X.X%"` in `Theme.MAUVE`, updated at 1 s interval
- `memoryLabel` shows `"Memory:  N/A"` (IOAccelerator PerformanceStatistics does not expose VRAM totals via this path per 23-04-SUMMARY)

On sandboxed app or no GPU service:
- `bigLabel` shows `"Unsupported"` in `Theme.MUTED`
- `memoryLabel` shows `"Memory:  N/A"`

On transient read failure (INVALID):
- `update()` returns early — last valid display value is preserved

## Key Changes

### GpuView.kt (90 lines)
- Mirrors `ThermalView.kt` exactly (header opt-in annotation, `@OverrideInit constructor`, NSTextField fields)
- Three fields: `bigLabel` (48pt monospaced bold), `unitLabel` ("GPU usage %"), `memoryLabel`
- `update(info: GpuInfo)`: guard INVALID → UNSUPPORTED → valid branch with defensive `< 0` check
- `drawRect` fills background with `Theme.BASE` (consistent with all other demo views)
- No stress button (GPU has no simulator)

### Main.kt
- Added imports: `GpuInfo`, `GpuModule`, `GpuView`
- `gpuView` field between `cpuView` and `fpsView`
- `buildTabView()`: GPU tab at position 1 (CPU=0, GPU=1, FPS=2, ..., Thermal=8); `segmentCount = 9`
- `setupKamper()`: `install(GpuModule)` after `install(CpuModule)`; `addInfoListener<GpuInfo> { gpuView.update(it) }`

### build.gradle.kts
- `implementation(project(":libs:modules:gpu"))` inserted between `:gc` and `:thermal` (alphabetical)

## Deviations from Plan

None — plan executed exactly as written.

## Known Stubs

None — GpuView renders live data from MacosGpuInfoSource (or UNSUPPORTED when IOAccelerator is absent). The memory fields display "N/A" by design (not a stub) because IOAccelerator does not expose VRAM totals via the PerformanceStatistics path documented in 23-04-SUMMARY.

## Threat Flags

None — no new network endpoints, auth paths, or trust boundary changes.

## Self-Check: PASSED

- GpuView.kt exists: FOUND
- build.gradle.kts contains gpu dep: FOUND
- Main.kt contains install(GpuModule): FOUND
- Main.kt contains addInfoListener<GpuInfo>: FOUND
- Commit 8fbcf2e exists: FOUND
- Commit a8dbb0f exists: FOUND
