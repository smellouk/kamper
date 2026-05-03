---
phase: 25-rename-the-library-from-kamper-to-konitor
plan: "01"
subsystem: build
tags: [rename, build-logic, maven, gradle, publish-plugin]
dependency_graph:
  requires: []
  provides:
    - KonitorPublishPlugin with com.smellouk.konitor groupId
    - rootProject.name = Konitor
    - KONITOR_GH_USER/KONITOR_GH_PAT credential keys in root build
  affects:
    - All modules applying id("kamper.publish") — now routed to KonitorPublishPlugin
tech_stack:
  added: []
  patterns:
    - Convention plugin rename: KamperPublishPlugin → KonitorPublishPlugin
key_files:
  created:
    - build-logic/src/main/kotlin/KonitorPublishPlugin.kt
  modified:
    - build-logic/build.gradle.kts
    - settings.gradle.kts
    - build.gradle.kts
  deleted:
    - build-logic/src/main/kotlin/KamperPublishPlugin.kt
decisions:
  - "Plugin ID kamper.publish left unchanged — changing it would break 20+ module build.gradle.kts files"
  - "GitHub repo URL (github.com/smellouk/kamper) left unchanged per D-15 (repo not renamed)"
metrics:
  duration: "~5 minutes"
  completed: "2026-05-03T03:17:29Z"
  tasks_completed: 2
  files_changed: 4
---

# Phase 25 Plan 01: Rename KamperPublishPlugin to KonitorPublishPlugin Summary

**One-liner:** Renamed Maven publish convention plugin to KonitorPublishPlugin, updated groupId to com.smellouk.konitor, rootProject.name to Konitor, and credential keys to KONITOR_GH_USER/KONITOR_GH_PAT.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Rename KamperPublishPlugin.kt to KonitorPublishPlugin.kt | f9c3f38 | build-logic/src/main/kotlin/KonitorPublishPlugin.kt (new), KamperPublishPlugin.kt (deleted) |
| 2 | Update build-logic/build.gradle.kts, settings.gradle.kts, root build.gradle.kts | d529c94 | build-logic/build.gradle.kts, settings.gradle.kts, build.gradle.kts |

## What Was Done

### Task 1: KonitorPublishPlugin.kt

Created `build-logic/src/main/kotlin/KonitorPublishPlugin.kt` with these changes from the original:
- Class name: `KamperPublishPlugin` → `KonitorPublishPlugin`
- Group ID: `project.group = "com.smellouk.kamper"` → `project.group = "com.smellouk.konitor"`
- Credential extra keys: `KAMPER_GH_USER`/`KAMPER_GH_PAT` → `KONITOR_GH_USER`/`KONITOR_GH_PAT`
- POM name: `kamper:$projectName` → `konitor:$projectName`
- POM description: "Kamper is small KMM/KMP..." → "Konitor is small KMM/KMP..."
- KDoc references to "Kamper" updated to "Konitor"
- KDoc credential key references updated to `KONITOR_GH_USER`/`KONITOR_GH_PAT`
- GitHub repo URLs left unchanged per D-15 (repository not renamed)

Deleted `build-logic/src/main/kotlin/KamperPublishPlugin.kt`.

### Task 2: Configuration Files

- `build-logic/build.gradle.kts`: `implementationClass = "KamperPublishPlugin"` → `"KonitorPublishPlugin"`
- `settings.gradle.kts`: `rootProject.name = "Kamper"` → `"Konitor"`
- `build.gradle.kts`: All credential key names updated — `KAMPER_GH_USER`/`KAMPER_GH_PAT` → `KONITOR_GH_USER`/`KONITOR_GH_PAT` and `kamperGhUser`/`kamperGhToken` → `konitorGhUser`/`konitorGhToken`

## Decisions Made

1. **Plugin ID preserved:** The plugin ID `"kamper.publish"` was intentionally left unchanged. It is applied via `id("kamper.publish")` in 20+ module `build.gradle.kts` files. Only the `implementationClass` was updated to `KonitorPublishPlugin`. Wave 2 plans will handle renaming the plugin ID itself when all module build files are updated together.

2. **GitHub repo URL preserved:** Per D-15, all `github.com/smellouk/kamper` URLs in the plugin remain unchanged because the GitHub repository is not being renamed as part of this phase.

## Deviations from Plan

None — plan executed exactly as written.

## Verification Results

All checks from the plan's `<verification>` section pass:

1. `grep -r "KamperPublishPlugin" build-logic/` → 0 results
2. `grep "com.smellouk.konitor" build-logic/src/main/kotlin/KonitorPublishPlugin.kt` → 1 result
3. `test -f build-logic/src/main/kotlin/KonitorPublishPlugin.kt` → exits 0
4. `test ! -f build-logic/src/main/kotlin/KamperPublishPlugin.kt` → exits 0
5. `grep "rootProject.name" settings.gradle.kts | grep -c "Konitor"` → 1

## Self-Check: PASSED

Files created/exist:
- FOUND: build-logic/src/main/kotlin/KonitorPublishPlugin.kt
- NOT FOUND: build-logic/src/main/kotlin/KamperPublishPlugin.kt (correctly deleted)

Commits exist:
- f9c3f38: chore(build): rename KamperPublishPlugin to KonitorPublishPlugin with konitor groupId
- d529c94: chore(build): update plugin registration, project name, and credential keys to konitor
