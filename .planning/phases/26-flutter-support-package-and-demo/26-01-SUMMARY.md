---
phase: 26-flutter-support-package-and-demo
plan: "01"
subsystem: flutter-plugin
tags: [flutter, dart, plugin, platform-channels, konitor_flutter]
dependency_graph:
  requires: []
  provides:
    - libs/ui/flutter/pubspec.yaml
    - libs/ui/flutter/lib/konitor_flutter.dart
    - libs/ui/flutter/lib/src/konitor.dart
    - libs/ui/flutter/lib/src/konitor_config.dart
    - libs/ui/flutter/lib/src/models/*.dart
  affects: []
tech_stack:
  added:
    - Dart 3.2+ (Flutter plugin package)
    - Flutter EventChannel / MethodChannel platform channel APIs
  patterns:
    - factory fromMap constructor pattern for Dart model classes
    - Barrel export file (library directive)
    - Static stream getters via EventChannel.receiveBroadcastStream()
key_files:
  created:
    - libs/ui/flutter/pubspec.yaml
    - libs/ui/flutter/lib/konitor_flutter.dart
    - libs/ui/flutter/lib/src/konitor.dart
    - libs/ui/flutter/lib/src/konitor_config.dart
    - libs/ui/flutter/lib/src/models/cpu_info.dart
    - libs/ui/flutter/lib/src/models/fps_info.dart
    - libs/ui/flutter/lib/src/models/memory_info.dart
    - libs/ui/flutter/lib/src/models/network_info.dart
    - libs/ui/flutter/lib/src/models/issue_info.dart
    - libs/ui/flutter/lib/src/models/jank_info.dart
    - libs/ui/flutter/lib/src/models/gc_info.dart
    - libs/ui/flutter/lib/src/models/thermal_info.dart
    - libs/ui/flutter/lib/src/models/gpu_info.dart
    - libs/ui/flutter/lib/src/models/user_event_info.dart
  modified: []
decisions:
  - "Channel naming follows com.smellouk.konitor/{module} pattern (D-02) — 9 event channels + 1 control channel"
  - "All model fields match RN types.ts canonical source exactly (D-04)"
  - "KonitorConfig uses nullable bool? fields with if-null guards in toMap() so absent fields are omitted from the map"
  - "userEventStream uses channel name com.smellouk.konitor/user_event (underscore separator, not slash)"
metrics:
  duration: "~5 minutes"
  completed: "2026-05-03T16:26:36Z"
  tasks_completed: 2
  tasks_total: 2
  files_created: 14
  files_modified: 0
---

# Phase 26 Plan 01: Flutter Dart Layer — pubspec, Models, and Konitor Service

## One-Liner

konitor_flutter Flutter plugin Dart layer: pubspec with android+ios plugin declaration, 10 typed model classes with fromMap constructors, KonitorConfig serialization, Konitor service class with 10 EventChannel streams and 7 MethodChannel control methods, and barrel export.

## What Was Built

The complete Dart API surface for the `konitor_flutter` Flutter plugin package. This plan establishes the pure-Dart layer that the demo app and consumers will depend on — no Android or iOS native code is included here.

### Files Created

**Package manifest** — `libs/ui/flutter/pubspec.yaml`
- Declares `name: konitor_flutter` with flutter plugin platforms for both android and ios
- Both platforms register `KonitorFlutterPlugin` as the plugin entry point
- Targets Dart SDK ^3.2.0, Flutter >=3.16.0

**Service class** — `libs/ui/flutter/lib/src/konitor.dart`
- `Konitor` class with 10 static EventChannel stream getters: `cpuStream`, `fpsStream`, `memoryStream`, `networkStream`, `issuesStream`, `jankStream`, `gcStream`, `thermalStream`, `gpuStream`, `userEventStream`
- All EventChannels use `com.smellouk.konitor/{module}` channel names (D-02)
- MethodChannel `com.smellouk.konitor/control` with 7 static control methods: `start(config)`, `stop()`, `showOverlay()`, `hideOverlay()`, `logEvent(name)`, `startEvent(name)→int`, `endEvent(tokenId)`
- Each stream maps raw Map events to typed Dart models via `XxxInfo.fromMap`

**Config** — `libs/ui/flutter/lib/src/konitor_config.dart`
- `KonitorConfig` with nullable `bool?` fields for all 9 modules: cpu, fps, memory, network, issues, jank, gc, thermal, gpu
- `toMap()` omits null fields using if-null collection-if guards

**10 typed model classes** — `libs/ui/flutter/lib/src/models/`
- Each class is immutable with named required parameters
- Factory `XxxInfo.fromMap(Map<dynamic, dynamic> m)` constructors parse EventChannel payloads
- Field names match the canonical RN `types.ts` exactly (D-04)
- Numeric fields cast via `(m['field'] as num).toDouble()` or `.toInt()` — handles both int and double wire values
- Nullable fields (`durationMs?`, `threadName?`) use null-guard before cast

**Barrel export** — `libs/ui/flutter/lib/konitor_flutter.dart`
- `library konitor_flutter` with 12 export lines: Konitor, KonitorConfig, and all 10 model classes

## Task Commits

| Task | Name | Commit | Key Files |
|------|------|--------|-----------|
| 1 | pubspec, KonitorConfig, 10 models | 4a8902c | pubspec.yaml, konitor_config.dart, models/*.dart (12 files) |
| 2 | Konitor service class and barrel export | ebda14d | konitor.dart, konitor_flutter.dart |

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

None — all model classes wire to real EventChannel payloads. No placeholder data sources.

## Threat Flags

None — this plan is pure Dart with no network endpoints, auth paths, file access, or schema changes.

## Self-Check: PASSED

- `libs/ui/flutter/pubspec.yaml` — FOUND
- `libs/ui/flutter/lib/konitor_flutter.dart` — FOUND
- `libs/ui/flutter/lib/src/konitor.dart` — FOUND
- `libs/ui/flutter/lib/src/konitor_config.dart` — FOUND
- `libs/ui/flutter/lib/src/models/cpu_info.dart` — FOUND
- `libs/ui/flutter/lib/src/models/fps_info.dart` — FOUND
- `libs/ui/flutter/lib/src/models/memory_info.dart` — FOUND
- `libs/ui/flutter/lib/src/models/network_info.dart` — FOUND
- `libs/ui/flutter/lib/src/models/issue_info.dart` — FOUND
- `libs/ui/flutter/lib/src/models/jank_info.dart` — FOUND
- `libs/ui/flutter/lib/src/models/gc_info.dart` — FOUND
- `libs/ui/flutter/lib/src/models/thermal_info.dart` — FOUND
- `libs/ui/flutter/lib/src/models/gpu_info.dart` — FOUND
- `libs/ui/flutter/lib/src/models/user_event_info.dart` — FOUND
- Commit 4a8902c — FOUND
- Commit ebda14d — FOUND
