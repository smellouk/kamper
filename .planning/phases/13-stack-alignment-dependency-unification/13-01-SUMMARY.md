---
phase: 13-stack-alignment-dependency-unification
plan: "01"
subsystem: build
tags: [gradle, namespace, android, kmp, build-config]

# Dependency graph
requires:
  - phase: 12-kotlin-gradle-first-monorepo-consolidation
    provides: composite build with convention plugins and version catalog
provides:
  - All 10 KMP module Android namespace declarations unified to com.smellouk.kamper.* pattern
  - CpuInfoRepositoryImpl BuildConfig import corrected to match new namespace
affects: [14-react-native-package-library-engine-ui, any module consuming BuildConfig]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Android namespace convention: com.smellouk.kamper + optional .{module} suffix — matches Maven group ID"
    - "Build-file-only change: namespace only controls generated R/BuildConfig class packages, no source files changed (except BuildConfig import fix)"

key-files:
  created: []
  modified:
    - kamper/api/build.gradle.kts
    - kamper/engine/build.gradle.kts
    - kamper/modules/cpu/build.gradle.kts
    - kamper/modules/fps/build.gradle.kts
    - kamper/modules/memory/build.gradle.kts
    - kamper/modules/network/build.gradle.kts
    - kamper/modules/thermal/build.gradle.kts
    - kamper/modules/gc/build.gradle.kts
    - kamper/modules/jank/build.gradle.kts
    - kamper/modules/issues/build.gradle.kts
    - kamper/modules/cpu/src/androidMain/kotlin/com/smellouk/kamper/cpu/repository/CpuInfoRepositoryImpl.kt

key-decisions:
  - "Namespace = com.smellouk.kamper.{module} is the canonical pattern for all modules — aligns with Maven group ID"
  - "CpuInfoRepositoryImpl BuildConfig import updated as Rule 1 fix — generated class package matches namespace"
  - "Pre-existing build failure (RN demo missing node_modules) documented as deferred — not namespace-related"

patterns-established:
  - "All Android module namespaces follow com.smellouk.kamper.{module} — any new module must follow this pattern"

requirements-completed: [ALIGN-D01, ALIGN-D02, ALIGN-D03]

# Metrics
duration: 8min
completed: 2026-04-26
---

# Phase 13 Plan 01: Namespace Alignment Summary

**All 10 KMP module Android namespaces unified from io.mellouk.kamper.* to com.smellouk.kamper.* across 10 build.gradle.kts files, with CpuInfoRepositoryImpl BuildConfig import corrected as a Rule 1 auto-fix**

## Performance

- **Duration:** ~8 min
- **Started:** 2026-04-26T21:10:00Z
- **Completed:** 2026-04-26T21:18:00Z
- **Tasks:** 3
- **Files modified:** 11

## Accomplishments

- Changed namespace in api and engine modules (Task 1)
- Changed namespace in all 8 remaining module build files: cpu, fps, memory, network, thermal, gc, jank, issues (Task 2)
- Verified zero io.mellouk occurrences remain in any .kts or .kt file (Task 3)
- Auto-fixed CpuInfoRepositoryImpl BuildConfig import (Rule 1) — the only Kotlin source file referencing the old namespace

## Task Commits

Each task was committed atomically:

1. **Task 1: Update namespace in api and engine modules** - `d2c9c38` (feat)
2. **Task 2: Update namespace in all 8 remaining module build files** - `c0bd7b6` (feat)
3. **Task 3 / Rule 1 fix: Update BuildConfig import in CpuInfoRepositoryImpl** - `614053a` (fix)

## Files Created/Modified

