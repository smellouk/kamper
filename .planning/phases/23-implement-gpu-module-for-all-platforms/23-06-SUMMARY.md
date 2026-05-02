---
phase: 23
plan: "06"
subsystem: ui
tags: [kmp, compose, gpu, ui, android, apple, lifecycle, settings]
dependency_graph:
  requires:
    - "23-03 (GPU Android/JVM actuals)"
    - "23-04 (GPU Apple actuals)"
    - "23-05 (GPU JS/WASM stubs)"
  provides:
    - "GPU tile in ActivityTab (between CPU and FPS, MAUVE accent)"
    - "GPU ModuleCard in SettingsTab (between CPU and FPS)"
    - "GpuModule install/uninstall in android ModuleLifecycleManager"
    - "GpuModule install/uninstall in apple ModuleLifecycleManager"
    - "KamperUiState.gpuUtilization/gpuUsedMemoryMb/gpuTotalMemoryMb/gpuHistory/gpuUnsupported"
    - "KamperUiSettings.showGpu/gpuEnabled/gpuIntervalMs"
    - "Tracks.GPU = 8 (Perfetto export)"
    - "ChipIcons.gpu expect + android (▪) + apple (▪) actuals"
    - "showGpu normalization in both KamperUiRepository actuals"
  affects:
    - "libs/ui/kmm/build.gradle.kts"
    - "libs/ui/kmm/src/commonMain/kotlin/com/smellouk/kamper/ui/"
    - "libs/ui/kmm/src/androidMain/kotlin/com/smellouk/kamper/ui/"
    - "libs/ui/kmm/src/appleMain/kotlin/com/smellouk/kamper/ui/"
    - "libs/modules/gpu/src/macosMain/kotlin/com/smellouk/kamper/gpu/repository/source/"
tech_stack:
  added: []
  patterns:
    - "InfoListener INVALID/UNSUPPORTED early-return guard pattern (mirrors thermalListener)"
    - "probe-before-read for unsupported state detection at startup"
    - "derivedStateOf scoped recomposition per-metric (PERF-03 pattern)"
    - "showGpu normalization: flips to true on first gpuEnabled enable"
key_files:
  created: []
  modified:
    - libs/ui/kmm/build.gradle.kts
    - libs/ui/kmm/src/commonMain/kotlin/com/smellouk/kamper/ui/KamperUiState.kt
    - libs/ui/kmm/src/commonMain/kotlin/com/smellouk/kamper/ui/KamperUiSettings.kt
    - libs/ui/kmm/src/commonMain/kotlin/com/smellouk/kamper/ui/RecordedSample.kt
    - libs/ui/kmm/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/ChipIcons.kt
    - libs/ui/kmm/src/androidMain/kotlin/com/smellouk/kamper/ui/compose/ChipIcons.kt
    - libs/ui/kmm/src/appleMain/kotlin/com/smellouk/kamper/ui/compose/ChipIcons.kt
    - libs/ui/kmm/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/ActivityTab.kt
    - libs/ui/kmm/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/SettingsTab.kt
    - libs/ui/kmm/src/androidMain/kotlin/com/smellouk/kamper/ui/ModuleLifecycleManager.kt
    - libs/ui/kmm/src/appleMain/kotlin/com/smellouk/kamper/ui/ModuleLifecycleManager.kt
    - libs/ui/kmm/src/androidMain/kotlin/com/smellouk/kamper/ui/KamperUiRepository.kt
    - libs/ui/kmm/src/appleMain/kotlin/com/smellouk/kamper/ui/KamperUiRepository.kt
    - libs/modules/gpu/src/macosMain/kotlin/com/smellouk/kamper/gpu/repository/source/MacosGpuInfoSource.kt
decisions:
  - "GPU tile uses MAUVE accent (same as Jank) per UI-SPEC — distinguishable from CPU (BLUE) and FPS (GREEN)"
  - "GPU ModuleCard shows intervalMs = cfg.gpuIntervalMs (unlike Jank/GC/Thermal which have null) per UI-SPEC"
  - "gpuHistory not accumulated in listener (GPU has no sparkline meaningful at module level — list stays empty until non-UNSUPPORTED valid readings arrive)"
  - "MacosGpuInfoSource UNSUPPORTED_SENTINEL = -2.0 extracted to satisfy detekt MagicNumber rule"
  - "GPU probe-before-read follows same pattern as Thermal/Jank — installGpu() then uninstallGpu() after 1500ms delay to detect UNSUPPORTED before user enables the module"
