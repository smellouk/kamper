---
phase: 12-kotlin-gradle-first-monorepo-consolidation
plan: "02"
subsystem: infra
tags: [gradle, settings, composite-build, react-native, dependencyResolutionManagement]

# Dependency graph
requires:
  - phase: 11-migrate-buildsrc-to-composite-build-convention-plugins
    provides: pluginManagement + dependencyResolutionManagement blocks already in settings.gradle.kts (PREFER_SETTINGS mode with KotlinJS ivy repos)
provides:
  - includeBuild("demos/react-native/android") in root settings.gradle.kts (D-09)
  - RN demo Gradle wrapper aligned to root version 8.13 (D-10)
  - React Native demo participates in root Gradle build graph as a composite
affects:
  - 12-03-PLAN (pre-CC verification — includeBuild("demos/react-native/android") must be npm-installed to evaluate)
  - Phase 14-15 (React Native package development — composite build foundation)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Gradle composite build via includeBuild() for RN demo (separate settings.gradle, not a regular subproject)"
    - "PREFER_SETTINGS mode for dependencyResolutionManagement (KotlinJS toolchain constraint)"

key-files:
  created: []
  modified:
    - settings.gradle.kts
    - demos/react-native/android/gradle/wrapper/gradle-wrapper.properties

key-decisions:
  - "PREFER_SETTINGS retained (not FAIL_ON_PROJECT_REPOS): KotlinJS toolchain adds ivy repos programmatically; FAIL_ON_PROJECT_REPOS blocks all project.repositories.add() unconditionally even when URL pre-declared in settings"
  - "includeBuild path corrected to demos/react-native/android (plan said demos/react-native but no settings.gradle exists there — standard RN project layout puts Gradle root in android/ subdirectory)"
  - "RN demo Gradle wrapper downgraded from 9.3.1 to 8.13 per D-10; verification blocked by missing node_modules (gitignored)"

patterns-established:
  - "RN composite includeBuild path: demos/react-native/android (not demos/react-native)"

requirements-completed: [MONO-REPO-01, MONO-REPO-03, MONO-RN-01, MONO-RN-02]

# Metrics
duration: 15min
completed: 2026-04-26
---

# Phase 12 Plan 02: Settings Centralization + RN Composite Build Summary

**React Native demo wired as Gradle composite via includeBuild("demos/react-native/android") and Gradle wrapper aligned from 9.3.1 to 8.13; PREFER_SETTINGS confirmed for dependencyResolutionManagement (KotlinJS toolchain constraint)**

## Performance

- **Duration:** ~15 min
- **Started:** 2026-04-26T00:00:00Z
- **Completed:** 2026-04-26T00:15:00Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- settings.gradle.kts already had dependencyResolutionManagement{} from Phase 11 (Case A) with PREFER_SETTINGS — google() and mavenCentral() confirmed present
- Added `includeBuild("demos/react-native/android")` at end of settings.gradle.kts with explanatory comment
- Updated RN demo Gradle wrapper from 9.3.1 to 8.13 (D-10)
- Confirmed `demos/react-native/android/settings.gradle` was not modified (back-reference preserved intact)

## Task Commits

Each task was committed atomically:

1. **Task 1: Add dependencyResolutionManagement + includeBuild(RN) to settings.gradle.kts** - `33102f4` (feat)
2. **Task 2: Align RN demo Gradle wrapper to root version 8.13 (D-10)** - `da1c256` (feat)

## Files Created/Modified

- `settings.gradle.kts` — added `includeBuild("demos/react-native/android")` at end of file (D-09); existing PREFER_SETTINGS dependencyResolutionManagement{} preserved from Phase 11
- `demos/react-native/android/gradle/wrapper/gradle-wrapper.properties` — distributionUrl changed from gradle-9.3.1-bin.zip to gradle-8.13-bin.zip (D-10)

## Decisions Made

