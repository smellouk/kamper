---
status: partial
phase: 16-release-automation-github-releases-changelog-multi-registry-
source: [16-VERIFICATION.md]
started: 2026-04-28T00:00:00Z
updated: 2026-04-28T00:00:00Z
---

## Current Test

[awaiting human testing]

## Tests

### 1. GitHub Actions secrets configured
expected: All 8 secrets exist in https://github.com/smellouk/kamper/settings/secrets/actions
result: [pending]

### 2. Sonatype namespace verified
expected: com.smellouk shows "Verified" at central.sonatype.com
result: [pending]

### 3. CocoaPods trunk authenticated
expected: pod trunk me returns valid session; pod lib lint Kamper.podspec passes on macOS
result: [pending]

### 4. Release Please dry run
expected: Push a feat: commit to develop → Release Please PR opens within ~5 min
result: [pending]

### 5. RELEASE_PLEASE_TOKEN PAT scopes
expected: PAT has contents:write, pull-requests:write, issues:write on smellouk/kamper
result: [pending]

### 6. Version manifest reconciliation
expected: After first Release Please run, .release-please-manifest.json RN version aligns with kamper/ui/rn/package.json
result: [pending]

## Summary

total: 6
passed: 0
issues: 0
pending: 6
skipped: 0
blocked: 0

## Gaps
