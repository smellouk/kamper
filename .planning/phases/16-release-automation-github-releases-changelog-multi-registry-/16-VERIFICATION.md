---
phase: 16-release-automation-github-releases-changelog-multi-registry
verified: 2026-04-28T18:00:00Z
status: human_needed
score: 5/5
overrides_applied: 0
human_verification:
  - test: "Confirm RELEASE_PLEASE_TOKEN GitHub Actions secret exists in smellouk/kamper repo"
    expected: "Secret visible in https://github.com/smellouk/kamper/settings/secrets/actions"
    why_human: "Cannot read GitHub Actions secrets via CLI without authentication as repo owner"
  - test: "Confirm Sonatype Maven Central namespace com.smellouk is verified"
    expected: "Namespace shows 'Verified' at https://central.sonatype.com/publishing/namespaces"
    why_human: "Cannot verify Sonatype DNS TXT record or namespace verification status programmatically"
  - test: "Confirm 5 Maven Central + GPG signing secrets exist: SONATYPE_USERNAME, SONATYPE_PASSWORD, SIGNING_KEY, SIGNING_KEY_ID, SIGNING_PASSWORD"
    expected: "All 5 secrets visible in GitHub Actions repository secrets list"
    why_human: "Cannot read secret values or verify their presence via CLI"
  - test: "Confirm COCOAPODS_TRUNK_TOKEN secret exists in smellouk/kamper repo"
    expected: "Secret visible in GitHub Actions repository secrets list; pod trunk me shows authenticated session"
    why_human: "Requires CocoaPods trunk registration and macOS environment"
  - test: "Confirm NPM_TOKEN secret exists in smellouk/kamper repo"
    expected: "Secret visible in GitHub Actions repository secrets list"
    why_human: "Cannot verify npm token existence or validity programmatically"
  - test: "Run pod lib lint Kamper.podspec --allow-warnings --skip-import-validation on macOS"
    expected: "Lint passes — podspec structure is valid"
    why_human: "Requires macOS with CocoaPods installed; cannot run on Linux"
---

# Phase 16: Release Automation Verification Report

**Phase Goal:** Automate GitHub Releases, changelog generation, and multi-registry publishing (Maven Central, CocoaPods, npm) via Release Please and GitHub Actions workflows.
**Verified:** 2026-04-28T18:00:00Z
**Status:** human_needed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Git tag triggers automated GitHub Release with changelog | VERIFIED | release-please.yml uses googleapis/release-please-action@v4, triggers on push to develop, generates CHANGELOG.md per release-please-config.json changelog-path; bootstrap-sha set to real 40-char SHA 279472a0 |
| 2 | KMP artifacts published to Maven Central automatically | VERIFIED | publish-kotlin.yml triggers on release:published, runs publishToMavenCentral via vanniktech 0.36.0 in KamperPublishPlugin.kt; all 5 ORG_GRADLE_PROJECT_* env vars wired; RN-tag filter excludes non-KMP releases |
| 3 | RN package published to npm automatically | VERIFIED | publish-npm.yml triggers on release:published filtered to kamper/ui/rn- prefix; runs npm publish --access public --provenance from kamper/ui/rn/; NODE_AUTH_TOKEN from NPM_TOKEN; package.json name is react-native-kamper |
| 4 | Release automation is end-to-end wired (Release Please -> publish workflows) | VERIFIED | RELEASE_PLEASE_TOKEN PAT used (not GITHUB_TOKEN) enabling downstream release:published events; tag format v* (root) and kamper/ui/rn-* (RN) used consistently across config, manifest, and all workflows |
| 5 | CI pipeline enforces Conventional Commits PR title format | VERIFIED | pull-request.yml has parallel commitlint job using amannn/action-semantic-pull-request@v6; all v1/v2 actions upgraded to v4; JDK 17 temurin; legacy publisher.yml deleted |

