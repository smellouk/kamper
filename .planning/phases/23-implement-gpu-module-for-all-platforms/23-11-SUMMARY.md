---
phase: 23
plan: 11
subsystem: demos
tags: [gpu, ios, tvos, demo, uikit]
dependency_graph:
  requires: [23-04]
  provides: [ios-gpu-demo, tvos-gpu-demo]
  affects: [demos/ios, demos/tvos]
tech_stack:
  added: []
  patterns: [UIKit-ViewController-pattern, dispatch_async-main-queue-listener]
key_files:
  created:
    - demos/ios/src/iosMain/kotlin/com/smellouk/kamper/ios/ui/GpuViewController.kt
    - demos/tvos/src/tvosMain/kotlin/com/smellouk/kamper/tvos/ui/GpuViewController.kt
  modified:
    - demos/ios/build.gradle.kts
    - demos/ios/src/iosMain/kotlin/com/smellouk/kamper/ios/Main.kt
    - demos/tvos/build.gradle.kts
    - demos/tvos/src/tvosMain/kotlin/com/smellouk/kamper/tvos/Main.kt
decisions:
  - "Use systemImageNamed('display') for GPU tab bar icon (iOS 16 baseline safe)"
  - "GpuViewController omits stress button — no GPU stress simulator exists"
  - "hintLabel text: 'GPU access restricted on iOS/tvOS — see ADR-007'"
metrics:
  duration: ~25 minutes
  completed: 2026-05-02
  tasks_completed: 2
  files_changed: 6
---

# Phase 23 Plan 11: iOS + tvOS GPU Demo Wiring Summary

**One-liner:** Wire GpuModule into iOS UITabBarController and tvOS UISegmentedControl demos with GpuViewController rendering the permanent UNSUPPORTED state per D-07 App Store safety.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | iOS demo — build.gradle.kts dep + GpuViewController.kt + Main.kt wiring | af9730b | demos/ios/build.gradle.kts, GpuViewController.kt (new), Main.kt |
| 2 | tvOS demo — build.gradle.kts dep + GpuViewController.kt + Main.kt wiring | 4e0275a | demos/tvos/build.gradle.kts, GpuViewController.kt (new), Main.kt |

## Compile Status

| Target | Status |
|--------|--------|
| :demos:ios:compileKotlinIosArm64 | PASS |
| :demos:ios:compileKotlinIosSimulatorArm64 | PASS |
| :demos:tvos:compileKotlinTvosArm64 | PASS |
| :demos:tvos:compileKotlinTvosSimulatorArm64 | PASS |

## Detekt Status

`./gradlew detekt` — BUILD SUCCESSFUL (zero issues)

## D-07 Cinterop Guard

`grep -r 'com.smellouk.kamper.gpu.cinterop' demos/ios demos/tvos` — 0 results. No GPU cinterop imports in iosMain or tvosMain demo code.

## Wiring Points Verified

| Check | Result |
|-------|--------|
| `install(GpuModule)` in iOS Main.kt | 1 occurrence |
| `install(GpuModule)` in tvOS Main.kt | 1 occurrence |
| `addInfoListener<GpuInfo>` in iOS Main.kt | 1 occurrence |
| `addInfoListener<GpuInfo>` in tvOS Main.kt | 1 occurrence |
| iOS GpuViewController.kt exists | yes |
| tvOS GpuViewController.kt exists | yes |
| tvOS TAB_TITLES contains "CPU", "GPU", "FPS" in order | yes |
| `:libs:modules:gpu` in iOS build.gradle.kts | yes |
| `:libs:modules:gpu` in tvOS build.gradle.kts | yes |

## Implementation Notes

- Both GpuViewController files mirror ThermalViewController but omit the stress button (no GPU stress simulator)
- iOS version: bigLabel 48pt monospaced, pad=20.0 (matches iOS Thermal scale)
- tvOS version: bigLabel 100pt monospaced, pad=80.0 (matches tvOS Thermal scale)
- `update(info: GpuInfo)` handles all three states: INVALID (early return), UNSUPPORTED (grayed labels), valid data (MAUVE color + formatted memory)
- Since iosMain and tvosMain actuals return UNSUPPORTED unconditionally, the UI permanently shows "Unsupported" / "Memory: N/A"
- gpuVC placed between cpuVC and fpsVC in both iOS tab bar and tvOS segmented control

## Deviations from Plan

None — plan executed exactly as written.

## Known Stubs

None — GpuViewController always shows "Unsupported" which is the correct permanent state per D-07. This is intentional, not a stub.

## Self-Check: PASSED

- `demos/ios/src/iosMain/kotlin/com/smellouk/kamper/ios/ui/GpuViewController.kt` — FOUND
- `demos/tvos/src/tvosMain/kotlin/com/smellouk/kamper/tvos/ui/GpuViewController.kt` — FOUND
- Commit af9730b — FOUND (feat(gpu): wire GpuModule into iOS demo)
- Commit 4e0275a — FOUND (feat(gpu): wire GpuModule into tvOS demo)
