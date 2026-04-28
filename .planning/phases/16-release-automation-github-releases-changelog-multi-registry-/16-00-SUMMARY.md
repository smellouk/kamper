---
phase: 16
plan: "00"
subsystem: release-automation
tags: [release-automation, wave-0, stubs, tooling, ci-cd]
dependency_graph:
  requires: []
  provides:
    - release-please-config.json stub
    - .release-please-manifest.json stub
    - .github/workflows/release-please.yml stub
    - .github/workflows/publish-kotlin.yml stub
    - .github/workflows/publish-cocoapods.yml stub
    - .github/workflows/publish-npm.yml stub
    - Kamper.podspec stub
  affects:
    - Plans 16-01 through 16-07 (all consume stub files via test -f checks)
tech_stack:
  added:
    - actionlint 1.7.12 (GitHub Actions workflow linter, brew)
    - yamllint 1.38.0 (YAML linter, brew)
    - PyYAML 6.0.3 (Python YAML library for verification)
  patterns:
    - Wave 0 stub pattern: minimal valid files that satisfy test -f but fail Wave 1+ structural greps
key_files:
  created:
    - release-please-config.json
    - .release-please-manifest.json
    - .github/workflows/release-please.yml
    - .github/workflows/publish-kotlin.yml
    - .github/workflows/publish-cocoapods.yml
    - .github/workflows/publish-npm.yml
    - Kamper.podspec
  modified: []
decisions:
  - Stubs use STUB token so Wave 1+ structural greps cannot accidentally pass on unfilled stubs
  - actionlint + yamllint installed via brew on macOS executor (1.7.12 and 1.38.0 respectively)
  - All YAML stubs use workflow_dispatch trigger only (no on:push) to prevent accidental CI runs
metrics:
  duration: "~8 minutes (includes brew auto-update and actionlint dependency shellcheck install)"
  completed: "2026-04-27"
  tasks_completed: 2
  files_created: 7
---

# Phase 16 Plan 00: Wave 0 Bootstrap — Validation Tooling + Stub Files Summary

Wave 0 bootstrap installing actionlint 1.7.12 + yamllint 1.38.0 via brew and creating seven intentionally-minimal stub files at exact paths that Wave 1–3 plans will overwrite.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Install actionlint + yamllint (idempotent) | (no files) | Tool install only |
| 2 | Create JSON and YAML stub files | 7df800a | 7 stub files created |

## Tooling Install State

| Tool | Version | Install Method | Status |
|------|---------|----------------|--------|
| actionlint | 1.7.12 | `brew install actionlint` | Installed (with shellcheck 0.11.0 dependency) |
| yamllint | 1.38.0 | `brew install yamllint` | Installed |
| PyYAML | 6.0.3 | `pip3 install pyyaml --break-system-packages` | Installed (needed for verification) |

## Stub Files Created

| File | Format | STUB Token | Wave 1+ Structural Token ABSENT |
|------|--------|------------|--------------------------------|
| `release-please-config.json` | JSON (valid) | YES | `googleapis/release-please-action@v4` absent |
| `.release-please-manifest.json` | JSON (valid) | YES (version `0.0.0-STUB`) | `release-notes-body` absent |
| `.github/workflows/release-please.yml` | YAML (valid) | YES | `googleapis/release-please-action@v4` absent |
| `.github/workflows/publish-kotlin.yml` | YAML (valid) | YES | `publishToMavenCentral` absent |
| `.github/workflows/publish-cocoapods.yml` | YAML (valid) | YES | `assembleKamperReleaseXCFramework` absent |
| `.github/workflows/publish-npm.yml` | YAML (valid) | YES | `npm publish` absent |
| `Kamper.podspec` | Ruby (minimal) | YES | `vendored_frameworks` absent |

All 7 files verified:
- `test -f <path>` exits 0 for each
- `git check-ignore` returns nothing (all trackable by git)
- JSON files pass `jq .`
- YAML files pass `python3 -c "import yaml; yaml.safe_load(...)"`
- None contain Wave 1+ structural grep tokens

## Stub Failure Confirmation

Each stub deliberately fails its corresponding Wave 1+ plan structural grep:

| Stub | Wave 1+ Plan | Grep that FAILS on stub |
|------|-------------|------------------------|
| `release-please-config.json` | Plan 16-02 Task 1 | `grep "googleapis/release-please-action@v4"` |
| `.release-please-manifest.json` | Plan 16-02 Task 2 | `grep "release-notes-body"` |
| `.github/workflows/release-please.yml` | Plan 16-02 Task 3 | `grep "googleapis/release-please-action@v4"` |
| `.github/workflows/publish-kotlin.yml` | Plan 16-03 Task 3 | `grep "publishToMavenCentral"` |
| `.github/workflows/publish-cocoapods.yml` | Plan 16-04 Task 2 | `grep "assembleKamperReleaseXCFramework"` |
| `.github/workflows/publish-npm.yml` | Plan 16-05 Task 1 | `grep "npm publish"` |
| `Kamper.podspec` | Plan 16-04 Task 1 | `grep "vendored_frameworks"` |

## Deviations from Plan

None — plan executed exactly as written.

Minor note: `python3 -c "import yaml"` failed initially (no PyYAML installed). Installed via
`pip3 install pyyaml --break-system-packages` as a Rule 3 auto-fix (blocking issue for verification).
This is a local developer environment fix only; CI environments typically have PyYAML pre-installed.

## Known Stubs

All files created by this plan are intentional stubs. Each is listed in the stub table above
with the plan that will replace it. These stubs are tracked for Wave 1–3 plans.

## Threat Flags

None — stubs contain only placeholder content with `example.invalid` URLs and no real config or secrets.

## Self-Check

### Created files exist:
- release-please-config.json: FOUND
- .release-please-manifest.json: FOUND
- .github/workflows/release-please.yml: FOUND
- .github/workflows/publish-kotlin.yml: FOUND
- .github/workflows/publish-cocoapods.yml: FOUND
- .github/workflows/publish-npm.yml: FOUND
- Kamper.podspec: FOUND

### Commits exist:
- 7df800a: FOUND (chore(16-00): create Wave 0 stub files for release automation)

## Self-Check: PASSED