**Score:** 5/5 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `release-please-config.json` | Two-component manifest (root + RN) with extra-files and release-notes-body | VERIFIED | Both components present; root=simple, RN=node; extra-files point to gradle.properties and Kamper.podspec; release-notes-body in both components with {{version}} templating; column-1 headings confirmed |
| `.release-please-manifest.json` | Initial 1.0.0 versions for both components | VERIFIED | Contains "." => "1.0.0" and "kamper/ui/rn" => "1.0.0" |
| `.github/workflows/release-please.yml` | Release Please workflow on push to develop | VERIFIED | triggers on push:develop; uses googleapis/release-please-action@v4; RELEASE_PLEASE_TOKEN; target-branch:develop; exposes releases_created, tag_name, upload_url, rn_release_created; actionlint clean |
| `gradle.properties` | VERSION_NAME=1.0.0 with x-release-please-version annotation | VERIFIED | Lines 23-24: annotation immediately above VERSION_NAME with no blank line (awk check passes) |
| `build.gradle.kts` | Reads VERSION_NAME from gradle.properties via java.util.Properties | VERIFIED | versionPropertiesFile + versionProperties["VERSION_NAME"] present; generateVersionName() removed |
| `gradle/libs.versions.toml` | vanniktech-publish = "0.36.0" | VERIFIED | Line 19 confirms exact version |
| `build-logic/build.gradle.kts` | vanniktech-publish-plugin compileOnly dependency | VERIFIED | implementation(libs.vanniktech.publish.plugin) present |
| `build-logic/src/main/kotlin/KamperPublishPlugin.kt` | vanniktech apply + publishToMavenCentral + signAllPublications + developers block + scm:git: prefixes | VERIFIED | All 5 items confirmed: pluginManager.apply("com.vanniktech.maven.publish"), publishToMavenCentral(automaticRelease=true), signAllPublications(), developers{id.set("smellouk"), email.set("sidali.mellouk@zattoo.com")}, connection.set("scm:git:git://...") |
| `.github/workflows/publish-kotlin.yml` | Maven Central + GitHub Packages publish on release:published | VERIFIED | triggers on release:published; !startsWith(kamper/ui/rn-) filter; ubuntu-latest; JDK 17 temurin; publishToMavenCentral + publishAllPublicationsToGithubPackagesRepository; all 5 ORG_GRADLE_PROJECT_* env vars |
| `Kamper.podspec` | CocoaPods spec with Release Please annotation and remote source | VERIFIED | x-release-please-version annotation immediately above s.version; s.version=1.0.0; source URL uses v#{s.version}/Kamper.xcframework.zip; s.vendored_frameworks='Kamper.xcframework'; LICENSE.txt (correct — actual file) |
| `.github/workflows/publish-cocoapods.yml` | macOS workflow: build -> verify -> zip -> upload -> trunk push | VERIFIED | macos-latest; assembleKamperReleaseXCFramework; Verify XCFramework output step; zip command; gh release upload --clobber; pod trunk push --allow-warnings --skip-import-validation; COCOAPODS_TRUNK_TOKEN wired; !startsWith(kamper/ui/rn-) filter; actionlint clean |
| `.github/workflows/publish-npm.yml` | npm publish on RN-prefixed releases | VERIFIED | ubuntu-latest; Node 22; setup-node@v4 with registry-url; working-directory:kamper/ui/rn; npm ci + npm run build --if-present + npm publish --access public --provenance; id-token:write; startsWith(kamper/ui/rn-) filter; actionlint clean |
| `.github/workflows/pull-request.yml` | Modernized v4 actions + JDK 17 + commitlint | VERIFIED | checkout@v4, cache@v4, setup-java@v4 with temurin/17; amannn/action-semantic-pull-request@v6; zero v1/v2 references; all 4 gradle commands preserved; actionlint clean |
| `.github/workflows/publisher.yml` | Must be DELETED | VERIFIED | File does not exist |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| release-please-config.json | gradle.properties | extra-files generic updater | WIRED | extra-files[0].path = "gradle.properties"; annotation present in gradle.properties |
| release-please-config.json | Kamper.podspec | extra-files generic updater | WIRED | extra-files[1].path = "Kamper.podspec"; annotation present in Kamper.podspec |
| release-please.yml | release-please-config.json + .release-please-manifest.json | config-file + manifest-file inputs | WIRED | config-file: release-please-config.json; manifest-file: .release-please-manifest.json |
| release-please.yml | downstream publish workflows | RELEASE_PLEASE_TOKEN PAT releases triggering on:release:published | WIRED | token: secrets.RELEASE_PLEASE_TOKEN |
| publish-kotlin.yml | KamperPublishPlugin.kt (via Gradle) | ./gradlew publishToMavenCentral publishAllPublicationsToGithubPackagesRepository | WIRED | Task registered by vanniktech plugin applied in KamperPublishPlugin.kt; all publishing modules use id("kamper.publish") |
| publish-kotlin.yml | Maven Central Portal | ORG_GRADLE_PROJECT_mavenCentralUsername + mavenCentralPassword + signingInMemoryKey* | WIRED | All 5 env vars mapped from GitHub secrets |
| publish-cocoapods.yml | Kamper.podspec | pod trunk push Kamper.podspec | WIRED | pod trunk push command references file at repo root |
| publish-cocoapods.yml | XCFramework build | :kamper:xcframework:assembleKamperReleaseXCFramework | WIRED | Gradle task call in workflow |
| publish-npm.yml | npm registry | npm publish --access public with NODE_AUTH_TOKEN | WIRED | NODE_AUTH_TOKEN: secrets.NPM_TOKEN; registry-url: https://registry.npmjs.org via setup-node@v4 |

