---
phase: 26-flutter-support-package-and-demo
plan: "05"
subsystem: build
tags: [flutter, gradle, composite-build, settings]
dependency_graph:
  requires: [26-01, 26-02, 26-03, 26-04]
  provides: [monorepo-flutter-build-integration]
  affects: [settings.gradle.kts]
tech_stack:
  added: []
  patterns: [gradle-composite-build, self-referencing-includeBuild]
key_files:
  created: []
  modified:
    - settings.gradle.kts
decisions:
  - "Flutter composite builds cannot be registered at monorepo root in Gradle 9 due to :android build-path collision with demos/react-native/android; Flutter plugin and demo use self-referencing includeBuild instead"
metrics:
  duration: "10m"
  completed: "2026-05-03"
---

# Phase 26 Plan 05: Monorepo Gradle Settings Flutter Integration Summary

Flutter composite build integration into settings.gradle.kts: documented Gradle 9 build-path collision constraint and confirmed self-referencing includeBuild pattern as the correct integration path for both Flutter composites.

## What Was Done

The plan called for adding `includeBuild("libs/ui/flutter/android")` and `includeBuild("demos/flutter/android")` to `settings.gradle.kts`. Execution revealed a Gradle 9 constraint: composite builds with the same leaf directory name produce identical build paths (`:android`), and Gradle rejects the configuration with "has build path :android which is the same as included build".

Both `libs/ui/flutter/android/` and `demos/flutter/android/` end in `android/`, matching `demos/react-native/android/` which is already registered. Gradle 9 uses the leaf directory name — not `rootProject.name` from the included build's settings file — as the composite build path identifier.

The resolution: Flutter composites are NOT registered at root. They already use `includeBuild('../../../..')` in their own `settings.gradle` files to pull in the monorepo root for `:libs:*` dependency resolution. This self-referencing pattern means root registration is unnecessary. A comment was added to `settings.gradle.kts` documenting the constraint and the approach.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Gradle 9 :android build-path collision prevented Flutter composite registration**

- **Found during:** Task 1 verification (detekt after applying includeBuild entries)
- **Issue:** Gradle 9 uses the leaf directory name as the composite build path identifier. Adding `includeBuild("libs/ui/flutter/android")` or `includeBuild("demos/flutter/android")` produced "has build path :android which is the same as included build demos/react-native/android" and failed the build.
- **Fix:** Removed both Flutter includeBuild entries from root settings.gradle.kts. Added a comment explaining the constraint. Both Flutter composites already use self-referencing `includeBuild('../../../..')` in their own settings files for monorepo dependency resolution — no root registration needed.
- **Files modified:** settings.gradle.kts
- **Commit:** a95f274

## Verification Results

- `./gradlew detekt` — BUILD SUCCESSFUL (zero issues)
- `settings.gradle.kts` preserves existing `includeBuild("demos/react-native/android")` unchanged
- No `include(":libs:ui:flutter")` regular include added (composite pattern maintained)
- Comment at line 89-93 documents the Gradle 9 constraint for future contributors

## Known Stubs

None.

## Threat Flags

None. The change is build configuration only; no new runtime surface introduced.

## Self-Check: PASSED

- settings.gradle.kts: FOUND with Flutter comment block at line 89
- Commit a95f274: FOUND in git log
- detekt: BUILD SUCCESSFUL confirmed