- `kamper/api/build.gradle.kts` - namespace: io.mellouk.kamper.api -> com.smellouk.kamper.api
- `kamper/engine/build.gradle.kts` - namespace: io.mellouk.kamper -> com.smellouk.kamper
- `kamper/modules/cpu/build.gradle.kts` - namespace: io.mellouk.kamper.cpu -> com.smellouk.kamper.cpu
- `kamper/modules/fps/build.gradle.kts` - namespace: io.mellouk.kamper.fps -> com.smellouk.kamper.fps
- `kamper/modules/memory/build.gradle.kts` - namespace: io.mellouk.kamper.memory -> com.smellouk.kamper.memory
- `kamper/modules/network/build.gradle.kts` - namespace: io.mellouk.kamper.network -> com.smellouk.kamper.network
- `kamper/modules/thermal/build.gradle.kts` - namespace: io.mellouk.kamper.thermal -> com.smellouk.kamper.thermal
- `kamper/modules/gc/build.gradle.kts` - namespace: io.mellouk.kamper.gc -> com.smellouk.kamper.gc
- `kamper/modules/jank/build.gradle.kts` - namespace: io.mellouk.kamper.jank -> com.smellouk.kamper.jank
- `kamper/modules/issues/build.gradle.kts` - namespace: io.mellouk.kamper.issues -> com.smellouk.kamper.issues
- `kamper/modules/cpu/src/androidMain/kotlin/com/smellouk/kamper/cpu/repository/CpuInfoRepositoryImpl.kt` - import updated: io.mellouk.kamper.cpu.BuildConfig -> com.smellouk.kamper.cpu.BuildConfig

## Decisions Made

- Namespace convention com.smellouk.kamper.{module} is the canonical pattern per D-01..D-03, following the kamper/ui/android reference implementation.
- CpuInfoRepositoryImpl was the only Kotlin source file importing a generated class (BuildConfig) under the old namespace — updated as Rule 1 fix to match new namespace.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Updated BuildConfig import in CpuInfoRepositoryImpl**
- **Found during:** Task 3 (verification pass)
- **Issue:** `CpuInfoRepositoryImpl.kt` imported `io.mellouk.kamper.cpu.BuildConfig` — the generated `BuildConfig` class package name is controlled by the `namespace` property. After changing the namespace to `com.smellouk.kamper.cpu`, the generated class lives in `com.smellouk.kamper.cpu`, making the old import a compile-time error.
- **Fix:** Changed import to `com.smellouk.kamper.cpu.BuildConfig`
- **Files modified:** `kamper/modules/cpu/src/androidMain/kotlin/com/smellouk/kamper/cpu/repository/CpuInfoRepositoryImpl.kt`
- **Verification:** `grep -r "io.mellouk" . --include="*.kt" | grep -v "/build/"` returns zero output
- **Committed in:** `614053a`

---

**Total deviations:** 1 auto-fixed (Rule 1 — bug: stale BuildConfig import after namespace change)
**Impact on plan:** Fix is necessary for correctness — the build would fail without it. No scope creep.

## Issues Encountered

- `./gradlew assemble` fails due to pre-existing React Native demo composite build issue: `demos/react-native/node_modules/@react-native/gradle-plugin` does not exist (npm install not run). This failure pre-dates the namespace changes — confirmed by testing at base commit `47ca89d`. The failure is unrelated to namespace alignment. Logged to deferred items.
- The namespace-only changes (build file edits) are verified correct via grep: 0 io.mellouk namespace occurrences, 11 com.smellouk namespace declarations in .kts files.

## Deferred Items

- Pre-existing build failure: `includeBuild("demos/react-native/android")` in `settings.gradle.kts` requires npm install for the React Native demo. Run `cd demos/react-native && npm install` before running `./gradlew assemble`. This predates Phase 13 (introduced in Phase 12 commit `47ca89d`).

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- All 10 module namespaces are aligned: com.smellouk.kamper.{module}
- CpuInfoRepositoryImpl BuildConfig import corrected
- Zero io.mellouk references in any .kts or .kt source file
- Ready for Plan 13-02 (Jetifier removal) or subsequent plans

---
*Phase: 13-stack-alignment-dependency-unification*
*Completed: 2026-04-26*

## Self-Check: PASSED

Files verified:
- kamper/api/build.gradle.kts: FOUND
- kamper/engine/build.gradle.kts: FOUND
- kamper/modules/cpu/build.gradle.kts: FOUND
- kamper/modules/fps/build.gradle.kts: FOUND
- kamper/modules/memory/build.gradle.kts: FOUND
- kamper/modules/network/build.gradle.kts: FOUND
- kamper/modules/thermal/build.gradle.kts: FOUND
- kamper/modules/gc/build.gradle.kts: FOUND
- kamper/modules/jank/build.gradle.kts: FOUND
- kamper/modules/issues/build.gradle.kts: FOUND
- kamper/modules/cpu/src/androidMain/kotlin/com/smellouk/kamper/cpu/repository/CpuInfoRepositoryImpl.kt: FOUND

Commits verified:
- d2c9c38: FOUND
- c0bd7b6: FOUND
- 614053a: FOUND
