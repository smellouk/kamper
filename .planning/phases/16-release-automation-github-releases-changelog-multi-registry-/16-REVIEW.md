---
phase: 16-release-automation
reviewed: 2026-04-28T00:00:00Z
depth: standard
files_reviewed: 13
files_reviewed_list:
  - .github/workflows/publish-cocoapods.yml
  - .github/workflows/publish-kotlin.yml
  - .github/workflows/publish-npm.yml
  - .github/workflows/pull-request.yml
  - .github/workflows/release-please.yml
  - .release-please-manifest.json
  - Kamper.podspec
  - build-logic/build.gradle.kts
  - build-logic/src/main/kotlin/KamperPublishPlugin.kt
  - build.gradle.kts
  - gradle.properties
  - gradle/libs.versions.toml
  - release-please-config.json
findings:
  critical: 4
  warning: 3
  info: 2
  total: 9
status: issues_found
---

# Phase 16: Code Review Report

**Reviewed:** 2026-04-28T00:00:00Z
**Depth:** standard
**Files Reviewed:** 13
**Status:** issues_found

## Summary

This phase wires release automation via release-please, GitHub Actions publish workflows, Maven Central, CocoaPods, and npm. The overall architecture is sound: tag-based workflow dispatch with component-prefix filtering is the correct pattern for a multi-registry monorepo. However, four blockers exist that would prevent any release from succeeding end-to-end, plus three additional quality warnings.

The most severe problems are: (1) the React Native package path is wrong in every config and workflow file that references it; (2) `Kamper.podspec` is missing from `release-please-config.json` extra-files, so its version will never be bumped automatically; (3) the npm package name in `package.json` does not match the name advertised in the release notes; and (4) the `bootstrap-sha` placeholder was never replaced with an actual commit SHA, meaning release-please will refuse to run.

---

## Critical Issues

### CR-01: React Native package path is wrong in every configuration file

**Files:**
- `release-please-config.json:22`
- `.release-please-manifest.json:3`
- `.github/workflows/publish-npm.yml:19`

**Issue:** The React Native package lives at `kamper/ui/rn/` (confirmed by `kamper/ui/rn/package.json`), but every config file references `kamper/react-native`:

- `release-please-config.json` declares the package at key `"kamper/react-native"` — this directory does not exist, so release-please's `node` release type will fail to locate or update `package.json`.
- `.release-please-manifest.json` stores the version under `"kamper/react-native"` — this path is never matched to an actual file tree.
- `publish-npm.yml` sets `working-directory: kamper/react-native` — `npm ci` will fail immediately with "directory not found."

All three release-automation paths for the React Native component are dead on arrival.

**Fix:** Either move the package to `kamper/react-native/` (preferred, matches the release-please config key and the manifest) or update all three files to use the real path `kamper/ui/rn`:

```json
// .release-please-manifest.json
{
  ".": "1.0.0",
  "kamper/ui/rn": "1.0.0"
}
```

```json
// release-please-config.json — change the package key
"kamper/ui/rn": {
  "release-type": "node",
  "package-name": "react-native-kamper",   // see CR-03
  ...
}
```

```yaml
# publish-npm.yml
defaults:
  run:
    working-directory: kamper/ui/rn
```

---

### CR-02: `bootstrap-sha` is an unreplaced placeholder — release-please will never run

**File:** `release-please-config.json:4`

**Issue:** The `bootstrap-sha` field contains the literal string `"REPLACE_WITH_HEAD_SHA_AT_MERGE_TIME"`:

```json
"bootstrap-sha": "REPLACE_WITH_HEAD_SHA_AT_MERGE_TIME",
```

`release-please-action` uses `bootstrap-sha` to determine the starting commit for changelog generation. With a non-existent SHA, the action will either error out or scan the entire history and produce a malformed changelog. This value must be set to the actual SHA of the merge commit that introduced this configuration before any release PR can be created correctly.

**Fix:** After merging this branch to `develop`, record the resulting HEAD SHA and set it:

```bash
git rev-parse HEAD
```

```json
"bootstrap-sha": "a591604...",  // actual SHA from above
```

---

### CR-03: npm package name mismatch between `package.json` and release notes

**Files:**
- `kamper/ui/rn/package.json:3`
- `release-please-config.json:24`

**Issue:** `package.json` declares `"name": "react-native-kamper"`. Release-please config declares `"package-name": "kamper-react-native"`. The release notes body instructs users to run `npm install kamper-react-native@{{version}}`.

When `npm publish` runs, it publishes under the name from `package.json` (`react-native-kamper`). Users following the installation instructions in the changelog (`npm install kamper-react-native`) will get a 404 from the npm registry.

Additionally, the `package-name` field in release-please config is used as the `tag` prefix for the `node` release type — if release-please creates a tag for `kamper-react-native` but the actual npm package is `react-native-kamper`, the version pinning advertised in release notes is broken.

**Fix:** Decide on one canonical name and apply it consistently. The React Native community convention is `react-native-<name>`, so `react-native-kamper` is the more correct form:

```json
// release-please-config.json
"package-name": "react-native-kamper",
```

And update the release notes body to match:
```
npm install react-native-kamper@{{version}}
```

---

### CR-04: `Kamper.podspec` is not listed in `extra-files` — version will never be auto-bumped

**Files:**
- `release-please-config.json:14-19`
- `Kamper.podspec:3-4`

**Issue:** `Kamper.podspec` contains the `# x-release-please-version` annotation on line 3, indicating it was intended to be managed by release-please. However, the root package's `extra-files` list only contains `gradle.properties`:

```json
"extra-files": [
  {
    "type": "generic",
    "path": "gradle.properties"
  }
]
```

