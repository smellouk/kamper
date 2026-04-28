---
phase: 16
plan: "05"
subsystem: release-automation
tags: [release-automation, npm, react-native, github-actions]
dependency_graph:
  requires:
    - "16-02 (release-please.yml with RELEASE_PLEASE_TOKEN PAT, enabling on:release:published triggers)"
  provides:
    - "publish-npm.yml workflow for automated kamper-react-native npm publishing"
  affects:
    - "npm registry (kamper-react-native package)"
tech_stack:
  added:
    - "actions/setup-node@v4 with registry-url for .npmrc generation"
    - "npm provenance attestation via --provenance flag + id-token: write"
  patterns:
    - "on:release:published filtered to kamper/react-native- tag prefix"
    - "working-directory default for all run steps"
    - "NODE_AUTH_TOKEN from secrets.NPM_TOKEN via setup-node@v4 .npmrc injection"
key_files:
  created:
    - ".github/workflows/publish-npm.yml"
  modified: []
decisions:
  - "Use on:release:published trigger (not in-workflow npm job) to keep npm publish isolated from Gradle release events"
  - "Job-level if filter startsWith('kamper/react-native-') ensures RN-only triggering; root releases do not trigger npm publish (Pitfall 7 prevention)"
  - "npm publish --access public --provenance: public flag handles potential scoped name edge case; provenance adds supply-chain security"
  - "id-token: write permission added to job for Sigstore provenance attestation"
metrics:
  duration: "77s"
  completed_date: "2026-04-28"
  tasks_completed: 1
  tasks_total: 2
  files_created: 1
  files_modified: 0
---

# Phase 16 Plan 05: npm Publish Workflow Summary

**One-liner:** GitHub Actions workflow publishing kamper-react-native to npm on RN-prefixed Release Please tags with provenance attestation.

## Status: PAUSED AT CHECKPOINT

Plan paused at Task 2 (human-verify checkpoint) after completing Task 1.

## Completed Tasks

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Create .github/workflows/publish-npm.yml | 8b06bc1 | .github/workflows/publish-npm.yml |

## What Was Built

`.github/workflows/publish-npm.yml` is a GitHub Actions workflow that:

- Triggers on `release: published` events filtered to tags starting with `kamper/react-native-`
- Runs on `ubuntu-latest` with Node 22 via `actions/setup-node@v4`
- Sets `defaults.run.working-directory: kamper/react-native` for all steps
- Runs `npm ci` (lockfile-exact install), `npm run build --if-present`, and `npm publish --access public --provenance`
- Authenticates via `NODE_AUTH_TOKEN: ${{ secrets.NPM_TOKEN }}` (injected into `.npmrc` by `setup-node@v4`)
- Grants `id-token: write` permission for npm provenance attestation via Sigstore

```yaml
# .github/workflows/publish-npm.yml (key sections)
jobs:
  publish:
    if: ${{ startsWith(github.event.release.tag_name, 'kamper/react-native-') }}
    runs-on: ubuntu-latest
    permissions:
      contents: read
      id-token: write
    defaults:
      run:
        working-directory: kamper/react-native
```

## Checkpoint Verification Results (automated)

- **NPM_TOKEN secret:** MISSING — must be configured before first release
- **kamper-react-native package name:** AVAILABLE (HTTP 404 on npmjs.com registry) — name is free to claim

## Deviations from Plan

None — plan executed exactly as written. The stub `publish-npm.yml` was replaced with the full implementation.

## Known Stubs

None — the workflow is complete. The stub placeholder has been fully replaced.

## Threat Flags

No new threat surface beyond what is documented in the plan's threat model. The workflow correctly:
- Scopes `NPM_TOKEN` to `NODE_AUTH_TOKEN` env var (masked by GitHub Actions)
- Uses `--provenance` for Sigstore-signed artifact attestation (T-14-05-02 mitigated)
- Filters on RN-only tag prefix (T-14-05-03 mitigated — wrong package publish prevented)

## Self-Check

- [x] .github/workflows/publish-npm.yml created and exists
- [x] Commit 8b06bc1 exists
- [x] actionlint exits 0 (no workflow errors)
- [x] All acceptance criteria pass
---
## Self-Check: PASSED
