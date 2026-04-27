---
phase: 13-stack-alignment-dependency-unification
plan: "03"
subsystem: build
tags: [gradle, versions, dependencies, kmp, android]

# Dependency graph
requires:
  - phase: 13-stack-alignment-dependency-unification
    plan: "01"
    provides: namespace alignment (wave 1 predecessor)
provides:
  - All library versions in gradle/libs.versions.toml updated to latest stable compatible with minSdk=21 and compileSdk=35
  - AGP 8.13.0 + KGP 2.3.21 lockstep enforced
  - Build assembles and all 32 unit tests pass
affects: [build-logic, all KMP modules, 13-04, 13-05]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Version ceiling pattern: Compose MP, lifecycle, and core-ktx versions bounded by minSdk=21 and compileSdk=35 constraints — cannot freely take latest when those libraries raised minSdk"
    - "AGP/KGP lockstep: AGP 8.13.0 always paired with KGP 2.3.21 (D-08)"

key-files:
  created: []
  modified:
    - gradle/libs.versions.toml
    - build-logic/build.gradle.kts
    - build-logic/src/main/kotlin/KmpLibraryPlugin.kt
    - kamper/ui/android/build.gradle.kts

key-decisions:
  - "Compose MP capped at 1.9.3 (not 1.10.3): 1.10.x transitively requires minSdk=23 via animation-core-android; Kamper hard constraint is minSdk=21"
  - "lifecycle capped at 2.9.4 (not 2.10.0): 2.10.x transitively requires minSdk=23 via lifecycle-runtime-ktx-android"
  - "core-ktx kept at 1.16.0 (not 1.18.0): 1.17+ requires compileSdk=36; project is compileSdk=35"
  - "KmpLibraryPlugin.kt hardcoded versions updated in sync with catalog to prevent version drift between catalog and plugin"
  - "isReturnDefaultValues=true added to kamper/ui/android/build.gradle.kts as Rule 1 fix — was supposed to be in Phase 06 but was missing"

patterns-established:
  - "Before bumping a library to latest-stable, verify minSdk requirement in transitive AAR manifests — Google/JetBrains libraries regularly bump minSdk in minor versions"
  - "KmpLibraryPlugin.kt hardcoded versions must be kept in sync with catalog manually (no type-safe accessor available in convention plugin code)"

requirements-completed: [ALIGN-D07, ALIGN-D08, ALIGN-D09, ALIGN-D10]

# Metrics
duration: 123min
completed: 2026-04-26
---

# Phase 13 Plan 03: Library Version Updates Summary

**All library versions updated to latest stable compatible with minSdk=21 and compileSdk=35 constraints; AGP 8.13.0 + KGP 2.3.21 lockstep enforced; build assembles and all 32 unit tests pass after fixing three minSdk/compileSdk ceiling violations and one missing test configuration**

## Performance

- **Duration:** ~123 min (includes build verification and version ceiling discovery)
- **Started:** 2026-04-26T21:41:21Z
- **Completed:** 2026-04-26T23:44:49Z
- **Tasks:** 3
- **Files modified:** 4

## Accomplishments

- Task 1: Verified all MEDIUM-confidence library versions against Maven Central and Google Maven; confirmed latest stable for each dependency
- Task 2: Updated `gradle/libs.versions.toml`, `build-logic/build.gradle.kts`, and `KmpLibraryPlugin.kt` to updated versions
- Task 3: Discovered and fixed three version-bump breakages (D-09) — Compose 1.10.x, lifecycle 2.10.x, and core-ktx 1.17+ all exceed minSdk=21/compileSdk=35 project constraints; also fixed pre-existing missing `isReturnDefaultValues=true` in ui/android build

## Confirmed Version Matrix

| Library | Before | After | Notes |
|---------|--------|-------|-------|
| Kotlin / KGP | 2.3.20 | 2.3.21 | Lockstep with AGP 8.13.0 |
| AGP | 8.12.0 | 8.13.0 | Lockstep with KGP 2.3.21 |
| Compose Multiplatform | 1.8.0 | 1.9.3 | 1.10.x requires minSdk=23 — capped |
| kotlinx-coroutines | 1.10.1 | 1.10.2 | No constraints violated |
| mockk | 1.14.3 | 1.14.5 | No constraints violated |
| detekt | 1.23.7 | 1.23.8 | No constraints violated |
| androidx-appcompat | 1.7.0 | 1.7.1 | No constraints violated |
| androidx-annotations | 1.9.1 | 1.10.0 | No constraints violated |
| androidx-lifecycle | 2.8.7 | 2.9.4 | 2.10.x requires minSdk=23 — capped |
| androidx-material | 1.12.0 | 1.13.0 | No constraints violated |
| androidx-constraintlayout | 2.2.1 | 2.2.1 | Already at latest stable |
| androidx-core-ktx | 1.16.0 | 1.16.0 | 1.17+ requires compileSdk=36 — no change |
| mokkery | 3.3.0 | 3.3.0 | No change (latest stable) |
| gradle-test-logger | 4.0.0 | 4.0.0 | No change (latest stable) |

