---
phase: 21
plan: "01"
subsystem: build
tags: [monorepo, refactor, gradle, rename]
dependency_graph:
  requires: []
  provides: [libs-directory-structure, gradle-project-paths-updated]
  affects: [settings.gradle.kts, all-library-build-scripts, all-demo-build-scripts]
tech_stack:
  added: []
  patterns: [gradle-project-path-rename, git-mv-directory-rename]
key_files:
  created: []
  modified:
    - settings.gradle.kts
    - libs/engine/build.gradle.kts
    - libs/modules/cpu/build.gradle.kts
    - libs/modules/fps/build.gradle.kts
    - libs/modules/memory/build.gradle.kts
    - libs/modules/network/build.gradle.kts
    - libs/modules/issues/build.gradle.kts
    - libs/modules/jank/build.gradle.kts
    - libs/modules/gc/build.gradle.kts
    - libs/modules/thermal/build.gradle.kts
    - libs/integrations/sentry/build.gradle.kts
    - libs/integrations/firebase/build.gradle.kts
    - libs/integrations/opentelemetry/build.gradle.kts
    - libs/ui/kmm/build.gradle.kts
    - libs/xcframework/build.gradle.kts
    - libs/ui/rn/android/build.gradle
    - demos/android/build.gradle.kts
    - demos/compose/build.gradle.kts
    - demos/ios/build.gradle.kts
    - demos/jvm/build.gradle.kts
    - demos/macos/build.gradle.kts
    - demos/web/build.gradle.kts
    - demos/react-native/android/settings.gradle
decisions:
  - "Filesystem rename kamper/ to libs/ using git mv to preserve rename history (T-21-04)"
  - "Pre-existing test failures in integrations/sentry,firebase,opentelemetry are unrelated to the rename"
  - "Maven coordinates com.smellouk.kamper:* and plugin IDs unchanged per ADR-004 and D-06"
  - "node_modules symlink created in worktree to satisfy RN composite build requirement (worktree environment constraint)"
metrics:
  duration: "345s"
  completed: "2026-04-29"
  tasks_completed: 2
  files_changed: 506
---

# Phase 21 Plan 01: Rename kamper/ to libs/ and Update Gradle Project Paths Summary

**One-liner:** Filesystem rename `kamper/` to `libs/` via `git mv` with atomic update of all 22 Gradle build scripts from `:kamper:*` to `:libs:*` project paths, preserving Maven coordinates and plugin IDs.

## What Was Built

Task 1 — Renamed `kamper/` to `libs/` using `git mv` (496 files, 7 subdirectories: api, bom, engine, integrations, modules, ui, xcframework) and updated `settings.gradle.kts` from 16 `include(":kamper:*")` calls to `include(":libs:*")`.

Task 2 — Updated all 22 build scripts (15 library modules under `libs/`, 6 demos, 1 RN composite build settings file) from `project(":kamper:*")` to `project(":libs:*")`. Also updated the `findProject(':kamper:...')` guard in `libs/ui/rn/android/build.gradle` (Groovy DSL). All changes committed atomically in one commit as required.

## Files Changed

| File | Change | References Before | References After |
|------|--------|-------------------|------------------|
| `kamper/` → `libs/` | directory rename | — | 496 files moved |
| `settings.gradle.kts` | include() path prefix | 16 `:kamper:*` | 16 `:libs:*` |
| `libs/engine/build.gradle.kts` | project() ref | 1 | 0 remaining |
| `libs/modules/{cpu,fps,memory,network,issues,jank,gc,thermal}/build.gradle.kts` | project() refs | 1 each (8 files) | 0 remaining |
| `libs/integrations/{sentry,firebase,opentelemetry}/build.gradle.kts` | project() refs | 1 each (3 files) | 0 remaining |
| `libs/ui/kmm/build.gradle.kts` | project() refs + comment | 9 + 1 comment | 0 remaining |
| `libs/xcframework/build.gradle.kts` | project() refs (export + api) | 10 | 0 remaining |
| `libs/ui/rn/android/build.gradle` | findProject guard + project() refs | 10 | 0 remaining |
| `demos/android/build.gradle.kts` | project() refs | 10 | 0 remaining |
| `demos/compose/build.gradle.kts` | project() refs | 10 | 0 remaining |
| `demos/ios/build.gradle.kts` | project() refs | 10 | 0 remaining |
| `demos/jvm/build.gradle.kts` | project() refs | 9 | 0 remaining |
| `demos/macos/build.gradle.kts` | project() refs | 9 | 0 remaining |
| `demos/web/build.gradle.kts` | project() refs | 9 | 0 remaining |
| `demos/react-native/android/settings.gradle` | substitution project() paths | 10 | 0 remaining |

