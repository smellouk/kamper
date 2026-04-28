---
phase: 16-release-automation-github-releases-changelog-multi-registry-
plan: 06
subsystem: infra
tags: [ci, github-actions, commitlint, conventional-commits, release-automation]

# Dependency graph
requires:
  - phase: 16-release-automation-github-releases-changelog-multi-registry-
    provides: "Plans 02-05: release-please.yml, publish-kotlin.yml, publish-cocoapods.yml, publish-npm.yml"
provides:
  - Modernized pull-request.yml with v4 GitHub Actions, JDK 17 temurin, and parallel commitlint job
  - Conventional Commits PR title enforcement via amannn/action-semantic-pull-request@v6
  - Deletion of legacy publisher.yml (replaced by Plans 02-05 workflows)
affects: [future-ci-changes, developer-workflow, release-automation]

# Tech tracking
tech-stack:
  added: [amannn/action-semantic-pull-request@v6]
  patterns:
    - Parallel CI jobs for independent checks (gradle vs commitlint)
    - PR title Conventional Commits enforcement as release pipeline gate

key-files:
  created: []
  modified:
    - .github/workflows/pull-request.yml
  deleted:
    - .github/workflows/publisher.yml

key-decisions:
  - "actions/cache@v4 with cache-gradle key preserved verbatim — D-09 says modernize action versions, not key strategy"
  - "amannn/action-semantic-pull-request@v6 with zero with: block — defaults accept all standard CC types"
  - "commitlint job runs in parallel with gradle job — no needs: dependency preserves wall-clock time"
  - "publisher.yml deleted — its responsibilities fully covered by Plans 02-05 workflows"

patterns-established:
  - "Pattern: Commitlint as parallel CI job, not a sequential gate — fast feedback without slowing builds"
  - "Pattern: permissions: pull-requests: read minimum scope for PR-title-reading actions"

requirements-completed: []

# Metrics
duration: 8min
completed: 2026-04-28
---

# Phase 16 Plan 06: CI Modernization and Commitlint Enforcement Summary

**Modernized pull-request.yml to v4 actions + JDK 17 temurin, added parallel commitlint job via amannn/action-semantic-pull-request@v6, and deleted legacy publisher.yml replaced by Plans 02-05**

## Performance

- **Duration:** ~8 min
- **Started:** 2026-04-28T00:00:00Z
- **Completed:** 2026-04-28T00:08:00Z
- **Tasks:** 2
- **Files modified:** 2 (1 modified, 1 deleted)

## Accomplishments
- Upgraded pull-request.yml: actions/checkout@v4, actions/cache@v4, actions/setup-java@v4 with temurin + JDK 17
- Added parallel `commitlint` job using `amannn/action-semantic-pull-request@v6` (zero-config, all standard CC types accepted)
- Preserved all existing PR-checker Gradle commands: detekt, lint, test, assemble
- Deleted .github/workflows/publisher.yml — its tag-push trigger replaced by release/published triggers in Plans 02-05

## Task Commits

Each task was committed atomically:

1. **Task 1: Modernize pull-request.yml** - `888027e` (feat)
2. **Task 2: Delete legacy publisher.yml** - `dfea66a` (chore)

## Files Created/Modified
- `.github/workflows/pull-request.yml` - Modernized with v4 actions, JDK 17 temurin, cache@v4, parallel commitlint job
- `.github/workflows/publisher.yml` - DELETED (legacy tag-push publisher replaced by Plans 02-05)

## Current .github/workflows/ inventory
After this plan, the complete set of workflows:
- `pull-request.yml` — PR checks (gradle + commitlint, both parallel)
- `release-please.yml` — creates GitHub Releases on main pushes (Plan 02)
- `publish-kotlin.yml` — Maven Central + GitHub Packages publish on release (Plan 03)
- `publish-cocoapods.yml` — XCFramework + pod trunk push on release (Plan 04)
- `publish-npm.yml` — RN package npm publish on release (Plan 05)

## Decisions Made
- `actions/cache@v4` used instead of `gradle/actions/setup-gradle@v4` which was in the current file — plan specifies explicit cache@v4 with `cache-gradle` key for consistency
- `amannn/action-semantic-pull-request@v6` with no `with:` block — default config accepts all standard Conventional Commit types which is correct for this project
- Commitlint runs in parallel with gradle job (no `needs:` dependency) to preserve wall-clock time

## Deviations from Plan

**Note:** The existing pull-request.yml already had v4 actions (checkout@v4, setup-java@v4 with temurin/17), but was using `gradle/actions/setup-gradle@v4` instead of `actions/cache@v4`. The plan specified `actions/cache@v4` with explicit path configuration and `cache-gradle` key. Replaced as specified — this is consistent with the plan's interface specification.

Otherwise: None — plan executed exactly as written.

## Issues Encountered
None

## User Setup Required (Manual - NOT a file change)
**Branch protection configuration required:**
The commitlint job creates a new CI status check (`PR title lint`). For Conventional Commits enforcement to be mandatory before merge, a developer must configure branch protection in GitHub repo settings:
1. Go to Settings → Branches → Branch protection rules for `develop`
2. Enable "Require status checks to pass before merging"
3. Add `PR title lint` as a required status check

This is a one-time manual configuration in GitHub — it is NOT a file change and is out of scope for Phase 16.

## Threat Surface Scan
No new network endpoints, auth paths, or file access patterns introduced. The commitlint job reads PR titles via `GITHUB_TOKEN` with minimum `permissions: pull-requests: read` scope — consistent with T-14-06-01 and T-14-06-02 mitigations in the plan's threat model.

## Next Phase Readiness
- Phase 16 release automation is fully wired: release-please creates GitHub Releases, publish-kotlin/cocoapods/npm trigger on release events, commitlint enforces CC format on PRs
- Ready for wave merge and Phase 16 completion
- Manual step: enable branch protection on `develop` to require `PR title lint` status check

---
*Phase: 16-release-automation-github-releases-changelog-multi-registry-*
*Completed: 2026-04-28*