## Task Commits

Each task was committed atomically:

1. **Task 2: Update all library versions to latest stable** — `6804de2` (feat)
2. **Task 3: Fix version-bump breakages per D-09** — `688d1d7` (fix)

(Task 1 was pure verification with no file changes — no separate commit)

## Files Created/Modified

- `gradle/libs.versions.toml` — Updated 9 version entries; 3 subsequently capped back due to minSdk/compileSdk constraints
- `build-logic/build.gradle.kts` — Updated KGP version comment from 2.3.20 to 2.3.21
- `build-logic/src/main/kotlin/KmpLibraryPlugin.kt` — Updated hardcoded coroutines-test (1.10.1→1.10.2) and mockk (1.14.3→1.14.5) dependency strings to match catalog
- `kamper/ui/android/build.gradle.kts` — Added missing `android.testOptions.unitTests.isReturnDefaultValues = true` (Rule 1 fix)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed version ceiling violations: Compose 1.10.3, lifecycle 2.10.0, core-ktx 1.18.0**
- **Found during:** Task 3 (build verification)
- **Issue:** Three libraries' latest stable versions exceed Kamper's hard project constraints:
  - Compose MP 1.10.x: transitively requires `minSdk=23` via `animation-core-android:1.10.5` (Kamper = minSdk=21)
  - lifecycle 2.10.x: transitively requires `minSdk=23` via `lifecycle-runtime-ktx-android:2.10.0` (Kamper = minSdk=21)
  - core-ktx 1.17.x/1.18.x: requires `compileSdk=36` (Kamper = compileSdk=35)
- **Fix:** Downgraded to the highest version compatible with constraints: Compose→1.9.3, lifecycle→2.9.4, core-ktx→1.16.0 (no change)
- **Files modified:** `gradle/libs.versions.toml`
- **Commits:** `688d1d7`

**2. [Rule 1 - Bug] Fixed KmpLibraryPlugin.kt hardcoded version strings**
- **Found during:** Task 2 (grep for old versions)
- **Issue:** `KmpLibraryPlugin.kt` hardcoded `kotlinx-coroutines-test:1.10.1` and `mockk:1.14.3` directly (not via catalog). These were stale after the catalog bump.
- **Fix:** Updated to `1.10.2` and `1.14.5` to match catalog
- **Files modified:** `build-logic/src/main/kotlin/KmpLibraryPlugin.kt`
- **Commit:** `6804de2`

**3. [Rule 1 - Bug] Added missing isReturnDefaultValues=true to ui/android build**
- **Found during:** Task 3 (test failures in RecordingManagerTest)
- **Issue:** 5 tests in `RecordingManagerTest` failed with `Method elapsedRealtimeNanos in android.os.SystemClock not mocked`. Phase 06 plan specified this configuration must be added (06-01-PLAN.md), but it was missing from the current build file. `RecordingManager.nowNs()` calls `SystemClock.elapsedRealtimeNanos()` which is an Android framework method unavailable in JVM unit tests without `returnDefaultValues=true`.
- **Fix:** Added `testOptions { unitTests { isReturnDefaultValues = true } }` to `kamper/ui/android/build.gradle.kts`
- **Files modified:** `kamper/ui/android/build.gradle.kts`
- **Verification:** All 32 tests pass; `./gradlew test` → BUILD SUCCESSFUL
- **Commit:** `688d1d7`

---

**Total deviations:** 3 auto-fixed (all Rule 1 — bugs preventing build and test success)

## Deferred Items

- `includeBuild("demos/react-native/android")` fails at Gradle settings evaluation because `npm install` hasn't been run in `demos/react-native/`. This is a pre-existing issue (introduced in Phase 12, documented in 13-01-SUMMARY.md). The failure occurs at the composite build settings level and cannot be bypassed without running npm install. All non-RN builds and tests pass with this limitation.

## Build Verification Results

| Command | Result |
|---------|--------|
| `./gradlew assemble` (excluding RN demo settings failure) | BUILD SUCCESSFUL |
| `./gradlew test` | BUILD SUCCESSFUL (32 tests pass) |
| Old version strings scan | CLEAN — 0 occurrences |

## Next Phase Readiness

- All library versions at latest stable compatible with minSdk=21 and compileSdk=35
- AGP 8.13.0 + KGP 2.3.21 lockstep enforced (D-08)
- No compilation errors from version bumps (D-09)
- All unit tests pass
- Ready for Plan 13-04 (CI/CD alignment)

---
*Phase: 13-stack-alignment-dependency-unification*
*Completed: 2026-04-26*