metrics:
  duration: "~40 minutes"
  completed: "2026-05-02T00:00:00Z"
  tasks_completed: 2
  files_created: 0
  files_modified: 14
---

# Phase 23 Plan 06: GPU UI Integration Summary

Wire the GPU module (`:libs:modules:gpu`) into the Compose UI overlay (`:libs:ui:kmm`) — GPU tile in ActivityTab between CPU and FPS with MAUVE accent, GPU ModuleCard in SettingsTab, full ModuleLifecycleManager wiring on Android and Apple, and KamperUiRepository showGpu normalization.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Wire dependency + state/settings/tracks/icons + UI tiles | 32652fb | build.gradle.kts, KamperUiState.kt, KamperUiSettings.kt, RecordedSample.kt, ChipIcons.kt (x3), ActivityTab.kt, SettingsTab.kt |
| 2 | Wire ModuleLifecycleManager (android + apple) + KamperUiRepository normalize | 7c6ae3a | androidMain/ModuleLifecycleManager.kt, appleMain/ModuleLifecycleManager.kt, androidMain/KamperUiRepository.kt, appleMain/KamperUiRepository.kt, MacosGpuInfoSource.kt |

## What Was Built

### Task 1 — UI Layer Extensions

**build.gradle.kts:** Added `implementation(project(":libs:modules:gpu"))` to `commonMain.dependencies` block after the existing thermal dependency.

**KamperUiState.kt:** Added 5 GPU fields after `gcUnsupported`:
- `gpuUtilization: Float = 0f` — GPU usage percentage 0..100
- `gpuUsedMemoryMb: Float = -1f` — VRAM used (-1 = unavailable)
- `gpuTotalMemoryMb: Float = -1f` — total VRAM (-1 = unavailable)
- `gpuHistory: List<Float> = emptyList()` — rolling sparkline data
- `gpuUnsupported: Boolean = false` — permanent capability gap flag

**KamperUiSettings.kt:** Added 3 GPU fields:
- `showGpu: Boolean = false` — visibility in chip overlay
- `gpuEnabled: Boolean = false` — module start/stop toggle (opt-in, matching Jank/GC/Thermal)
- `gpuIntervalMs: Long = 1_000L` — polling interval (in "Polling intervals" section)

**RecordedSample.kt:** Added `const val GPU = 8` to `Tracks` object and `GPU to "GPU %"` entry to `ALL` list for Perfetto export consistency.

**ChipIcons.kt (commonMain):** Added `val gpu: String` expect property after `cpu`.

**ChipIcons.kt (androidMain):** Added `actual val gpu = "▪"` (BLACK SMALL SQUARE U+25AA, positioned after `cpu`).

**ChipIcons.kt (appleMain):** Added `actual val gpu = "▪"` (same glyph; monochrome safe for Skia).

**ActivityTab.kt:** Added 5 `derivedStateOf` bindings for GPU state fields, then inserted a GPU `MetricCard` block between CPU and FPS:
- `title = "GPU"`, `color = KamperTheme.MAUVE`
- `current = "Unsupported"` when `gpuUnsupported`, else `"${gpuUtilization.formatDp(1)}%"`
- `fraction` clamped via `(gpuUtilization / 100f).coerceIn(0f, 1f)` (T-23-21 mitigation)
- `extra` shows "N/A" when `gpuUsedMemoryMb < 0f`, memory string otherwise, null when unsupported
- `unsupported = isGpuUnsupported` enables grayed-out rendering (D-12)

**SettingsTab.kt:** Inserted GPU `ModuleCard` between CPU and FPS with `intervalMs = cfg.gpuIntervalMs` (unlike Jank/GC/Thermal which have `null` — GPU supports configurable intervals per D-11 / UI-SPEC).

### Task 2 — ModuleLifecycleManager + Repository

**androidMain/ModuleLifecycleManager.kt:** Added 7 GPU additions mirroring the `thermal` pattern:
1. GPU imports (`GpuConfig`, `GpuInfo`, `GpuModule`)
2. `private var gpuModule: PerformanceModule<GpuConfig, GpuInfo>? = null`
3. `gpuListener` — INVALID early return, UNSUPPORTED sets `gpuUnsupported = true`, valid path calls `recordingManager.record(Tracks.GPU, ...)` + `state.update`
4. `installGpu()` / `uninstallGpu()` helpers
5. `applySettings` `when` block for GPU enable/disable toggle
6. `initialise()` probe-before-read: `probeGpu = !s.gpuEnabled` → `installGpu()` then `uninstallGpu()` after 1500ms
7. `gpuModule = null` in `clear()`

