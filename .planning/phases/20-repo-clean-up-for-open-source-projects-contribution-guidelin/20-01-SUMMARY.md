---
phase: 20-repo-clean-up-for-open-source-projects-contribution-guidelin
plan: "01"
subsystem: github-templates
tags:
  - documentation
  - github-templates
  - oss-readiness
dependency_graph:
  requires: []
  provides:
    - .github/ISSUE_TEMPLATE/bug-report.yml
    - .github/ISSUE_TEMPLATE/feature-request.yml
    - .github/PULL_REQUEST_TEMPLATE.md
  affects: []
tech_stack:
  added: []
  patterns:
    - GitHub Forms YAML schema for structured issue templates
key_files:
  created:
    - .github/ISSUE_TEMPLATE/bug-report.yml
    - .github/ISSUE_TEMPLATE/feature-request.yml
  modified:
    - .github/PULL_REQUEST_TEMPLATE.md
decisions:
  - GitHub Forms YAML chosen over legacy Markdown templates for structured bug reports (required fields, dropdowns)
  - PR template fully replaced (not patched) to eliminate all legacy emoji/develop-branch content
  - feature-request.yml keeps proposed_api and alternatives optional to reduce contributor friction
metrics:
  duration: "77 seconds"
  completed: "2026-04-29"
  tasks_completed: 2
  tasks_total: 2
  files_changed: 3
---

# Phase 20 Plan 01: GitHub Issue and PR Templates Summary

Two new GitHub Forms YAML issue templates and a conventions-aligned PR template replacement implementing D-03 and D-04 of the open-source readiness requirements.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Create bug-report and feature-request GitHub Forms templates (D-03) | 5889558 | .github/ISSUE_TEMPLATE/bug-report.yml, .github/ISSUE_TEMPLATE/feature-request.yml |
| 2 | Replace .github/PULL_REQUEST_TEMPLATE.md (D-04) | 8cd35d4 | .github/PULL_REQUEST_TEMPLATE.md |

## What Was Built

### bug-report.yml
Structured GitHub Forms template requiring:
- Description, Reproduction steps, Expected behavior, Actual behavior (all required)
- Platform dropdown with all 7 KMP targets: Android, iOS, JVM, macOS, tvOS, JS, WasmJS
- Kamper version (required), Module(s) affected (optional)
- Labels: `["bug", "triage"]`

### feature-request.yml
Structured GitHub Forms template with:
- Use case (required)
- Proposed API or behavior (optional)
- Alternatives considered (optional)
- Label: `["enhancement"]`

### PULL_REQUEST_TEMPLATE.md (full replacement)
Replaced the emoji-laden, develop-branch-referencing legacy template with:
- "What changed and why" section header
- Four-item checklist mirroring CLAUDE.md PR checklist exactly:
  1. `./gradlew detekt` passes (zero issues)
  2. `./gradlew :kamper:modules:<name>:jvmTest` passes for every touched module
  3. No `TODO:` or `FIXME:` in changed files
  4. Public API changes are additive only (no removals or signature breaks)

## Verification Results

```
All three files exist:       PASS
bug-report.yml name field:   PASS (name: Bug Report)
feature-request.yml name:    PASS (name: Feature Request)
All 7 KMP platforms present: PASS (Android through WasmJS)
All required fields present: PASS (description, reproduction, expected, actual, version, module)

PR template checks:
  emojis (🚀📄🧪✅📷):       0 (PASS)
  develop branch refs:        0 in PULL_REQUEST_TEMPLATE.md (PASS)
  ## What changed and why:   1 (PASS)
  ## Checklist:              1 (PASS)
  checklist items (- [ ]):   4 (PASS)
  gradlew detekt:            1 (PASS)
  gradlew :kamper:modules:   1 (PASS)
  TODO: reference:           1 (PASS - in checklist only)
  additive only:             1 (PASS)
  line count:                8 (PASS - within 6-10 range)
```

Note: The overall stale-reference grep `grep -rE "develop"` across ISSUE_TEMPLATE/ matches the word "developer" in feature-request.yml line 18 (`description: How would a developer use this feature?`). This is a false positive — "developer" is the English word for a software engineer, not a reference to the `develop` branch. Zero actual develop-branch references exist in any of the three files.

## Deviations from Plan

None — plan executed exactly as written.

## Threat Flags

None — these are static documentation files with no network endpoints, auth paths, or data access.

## Known Stubs

None — all fields are wired to GitHub Forms rendering; no placeholder data sources.

## Self-Check

- [x] `.github/ISSUE_TEMPLATE/bug-report.yml` exists
- [x] `.github/ISSUE_TEMPLATE/feature-request.yml` exists
- [x] `.github/PULL_REQUEST_TEMPLATE.md` exists (replaced)
- [x] Commit 5889558 exists (Task 1)
- [x] Commit 8cd35d4 exists (Task 2)
