---
phase: 16-release-automation-github-releases-changelog-multi-registry-
plan: "02"
subsystem: infra
tags: [release-automation, release-please, github-actions, ci, changelog, semver]

requires:
  - phase: 16-01
    provides: "gradle.properties with VERSION_NAME=1.0.0 and x-release-please-version annotation"

provides:
  - "release-please-config.json: two-component manifest (root simple + kamper/react-native node)"
  - ".release-please-manifest.json: initial 1.0.0 version state for both components"
  - ".github/workflows/release-please.yml: Release Please workflow triggering on push to develop"

affects:
  - "16-03 (Maven Central publish — triggered by releases_created output)"
  - "16-04 (CocoaPods publish — triggered by releases_created output)"
  - "16-05 (npm publish — triggered by rn_release_created output)"

tech-stack:
  added:
    - "googleapis/release-please-action@v4"
    - "Release Please manifest multi-component config"
  patterns:
    - "Two-component Release Please manifest: root (simple) + npm (node)"
    - "RELEASE_PLEASE_TOKEN PAT for downstream workflow triggering"
    - "Path-prefixed RN output (kamper/react-native--release_created) for gated npm publish"

key-files:
  created:
    - release-please-config.json
    - .release-please-manifest.json
    - .github/workflows/release-please.yml
  modified: []

key-decisions:
  - "Use RELEASE_PLEASE_TOKEN (PAT) instead of GITHUB_TOKEN — enables downstream on:release workflows to trigger (avoids GitHub recursion prevention)"
  - "target-branch: develop — matches repo convention confirmed in pull-request.yml"
  - "separate-pull-requests: false — both components share one version-bump PR to reduce noise"
  - "include-component-in-tag: false for root — root releases as v1.0.0; RN as kamper/react-native-v1.0.0"
  - "Pin release-please-action to @v4 — v5 just released but v4 is verified stable"
  - "bootstrap-sha left as placeholder — must be replaced with actual merge SHA before first Release Please run"
  - "Publish jobs NOT in this workflow — Plans 03/04/05 own separate workflows to isolate failures"

patterns-established:
  - "Release Please outputs (releases_created, tag_name, upload_url) exposed as job outputs for downstream consumers"
  - "RN-specific path-prefixed output rn_release_created gates npm publish to prevent spurious publishes on Kotlin-only changes"

requirements-completed: []

duration: 12min
completed: 2026-04-27
---

# Phase 16 Plan 02: Release Please Configuration and Workflow Summary

**Three-file Release Please engine: two-component manifest config (Kotlin root + RN npm), initial 1.0.0 manifest, and workflow using RELEASE_PLEASE_TOKEN PAT to open version-bump PRs from Conventional Commits on develop**

## Performance

- **Duration:** 12 min
- **Started:** 2026-04-27T09:00:00Z
- **Completed:** 2026-04-27T09:12:00Z
- **Tasks:** 3 of 4 completed (Task 4 is a blocking human-action checkpoint — not executor work)
- **Files modified:** 3

## Accomplishments

- Created `release-please-config.json` with two-component manifest (root `simple` + `kamper/react-native` `node`) pointing the generic updater at `gradle.properties` via the `x-release-please-version` annotation established in Plan 01
- Created `.release-please-manifest.json` initializing both components to `1.0.0`, matching the current library version
- Created `.github/workflows/release-please.yml` as the Release Please trigger workflow — runs on push to `develop`, uses `RELEASE_PLEASE_TOKEN` PAT for downstream workflow compatibility, exposes both root and RN-specific outputs for Plans 03-05

## Task Commits

Each task was committed atomically:

1. **Task 1: Create release-please-config.json** - `470ab5e` (feat)
2. **Task 2: Create .release-please-manifest.json** - `e8da16d` (feat)
3. **Task 3: Create .github/workflows/release-please.yml** - `a77f383` (feat)

_Task 4 is a blocking human checkpoint — no executor commit._

## Files Created/Modified

