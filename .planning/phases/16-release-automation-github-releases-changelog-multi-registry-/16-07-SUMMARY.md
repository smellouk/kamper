---
phase: 16-release-automation-github-releases-changelog-multi-registry-
plan: "07"
subsystem: infra
tags: [release-please, release-automation, github-releases, changelog, maven-central, cocoapods, npm, xcframework]

requires:
  - phase: 16-release-automation-github-releases-changelog-multi-registry-
    provides: release-please-config.json created in Plan 02 with two-component manifest structure

provides:
  - release-notes-body template for root component (Gradle + CocoaPods + XCFramework installation snippets)
  - release-notes-body template for RN component (npm + yarn + iOS/Android/New Architecture snippets)
  - Both templates use {{version}} token for automated version substitution at PR-creation time

affects:
  - GitHub Release pages for kamper and kamper-react-native
  - Library consumers onboarding via GitHub Releases

tech-stack:
  added: []
  patterns:
    - "release-notes-body in release-please-config.json appends custom markdown after auto-generated changelog"
    - "{{version}} token templating — Release Please substitutes the released version at PR-creation time"
    - "jq --arg body pattern for heredoc-safe JSON string insertion (handles newlines, quotes, backticks)"

key-files:
  created: []
  modified:
    - release-please-config.json

key-decisions:
  - "jq --arg body + script-file approach used to avoid heredoc 4-space indentation pitfall that would render headings as <pre> code blocks on GitHub"
  - "Root component body covers all 4 distribution paths: Gradle Maven Central, CocoaPods, XCFramework direct download"
  - "RN component body includes New Architecture / TurboModule requirement to set clear compatibility expectations"

patterns-established:
  - "Heredoc indentation: write body to a script file at column 0, not inline with 4-space indentation — prevents GitHub markdown rendering bugs"

requirements-completed: []

duration: 10min
completed: 2026-04-28
---

# Phase 16 Plan 07: Release Notes Body Customization Summary

**Custom GitHub Release body templates added to release-please-config.json — both Kamper root component (Gradle + CocoaPods + XCFramework) and RN component (npm/yarn + New Architecture) now have installation snippet bodies with {{version}} templating**

## Performance

- **Duration:** ~10 min
- **Started:** 2026-04-28T00:00:00Z
- **Completed:** 2026-04-28T00:10:00Z
- **Tasks:** 2
- **Files modified:** 1

## Accomplishments

- Added `release-notes-body` to the root component in `release-please-config.json` covering Gradle Maven Central (engine + 4 metric modules + ui-android), CocoaPods Podfile line, and direct XCFramework download link
- Added `release-notes-body` to the `kamper/react-native` component covering npm install, yarn add, iOS pod install, Android autolinking, and New Architecture/TurboModule requirement
- All markdown headings verified at column 1 (no 4-space prefix indentation that would render as `<pre>` blocks on GitHub)
- All installation snippets use `{{version}}` token — Release Please substitutes the released version automatically

## Task Commits

Each task was committed atomically:

1. **Task 1: Add release-notes-body to root component (Gradle + CocoaPods + XCFramework snippets)** - `aef9d5e` (feat)
2. **Task 2: Add release-notes-body to RN component (npm install snippet)** - `d974e2a` (feat)

## Files Created/Modified

- `release-please-config.json` - Added `release-notes-body` to both the `.` root component and `kamper/react-native` component

## Root Component Body (rendered markdown)

```
## Installation

### Gradle (Maven Central)

Add to your `build.gradle.kts` (Kotlin DSL) or `build.gradle` (Groovy DSL):

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("com.smellouk.kamper:engine:{{version}}")
    // Optional metric modules:
    implementation("com.smellouk.kamper:cpu-module:{{version}}")
    implementation("com.smellouk.kamper:fps-module:{{version}}")
    implementation("com.smellouk.kamper:memory-module:{{version}}")
    implementation("com.smellouk.kamper:network-module:{{version}}")
    // Optional Android UI overlay:
    implementation("com.smellouk.kamper:ui-android:{{version}}")
}
```

### CocoaPods (iOS via XCFramework)

Add to your `Podfile`:

```ruby
pod 'Kamper', '~> {{version}}'
```

Then run `pod install`.

### Direct XCFramework download

[Kamper.xcframework.zip](https://github.com/smellouk/kamper/releases/download/v{{version}}/Kamper.xcframework.zip)
```

Column-1 heading verification: `jq -re '.packages["."]."release-notes-body"' release-please-config.json | grep -E "^## Installation$"` returns `## Installation`.

## RN Component Body (rendered markdown)

```
## Installation

### npm

```bash
npm install kamper-react-native@{{version}}
```

or with yarn:

```bash
yarn add kamper-react-native@{{version}}
```

### iOS (CocoaPods)

Run `pod install` from your `ios/` directory after installing the package.

### Android

No manual `MainApplication.kt` changes required — React Native autolinking handles it.

### React Native New Architecture (TurboModule)

`kamper-react-native` requires React Native >= 0.73 with the New Architecture enabled.
```

Column-1 heading verification: `jq -re '.packages["kamper/react-native"]."release-notes-body"' release-please-config.json | grep -E "^## Installation$"` returns `## Installation`.

## Decisions Made

- Used `jq --arg body` with a script-file approach (not inline heredoc) to avoid the 4-space indentation pitfall where `    ## Installation` would render as `<pre>## Installation</pre>` on GitHub instead of a real heading
- Root component body covers all 4 consumer entry points: Gradle (primary + optional modules), CocoaPods (iOS), and direct XCFramework download (non-CocoaPods iOS)
- RN body explicitly states TurboModule/New Architecture ≥ 0.73 requirement to prevent confusion for users on the legacy Bridge architecture

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## Threat Surface Scan

No new network endpoints, auth paths, or schema changes introduced. The `release-notes-body` fields are static template strings — no secrets, internal info, or trust boundary changes. T-14-07-02 (mistyped Maven coordinate) mitigated by acceptance criteria grep-validation.

## Next Phase Readiness

- `release-please-config.json` is fully configured with installation snippets for all four distribution channels
- When the first GitHub Release is created by Release Please, consumers will see ready-to-paste installation instructions immediately after the auto-generated changelog
- No further configuration needed for release body customization

---
*Phase: 16-release-automation-github-releases-changelog-multi-registry-*
*Completed: 2026-04-28*

## Self-Check: PASSED

- `release-please-config.json` exists and has both `release-notes-body` fields (verified by `jq -e`)
- Task 1 commit `aef9d5e` exists in git log
- Task 2 commit `d974e2a` exists in git log
- JSON valid, all acceptance criteria passing