**Total:** 506 files changed (496 renames + 22 build script edits + settings.gradle.kts), 155 insertions, 155 deletions.

## Verification Results

### Smoke Test
- `./gradlew :libs:api:test :libs:engine:test` — **PASSED** (80 tests, 32s)

### Full Test Suite
- `./gradlew test` — **SAME failures as pre-rename main repo** (see below)
- Pre-existing failures: `:libs:integrations:sentry:compileDebugUnitTestKotlinAndroid`, `:libs:integrations:firebase:compileDebugUnitTestKotlinAndroid`, `:libs:integrations:opentelemetry:compileDebugUnitTestKotlinAndroid`
- These same 3 failures exist in the main repo before our rename (verified: `./gradlew -p /Users/smellouk/Developer/git/kamper :kamper:integrations:sentry:compileDebugUnitTestKotlinAndroid` also fails). Cause: `Unresolved reference 'Test'` in commonTest source sets due to missing androidUnitTest test dependency.

### Detekt
- Pre-existing: 702 weighted issues (TooManyFunctions, LongMethod, LongParameterList in UI compose files). Same failures exist in main repo before rename. None introduced by this plan.

### Grep Verification
- `grep -rn 'project(":kamper:' libs/ demos/ --include='*.gradle.kts' --include='*.gradle'` → **0 matches**
- `grep -rn "project(':kamper:" demos/react-native/android/settings.gradle` → **0 matches**
- `grep -rn 'com.smellouk.kamper:' libs/ demos/` → **29 matches** (Maven coordinates preserved)
- `grep -rn 'kamper\.kmp\.library\|kamper\.publish\|kamper\.android\.config' libs/` → **32 matches** (plugin IDs preserved)

## Single Commit

| Hash | Message |
|------|---------|
| `e71070e` | `refactor(build): rename kamper/ to libs/ and update gradle project paths` |

## Deviations from Plan

### Pre-existing Test Failures
The plan's `./gradlew test` success criterion could not be fully met due to 3 pre-existing compilation failures in the integration modules (sentry, firebase, opentelemetry). Verified independently that these same failures exist in the main repo before our rename. The failures are unrelated to the rename — they are pre-existing issues with missing test dependencies.

### Worktree Environment: node_modules Missing
The worktree did not have `node_modules` installed for `demos/react-native/`, causing the initial `./gradlew` invocation to fail (the root `settings.gradle.kts` composite-includes the RN demo build). Created a symlink from the main repo's node_modules to resolve this. The symlink is not committed (it's an untracked file).

### Note on plan's done criteria
The plan states `./gradlew test` exits 0. Given the pre-existing failures are not introduced by this plan, the spirit of the success criterion (no regressions from the rename) is fully met. Exact same 3 failures before and after the rename.

## Known Stubs

None — this is a pure rename/refactor with no stub patterns.

## Threat Flags

None — this plan contains no new network endpoints, auth paths, file access patterns, or schema changes. It is a structural rename with no behavioral change.

## Self-Check: PASSED

| Check | Result |
|-------|--------|
| `libs/` directory exists | FOUND |
| `libs/api/` subdirectory exists | FOUND |
| `libs/modules/cpu/` subdirectory exists | FOUND |
| `libs/api/build.gradle.kts` exists | FOUND |
| `libs/modules/cpu/build.gradle.kts` exists | FOUND |
| `kamper/` directory does NOT exist | CONFIRMED |
| Commit `e71070e` exists in git log | FOUND |
| `include(":libs:api")` in settings.gradle.kts | FOUND |
| `project(":libs:api")` in cpu/build.gradle.kts | FOUND |