### Data-Flow Trace (Level 4)

Not applicable — this phase delivers CI/CD configuration files, not data-rendering components. No state variables or data fetches to trace.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| YAML validity: release-please.yml | python3 yaml.safe_load | YAML valid | PASS |
| YAML validity: publish-kotlin.yml | python3 yaml.safe_load | YAML valid | PASS |
| YAML validity: publish-cocoapods.yml | python3 yaml.safe_load | YAML valid | PASS |
| YAML validity: publish-npm.yml | python3 yaml.safe_load | YAML valid | PASS |
| YAML validity: pull-request.yml | python3 yaml.safe_load | YAML valid | PASS |
| actionlint: all 5 workflows | actionlint *.yml | Exit 0, no errors | PASS |
| gradle.properties annotation | awk annotation check | annotation immediately above VERSION_NAME | PASS |
| release-please-config.json valid JSON | jq . | Exit 0 | PASS |
| bootstrap-sha is real SHA | grep -E [a-f0-9]{40} | 279472a081ac9a840c5aa11479eed363070c1232 | PASS |
| release-please-config.json column-1 headings | grep -E "^## Installation$" | Match found | PASS |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|------------|-------------|--------|----------|
| REL-01 | 16-02, 16-03, 16-04, 16-05 | Release automation (GitHub Releases, changelogs, multi-registry publishing) | SATISFIED | Release Please config + all 4 publish workflows implemented; ROADMAP SC1/SC2/SC3 all verified |

REQUIREMENTS.md does not exist in .planning/ directory. REL-01 is referenced only in ROADMAP.md Phase 16 section — treated as satisfied based on ROADMAP success criteria evidence above.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| .release-please-manifest.json | 3 | kamper/ui/rn version is "1.0.0" but package.json version is "0.1.0" | WARNING | Mismatch between manifest and actual package version — Release Please may create an unexpected version bump on first run; not a blocker since Release Please is designed to manage this file |

No TODO/FIXME/STUB/placeholder tokens found in any key artifact files. All stubs from Plan 00 have been fully replaced.

### Deviation Notes (Notable but Non-Blocking)

