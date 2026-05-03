---
phase: 26-flutter-support-package-and-demo
plan: "04"
subsystem: demos/flutter
tags: [flutter, demo, dart, konitor, catppuccin, platform-channels]
dependency_graph:
  requires: [26-01, 26-02, 26-03]
  provides: [demos/flutter project, demos/flutter/lib/main.dart, demos/flutter/android/settings.gradle]
  affects: [26-05, 26-06]
tech_stack:
  added: [http: ^1.1.0]
  patterns: [DefaultTabController, StatefulWidget StreamSubscription lifecycle, Platform.isAndroid conditional tabs]
key_files:
  created:
    - demos/flutter/ (full Flutter project, 66 files via flutter create)
    - demos/flutter/lib/main.dart
  modified:
    - demos/flutter/pubspec.yaml
    - demos/flutter/android/settings.gradle
decisions:
  - Used http package for network speed test (Cloudflare 5 MB)
  - StatefulWidget _subs List<StreamSubscription> cancelled in dispose for clean lifecycle
  - Platform.isAndroid conditional used for tab list and stream subscriptions
metrics:
  duration: ~12 minutes
  completed: 2026-05-03
  tasks_completed: 2
  tasks_total: 2
  files_created: 67
  files_modified: 2
---

# Phase 26 Plan 04: Flutter Demo App Summary

**One-liner:** Flutter demo app at demos/flutter/ with 10-tab Android / 4-tab iOS UI using Catppuccin Mocha theme, all Konitor metric streams, and interactive stress/test features.

---

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Scaffold Flutter demo project structure and pubspec.yaml + android settings | 813137c | demos/flutter/ (66 files), pubspec.yaml, android/settings.gradle |
| 2 | Implement demos/flutter/lib/main.dart with all metric tabs | 6103124 | demos/flutter/lib/main.dart, demos/flutter/pubspec.yaml |

---

## What Was Built

### demos/flutter/ (Task 1)

Standard Flutter project scaffolded via `flutter create --org com.smellouk.konitor --project-name konitor_flutter_demo --platforms android,ios`. Contains:

- **demos/flutter/pubspec.yaml** — `konitor_flutter: path: ../../libs/ui/flutter` path dependency + `http: ^1.1.0` for network test
- **demos/flutter/android/settings.gradle** — Generated file appended with `includeBuild('../../../..')` and `dependencySubstitution` block for all 10 Konitor modules (engine, cpu-module, fps-module, memory-module, network-module, issues-module, jank-module, gc-module, thermal-module, gpu-module, kmm)
- **demos/flutter/ios/** — Standard iOS Flutter project (Runner.xcworkspace, Podfile, etc.)
- **demos/flutter/android/** — Standard Android Flutter project (app/build.gradle, AndroidManifest, etc.)

### demos/flutter/lib/main.dart (Task 2)

Full single-file Flutter app (670+ lines) implementing:

**Color palette:** Catppuccin Mocha constants (14 colors: base, mantle, surface0, surface1, overlay1, text, muted, blue, green, yellow, peach, mauve, teal, red)

**App structure:**
- `KonitorDemoApp` — MaterialApp with dark theme, scaffold background `_base`, AppBar/TabBar using `_mantle`
- `HomePage` — StatefulWidget with `DefaultTabController`
- `_HomePageState` — 15+ state fields, `_subs` list for lifecycle management

**Platform-conditional tabs:**
- Android (10): CPU, GPU, FPS, MEMORY, EVENTS, NETWORK, ISSUES, JANK, GC, THERMAL
- iOS (4): CPU, FPS, MEMORY, NETWORK

**Stream subscriptions** (in `_startMonitoring`):
- cpuStream, fpsStream, memoryStream, networkStream (both platforms)
- issuesStream, jankStream, gcStream, thermalStream, gpuStream (Android only, Platform.isAndroid guard)
- userEventStream (both platforms)

**Per-tab features:**
- CPU: 5 metric rows + START/STOP CPU LOAD stress button
- GPU: Large 80sp utilization %, VRAM bars, App/Renderer utilization rows, STRESS GPU toggle
- FPS: Large 80sp fps number (green/yellow/red thresholds), Choreographer label
- MEMORY: Heap and RAM progress bars, Low Memory warning badge
- EVENTS: Preset event buttons (USER_LOGIN, PURCHASE, SCREEN_VIEW, VIDEO_PLAYBACK with startEvent/endEvent toggle), custom TextField + LOG, event list with type bars (mauve=duration, green=instant), CLEAR button
- NETWORK: RX/TX metric rows, TEST DOWNLOAD button (Cloudflare 5 MB)
- ISSUES: Scrollable list with 4dp severity bars (red/peach/yellow/blue), type chip, CLEAR button
- JANK: Large 80sp dropped-frames counter, jankyRatio/worstFrameMs rows, SIMULATE JANK button
- GC: gcCountDelta/gcPauseMsDelta/gcCount rows, SIMULATE GC button
- THERMAL: Large state text (thermal-colored), THROTTLING indicator, temperature row, START/STOP CPU STRESS

**AppBar:** Running indicator dot (8dp circle, green=running / surface1=stopped), title, START/STOP toggle button (blue fill when stopped, surface1 when running)

---

## Success Criteria Verification

- [x] demos/flutter/pubspec.yaml declares `konitor_flutter: path: ../../libs/ui/flutter`
- [x] demos/flutter/android/settings.gradle has `includeBuild('../../../..')` with all substitution rules
- [x] demos/flutter/lib/main.dart shows 10 tabs on Android, 4 on iOS
- [x] Catppuccin Mocha palette: `_base = Color(0xFF1E1E2E)` as scaffold background, `_mantle = Color(0xFF181825)` as AppBar/TabBar
- [x] Engine start/stop toggle with running indicator dot
- [x] All metric data from StreamSubscription in `_startMonitoring`, cancelled in `_stopMonitoring`/`dispose`
- [x] demos/flutter/ios/ exists
- [x] demos/flutter/android/ exists

---

## Deviations from Plan

### Auto-added

**1. [Rule 2 - Missing Dependency] Added http: ^1.1.0 to pubspec.yaml**
- **Found during:** Task 2
- **Issue:** The plan specifies `import 'package:http/http.dart' as http` for the network test download but does not include `http` in the pubspec.yaml dependencies section
- **Fix:** Added `http: ^1.1.0` to `demos/flutter/pubspec.yaml`
- **Files modified:** demos/flutter/pubspec.yaml
- **Commit:** 6103124

None other — plan executed as specified.

---

## Threat Surface Scan

No new network endpoints beyond those described in the plan's threat model. The network test button calls `speed.cloudflare.com/__down?bytes=5000000` — accepted per T-26-05. Stress test buttons are accepted per T-26-06. No credentials involved in any path.

---

## Self-Check

- FOUND: demos/flutter/pubspec.yaml
- FOUND: demos/flutter/android/settings.gradle
- FOUND: demos/flutter/lib/main.dart
- FOUND: demos/flutter/ios/
- FOUND: demos/flutter/android/
- FOUND: commit 813137c (Task 1)
- FOUND: commit 6103124 (Task 2)

## Self-Check: PASSED