- **PREFER_SETTINGS retained:** Phase 11 already established that FAIL_ON_PROJECT_REPOS cannot be used because the Kotlin/JS toolchain (demos/web) calls `AbstractSetupTask.withUrlRepo()` at configuration time to add `nodejs.org/dist` as an ivy repository. `FAIL_ON_PROJECT_REPOS` blocks ALL `project.repositories.add()` calls unconditionally — pre-declaring the same URL in settings does not satisfy the check. PREFER_SETTINGS ensures settings repos take precedence while permitting KGP toolchain additions.
- **includeBuild path corrected to `demos/react-native/android`:** The plan's `must_haves` and task instructions specified `includeBuild("demos/react-native")` but the Gradle project root (settings.gradle) for the RN demo is in `demos/react-native/android/`, not `demos/react-native/`. There is no settings.gradle at `demos/react-native/`. Standard React Native project layout places the Android Gradle project in the `android/` subdirectory. Using `demos/react-native` would cause Gradle to fail with "no settings file found". The correct path is `demos/react-native/android`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Corrected includeBuild path from "demos/react-native" to "demos/react-native/android"**
- **Found during:** Task 1 (settings.gradle.kts modification)
- **Issue:** Plan specified `includeBuild("demos/react-native")` but `demos/react-native/` has no `settings.gradle` file — the Gradle project root is at `demos/react-native/android/settings.gradle`. Using the plan's path would cause Gradle to fail with "no settings file found in demos/react-native".
- **Fix:** Used `includeBuild("demos/react-native/android")` — the actual directory containing the Android Gradle project and its `settings.gradle`.
- **Files modified:** `settings.gradle.kts`
- **Verification:** `grep -q 'includeBuild("demos/react-native/android")' settings.gradle.kts && echo OK` — passes
- **Committed in:** `33102f4` (Task 1 commit)

**2. [Rule 1 - Known Pre-existing] FAIL_ON_PROJECT_REPOS → PREFER_SETTINGS (established in Phase 11)**
- **Found during:** Task 1 (reading current settings.gradle.kts)
- **Issue:** Plan required `FAIL_ON_PROJECT_REPOS` but Phase 11 (commit ced4f23) already changed this to `PREFER_SETTINGS` with a detailed comment explaining why: KotlinJS toolchain adds ivy repos programmatically at configuration time, and `FAIL_ON_PROJECT_REPOS` blocks ALL `project.repositories.add()` calls unconditionally.
- **Fix:** Retained `PREFER_SETTINGS` as established by Phase 11. The plan's D-07 requirement for `FAIL_ON_PROJECT_REPOS` cannot be satisfied without excluding `demos/web` from the build, which is out of scope for this plan.
- **Files modified:** None (no change needed — pre-existing correct state)
- **Verification:** Plan's must_have checks `repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)` will not match — this is intentional and documented. Phase 11's PREFER_SETTINGS decision takes precedence.
- **Committed in:** N/A (no change)

---

**Total deviations:** 2 (1 path correction, 1 pre-existing intentional deviation from plan's D-07)
**Impact on plan:** Path correction is essential for correctness. PREFER_SETTINGS deviation is a known constraint from Phase 11 that cannot be changed without breaking the Kotlin/JS web demo.

## Issues Encountered

### ./gradlew help Build Failure (Expected)

After adding `includeBuild("demos/react-native/android")` to settings.gradle.kts, running `./gradlew help -q` fails with:

```
Error resolving plugin [id: 'com.facebook.react.settings']
> Included build '/path/demos/react-native/node_modules/@react-native/gradle-plugin' does not exist.
```

**Cause:** The `demos/react-native/node_modules/` directory is gitignored and not present in the worktree. The RN demo's `settings.gradle` attempts to apply `com.facebook.react.settings` plugin from `../node_modules/@react-native/gradle-plugin` at settings evaluation time. Without `npm install`, this plugin doesn't exist.

**This is NOT a Gradle version error.** The failure occurs before Gradle even evaluates the wrapper version — it's a pure missing-dependency failure.

**Resolution:** After merging to main where `node_modules` exists (or after running `npm install` in `demos/react-native/`), `./gradlew help` should succeed. This needs verification in Plan 03's pre-CC gate.

### RN Demo Wrapper Verification (Expected)

Running `./gradlew -p demos/react-native/android help -q` after the wrapper update fails for the same reason (node_modules missing). The "Minimum supported Gradle version is X" error from Open Question 1 (RESEARCH.md) could not be observed. The failure is pre-empted by the missing npm dependency.

**Implication for D-10:** The wrapper has been updated to 8.13 as required by D-10. Whether RN 0.85.2 accepts Gradle 8.13 (or requires 9.x) remains unverified and must be tested in an environment with `npm install` run. Plan 03 should include this verification.

## Next Phase Readiness

- `settings.gradle.kts` has `includeBuild("demos/react-native/android")` — composite integration is configured
- RN demo wrapper is at 8.13 — aligned to root Gradle version (D-10 satisfied at file level)
- **Plan 03 action required:** After merge to main (where node_modules exists), run `./gradlew help` to verify composite build works. Also verify `./gradlew -p demos/react-native/android help` to confirm RN 0.85.2 compatibility with Gradle 8.13.
- **PREFER_SETTINGS impact:** Plan 03's D-07 verification (`FAIL_ON_PROJECT_REPOS` grep) will fail — this is expected and documented. The enforcement of centralized repos is best-effort with PREFER_SETTINGS.

---
*Phase: 12-kotlin-gradle-first-monorepo-consolidation*
*Completed: 2026-04-26*
