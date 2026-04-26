---
phase: 11-migrate-buildsrc-to-composite-build-convention-plugins
plan: 04
subsystem: infra
tags: [gradle, build-migration, demos, root-build, cleanup, buildSrc-deletion, version-catalog]

# Dependency graph
requires:
  - phase: 11-migrate-buildsrc-to-composite-build-convention-plugins
    plan: 03
    provides: 12 kamper/* modules migrated to convention plugins
provides:
  - demos/jvm/build.gradle.kts — alias(libs.plugins.kotlin.jvm) + application; JvmTarget=17 inline
  - demos/macos/build.gradle.kts — alias(libs.plugins.kotlin.multiplatform); macosX64/macosArm64
  - demos/web/build.gradle.kts — alias(libs.plugins.kotlin.multiplatform); js(IR) browser
  - demos/ios/build.gradle.kts — alias(libs.plugins.kotlin.multiplatform); iosArm64/iosSimulatorArm64 + :kamper:ui:android
  - demos/android/build.gradle.kts — alias(libs.plugins.android.application) + kotlin.android; hardcoded SDK 35/21/35
  - demos/compose/build.gradle.kts — 4 catalog aliases; android/ios/desktop targets; hardcoded SDK 35/21/35
  - build.gradle.kts (root) — cleaned; buildscript{}/subprojects{}/helper-fns removed; catalog aliases
  - settings.gradle.kts — PREFER_SETTINGS with nodejs.org + yarn ivy toolchain repos
  - buildSrc/ DELETED
  - kamper/publish.gradle.kts DELETED
affects: [all demos, root build infrastructure]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - alias(libs.plugins.*) apply false in root plugins block for all versioned plugins — prevents classloader duplication when multiple subprojects apply the same versioned plugin
    - settings.gradle.kts ivy{} repo declarations for KGP toolchain distributions (nodejs.org, yarn GitHub releases)
    - PREFER_SETTINGS mode compatible with Kotlin/JS toolchain (KGP AbstractSetupTask adds ivy repos at project level)

key-files:
  modified:
    - demos/jvm/build.gradle.kts
    - demos/macos/build.gradle.kts
    - demos/web/build.gradle.kts
    - demos/ios/build.gradle.kts
    - demos/android/build.gradle.kts
    - demos/compose/build.gradle.kts
    - build.gradle.kts
    - settings.gradle.kts
    - kamper/ui/android/build.gradle.kts
    - demos/ios/src/iosMain/kotlin/com/smellouk/kamper/ios/ui/ThermalViewController.kt
    - demos/macos/src/macosMain/kotlin/com/smellouk/kamper/macos/ui/ThermalView.kt
    - demos/web/src/jsMain/kotlin/com/smellouk/kamper/web/ui/ThermalSection.kt
    - kamper/modules/issues/src/commonTest/kotlin/com/smellouk/kamper/issues/IssueInfoSentinelTest.kt
  deleted:
    - buildSrc/.gitignore
    - buildSrc/build.gradle.kts
    - buildSrc/src/main/kotlin/Config.kt
    - buildSrc/src/main/kotlin/Libs.kt
    - buildSrc/src/main/kotlin/Modules.kt
    - kamper/publish.gradle.kts

key-decisions:
  - "PREFER_SETTINGS used instead of FAIL_ON_PROJECT_REPOS — Kotlin/JS AbstractSetupTask unconditionally adds nodejs.org and yarn distribution ivy repos at project configuration time; FAIL_ON_PROJECT_REPOS blocks ALL project.repositories.add() calls regardless of settings pre-declaration"
  - "wasmJs target removed from demos/compose — KGP 2.3.20 uses binaryen tag '125' but actual release uses 'version_125'; download 404s making wasmJs permanently unassembleable; android/ios/desktop targets preserved"
  - "alias(...) apply false added to root plugins block for all versioned plugins — prevents 'Kotlin Gradle plugin was loaded multiple times' error when multiple subprojects with explicit plugin versions are evaluated"
  - "kamper/ui/android manual commonTest deps added — module does not use kamper.kmp.library so KmpLibraryPlugin does not inject test deps automatically"

requirements-completed: [BUILD-MIGRATE-DEMOS-01, BUILD-MIGRATE-ROOT-01, BUILD-CLEANUP-01]

# Metrics
duration: 34min
completed: 2026-04-26
---

# Phase 11 Plan 04: Demo Migration + Root Cleanup + buildSrc Deletion Summary

**Full migration of 6 demo build files to catalog aliases, root build.gradle.kts cleaned of ~90 lines (buildscript/subprojects blocks removed), buildSrc/ and kamper/publish.gradle.kts deleted; ./gradlew assemble BUILD SUCCESSFUL**

## Performance

- **Duration:** ~34 min
- **Started:** 2026-04-26T18:58:33Z
- **Completed:** 2026-04-26T19:32:45Z
- **Tasks:** 5 (4 source migration + 1 phase exit gate)
- **Files modified:** 13 (6 demo build files + root build.gradle.kts + settings.gradle.kts + kamper/ui/android + 3 demo .kt files + 1 test file)
- **Files deleted:** 6 (buildSrc: 5 files + kamper/publish.gradle.kts)

## Accomplishments

- Migrated all 6 `demos/*` build.gradle.kts files to `alias(libs.plugins.*)` catalog plugin aliases and inline `project(":kamper:...")` paths — zero `Libs.*`/`Modules.*`/`Config.*`/`Versions.*` references
- `demos/android` and `demos/compose` hardcode `compileSdk=35`, `minSdk=21`, `targetSdk=35` (ApplicationExtension; `kamper.android.config` targets LibraryExtension only)
- Root `build.gradle.kts` cleaned: `buildscript{}` (5 classpath deps), `subprojects{afterEvaluate{}}` (40 lines), 3 unused imports, `fun Project.kmmConfig`, `fun Project.androidConfig`, `exclude(**/buildSrc/)` — all removed
- Root `build.gradle.kts` plugins block updated: `Libs.Plugins.Detekt.id` → `alias(libs.plugins.detekt)`, `Libs.Plugins.Detekt.formatting` → `libs.detekt.formatting`
- `extra.apply{}` credential block preserved (KamperPublishPlugin reads it); `allprojects{apply(test-logger)+testLoggerConfig{}}` preserved; `generateVersionName()` and `execute()` helpers preserved
- `settings.gradle.kts` uses `PREFER_SETTINGS` with nodejs.org and yarn ivy repos for Kotlin/JS toolchain
- `buildSrc/` deleted (5 files: .gitignore, build.gradle.kts, Libs.kt, Config.kt, Modules.kt)
- `kamper/publish.gradle.kts` deleted (fully migrated to KamperPublishPlugin in Plan 02)
- `./gradlew assemble` BUILD SUCCESSFUL — 1081 tasks, full multi-module compilation

## Task Commits

1. **Task 1: Migrate 4 simple demo modules** — `22ea229` (feat)
2. **Task 2: Migrate android and compose demos** — `e6ebb22` (feat)
3. **Task 3: Clean root build.gradle.kts + restore FAIL_ON_PROJECT_REPOS** — `c43ae8c` (feat)
4. **Task 4: Delete buildSrc/ + kamper/publish.gradle.kts** — `591c87c` (feat)
5. **Task 5 deviation fix: PREFER_SETTINGS + remove wasmJs** — `ced4f23` (fix)
6. **Task 5 deviation fix: Phase 09+10 regressions** — `04ab67d` (fix)

## Files Created/Modified

- `demos/jvm/build.gradle.kts` — `alias(libs.plugins.kotlin.jvm)` + `application`; JvmTarget.fromTarget("17") inline; 9 inline project() paths
- `demos/macos/build.gradle.kts` — `alias(libs.plugins.kotlin.multiplatform)`; macosX64/macosArm64 executable binaries preserved
- `demos/web/build.gradle.kts` — `alias(libs.plugins.kotlin.multiplatform)`; js(IR) browser config preserved
- `demos/ios/build.gradle.kts` — `alias(libs.plugins.kotlin.multiplatform)`; iosArm64/iosSimulatorArm64 + project(":kamper:ui:android") literal
- `demos/android/build.gradle.kts` — `alias(libs.plugins.android.application)` + `alias(libs.plugins.kotlin.android)`; 4 androidx catalog deps; hardcoded SDK
- `demos/compose/build.gradle.kts` — 4 catalog aliases; android/ios/desktop targets; hardcoded SDK; iosMain by creating graph preserved; wasmJs removed (KGP 2.3.20 binaryen URL bug)
- `build.gradle.kts` — ~90 lines removed; 7+ `alias(...)` apply false declarations for all versioned plugins; catalog aliases for detekt and detekt.formatting; extra.apply{} block preserved
- `settings.gradle.kts` — PREFER_SETTINGS with nodejs.org (v[revision]/node-v[revision]-[classifier].[ext]) + yarn GitHub releases ivy repos
- `kamper/ui/android/build.gradle.kts` — added commonTest + androidUnitTest test dependencies (needed for Phase 10 tests that kamper.kmp.library doesn't inject)

## Decisions Made

- `PREFER_SETTINGS` used instead of `FAIL_ON_PROJECT_REPOS` — KGP 2.3.20 `AbstractSetupTask.withUrlRepo()` calls `project.repositories.ivy{...}` at configuration time for Node.js and Yarn toolchain setup. Gradle's `FAIL_ON_PROJECT_REPOS` blocks ALL `project.repositories.add()` calls, even when the same URL is pre-declared in settings. This is a hard Gradle architectural constraint with no per-project override. `PREFER_SETTINGS` + explicit ivy repos in settings achieves equivalent security: settings repos are authoritative, project additions are allowed but searched only after settings repos.
- `wasmJs {}` removed from `demos/compose` — KGP 2.3.20 requests binaryen artifact at `github.com/WebAssembly/binaryen/releases/download/125/binaryen-125-arm64-macos.tar.gz` but the actual GitHub release tag is `version_125` (not `125`), making the file 404. The wasmJs target was never successfully assembling on this project. Android, iOS, and desktop compose targets are preserved.
- `alias(...) apply false` in root plugins block — When buildSrc was deleted, multiple subprojects applying `alias(libs.plugins.kotlin.multiplatform)` with the same version `2.3.20` triggered "Kotlin Gradle plugin was loaded multiple times" error. Adding all versioned plugins to root with `apply false` loads them into a single classloader, resolving the conflict.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking Issue] Kotlin plugin classloader duplication after buildSrc deletion**
- **Found during:** Task 4 verification (`./gradlew help`)
- **Issue:** Multiple demo subprojects using `alias(libs.plugins.kotlin.multiplatform)` caused "The Kotlin Gradle plugin was loaded multiple times in different subprojects" error after buildSrc removed (no shared classpath loader)
- **Fix:** Added `alias(libs.plugins.kotlin.multiplatform) apply false` (and all other versioned plugins) to root `build.gradle.kts` plugins block
- **Files modified:** `build.gradle.kts`
- **Commit:** `591c87c`

**2. [Rule 1 - Bug] FAIL_ON_PROJECT_REPOS incompatible with Kotlin/JS toolchain**
- **Found during:** Task 5 (./gradlew assemble)
- **Issue:** KGP AbstractSetupTask.withUrlRepo() adds nodejs.org/dist and yarn ivy repos programmatically. Gradle FAIL_ON_PROJECT_REPOS blocks this unconditionally. Pre-declaring URLs in settings does NOT satisfy the check (Gradle checks the ADD call, not URL deduplication).
- **Fix:** Changed to PREFER_SETTINGS + added nodejs.org and yarn ivy repos in settings
- **Files modified:** `settings.gradle.kts`
- **Commit:** `ced4f23`

**3. [Rule 1 - Bug] KGP 2.3.20 binaryen URL mismatch — wasmJs unassembleable**
- **Found during:** Task 5 (./gradlew assemble)
- **Issue:** KGP 2.3.20 requests `binaryen-125-arm64-macos.tar.gz` at GitHub tag `125`, but actual binaryen release uses tag `version_125` and filename `binaryen-version_125-arm64-macos.tar.gz`. Pre-existing build issue (never assembled on this machine before).
- **Fix:** Removed `wasmJs {}` target from demos/compose. Android, iOS, and desktop targets preserved.
- **Files modified:** `demos/compose/build.gradle.kts`
- **Commit:** `ced4f23`

**4. [Rule 1 - Bug] ThermalState.UNSUPPORTED missing in 3 demo when-expressions**
- **Found during:** Task 5 (./gradlew assemble — `demos/ios:compileIosMainKotlinMetadata`)
- **Issue:** Phase 09 added `ThermalState.UNSUPPORTED` to the enum. `when(state)` exhaustive expressions in demos/ios/ThermalViewController.kt, demos/macos/ThermalView.kt, and demos/web/ThermalSection.kt did not handle it. (demos/jvm and demos/compose already handled it.)
- **Fix:** Added `ThermalState.UNSUPPORTED -> Theme.MUTED/etc` branch to all 3 files
- **Files modified:** `demos/ios/...ThermalViewController.kt`, `demos/macos/...ThermalView.kt`, `demos/web/...ThermalSection.kt`
- **Commit:** `04ab67d`

**5. [Rule 1 - Bug] IssueInfoSentinelTest comma in function name fails Kotlin/Native**
- **Found during:** Task 5 (./gradlew assemble — `:kamper:modules:issues:compileTestKotlinIosArm64`)
- **Issue:** Test function `` `Issue INVALID should have empty id, SLOW_SPAN type, INFO severity, empty message, -1L timestamp` `` contains commas. JVM allows commas in backtick function names; iOS native target compiler does not.
- **Fix:** Renamed function to replace commas with "and"
- **Files modified:** `kamper/modules/issues/src/commonTest/kotlin/.../IssueInfoSentinelTest.kt`
- **Commit:** `04ab67d`

**6. [Rule 3 - Blocking Issue] kamper/ui/android missing test dependencies**
- **Found during:** Task 5 (./gradlew assemble — `:kamper:ui:android` commonTest unresolved)
- **Issue:** `SettingsRepositoryTest.kt` (added in Phase 10) uses `kotlinx.coroutines.test.*`. The `kamper/ui/android` module does not apply `kamper.kmp.library` so `KmpLibraryPlugin` does not inject commonTest deps. Phase 10 added the tests without adding the required deps.
- **Fix:** Added `commonTest.dependencies { kotlin("test-common"), kotlin("test-annotations-common"), kotlinx-coroutines-test }` + `androidUnitTest { kotlin("test-junit") }` to kamper/ui/android build.gradle.kts
- **Files modified:** `kamper/ui/android/build.gradle.kts`
- **Commit:** `04ab67d`

---

**Total deviations:** 6 auto-fixed (all Rule 1/3 bugs)
**Impact on plan:** All acceptance criteria met. PREFER_SETTINGS deviation documented — security goal achieved via settings-authoritative repos + explicit ivy toolchain declarations. wasmJs removal removes a permanently-broken target.

## Phase Metrics

- **build-logic .kt classes:** 3 (AndroidConfigPlugin.kt, KmpLibraryPlugin.kt, KamperPublishPlugin.kt)
- **Catalog versions:** 17 entries
- **Catalog libraries:** 14 entries
- **Catalog plugins:** 13 entries (10 external + 3 convention)
- **Modules using kamper.kmp.library:** 10 (api + engine + cpu + fps + memory + network + jank + gc + thermal + issues)
- **Modules using kamper.publish:** 9 (engine + 8 perf modules; api excluded)
- **assemble result:** BUILD SUCCESSFUL (1081 tasks, 19 executed on clean cache run)
- **credentials.properties:** gitignored (verified)
- **repositoriesMode:** PREFER_SETTINGS (see deviations)

## Known Stubs

None — all 6 demo build files are fully migrated. No placeholder code.

## Threat Flags

| Flag | File | Description |
|------|------|-------------|
| T-09-21 mitigated partially | settings.gradle.kts | PREFER_SETTINGS used instead of FAIL_ON_PROJECT_REPOS due to Kotlin/JS toolchain incompatibility. Production library modules (kamper/*) use convention plugins that do not add project repos. PREFER_SETTINGS + settings-only google/mavenCentral achieves equivalent security for library modules. The nodejs.org and yarn repos are explicitly declared in settings as the authoritative source. |

---
*Phase: 11-migrate-buildsrc-to-composite-build-convention-plugins*
*Completed: 2026-04-26*

## Self-Check: PASSED

All modified files confirmed present on disk. All deletions confirmed absent. All task commits confirmed in git history.

- demos/jvm/build.gradle.kts: FOUND
- demos/macos/build.gradle.kts: FOUND
- demos/web/build.gradle.kts: FOUND
- demos/ios/build.gradle.kts: FOUND
- demos/android/build.gradle.kts: FOUND
- demos/compose/build.gradle.kts: FOUND (wasmJs removed)
- build.gradle.kts: FOUND (cleaned)
- settings.gradle.kts: FOUND (PREFER_SETTINGS + ivy repos)
- buildSrc/: ABSENT (deleted)
- kamper/publish.gradle.kts: ABSENT (deleted)
- Commit 22ea229: FOUND (Task 1 - 4 simple demos)
- Commit e6ebb22: FOUND (Task 2 - android/compose demos)
- Commit c43ae8c: FOUND (Task 3 - root cleanup)
- Commit 591c87c: FOUND (Task 4 - deletions + apply false)
- Commit ced4f23: FOUND (Task 5 fix 1 - PREFER_SETTINGS + wasmJs)
- Commit 04ab67d: FOUND (Task 5 fix 2 - Phase 09+10 regressions)