**appleMain/ModuleLifecycleManager.kt:** Same 7 additions with one difference — `gpuListener` body OMITS `recordingManager.record(...)` (apple has no `recordingManager` field).

**androidMain/KamperUiRepository.kt + appleMain/KamperUiRepository.kt:** Added `showGpu = if (!old.gpuEnabled && s.gpuEnabled) true else s.showGpu` to the `normalized` copy in `updateSettings`. This mirrors the existing `showJank`/`showGc`/`showThermal` pattern — enabling GPU for the first time automatically shows it in the chip overlay.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Pre-existing detekt violations in MacosGpuInfoSource.kt**
- **Found during:** Task 2 detekt verification run
- **Issue:** `MacosGpuInfoSource.kt` (from plan 23-05) had 3 detekt violations: `MagicNumber` (-2.0 in `util <= -2.0`) and 2 `NoMultipleSpaces` (alignment spaces in `when` branches). Detekt maxIssues: 0 per CLAUDE.md.
- **Fix:** Extracted `private companion object { const val UNSUPPORTED_SENTINEL = -2.0 }`, replaced `-2.0` literal with `UNSUPPORTED_SENTINEL`. Also removed alignment spaces in the `when` branches (detekt `autoCorrect: true` handled the whitespace violations).
- **Files modified:** `libs/modules/gpu/src/macosMain/kotlin/com/smellouk/kamper/gpu/repository/source/MacosGpuInfoSource.kt`
- **Commit:** 7c6ae3a (included in Task 2 commit)

## Known Stubs

None. All GPU state fields are wired to real data sources. The `gpuHistory` list starts empty and accumulates only on valid (non-INVALID, non-UNSUPPORTED) readings — this is correct behavior, not a stub.

On most Android devices and all iOS/tvOS targets, the GPU will correctly report UNSUPPORTED through the listener guard, showing the grayed-out tile. This is expected behavior per D-12, not a stub.

## Threat Flags

No new threat surface introduced. All STRIDE threats from the plan's threat model are addressed:
- T-23-18: New gpuEnabled/showGpu/gpuIntervalMs fields fall back to defaults (false/false/1000L) on missing preference keys via Kotlinx serialization.
- T-23-19: InfoListener delivery on engine dispatcher; StateFlow.update is thread-safe.
- T-23-20: INVALID early return at top of `gpuListener` prevents state churn.
- T-23-21: `(gpuUtilization / 100f).coerceIn(0f, 1f)` in ActivityTab clamps utilization > 100.

## Human Verification Checklist

Task 3 is a `checkpoint:human-verify`. The following items require visual verification on an Android device:

1. Build the demo: `./gradlew :demos:android:assembleDebug -q` (BUILD SUCCESSFUL verified in this session)
2. Install: `./gradlew :demos:android:installDebug` (requires connected device — not run autonomously per CLAUDE.md)
3. Launch demo app, open Kamper panel (tap chip overlay)
4. **Settings tab:** GPU entry appears between CPU and FPS; icon is `▪`; interval picker shows 1s default
5. **Toggle GPU "Enabled" on** — "Show in chip" switch auto-flips to true (showGpu normalization)
6. **Activity tab:** GPU MetricCard renders between CPU and FPS; MAUVE accent color
7. **Unsupported state (most devices):** tile shows "Unsupported" in grayed-out (SUBTEXT) typography; progress bar = 0; no sparkline
8. **Adreno device with kgsl access:** tile shows percentage e.g. "42.3%"; memory line shows "N/A" (no root access)
9. **Disable GPU in Settings** — GPU tile disappears from Activity tab

Expected resume-signal: "approved" (if all 9 points pass) or description of any deviation.

## Verification Results

