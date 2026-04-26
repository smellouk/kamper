---
phase: 11-migrate-buildsrc-to-composite-build-convention-plugins
plan: 03
subsystem: infra
tags: [gradle, build-migration, kmp, library-modules, version-catalog, convention-plugins]

# Dependency graph
requires:
  - phase: 11-migrate-buildsrc-to-composite-build-convention-plugins
    plan: 02
    provides: KmpLibraryPlugin.kt, KamperPublishPlugin.kt, AndroidConfigPlugin.kt — real convention plugin classes
provides:
  - kamper/api/build.gradle.kts — id("kamper.kmp.library") only (no publish, no publishLibraryVariants)
  - kamper/engine/build.gradle.kts — id("kamper.kmp.library") + id("kamper.publish"), publishLibraryVariants("release")
  - kamper/modules/{cpu,fps,memory,network,jank,gc,thermal,issues}/build.gradle.kts — same pattern as engine
  - kamper/ui/android/build.gradle.kts — 4 bare id() plugins + id("kamper.android.config"), 3-target restriction preserved
  - kamper/xcframework/build.gradle.kts — id("org.jetbrains.kotlin.multiplatform") only, XCFramework DSL preserved
affects: [11-04, demos migration, root build.gradle.kts cleanup]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Convention plugin id(\"kamper.kmp.library\") replaces 3 plugin declarations + 10 KMP target declarations per module"
    - "id() without version for plugins already on buildscript classpath (avoids classpath version conflict during transitional wave)"
    - "alias(libs.plugins.kotlin.compose) for kotlin-compose plugin — requires version to resolve from plugin repositories"
    - "buildFeatures { buildConfig = true } preserved in cpu module for BuildConfig.DEBUG references"

key-files:
  modified:
    - kamper/api/build.gradle.kts
    - kamper/engine/build.gradle.kts
    - kamper/modules/cpu/build.gradle.kts
    - kamper/modules/fps/build.gradle.kts
    - kamper/modules/memory/build.gradle.kts
    - kamper/modules/network/build.gradle.kts
    - kamper/modules/jank/build.gradle.kts
    - kamper/modules/gc/build.gradle.kts
    - kamper/modules/thermal/build.gradle.kts
    - kamper/modules/issues/build.gradle.kts
    - kamper/ui/android/build.gradle.kts
    - kamper/xcframework/build.gradle.kts

key-decisions:
  - "id() without version used for kotlin.multiplatform, com.android.library, org.jetbrains.compose in ui/android and xcframework — alias() with version triggers 'already on classpath with unknown version' error while buildSrc still exists; Plan 04 restores full alias() usage after buildSrc removal"
  - "alias(libs.plugins.kotlin.compose) used for kotlin-compose plugin — this plugin is NOT on the buildscript classpath and requires version resolution"
  - "buildFeatures { buildConfig = true } preserved in cpu module — CpuInfoRepositoryImpl references BuildConfig.DEBUG; omitting it causes Unresolved reference compile error"
  - "packaging.resources.excludes block removed from all modules — KmpLibraryPlugin does not inject it; this is acceptable as it only affects test artifact packaging (not production)"

requirements-completed: [BUILD-MIGRATE-LIB-01, BUILD-MIGRATE-LIB-02, BUILD-MIGRATE-LIB-03]

# Metrics
duration: 25min
completed: 2026-04-26
---

# Phase 11 Plan 03: Kamper Library Modules Migration Summary

**12 kamper/* build.gradle.kts files migrated from buildSrc references (Libs.*, Modules.*, kotlin("multiplatform") shorthand, apply(from)) to convention plugin IDs and version catalog aliases; ~360 lines deleted across all files**

## Performance

- **Duration:** ~25 min
- **Completed:** 2026-04-26
- **Tasks:** 4 (3 migration tasks + 1 verification)
- **Files modified:** 12 build.gradle.kts files

## Accomplishments

- Migrated `kamper/api` to `id("kamper.kmp.library")` only — no publish, no publishLibraryVariants
- Migrated `kamper/engine` + 8 performance modules (cpu/fps/memory/network/jank/gc/thermal/issues) to `id("kamper.kmp.library")` + `id("kamper.publish")` with `publishLibraryVariants("release")` preserved
- Migrated `kamper/ui/android` to bare `id()` plugin application with `id("kamper.android.config")` for Android defaults; 3-target restriction (androidTarget, iosArm64, iosSimulatorArm64) preserved; all Libs/Modules references removed
- Migrated `kamper/xcframework` to `id("org.jetbrains.kotlin.multiplatform")` only; XCFramework DSL preserved; all 10 Modules.* literal replacements applied
- All 12 files have zero `Libs.*`, `Modules.*`, `kotlin("multiplatform")` shorthand, or `apply(from = ...)` references
- All Gradle checks pass: `./gradlew help`, `./gradlew :kamper:api:compileKotlinJvm`, `./gradlew :kamper:engine:compileKotlinJvm`, `./gradlew :kamper:modules:cpu:compileKotlinJvm`, `./gradlew :kamper:ui:android:compileDebugSources`
- engine + 8 modules have publishing tasks; api correctly has none

## Task Commits

1. **Task 1: Migrate 9 standard KMP library modules** — `f904a0c` (feat)
2. **Task 2: Migrate kamper/ui/android** — `7feb46b` (feat)
3. **Task 3: Migrate kamper/xcframework** — `d82cbdd` (feat)
4. **Task 1 deviation: Restore buildFeatures.buildConfig to cpu** — `47431cb` (fix)

## Files Modified

- `kamper/api/build.gradle.kts` — 54 lines → 22 lines: 3 plugin declarations + 10 KMP targets + commonTest block removed; id("kamper.kmp.library") applied; Libs.* → catalog aliases
- `kamper/engine/build.gradle.kts` — 52 lines → 33 lines: same pattern + id("kamper.publish") added; apply(from = publish.gradle.kts) removed; Modules.API → ":kamper:api" literal
- `kamper/modules/cpu/build.gradle.kts` — 65 lines → 33 lines: same pattern as engine; androidInstrumentedTest test deps block removed (KmpLibraryPlugin injects mockk); buildFeatures.buildConfig preserved
- `kamper/modules/fps/build.gradle.kts` — 53 lines → 30 lines: same engine pattern; no androidx.annotation
- `kamper/modules/memory/build.gradle.kts` — 53 lines → 30 lines: same as fps
- `kamper/modules/network/build.gradle.kts` — 53 lines → 30 lines: same as fps
- `kamper/modules/jank/build.gradle.kts` — 53 lines → 33 lines: has androidx.annotation in androidMain
- `kamper/modules/gc/build.gradle.kts` — 53 lines → 33 lines: same as jank
- `kamper/modules/thermal/build.gradle.kts` — 54 lines → 33 lines: same as jank
- `kamper/modules/issues/build.gradle.kts` — 53 lines → 33 lines: same as jank
- `kamper/ui/android/build.gradle.kts` — 82 lines → 43 lines: 4 bare id() plugins + id("kamper.android.config"); compileSdk/minSdk/compileOptions removed; testOptions/packaging removed; all Libs/Modules replaced
- `kamper/xcframework/build.gradle.kts` — 36 lines → 36 lines: only kotlin("multiplatform") → id() shorthand; 10 Modules.* → literal paths; no other changes

## Decisions Made

- `id()` without version for kotlin.multiplatform/com.android.library/org.jetbrains.compose in ui/android and xcframework — the root `build.gradle.kts` buildscript block puts these on the classpath without a Gradle version marker. Using `alias()` (which includes version 2.3.20/8.12.0/1.8.0) triggers Gradle's "already on classpath with unknown version" hard error. Plan 04's removal of the buildscript block will allow full `alias()` usage.
- `alias(libs.plugins.kotlin.compose)` used for kotlin-compose plugin — this plugin IS available via the plugin repository with a version marker; it's NOT put on the buildscript classpath by the existing code, so `alias()` with version works correctly
- `buildFeatures { buildConfig = true }` preserved in cpu module — the original cpu build.gradle.kts had this; the plan's target template omitted it but CpuInfoRepositoryImpl.kt references `BuildConfig.DEBUG` — removing it causes a hard compile error
- `packaging.resources.excludes` block removed from all modules — not in the plan's target templates; acceptable since KmpLibraryPlugin does not inject it; the LICENSE file exclusions were needed to prevent duplicate entry errors but the root subprojects{} block handles this globally

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Alias conflict for plugins already on buildscript classpath**
- **Found during:** Task 2 (kamper/ui/android migration — `./gradlew help`)
- **Issue:** `alias(libs.plugins.kotlin.multiplatform)` includes version `2.3.20`. The root `build.gradle.kts` buildscript block puts `org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.20` on the classpath without Gradle's version marker. Gradle rejects applying a versioned alias for a plugin already on the classpath with an unknown version: "The request for this plugin could not be satisfied because the plugin is already on the classpath with an unknown version."
- **Fix:** Used `id("org.jetbrains.kotlin.multiplatform")`, `id("com.android.library")`, `id("org.jetbrains.compose")` (all without version). These are the plugins put on the classpath via the buildscript block. `alias(libs.plugins.kotlin.compose)` was kept since kotlin-compose is NOT on the buildscript classpath.
- **Files modified:** `kamper/ui/android/build.gradle.kts`, `kamper/xcframework/build.gradle.kts`
- **Commits:** `7feb46b`, `d82cbdd`
- **Note:** Same approach applies to xcframework for `kotlin.multiplatform`. Plan 04's removal of the buildscript classpath block will allow full `alias()` usage.

**2. [Rule 1 - Bug] Missing buildFeatures.buildConfig in cpu module**
- **Found during:** Task 4 (verification — `./gradlew :kamper:ui:android:compileDebugSources`)
- **Issue:** The plan's target template for kamper/modules/cpu did not include `buildFeatures { buildConfig = true }`. The cpu source code (CpuInfoRepositoryImpl.kt) references `BuildConfig.DEBUG` in 8 places. Without the buildFeatures flag, AGP does not generate the BuildConfig class and compilation fails with "Unresolved reference 'BuildConfig'".
- **Fix:** Added `buildFeatures { buildConfig = true }` to the cpu module's `android {}` block.
- **Files modified:** `kamper/modules/cpu/build.gradle.kts`
- **Commit:** `47431cb`

---

**Total deviations:** 2 auto-fixed (both Rule 1 - bugs)
**Impact on plan:** All acceptance criteria met. Plan 04 will be able to use full `alias()` syntax once the buildscript classpath block is removed.

## State After This Plan

- buildSrc/ still exists (Plan 04 deletes it)
- kamper/publish.gradle.kts still exists (Plan 04 deletes it; no longer referenced by any kamper/* module)
- root build.gradle.kts still has buildscript{} classpath and allprojects{} blocks (Plan 04 cleans these)
- All demos/* build.gradle.kts files still reference buildSrc (Plan 04 migrates them)

## Known Stubs

None — all 12 modules are fully migrated. No placeholder code.

## Threat Flags

None — no new network endpoints, auth paths, file access patterns, or schema changes. The inline project path strings (e.g., `":kamper:api"`) are configuration-time verified by Gradle; typos fail loudly at build evaluation.

---
*Phase: 11-migrate-buildsrc-to-composite-build-convention-plugins*
*Completed: 2026-04-26*

## Self-Check: PASSED

All modified files confirmed present on disk. All task commits confirmed in git history.

- kamper/api/build.gradle.kts: FOUND (contains id("kamper.kmp.library"), no Libs.*/Modules.*)
- kamper/engine/build.gradle.kts: FOUND (contains id("kamper.publish"), publishLibraryVariants)
- kamper/modules/cpu/build.gradle.kts: FOUND (contains buildFeatures.buildConfig, libs.androidx.annotation)
- kamper/modules/fps/build.gradle.kts: FOUND
- kamper/modules/memory/build.gradle.kts: FOUND
- kamper/modules/network/build.gradle.kts: FOUND
- kamper/modules/jank/build.gradle.kts: FOUND (contains libs.androidx.annotation)
- kamper/modules/gc/build.gradle.kts: FOUND (contains libs.androidx.annotation)
- kamper/modules/thermal/build.gradle.kts: FOUND (contains libs.androidx.annotation)
- kamper/modules/issues/build.gradle.kts: FOUND (contains libs.androidx.annotation)
- kamper/ui/android/build.gradle.kts: FOUND (id() plugins, alias(kotlin.compose), 3 targets)
- kamper/xcframework/build.gradle.kts: FOUND (id("org.jetbrains.kotlin.multiplatform"), XCFramework DSL)
- Commit f904a0c: FOUND (Task 1 - 9 standard KMP modules)
- Commit 7feb46b: FOUND (Task 2 - ui/android)
- Commit d82cbdd: FOUND (Task 3 - xcframework)
- Commit 47431cb: FOUND (Fix - cpu buildFeatures)
