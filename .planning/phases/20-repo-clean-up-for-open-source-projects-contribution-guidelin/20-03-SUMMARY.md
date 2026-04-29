---
phase: 20-repo-clean-up-for-open-source-projects-contribution-guidelin
plan: "03"
subsystem: documentation
tags:
  - documentation
  - readme
  - oss-readiness
  - security-md-removal
dependency_graph:
  requires: []
  provides:
    - README.md with Maven Central installation block
    - README.md Versioning section
    - SECURITY.md removed
  affects:
    - README.md
    - SECURITY.md (deleted)
tech_stack:
  added: []
  patterns:
    - Surgical README edits (three localized changes, no full rewrite)
key_files:
  created: []
  modified:
    - README.md
  deleted:
    - SECURITY.md
decisions:
  - Replace GitHub Packages block with Maven Central (no repositories{} block needed — mavenCentral() is the standard)
  - Use versioned "1.0.0" coordinate in install block rather than "<latest-version>" placeholder (per RESEARCH.md verified artifact map)
  - Omit CI badge — pull-request.yml targets develop branch only; badge would render misleading
  - Delete SECURITY.md entirely without archiving any content per D-05 (lines 102-162 contain internal Phase 14 audit data)
metrics:
  duration_minutes: 5
  completed_date: "2026-04-29"
  tasks_completed: 2
  tasks_total: 2
  files_changed: 2
---

# Phase 20 Plan 03: README and SECURITY.md Refresh Summary

README.md surgical update to Maven Central install block, Security Considerations removal, and Versioning section addition; SECURITY.md deleted in full per D-05 to eliminate internal Phase 14 audit content leak path.

## What Was Built

### Task 1: README.md Surgical Changes (commit 7b1105a)

Three localized edits applied to README.md (585 lines before, 554 lines after, -31 lines net):

**Change 1 — Installation block replacement (lines 91-120):**
- Removed: GitHub Packages repository block (`maven("https://maven.pkg.github.com/smellouk/kamper")`) and the associated `<latest-version>` dependency block
- Added: Maven Central installation block with pinned `1.0.0` version for all 10 artifacts (engine + 8 modules + ui-android)
- Added: advisory note that `mavenCentral()` must be in the `repositories {}` block

**Change 2 — Security Considerations section deletion (lines 494-535):**
- Removed 42-line section `## Security Considerations` entirely
- Content duplicated SECURITY.md and exposed `KamperUiInitProvider` implementation internals
- No sentences preserved — per plan Pitfall 2 guidance

**Change 3 — Versioning section insertion (before `## Contributing`):**
- Added 8-line section with semver explanation (Patch/Minor/Major)
- Links to `https://github.com/smellouk/kamper/releases` (GitHub Releases)
- Links to `CHANGELOG.md`

### Task 2: SECURITY.md Deletion (commit 80814fe)

- `git rm SECURITY.md` removed the 162-line file from both working tree and git index
- Lines 1-101: public-facing security policy (vulnerability reporting, contact email, response timeline)
- Lines 102-162: internal Phase 14 audit (threat IDs T-12-W0-01 through T-12-21, ASVS Level 1 annotations, accepted risks log)
- No content copied or archived — deletion is the only correct action per D-05 and RESEARCH.md Pitfall 3

## Verification Results

OSS-02g verification script — all 5 PASS lines printed:

```
=== Stale references check ===
PASS: no develop branch refs in README
PASS: no GPR URL in README
PASS: no TODO/FIXME in public files
PASS: SECURITY.md deleted
PASS: no internal audit content leaked
```

Full acceptance criteria results:

| Check | Result |
|-------|--------|
| `test -f README.md` | PASS |
| `wc -l README.md` >= 400 | PASS (554 lines) |
| `grep -c "maven.pkg.github.com" README.md` = 0 | PASS |
| `grep -c "GitHub Packages" README.md` = 0 | PASS |
| `grep -c "com.smellouk.kamper:engine" README.md` >= 1 | PASS |
| `grep -c "com.smellouk.kamper:cpu-module" README.md` >= 1 | PASS |
| All 9 module coordinates present | PASS |
| `grep -c "## Installation" README.md` = 1 | PASS |
| `grep -c "## Versioning" README.md` = 1 | PASS |
| `grep -c "## Security Considerations" README.md` = 0 | PASS |
| `grep -c "KamperUiInitProvider" README.md` = 0 | PASS |
| `grep -c "github.com/smellouk/kamper/releases"` >= 1 | PASS |
| `grep -c "CHANGELOG.md"` >= 1 | PASS |
| `grep -c "semver.org"` >= 1 | PASS |
| No `blob/develop/` links | PASS |
| Badge strip (shields.io count >= 3) | PASS (4 badges) |
| Platform matrix preserved | PASS (32 matches for Android/iOS/JVM/macOS/tvOS) |
| Service Integrations preserved | PASS (14 matches for Sentry/Firebase/OpenTelemetry) |
| `test ! -f SECURITY.md` | PASS |
| `git ls-files SECURITY.md` empty | PASS |
| No T-12-* threat IDs leaked | PASS |
| No ASVS Level annotations leaked | PASS |
| No Phase 14 references leaked | PASS |
| No SECURITY*.md files remain | PASS |

## Deviations from Plan

None — plan executed exactly as written.

The discretionary items noted in Task 1 were honored:
- No CI badge added (pull-request.yml targets `develop` branch; badge would render misleading)
- No CODE_OF_CONDUCT.md added (separate file, not in scope for this plan)
- License footer preserved unchanged

## Known Stubs

None. README.md is fully wired with real Maven Central coordinates at version `1.0.0`.

## Threat Flags

No new threat surface introduced. Both changes are documentation-only — no code paths, network endpoints, auth flows, or schema changes were added.

## Self-Check: PASSED

- README.md exists at expected path: CONFIRMED
- SECURITY.md absent: CONFIRMED
- Commit 7b1105a (README refresh): CONFIRMED
- Commit 80814fe (SECURITY.md deletion): CONFIRMED
- All 24 acceptance criteria: PASS