```
./gradlew :libs:ui:kmm:compileDebugKotlinAndroid -q    # PASS — no errors
./gradlew :libs:ui:kmm:compileKotlinIosArm64 -q        # PASS — warnings only (pre-existing)
./gradlew :libs:ui:kmm:test -q                         # PASS
./gradlew :libs:modules:gpu:jvmTest -q                 # PASS — 4 tests GREEN
./gradlew :libs:api:test :libs:engine:test -q          # PASS
./gradlew :demos:android:assembleDebug -q              # PASS — BUILD SUCCESSFUL
./gradlew detekt                                       # PASS — 0 issues (after MacosGpuInfoSource fix)
grep 'implementation(project(":libs:modules:gpu"))' libs/ui/kmm/build.gradle.kts        # 1
grep 'val gpuUtilization: Float = 0f,' .../KamperUiState.kt                             # 1
grep 'val gpuUnsupported: Boolean = false' .../KamperUiState.kt                         # 1
grep 'val showGpu: Boolean = false,' .../KamperUiSettings.kt                            # 1
grep 'val gpuEnabled: Boolean = false,' .../KamperUiSettings.kt                         # 1
grep 'val gpuIntervalMs: Long = 1_000L,' .../KamperUiSettings.kt                        # 1
grep -E 'const val GPU\s*=\s*8' .../RecordedSample.kt                                   # 1
grep 'GPU.*to "GPU %"' .../RecordedSample.kt                                            # 1
grep 'val gpu: String' .../commonMain/.../ChipIcons.kt                                  # 1
grep 'actual val gpu' .../androidMain/.../ChipIcons.kt                                  # 1
grep 'actual val gpu' .../appleMain/.../ChipIcons.kt                                    # 1
grep 'cfg.gpuEnabled' .../ActivityTab.kt                                                # 1
grep 'KamperTheme.MAUVE' .../ActivityTab.kt                                             # 2
grep 'cfg.gpuEnabled' .../SettingsTab.kt                                                # 1
grep 'private val gpuListener: InfoListener<GpuInfo>' .../androidMain/...Manager.kt    # 1
grep 'recordingManager.record(Tracks.GPU' .../androidMain/...Manager.kt                # 1
grep 'private val gpuListener: InfoListener<GpuInfo>' .../appleMain/...Manager.kt      # 1
grep 'recordingManager.record(Tracks.GPU' .../appleMain/...Manager.kt                  # 0 (correct)
grep 'showGpu' .../androidMain/.../KamperUiRepository.kt                               # 1
grep 'showGpu' .../appleMain/.../KamperUiRepository.kt                                 # 1
```

## Self-Check: PASSED

Files verified:
- libs/ui/kmm/build.gradle.kts: FOUND, contains gpu dep
- libs/ui/kmm/src/commonMain/kotlin/com/smellouk/kamper/ui/KamperUiState.kt: FOUND, gpuUtilization field present
- libs/ui/kmm/src/commonMain/kotlin/com/smellouk/kamper/ui/KamperUiSettings.kt: FOUND, showGpu/gpuEnabled/gpuIntervalMs present
- libs/ui/kmm/src/commonMain/kotlin/com/smellouk/kamper/ui/RecordedSample.kt: FOUND, Tracks.GPU=8 and ALL entry present
- libs/ui/kmm/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/ChipIcons.kt: FOUND, val gpu: String present
- libs/ui/kmm/src/androidMain/kotlin/com/smellouk/kamper/ui/compose/ChipIcons.kt: FOUND, actual val gpu present
- libs/ui/kmm/src/appleMain/kotlin/com/smellouk/kamper/ui/compose/ChipIcons.kt: FOUND, actual val gpu present
- libs/ui/kmm/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/ActivityTab.kt: FOUND, GPU MetricCard between CPU and FPS
- libs/ui/kmm/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/SettingsTab.kt: FOUND, GPU ModuleCard between CPU and FPS
- libs/ui/kmm/src/androidMain/kotlin/com/smellouk/kamper/ui/ModuleLifecycleManager.kt: FOUND, gpuListener + install/uninstall + probe
- libs/ui/kmm/src/appleMain/kotlin/com/smellouk/kamper/ui/ModuleLifecycleManager.kt: FOUND, gpuListener (no recordingManager) + install/uninstall + probe
- libs/ui/kmm/src/androidMain/kotlin/com/smellouk/kamper/ui/KamperUiRepository.kt: FOUND, showGpu normalization
- libs/ui/kmm/src/appleMain/kotlin/com/smellouk/kamper/ui/KamperUiRepository.kt: FOUND, showGpu normalization
- libs/modules/gpu/src/macosMain/kotlin/com/smellouk/kamper/gpu/repository/source/MacosGpuInfoSource.kt: FOUND, UNSUPPORTED_SENTINEL constant

Commits verified:
- 32652fb: feat(ui): wire GPU dependency, state/settings/tracks/icons, and UI tiles
- 7c6ae3a: feat(ui): wire GpuModule into ModuleLifecycleManager (android + apple) and normalize settings