**RN package path and name differ from Plan 02 specification:**
- Plan 02 specified: package path `kamper/react-native`, package-name `kamper-react-native`
- Actual implementation: package path `kamper/ui/rn`, package-name `react-native-kamper`
- Root cause: Phase 14 (Plan 00) moved the RN package to `kamper/ui/rn/` and named it `react-native-kamper` per React Native naming conventions; Phase 16 correctly adapted to match the actual structure
- Impact: None — all Phase 16 artifacts (release-please-config.json, publish-npm.yml, release-please.yml outputs) use `kamper/ui/rn` consistently; the tag prefix `kamper/ui/rn-` is used across all three workflows that reference it

**Kamper.podspec extra-files addition (positive deviation):**
- Plan 02 specified only `gradle.properties` in extra-files
- Actual config also includes `Kamper.podspec` — allows Release Please to bump the podspec version in sync with gradle.properties
- Impact: Positive — more complete version automation

### Human Verification Required

#### 1. GitHub Actions Secrets Configuration

**Test:** Visit https://github.com/smellouk/kamper/settings/secrets/actions and confirm ALL of the following secrets exist:
- `RELEASE_PLEASE_TOKEN` — PAT for Release Please downstream triggering
- `SONATYPE_USERNAME` — Sonatype Portal user token username
- `SONATYPE_PASSWORD` — Sonatype Portal user token password
- `SIGNING_KEY` — ASCII-armored GPG private key block
- `SIGNING_KEY_ID` — Short GPG key ID (last 8 hex chars of fingerprint)
- `SIGNING_PASSWORD` — GPG key passphrase
- `COCOAPODS_TRUNK_TOKEN` — CocoaPods trunk authentication token
- `NPM_TOKEN` — npmjs.com granular access token

**Expected:** All 8 secrets visible in the secrets list (values are hidden but presence is visible)
**Why human:** Cannot query GitHub Actions secrets via CLI without owner authentication

#### 2. Sonatype Maven Central Namespace Verification

**Test:** Visit https://central.sonatype.com/publishing/namespaces and confirm `com.smellouk` shows status "Verified"
**Expected:** Namespace status is "Verified" — required for Maven Central publishing
**Why human:** Requires Sonatype account access and DNS TXT record setup

#### 3. CocoaPods Trunk Authentication (macOS only)

**Test:** Run `pod trunk me` on macOS. Also run `pod lib lint Kamper.podspec --allow-warnings --skip-import-validation`
**Expected:** `pod trunk me` shows authenticated session for sidali.mellouk@zattoo.com; podspec lints clean
**Why human:** Requires macOS with CocoaPods installed; XCFramework build requires Xcode

#### 4. First End-to-End Release Dry Run

**Test:** Push a `feat:` commit to `develop` branch and verify Release Please opens a version-bump PR within ~5 minutes
**Expected:** PR appears titled "chore(release): release 1.0.1" or similar, bumping VERSION_NAME in gradle.properties and s.version in Kamper.podspec
**Why human:** Requires real GitHub push to develop branch; Release Please runs asynchronously

---

### Gaps Summary

No blocking gaps found. All automated verifiable must-haves are VERIFIED:
- All 8 key workflow/config files exist and are substantive (not stubs)
- All workflows pass YAML validation and actionlint
- Bootstrap SHA is a real commit SHA (not placeholder)
- Version annotation chain is intact (gradle.properties -> build.gradle.kts -> KamperPublishPlugin.kt)
- vanniktech 0.36.0 correctly wired in version catalog, build-logic, and KamperPublishPlugin.kt
- POM improvements in place: developers block, scm:git: prefixes
- Release notes bodies present in both config components with {{version}} templating
- publisher.yml correctly deleted
- All workflows use correct tag-prefix filters (root vs RN)

The only items requiring resolution are operational prerequisites that cannot be verified programmatically:
- GitHub Actions secrets (8 total)
- Sonatype namespace DNS verification
- CocoaPods trunk authentication
- First real Release Please run

---

_Verified: 2026-04-28T18:00:00Z_
_Verifier: Claude (gsd-verifier)_
