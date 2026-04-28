---
status: complete
phase: 16-release-automation-github-releases-changelog-multi-registry-
source: [16-00-SUMMARY.md, 16-01-SUMMARY.md, 16-02-SUMMARY.md, 16-03-SUMMARY.md, 16-04-SUMMARY.md, 16-05-SUMMARY.md, 16-06-SUMMARY.md, 16-07-SUMMARY.md]
started: 2026-04-28T00:00:00Z
updated: 2026-04-28T00:00:00Z
---

## Current Test

[testing complete]

## Tests

### 1. Version source of truth in gradle.properties
expected: gradle.properties contains VERSION_NAME=1.0.0 with # x-release-please-version annotation on the line immediately above it. build.gradle.kts reads it via java.util.Properties.
result: pass

### 2. Release Please config — component paths
expected: release-please-config.json has two packages "." and "kamper/ui/rn". RN package-name is "react-native-kamper" matching kamper/ui/rn/package.json.
result: pass

### 3. Release Please config — bootstrap SHA
expected: bootstrap-sha is a real 40-char hex SHA (not the placeholder).
result: pass

### 4. Kamper.podspec — version annotation and extra-files
expected: Kamper.podspec has # x-release-please-version above s.version. Podspec listed in extra-files in release-please-config.json.
result: pass

### 5. publish-kotlin.yml — secrets and tag filter
expected: Triggers on release:published, skips kamper/ui/rn- tags, wires SONATYPE_USERNAME/PASSWORD and SIGNING_KEY env vars.
result: pass

### 6. publish-cocoapods.yml — step ordering and token
expected: Runs on macos-latest. Step order: Build XCFramework → Verify → Zip → Upload (--clobber) → pod trunk push. COCOAPODS_TRUNK_TOKEN wired.
result: pass

### 7. publish-npm.yml — path and package name
expected: working-directory is kamper/ui/rn. Tag filter is kamper/ui/rn-. NODE_AUTH_TOKEN from NPM_TOKEN. --provenance flag present.
result: pass

### 8. pull-request.yml — commitlint and legacy cleanup
expected: Parallel commitlint job with action-semantic-pull-request@v6. JDK 17 temurin. publisher.yml deleted.
result: pass

### 9. BOM module
expected: kamper/bom/build.gradle.kts exists with all 9 module constraints. settings.gradle.kts includes :kamper:bom.
result: pass

### 10. Release notes body
expected: Root component body has BOM platform() snippet. No ui-android mention. {{version}} token present.
result: pass

## Summary

total: 10
passed: 10
issues: 0
pending: 0
skipped: 0
blocked: 0

## Gaps
