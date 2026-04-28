---
phase: 16-release-automation-github-releases-changelog-multi-registry-
plan: 01
subsystem: infra
tags: [release-automation, gradle, version-management, gradle-properties, release-please]

# Dependency graph
requires:
  - phase: 11-migrate-buildsrc-to-composite-build-convention-plugins
    provides: build-logic/KamperPublishPlugin.kt convention plugin that reads LIB_VERSION_NAME from rootProject.extra
provides:
  - gradle.properties as the single source of truth for VERSION_NAME=1.0.0
  - build.gradle.kts reading VERSION_NAME via java.util.Properties (no git dependency)
  - Release Please compatible annotation (x-release-please-version) in gradle.properties
affects: [16-02, 16-03, 16-04, 16-05, 16-06, 16-07, release-please-config]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "java.util.Properties read in build.gradle.kts extra.apply block for version source of truth"
    - "# x-release-please-version annotation on line immediately above VERSION_NAME in gradle.properties"

key-files:
  created: []
  modified:
    - gradle.properties
    - build.gradle.kts

key-decisions:
  - "Use versionPropertiesFile variable name (not propertiesFile) in build.gradle.kts to avoid shadowing the credentials.properties variable at line 21"
  - "VERSION_NAME annotation placed on line immediately following # x-release-please-version with no blank line (per Pitfall 5 in RESEARCH.md)"
  - "Task 3 (remove buildSrc Config.versionName) is a no-op: phase 11 already migrated buildSrc to build-logic and removed versionName"

patterns-established:
  - "Pattern: Version source of truth in gradle.properties with # x-release-please-version annotation enables Release Please mechanical updates"

requirements-completed: []

# Metrics
duration: 25min
completed: 2026-04-27
---

# Phase 16 Plan 01: Version Source of Truth Migration Summary

**Migrated library version from git-tag-derived generateVersionName() to gradle.properties VERSION_NAME=1.0.0, enabling Release Please mechanical version-bump PRs**

## Performance

- **Duration:** ~25 min
- **Started:** 2026-04-27T00:00:00Z
- **Completed:** 2026-04-27T00:25:00Z
- **Tasks:** 3 planned (2 executed, 1 already done by phase 11)
- **Files modified:** 2

## Accomplishments

- Added `VERSION_NAME=1.0.0` to `gradle.properties` with `# x-release-please-version` annotation on the immediately preceding line (Release Please extra-files generic updater requirement)
- Replaced `generateVersionName()` in `build.gradle.kts` with a `java.util.Properties` read from `gradle.properties` via `versionPropertiesFile` local variable
- Removed `generateVersionName()` function and `String.execute()` extension (git-describe-based, incompatible with Release Please)
- Chain verified: `gradle.properties → extra["LIB_VERSION_NAME"] → KamperPublishPlugin.kt version = 1.0.0`
- `./gradlew :kamper:engine:properties` shows `version: 1.0.0`
- `./gradlew :kamper:engine:publishToMavenLocal` succeeds with artifact at `~/.m2/repository/com/smellouk/kamper/engine/1.0.0/engine-1.0.0.module`

## Task Commits

Each task was committed atomically:

1. **Task 1: Add VERSION_NAME to gradle.properties with Release Please annotation** - `c745b31` (feat)
2. **Task 2: Replace generateVersionName() in build.gradle.kts with gradle.properties read** - `225c6ce` (feat)
3. **Task 3: Remove versionName from buildSrc Config.kt** - No commit needed (already done by phase 11)

**Plan metadata:** (see final commit below)

## Files Created/Modified

- `gradle.properties` - Appended `# x-release-please-version` + `VERSION_NAME=1.0.0` on consecutive lines (no blank line between); all 22 pre-existing lines untouched
- `build.gradle.kts` - Replaced `set("LIB_VERSION_NAME", generateVersionName())` with java.util.Properties read; deleted `generateVersionName()` (12 lines) and `String.execute()` (8 lines); `import java.lang.System.getenv as Env` preserved

## Decisions Made

- Used `versionPropertiesFile` as the local variable name (not `propertiesFile`) to avoid shadowing the `propertiesFile` variable on line 21 that loads `credentials.properties`.
- Annotation placed with zero blank lines between `# x-release-please-version` and `VERSION_NAME=1.0.0` per Pitfall 5 in RESEARCH.md.

## Deviations from Plan

### Auto-detected Already-Done Work

**1. [Rule 1 - Pre-existing] Task 3 (remove buildSrc Config.versionName) was already done by phase 11**
- **Found during:** Task 3 pre-check
- **Issue:** Plan references `buildSrc/src/main/kotlin/Config.kt` but `buildSrc` does not exist — phase 11 migrated the project to `build-logic` convention plugins. `Config.kt` was absorbed into `AndroidConfigPlugin.kt` and `KamperPublishPlugin.kt`; `versionName` was never carried over to `build-logic`.
- **Fix:** No action needed — `grep -rn "Config.versionName"` returns zero results; `grep -rn "const val versionName"` returns zero results.
- **Files modified:** None
- **Committed in:** N/A

---

**Total deviations:** 1 (pre-existing state, no action needed)
**Impact on plan:** Task 3 is already satisfied by prior phase work. Plan success criteria fully met.

### Worktree Environment Note

**Pre-existing blocker: `demos/react-native/node_modules` not present in worktree**
- The worktree doesn't have `node_modules` for the RN demo (gitignored), causing `./gradlew help` to fail at settings resolution.
- Fixed by creating a symlink `demos/react-native/node_modules -> <main-repo>/demos/react-native/node_modules` in the worktree (local runtime fix, not committed).
- This is a pre-existing worktree isolation issue, not caused by this plan's changes.

## Issues Encountered

- Worktree lacked `demos/react-native/node_modules` (gitignored file not present). Resolved by symlinking from main repo. This is out-of-scope for this plan.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- `gradle.properties` is now the single source of truth for `VERSION_NAME`
- Release Please can update `VERSION_NAME` via the `extra-files` generic updater (annotation in place)
- Ready for phase 16-02 (Release Please GitHub Actions workflow)
- The full chain `gradle.properties → build.gradle.kts extra["LIB_VERSION_NAME"] → KamperPublishPlugin.kt version` is verified and intact

---
*Phase: 16-release-automation-github-releases-changelog-multi-registry-*
*Completed: 2026-04-27*
