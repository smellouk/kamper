---
phase: 16
plan: "04"
subsystem: release-automation
tags: [release-automation, cocoapods, xcframework, ios, github-actions]
dependency_graph:
  requires: ["16-01", "16-02"]
  provides: [Kamper.podspec, .github/workflows/publish-cocoapods.yml]
  affects: [cocoapods-registry, xcframework-distribution]
tech_stack:
  added: [CocoaPods podspec, gh CLI release upload, pod trunk push]
  patterns: [remote-binary-pod, build-verify-zip-upload-push-ordering]
key_files:
  created:
    - Kamper.podspec
    - .github/workflows/publish-cocoapods.yml
  modified: []
decisions:
  - "Used LICENSE.txt (actual filename) instead of LICENSE in podspec :file reference — Rule 1 bug fix"
  - "Used gh release upload with --clobber instead of deprecated actions/upload-release-asset@v1 (plan specified this)"
  - "Added --skip-import-validation to pod trunk push to avoid chicken-and-egg CDN replication issue"
metrics:
  duration: "~1 minute"
  completed_date: "2026-04-28"
  tasks_completed: 2
  tasks_total: 3
  files_created: 2
  files_modified: 0
---

# Phase 16 Plan 04: CocoaPods XCFramework Distribution Summary

**One-liner:** CocoaPods binary pod distribution via GitHub Release XCFramework zip — podspec with Release Please annotation and macOS CI workflow enforcing build-verify-zip-upload-push ordering.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Create Kamper.podspec at repo root | b5e36ba | Kamper.podspec |
| 2 | Create publish-cocoapods.yml workflow | 721ae09 | .github/workflows/publish-cocoapods.yml |
| 3 | Local podspec lint + XCFramework build verification | CHECKPOINT | macOS human verify required |

## Files Created

### Kamper.podspec

Full content:

```ruby
Pod::Spec.new do |s|
  s.name         = 'Kamper'
  # x-release-please-version
  s.version      = '1.0.0'
  s.summary      = 'Kotlin Multiplatform performance monitoring library'
  s.description  = <<-DESC
    Kamper is a small Kotlin Multiplatform library that provides
    performance monitoring (CPU, FPS, memory, network) for your app.
  DESC
  s.homepage     = 'https://github.com/smellouk/kamper'
  s.license      = { :type => 'Apache-2.0', :file => 'LICENSE.txt' }
  s.author       = { 'Sidali Mellouk' => 'sidali.mellouk@zattoo.com' }
  s.platform     = :ios, '14.0'
  s.source       = {
    :http => "https://github.com/smellouk/kamper/releases/download/v#{s.version}/Kamper.xcframework.zip"
  }
  s.vendored_frameworks = 'Kamper.xcframework'
end
```

Key characteristics:
- `# x-release-please-version` annotation is on the line immediately above `s.version` (Pitfall 5 compliance)
- Source URL uses `v#{s.version}` tag format matching root-component Release Please tag format
- `vendored_frameworks` references `Kamper.xcframework` matching XCFramework baseName in `kamper/xcframework/build.gradle.kts`
- iOS platform set to `14.0` (aligns with project iOS targets)

### .github/workflows/publish-cocoapods.yml

Full content: macOS workflow triggered on `release: published`, filtered to skip `kamper/react-native-` tags.

Step sequence (Pitfall 3 compliance enforced):
1. Checkout (at release tag ref)
2. Configure JDK 17 (temurin)
3. Set up Gradle (gradle/actions/setup-gradle@v4)
4. Build XCFramework (`assembleKamperReleaseXCFramework --no-configuration-cache`)
5. Verify XCFramework output directory exists (fail-fast defensive check)
6. Zip XCFramework (in `release/` directory, produces `Kamper.xcframework.zip`)
7. Upload XCFramework to GitHub Release (`gh release upload` with `--clobber`)
8. Push to CocoaPods trunk (`pod trunk push Kamper.podspec --allow-warnings --skip-import-validation`)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Used LICENSE.txt instead of LICENSE in podspec**
- **Found during:** Task 1 — checked actual file existence
- **Issue:** Plan specified `:file => 'LICENSE'` but the actual license file is `LICENSE.txt` (verified with `ls LICENSE*` at repo root)
- **Fix:** Changed podspec to `:file => 'LICENSE.txt'`
- **Files modified:** Kamper.podspec
- **Commit:** b5e36ba

## Checkpoint Status

Task 3 is a `checkpoint:human-verify` gate (blocking). The automated executor cannot run XCFramework builds and `pod lib lint` locally. The checkpoint requires macOS with Xcode and CocoaPods installed.

**What to verify on macOS:**
1. `./gradlew :kamper:xcframework:assembleKamperReleaseXCFramework --no-configuration-cache` — BUILD SUCCESSFUL
2. `ls kamper/xcframework/build/XCFrameworks/release/Kamper.xcframework/` — shows ios-arm64/, ios-arm64_x86_64-simulator/ (or ios-arm64-simulator/), Info.plist
3. `zip -r Kamper.xcframework.zip Kamper.xcframework` in release dir — zip created
4. `pod lib lint Kamper.podspec --allow-warnings --skip-import-validation` — passes
5. `pod trunk me` — shows CocoaPods account (or run `pod trunk register sidali.mellouk@zattoo.com 'Sidali Mellouk' --description='kamper-ci'`)

**COCOAPODS_TRUNK_TOKEN status:** Pending — must be added to GitHub repository secrets at https://github.com/smellouk/kamper/settings/secrets/actions before first real release run.

## Threat Surface Scan

No new security surface beyond what was modeled in the plan's threat model:
- T-14-04-02: COCOAPODS_TRUNK_TOKEN stored as GitHub secret (pending human setup)
- T-14-04-03: Step ordering enforces upload-before-push (Verify XCFramework output step added)
- T-14-04-05: `permissions: contents: write` minimum scope for gh release upload

## Known Stubs

None — both files are fully wired production configuration (not stubs).

## Self-Check: PASSED

- Kamper.podspec exists at repo root: FOUND
- .github/workflows/publish-cocoapods.yml exists: FOUND
- Task 1 commit b5e36ba: present in git log
- Task 2 commit 721ae09: present in git log