- `release-please-config.json` — Two-component Release Please manifest: root component uses `simple` release-type with `extra-files` generic updater on `gradle.properties`; `kamper/react-native` uses `node` release-type for npm versioning. Includes `bootstrap-sha` placeholder (must be replaced).
- `.release-please-manifest.json` — Initial version state: both `.` and `kamper/react-native` at `1.0.0`. Release Please overwrites this file on each version-bump PR.
- `.github/workflows/release-please.yml` — Release Please workflow: triggers on `push: develop`, uses `RELEASE_PLEASE_TOKEN` PAT, pins to `@v4`, exposes `releases_created`/`tag_name`/`upload_url` plus `rn_release_created`/`rn_tag_name` for downstream gating. Verified with `actionlint` (pass).

## Decisions Made

- **PAT over GITHUB_TOKEN:** `RELEASE_PLEASE_TOKEN` (fine-grained PAT) used instead of `GITHUB_TOKEN` so that GitHub Releases created by Release Please trigger downstream `on: release: types: [published]` workflows (GITHUB_TOKEN-created releases do not trigger other workflows — Pitfall 2 in RESEARCH.md).
- **target-branch: develop:** Required because Release Please defaults to `main`; this repo uses `develop` (confirmed in `pull-request.yml`).
- **bootstrap-sha placeholder:** Per Pitfall 6, the `bootstrap-sha` must be set to the merge-commit SHA to prevent Release Please from scanning pre-Phase-16 history and conflicting with existing tag patterns. Left as `REPLACE_WITH_HEAD_SHA_AT_MERGE_TIME` — developer MUST replace before first workflow run (Task 4 Step 3).
- **Publish jobs excluded:** No publish steps in this workflow. Plans 03/04/05 each own their own publish workflow so a Maven Central failure cannot block npm or CocoaPods.
- **@v4 pinned:** v5 of release-please-action was recently released (2026-04-22) but v4 remains the verified stable target per RESEARCH.md.

## Deviations from Plan

None — plan executed exactly as written. The stub files (`release-please-config.json` with `STUB` content, `.release-please-manifest.json` with `"0.0.0-STUB"`, and `release-please.yml` placeholder) were replaced with full implementations as planned.

## Issues Encountered

None. All three files validated (JSON parsing via `jq`, YAML parsing via `python3 yaml`, workflow linting via `actionlint`).

## User Setup Required

**Task 4 is a blocking human-action checkpoint.** Wave 2 plans (16-03 Maven Central, 16-04 CocoaPods, 16-05 npm) MUST NOT execute until the developer completes:

**Step 1 — Verify Sonatype Maven Central namespace `com.smellouk`:**
1. Visit https://central.sonatype.com → Namespaces → Add Namespace → enter `com.smellouk`
2. Verify via DNS TXT record
3. Generate User Token at https://central.sonatype.com/account → "Generate User Token" (save username+password pair as `SONATYPE_USERNAME` + `SONATYPE_PASSWORD`)

**Step 2 — Create RELEASE_PLEASE_TOKEN secret:**
1. Create fine-grained PAT at https://github.com/settings/tokens?type=beta with:
   - Permissions: Contents=Read+Write, Pull Requests=Read+Write, Issues=Read+Write
   - Scoped to `smellouk/kamper` repository only
2. Store as repository secret `RELEASE_PLEASE_TOKEN` at https://github.com/smellouk/kamper/settings/secrets/actions

**Step 3 — Replace bootstrap-sha placeholder:**
After this plan's commits land on `develop`:
```bash
git rev-parse HEAD
```
Replace `REPLACE_WITH_HEAD_SHA_AT_MERGE_TIME` in `release-please-config.json` with the actual SHA. Commit as `chore(release): set bootstrap-sha for Release Please`.

**Verification:**
- Sonatype: namespace shows "Verified" at https://central.sonatype.com/publishing/namespaces
- GitHub: `RELEASE_PLEASE_TOKEN` visible in repository secrets list
- Bootstrap SHA: `grep "bootstrap-sha" release-please-config.json` shows a 40-char hex SHA

Reply with `approved` once all three sub-steps are complete to unblock Wave 2.

## Next Phase Readiness

- `release-please-config.json`, `.release-please-manifest.json`, `.github/workflows/release-please.yml` are committed and correct
- **Wave 2 is BLOCKED on Task 4 human checkpoint.** Plans 16-03, 16-04, 16-05 must not start until `approved` received.
- Once Task 4 is approved, push a `feat:` commit to `develop` to verify Release Please opens a version-bump PR within ~5 minutes

---
*Phase: 16-release-automation-github-releases-changelog-multi-registry-*
*Completed: 2026-04-27*
