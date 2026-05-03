---
phase: 26-flutter-support-package-and-demo
plan: "02"
subsystem: flutter-android-plugin
tags: [flutter, android, plugin, kotlin, platform-channel]
dependency_graph:
  requires: []
  provides:
    - libs/ui/flutter/android/build.gradle
    - libs/ui/flutter/android/settings.gradle
    - libs/ui/flutter/android/src/main/AndroidManifest.xml
    - libs/ui/flutter/android/src/main/kotlin/com/smellouk/konitor/flutter/KonitorFlutterPlugin.kt
  affects:
    - libs/ui/flutter (android native plugin side)
tech_stack:
  added:
    - Flutter EventChannel (io.flutter.plugin.common.EventChannel)
    - Flutter MethodChannel (io.flutter.plugin.common.MethodChannel)
    - Flutter ActivityAware plugin lifecycle
  patterns:
    - Dual-context Gradle (findProject vs Maven coordinates)
    - Composite build with dependencySubstitution (settings.gradle)
    - EventSink.success() wrapped in Handler(Looper.getMainLooper())
    - INVALID sentinel guard per module (established codebase pattern)
key_files:
  created:
    - libs/ui/flutter/android/build.gradle
    - libs/ui/flutter/android/settings.gradle
    - libs/ui/flutter/android/src/main/AndroidManifest.xml
    - libs/ui/flutter/android/src/main/kotlin/com/smellouk/konitor/flutter/KonitorFlutterPlugin.kt
  modified: []
decisions:
  - "UserEventInfo listener always wired (not config-gated), matching KonitorTurboModule pattern"
  - "IssueInfo has no INVALID guard — matches established KonitorTurboModule.kt codebase pattern"
  - "flag() extension defaults to true for null/missing keys (T-26-01 mitigation — ASVS V5)"
  - "settings.gradle includes monorepo root with 11 substitution rules (engine + 9 modules + kmm)"
metrics:
  duration: "135s"
  completed_date: "2026-05-03"
  tasks_completed: 2
  files_created: 4
---

# Phase 26 Plan 02: Android Flutter Plugin — Build Config and KonitorFlutterPlugin Summary

Android native plugin for Flutter: Kotlin `KonitorFlutterPlugin` implementing `FlutterPlugin` + `MethodCallHandler` + `ActivityAware`, with 10 EventChannels for all 9 Konitor metric modules plus UserEventInfo, and Groovy build files wiring into the monorepo composite build.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Create Android build.gradle, settings.gradle, and AndroidManifest.xml | 2da1c36 | libs/ui/flutter/android/build.gradle, libs/ui/flutter/android/settings.gradle, libs/ui/flutter/android/src/main/AndroidManifest.xml |
| 2 | Create KonitorFlutterPlugin.kt with all 9 metric listeners | 45d9e87 | libs/ui/flutter/android/src/main/kotlin/com/smellouk/konitor/flutter/KonitorFlutterPlugin.kt |

## Decisions Made

1. **UserEventInfo always wired** — not config-gated, matching the `KonitorTurboModule` reference pattern
2. **IssueInfo no INVALID guard** — `IssueInfo` has no `INVALID` sentinel in the established codebase pattern; listeners are invoked for every issue event
3. **flag() safe defaults** — `Map<*,*>?.flag(key)` returns `true` when map is null, key is absent, or value is non-boolean (ASVS V5 input validation, T-26-01 mitigation)
4. **EventSink dispatch on main thread** — `Handler(Looper.getMainLooper()).post { ... }` wraps every `eventSinks[name]?.success(...)` call (T-26-02 mitigation)
5. **Composite build with 11 substitutions** — settings.gradle includes monorepo root (`../../../..`) and substitutes all 10 modules + kmm

## Deviations from Plan

None — plan executed exactly as written. The plan's verification comment ("Expected: 8" for INVALID guard count) appears to have not included UserEventInfo's guard. Our implementation correctly guards UserEventInfo matching the `KonitorTurboModule.kt` canonical reference (line 238-243). This is not a deviation — the implementation is more correct than the comment.

## Verification Results

All plan success criteria met:

- build.gradle applies konitor.android.config in try/catch: PASS
- build.gradle has dual-context findProject() deps for 10 modules + kmm: PASS
- settings.gradle has includeBuild('../../../..') with 11 substitution rules: PASS
- KonitorFlutterPlugin implements FlutterPlugin + MethodCallHandler + ActivityAware: PASS
- 10 Konitor.addInfoListener<> blocks registered: PASS
- INVALID sentinel guards on all modules except IssueInfo: PASS (9 guards)
- EventSink.success() posted to main thread via Handler(Looper.getMainLooper()): PASS
- start method calls Konitor.stop() + Konitor.clear() before reinstalling: PASS
- showOverlay/hideOverlay guarded by BuildConfig.DEBUG + activity.runOnUiThread: PASS

## Known Stubs

None — all 10 metric channels are fully wired with real Konitor module installations and listener callbacks.

## Threat Flags

No new threat surface beyond what is documented in the plan's threat model (T-26-01, T-26-02, T-26-03 — all mitigated in implementation).

## Self-Check: PASSED

- libs/ui/flutter/android/build.gradle: FOUND
- libs/ui/flutter/android/settings.gradle: FOUND
- libs/ui/flutter/android/src/main/AndroidManifest.xml: FOUND
- libs/ui/flutter/android/src/main/kotlin/com/smellouk/konitor/flutter/KonitorFlutterPlugin.kt: FOUND
- Commit 2da1c36: FOUND
- Commit 45d9e87: FOUND
