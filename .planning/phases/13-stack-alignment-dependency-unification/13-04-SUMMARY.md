---
phase: 13-stack-alignment-dependency-unification
plan: "04"
subsystem: infra
tags: [github-actions, ci-cd, gradle, java, workflow]

# Dependency graph
requires:
  - phase: 12-kotlin-gradle-first-monorepo-consolidation
    provides: composite build with convention plugins and version catalog
provides:
  - Both GitHub Actions workflow files modernised to v4 actions (checkout@v4, setup-java@v4 temurin/17, gradle/actions/setup-gradle@v4)
  - publisher.yml split into two parallel jobs: publish-linux (ubuntu-latest) and publish-apple (macos-latest)
  - pull-request.yml updated to v4 actions, single ubuntu-latest job retained
affects: [14-react-native-package-library-engine-ui, any CI/CD pipeline change]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "GitHub Actions v4 actions: actions/checkout@v4, actions/setup-java@v4, gradle/actions/setup-gradle@v4"
    - "Publisher job split pattern: ubuntu-latest for Android/JVM/JS, macos-latest for iOS/macOS/tvOS"
    - "Gradle-aware CI caching via gradle/actions/setup-gradle@v4 (replaces actions/cache@v2 with manual key)"
    - "Secrets policy: workflow-level env block only — never echoed in run: steps"

key-files:
  created: []
  modified:
    - .github/workflows/pull-request.yml
    - .github/workflows/publisher.yml

key-decisions:
  - "publisher.yml split into ubuntu-latest (Android/JVM/JS/WASM) and macos-latest (iOS/macOS/tvOS) parallel jobs (D-12)"
  - "pull-request.yml stays on ubuntu-latest single job per D-13 — PR checks only verify Android/JVM/JS builds"
  - "Both jobs use publishAllPublicationsToGithubPackagesRepository — Gradle KMP plugin handles target-awareness per runner"
  - "gradle/actions/setup-gradle@v4 replaces actions/cache@v2 — provides configuration cache awareness"
  - "KAMPER_GH_USER and KAMPER_GH_PAT secrets remain in workflow-level env block only (T-11-04-01 mitigation)"

patterns-established:
  - "CI/CD pattern: split KMP publisher by runner (ubuntu for JVM targets, macos for Apple targets)"

requirements-completed: [ALIGN-D11, ALIGN-D12, ALIGN-D13]

# Metrics
duration: 2min
completed: 2026-04-26
---

# Phase 13 Plan 04: GitHub Actions CI/CD Modernisation Summary

**Both GitHub Actions workflows updated to v4 actions (checkout@v4, setup-java@v4 temurin/17, setup-gradle@v4) with publisher split into parallel ubuntu+macos jobs for KMP target-aware publishing**

## Performance

- **Duration:** ~2 min
- **Started:** 2026-04-26T21:41:09Z
- **Completed:** 2026-04-26T21:43:04Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- Updated pull-request.yml to v4 actions (checkout@v4, setup-java@v4 temurin/17, gradle/actions/setup-gradle@v4); single ubuntu-latest job retained per D-13 (Task 1)
- Rewrote publisher.yml with two parallel jobs — publish-linux (ubuntu-latest) and publish-apple (macos-latest) — both using v4 actions; secrets remain in workflow-level env block only (Task 2)

## Task Commits

Each task was committed atomically:

1. **Task 1: Modernise pull-request.yml to v4 actions** - `782b538` (feat)
2. **Task 2: Split publisher.yml into ubuntu+macos jobs** - `4923594` (feat)

## Files Created/Modified

- `.github/workflows/pull-request.yml` - actions/checkout@v2 → @v4, actions/cache@v2 → gradle/actions/setup-gradle@v4, actions/setup-java@v1 (adopt/11) → @v4 (temurin/17)
- `.github/workflows/publisher.yml` - single macos-latest job split into publish-linux (ubuntu-latest) + publish-apple (macos-latest), all actions updated to v4

## Decisions Made

- Both jobs run `publishAllPublicationsToGithubPackagesRepository` rather than per-target tasks — the KMP Gradle plugin is target-aware and only publishes targets compilable on the current runner. Task discovery (`./gradlew tasks --group=publishing`) returned no output due to the pre-existing React Native node_modules issue, confirming the existing task name is the correct one.
- `gradle/actions/setup-gradle@v4` replaces `actions/cache@v2` with a Gradle-native caching approach that understands configuration cache entries.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

- `./gradlew tasks --group=publishing` failed due to pre-existing React Native demo node_modules missing — confirmed deferred item from Plan 13-01. The task name `publishAllPublicationsToGithubPackagesRepository` was already present in the current publisher.yml, confirming it as correct.

## Threat Model Coverage

| Threat | Mitigation Applied |
|--------|--------------------|
| T-11-04-01: KAMPER_GH_PAT secret disclosure via logs | Secrets only in workflow-level env block; no `echo` of secrets in any `run:` step |
| T-11-04-02: Poison PR accessing publishing secrets | Publisher is tag-triggered only; PR workflow has no publish tasks |
| T-11-04-03: Deprecated action causing workflow failure | All actions updated to v4; temurin/17 matches project JDK requirement |

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Both workflow files are up to date with v4 GitHub Actions
- Publisher is split by runner — CI minute costs are optimised (linux targets run cheap ubuntu, Apple targets run macos)
- Ready for Plan 13-05 or subsequent dependency update plans

---
*Phase: 13-stack-alignment-dependency-unification*
*Completed: 2026-04-26*

## Self-Check: PASSED

Files verified:
- .github/workflows/pull-request.yml: FOUND
- .github/workflows/publisher.yml: FOUND

Commits verified:
- 782b538: pull-request.yml modernisation
- 4923594: publisher.yml two-job split
