---
phase: 11-migrate-buildsrc-to-composite-build-convention-plugins
plan: 01
subsystem: infra
tags: [gradle, build-logic, version-catalog, composite-build, convention-plugins, toml]

# Dependency graph
requires:
  - phase: 10-test-coverage
    provides: stable test suite before build system migration
provides:
  - gradle/libs.versions.toml — single source of truth for all 17 versions, 14 libraries, 13 plugin aliases
  - build-logic/ — valid standalone Gradle project skeleton with settings + build files + empty src/main/kotlin/
  - build-logic plugin registrations — kamper.kmp.library, kamper.android.config, kamper.publish registered via gradlePlugin{}
  - settings.gradle.kts — pluginManagement.includeBuild("build-logic") wired, 18 module includes preserved
affects: [11-02, 11-03, 11-04, convention-plugins, version-catalog]

# Tech tracking
tech-stack:
  added: [gradle-version-catalog, composite-build, build-logic]
  patterns: [version-catalog-toml, includeBuild-pluginManagement, compileOnly-classpath-deps, gradlePlugin-registration]

key-files:
  created:
    - gradle/libs.versions.toml
    - build-logic/settings.gradle.kts
    - build-logic/build.gradle.kts
    - build-logic/src/main/kotlin/.gitkeep
  modified:
    - settings.gradle.kts

key-decisions:
  - "repositoriesMode temporarily set to PREFER_SETTINGS (not FAIL_ON_PROJECT_REPOS) because root build.gradle.kts allprojects{repositories{}} conflicts with strict mode; Plan 04 restores FAIL_ON_PROJECT_REPOS"
  - "includeBuild(build-logic) placed inside pluginManagement{} block to enable plugin ID resolution in module plugins{} blocks"
  - "All three classpath deps in build-logic/build.gradle.kts declared as compileOnly to avoid runtime classpath pollution"
  - "buildSrc/ left untouched — this plan is purely additive; deletion is Plan 04 only"

patterns-established:
  - "Pattern: version catalog in gradle/libs.versions.toml with [versions], [libraries], [plugins] sections"
  - "Pattern: build-logic/settings.gradle.kts wires ../gradle/libs.versions.toml via versionCatalogs.create(libs)"
  - "Pattern: convention plugin IDs in [plugins] section have NO version.ref (resolved via includeBuild)"

requirements-completed: [BUILD-FOUNDATION-01, BUILD-FOUNDATION-02, BUILD-FOUNDATION-03]

# Metrics
duration: 20min
completed: 2026-04-26
---

# Phase 11 Plan 01: Build Logic Foundation Summary

**Gradle version catalog (libs.versions.toml) + build-logic composite build skeleton wired into root settings via pluginManagement.includeBuild**

## Performance

- **Duration:** ~20 min
- **Started:** 2026-04-26T18:15:00Z
- **Completed:** 2026-04-26T18:35:41Z
- **Tasks:** 4 (3 file-creation + 1 verification)
- **Files modified:** 5 (4 created, 1 modified)

## Accomplishments
- Created `gradle/libs.versions.toml` with full version catalog: 17 versions, 14 library entries, 13 plugin aliases (10 external + 3 convention plugins)
- Created `build-logic/` standalone Gradle project with settings, build files, and empty plugin source directory
- Wired `includeBuild("build-logic")` inside `pluginManagement {}` in root `settings.gradle.kts`
- All three Gradle commands pass: `./gradlew -p build-logic classes`, `./gradlew help`, `./gradlew :buildSrc:classes`

## Task Commits

Each task was committed atomically:

1. **Task 1: Create gradle/libs.versions.toml** - `e935494` (chore)
2. **Task 2: Create build-logic/ Gradle project skeleton** - `24abaf0` (chore)
3. **Task 3: Wire pluginManagement.includeBuild into root settings** - `3612588` (chore)
4. **Task 4 deviation: Downgrade repositoriesMode to PREFER_SETTINGS** - `f98af1a` (chore)

**Plan metadata:** (committed with this SUMMARY.md)

## Files Created/Modified
- `gradle/libs.versions.toml` - Full Gradle version catalog: all versions from buildSrc/Libs.kt + AGP 8.12.0 + SDK constants from Config.kt; all library coordinates and plugin aliases
- `build-logic/settings.gradle.kts` - Standalone build identity, FAIL→PREFER repositories mode, ../gradle/libs.versions.toml wired as 'libs'
- `build-logic/build.gradle.kts` - kotlin-dsl plugin, 3 compileOnly classpath deps (AGP/KGP/Mokkery), gradlePlugin{} registrations for 3 convention plugin IDs
- `build-logic/src/main/kotlin/.gitkeep` - Empty directory marker; plugin .kt classes added in Plan 02
- `settings.gradle.kts` - Added pluginManagement{includeBuild("build-logic")} at top + dependencyResolutionManagement; all 18 includes preserved

## Decisions Made
- `includeBuild("build-logic")` placed INSIDE `pluginManagement {}` (not at root level) per RESEARCH.md Pitfall 1 — required for plugin ID resolution in module `plugins {}` blocks
- All three classpath deps in build-logic declared `compileOnly` (not `implementation`) per RESEARCH.md Pitfall 4 — avoids runtime classpath pollution downstream
- `buildSrc/` NOT deleted — this plan is purely additive per plan objective; deletion is Plan 04 only
- Convention plugin catalog entries (`kamper-kmp-library`, etc.) have no `version.ref` — they are resolved via the included build

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Downgraded repositoriesMode from FAIL_ON_PROJECT_REPOS to PREFER_SETTINGS**
- **Found during:** Task 4 (verification — `./gradlew help`)
- **Issue:** `FAIL_ON_PROJECT_REPOS` caused a hard BUILD FAILED because root `build.gradle.kts` has `allprojects { repositories { google(); mavenCentral() } }` which Gradle's strict mode rejects
- **Fix:** Changed `repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)` to `repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)` with an inline comment noting Plan 04 must restore it
- **Files modified:** `settings.gradle.kts`
- **Verification:** `./gradlew help` exits 0 after downgrade
- **Committed in:** `f98af1a` (separate deviation commit within Task 4)

The plan explicitly anticipated this scenario: "If `./gradlew help` errors hard with `IllegalStateException: Repositories cannot be added when project mode is FAIL_ON_PROJECT_REPOS`, you may TEMPORARILY downgrade the mode in this plan to `repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)`".

---

**Total deviations:** 1 auto-fixed (Rule 1 - pre-anticipated build conflict)
**Impact on plan:** Expected downgrade; Plan 04 restores strict mode after removing the conflicting allprojects{} block from root build.gradle.kts. No scope creep.

## Issues Encountered
- FAIL_ON_PROJECT_REPOS caused hard failure on `./gradlew help` — resolved by downgrading to PREFER_SETTINGS as documented in the plan as an acceptable alternative

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Plan 02 can proceed: `build-logic/src/main/kotlin/` directory is in place, plugin IDs are registered, catalog is wired
- Plan 02 must create `KmpLibraryPlugin.kt`, `AndroidConfigPlugin.kt`, `KamperPublishPlugin.kt`
- Plan 03/04 module migrations can reference `libs.*` aliases once modules switch to convention plugins
- Concern: `repositoriesMode` is `PREFER_SETTINGS` — Plan 04 MUST restore it to `FAIL_ON_PROJECT_REPOS` after removing `allprojects { repositories {} }` from root `build.gradle.kts`

---
*Phase: 11-migrate-buildsrc-to-composite-build-convention-plugins*
*Completed: 2026-04-26*

## Self-Check: PASSED

All created files confirmed present on disk. All task commits confirmed in git history.