`Kamper.podspec` is absent. When release-please creates a version-bump PR, it will update `gradle.properties` and `CHANGELOG.md` but leave `Kamper.podspec` at the old version. The CocoaPods publish workflow then pushes a podspec whose `s.version` does not match the GitHub Release tag, causing `pod trunk push` to either publish the wrong version or fail validation.

**Fix:** Add `Kamper.podspec` to the `extra-files` array:

```json
"extra-files": [
  {
    "type": "generic",
    "path": "gradle.properties"
  },
  {
    "type": "generic",
    "path": "Kamper.podspec"
  }
]
```

Note: The annotation in `Kamper.podspec` is on a separate preceding line (line 3), not inline on the version line (line 4). Verify this is the format your release-please version supports; if not, change to the inline form: `s.version = '1.0.0' # x-release-please-version`.

---

## Warnings

### WR-01: Gradle cache key never invalidates in `pull-request.yml`

**File:** `.github/workflows/pull-request.yml:28`

**Issue:** The Gradle cache key is the static string `"cache-gradle"` with no hash of build files:

```yaml
key: cache-gradle
```

This cache will never be invalidated by dependency or wrapper changes. A stale cache can hide build failures and cause non-reproducible CI results. It will also collide across branches and OS runners if the workflow is ever expanded.

**Fix:** Use a content-addressable key:

```yaml
key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle/wrapper/gradle-wrapper.properties', '**/gradle/libs.versions.toml') }}
restore-keys: |
  ${{ runner.os }}-gradle-
```

---

### WR-02: `release-please.yml` defines job outputs that no downstream job consumes

**File:** `.github/workflows/release-please.yml:18-23`

**Issue:** The `release-please` job exposes five outputs (`releases_created`, `tag_name`, `upload_url`, `rn_release_created`, `rn_tag_name`), but there are no downstream jobs in the workflow that reference `needs.release-please.outputs.*`. The actual publish workflows are in separate files triggered by the `release: published` event.

The outputs are therefore dead code — they export values that are never read. This is harmless today but creates misleading signal about the workflow's design intent (someone may assume a downstream job uses them, or add one incorrectly).

**Fix:** Remove the unused `outputs:` block from the `release-please` job, or add a comment explaining they are reserved for future conditional dispatch:

```yaml
# outputs intentionally omitted — publish workflows are triggered
# directly by the 'release: published' event, not by job needs
```

---

### WR-03: `KamperPublishPlugin` configures GitHub Packages repository unconditionally when `CI=true`, but credentials may be empty strings

**File:** `build-logic/src/main/kotlin/KamperPublishPlugin.kt:60-69`

**Issue:** The GitHub Packages `maven {}` block is added whenever the `CI` environment variable is set. The credentials are sourced from `rootProject.extra["KAMPER_GH_USER"]` and `rootProject.extra["KAMPER_GH_PAT"]`, which `build.gradle.kts` defaults to empty strings `""` when neither `credentials.properties` nor the env vars are present (lines 34-37 of `build.gradle.kts`).

On a CI runner that sets `CI=true` but does not supply `KAMPER_GH_USER`/`KAMPER_GH_PAT` (e.g., a fork's PR runner, or `publish-cocoapods.yml` which does not set these env vars), Gradle will configure the GitHub Packages repository with empty credentials. Publishing to GitHub Packages will then fail mid-task with a 401 rather than failing fast with a clear configuration error.

`publish-kotlin.yml` correctly supplies both secrets. But `publish-cocoapods.yml` only runs `assembleKamperReleaseXCFramework` and not a publish Gradle task, so the repository configuration is created but never exercised — still wasteful.

**Fix:** Guard with a non-empty credential check:

```kotlin
val ghUser = project.rootProject.extra["KAMPER_GH_USER"].toString()
val ghPat = project.rootProject.extra["KAMPER_GH_PAT"].toString()
if (System.getenv().containsKey("CI") && ghUser.isNotEmpty() && ghPat.isNotEmpty()) {
    maven {
        name = "GithubPackages"
        url = project.uri("https://maven.pkg.github.com/smellouk/kamper")
        credentials {
            username = ghUser
            password = ghPat
        }
    }
}
```

---

## Info

### IN-01: `publish-npm.yml` lacks a version verification step before publishing

**File:** `.github/workflows/publish-npm.yml:38-41`

**Issue:** The workflow runs `npm publish` without first verifying that the version in `package.json` matches the GitHub Release tag. If the React Native package path is corrected (CR-01) and release-please bumps `package.json` correctly, this should be self-consistent. However, there is no guard against publishing a stale or mismatched version if a release is triggered manually.

**Fix (optional):** Add a sanity check step:

```yaml
- name: Verify package version matches release tag
  run: |
    PKG_VERSION=$(node -p "require('./package.json').version")
    TAG="${{ github.event.release.tag_name }}"
    EXPECTED_VERSION="${TAG##*-v}"
    if [ "$PKG_VERSION" != "$EXPECTED_VERSION" ]; then
      echo "Version mismatch: package.json=$PKG_VERSION, tag=$EXPECTED_VERSION"
      exit 1
    fi
```

---

### IN-02: `pull-request.yml` does not target the `main` branch

**File:** `.github/workflows/pull-request.yml:6-9`

**Issue:** The PR checker only runs for PRs targeting `develop`. Release-please will open its own version-bump PRs against `develop` (correct), but any future direct PR to `main` (e.g., a hotfix) will bypass CI entirely. This may be intentional if `main` only receives squash merges from `develop`, but it is not documented.

**Fix:** If the project follows a `develop` → `main` promotion model exclusively, add a comment to make the intent explicit. If hotfixes to `main` are possible, add `main` to the branches list:

```yaml
on:
  pull_request:
    branches:
      - develop
      - main  # add only if direct-to-main hotfixes are supported
```

---

_Reviewed: 2026-04-28T00:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
