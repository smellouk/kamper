# Phase 20: Open Source Cleanup - Research

**Researched:** 2026-04-29
**Domain:** Open source project hygiene — documentation, GitHub templates, public file audit
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**CONTRIBUTING.md**
- D-01: Full contributor guide covering: setup (JDK 17, Gradle wrapper), build and test (`./gradlew jvmTest`, detekt), module patterns overview (4-class structure, INVALID sentinel), Conventional Commits format (types, scopes, no emojis, no `resolves #N`), branch naming (`main` is integration branch, no `develop`), PR process (squash merge), code review expectations, and device requirement note for instrumented tests.
- D-02: CONTRIBUTING.md is authoritative for humans; CLAUDE.md is authoritative for Claude Code agents. They share the same rules but are completely independent documents — no cross-reference required between them.

**GitHub Templates**
- D-03: Create both issue templates in `.github/ISSUE_TEMPLATE/`: bug report (reproduction steps, expected vs actual, platform, Kamper version, module) and feature request (use case, proposed API or behavior). Use `.yml` format for structured GitHub Forms.
- D-04: Replace the existing `.github/PULL_REQUEST_TEMPLATE.md` entirely. New template: description of what changed and why, checklist (detekt passes, jvmTest passes, no TODO/FIXME in changed files, public API changes are additive only).

**Public File Audit**
- D-05: Delete `SECURITY.md` entirely — it is not needed.
- D-06: Audit all public-facing files for stale internal content: `README.md`, `.github/` directory (workflows, templates), any docs accessible from the repo root. Strip stale file:line references, internal snapshot URLs, and outdated version references.

**README.md Refresh**
- D-07: Full README refresh with this structure:
  1. Badge strip (license, release, issues, stars)
  2. Project tagline + one-paragraph description
  3. Modules table (all 8: cpu, fps, memory, network, jank, gc, thermal, issues — name, description, platform support)
  4. Quick start (Maven Central coordinates, engine install, one module install, one listener example)
  5. Service integrations section (Sentry, Firebase Crashlytics, OpenTelemetry — brief setup example each)
  6. Platform matrix (which modules work on which KMP targets)
  7. Links (CONTRIBUTING.md, CLAUDE.md, GitHub Releases, SECURITY policy if any)
- D-08: Maven Central coordinates are the canonical install path. Remove any `maven.pkg.github.com` snapshot references.

**Release / Versioning Docs**
- D-09: README includes a "Versioning" section: semantic versioning explanation in Kamper context (patch = bug fixes, minor = new modules or features, major = breaking API changes, which are frozen until post-v1.0), how to find the latest version (GitHub Releases), link to CHANGELOG.

### Claude's Discretion

- Whether to add `CODE_OF_CONDUCT.md` (Contributor Covenant) — standard OSS practice; include if it fits naturally.
- License attribution line in README footer.
- CI badge in README — check if GitHub Actions are wired to produce a status badge; include if available.
- Exact prose and section ordering within CONTRIBUTING.md, subject to D-01 content requirements.

### Deferred Ideas (OUT OF SCOPE)

- Monorepo structure cleanup (`kamper/` folder rename to `libs/`) — structural refactor affecting all module paths, build scripts, and published artifact coordinates. Requires its own phase.
- Per-module sub-CLAUDE.md files — deferred from Phase 19 discussion; root CLAUDE.md is sufficient for v1.0.
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| OSS-02 | CONTRIBUTING.md covers setup, coding standards, PR process; GitHub issue and PR templates in place; all TODOs and internal references removed from public-facing files | Covered by: CONTRIBUTING.md rewrite (D-01/D-02), GitHub Forms templates (D-03/D-04), SECURITY.md deletion (D-05), public file audit (D-06), README refresh (D-07/D-08/D-09) |
</phase_requirements>

---

## Summary

Phase 20 is a pure documentation and configuration phase — no Kotlin code is written. It operates entirely on files in the repository root and `.github/` directory. The technical surface is: one new CONTRIBUTING.md, two new `.github/ISSUE_TEMPLATE/*.yml` files, one replaced `.github/PULL_REQUEST_TEMPLATE.md`, a full README.md rewrite, and deletion of `SECURITY.md`.

The current state of the repo makes all required information available without external dependencies. CLAUDE.md (Phase 19 output) is authoritative for every rule CONTRIBUTING.md must mirror: Conventional Commits format (types, scopes, no emojis), build commands, module patterns, JDK 17 requirement, device guard for `connectedAndroidTest`, and squash-merge PR strategy. CONTRIBUTING.md must agree with CLAUDE.md on every rule — the two documents are independent but must be consistent.

The README already has a solid structure from a previous phase — the primary work is replacing the `maven.pkg.github.com` installation block with Maven Central coordinates and stripping the appended Security Considerations section (which duplicates SECURITY.md content that is being deleted). The SECURITY.md file additionally contains an internal Phase 14 security audit embedded at the end (lines 102–162) that must not appear in any public-facing file.

**Primary recommendation:** Implement in three waves: (1) write the three GitHub template files, (2) write CONTRIBUTING.md, (3) refresh README.md and delete SECURITY.md. Each wave is independently verifiable.

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Contributor guidance | Documentation | — | Human-readable prose, no code tier |
| GitHub issue/PR templates | Repository config (.github/) | — | Interpreted by GitHub UI, not by code |
| README public API docs | Documentation | — | Informs library consumers |
| Public file audit | Documentation | — | Sweep of root-level files |

All work in this phase is at the documentation/repository configuration tier. No code tier is involved.

---

## Standard Stack

### Core

This phase has no library dependencies. The deliverables are plain text and YAML files.

| Tool | Version | Purpose | Why Standard |
|------|---------|---------|--------------|
| GitHub Forms YAML | — | Structured issue templates | GitHub-native; `.yml` extension in `.github/ISSUE_TEMPLATE/` activates the forms UI |
| Contributor Covenant | 2.1 | `CODE_OF_CONDUCT.md` (discretionary) | Most widely adopted OSS code of conduct; version 2.1 is current |

### Verified Artifact Coordinates

These Maven Central artifact IDs are derived from `KamperPublishPlugin.kt` logic (lines 79–84): modules under `kamper/modules/` get `{name}-module` suffix; integrations get `{name}-integration`; engine and bom get their project name. [VERIFIED: build-logic/src/main/kotlin/KamperPublishPlugin.kt]

| Artifact ID | Group |
|-------------|-------|
| `engine` | `com.smellouk.kamper` |
| `bom` | `com.smellouk.kamper` |
| `cpu-module` | `com.smellouk.kamper` |
| `fps-module` | `com.smellouk.kamper` |
| `memory-module` | `com.smellouk.kamper` |
| `network-module` | `com.smellouk.kamper` |
| `jank-module` | `com.smellouk.kamper` |
| `gc-module` | `com.smellouk.kamper` |
| `thermal-module` | `com.smellouk.kamper` |
| `issues-module` | `com.smellouk.kamper` |
| `sentry-integration` | `com.smellouk.kamper` |
| `firebase-integration` | `com.smellouk.kamper` |
| `opentelemetry-integration` | `com.smellouk.kamper` |
| `ui-android` (KMM UI) | `com.smellouk.kamper` |

**Current version:** `1.0.0` [VERIFIED: gradle.properties, .release-please-manifest.json]

---

## Architecture Patterns

### System Architecture Diagram

```
Root repository files (public-facing)
┌─────────────────────────────────────────────────────────────────┐
│  README.md       ← full rewrite (D-07, D-08, D-09)             │
│  CONTRIBUTING.md ← full replacement (D-01, D-02)               │
│  SECURITY.md     ← DELETE (D-05)                                │
│  CHANGELOG.md    ← exists (1 line, empty — link only from README│
│  LICENSE.txt     ← no change needed                             │
│  CODE_OF_CONDUCT.md ← CREATE (discretionary)                   │
└─────────────────────────────────────────────────────────────────┘

.github/ directory
┌─────────────────────────────────────────────────────────────────┐
│  PULL_REQUEST_TEMPLATE.md        ← replace (D-04)               │
│  ISSUE_TEMPLATE/                 ← CREATE directory             │
│    bug_report.yml                ← CREATE (D-03)                │
│    feature_request.yml           ← CREATE (D-03)                │
│  workflows/                      ← audit only (no changes needed│
│    pull-request.yml              ← targets 'develop' branch     │
│    release-please.yml            ← targets 'develop' branch     │
│    publish-kotlin.yml            ← triggers on release publish  │
└─────────────────────────────────────────────────────────────────┘
```

### GitHub Forms Template Pattern

GitHub Forms (`.yml`) replaces the older Markdown template approach. [CITED: docs.github.com/en/communities/using-templates-to-encourage-useful-issues-and-pull-requests/syntax-for-issue-forms]

Required top-level fields: `name`, `description`, `body`.
Optional top-level fields: `title` (pre-populated), `labels` (auto-applied), `assignees`.

Supported input types in `body`:
- `markdown` — display-only text (instructions, separators)
- `textarea` — multi-line text (reproduction steps, description)
- `input` — single-line text (version number, module name)
- `dropdown` — selection list (platform, severity)
- `checkboxes` — multiple choice (confirmation checklist)

Each input type uses `id`, `attributes.label`, `attributes.description`, `attributes.placeholder`, and optionally `validations.required: true`.

### Bug Report Template Structure (D-03)

```yaml
name: Bug Report
description: Report a reproducible bug in Kamper
title: "[Bug]: "
labels: ["bug", "triage"]
body:
  - type: markdown
    attributes:
      value: |
        Thanks for taking the time to report a bug.
        Before opening, please check existing issues.

  - type: textarea
    id: description
    attributes:
      label: Description
      description: A clear description of what the bug is.
    validations:
      required: true

  - type: textarea
    id: reproduction
    attributes:
      label: Reproduction steps
      description: Minimal code or steps to reproduce the issue.
      placeholder: |
        1. Install CpuModule
        2. Call Kamper.start()
        3. ...
    validations:
      required: true

  - type: textarea
    id: expected
    attributes:
      label: Expected behavior
    validations:
      required: true

  - type: textarea
    id: actual
    attributes:
      label: Actual behavior
    validations:
      required: true

  - type: dropdown
    id: platform
    attributes:
      label: Platform
      options:
        - Android
        - iOS
        - JVM
        - macOS
        - tvOS
        - JS
        - WasmJS
    validations:
      required: true

  - type: input
    id: kamper_version
    attributes:
      label: Kamper version
      placeholder: "1.0.0"
    validations:
      required: true

  - type: input
    id: module
    attributes:
      label: Module(s) affected
      placeholder: "cpu, fps, memory, ..."
```

### Feature Request Template Structure (D-03)

```yaml
name: Feature Request
description: Propose a new feature or behavior change
title: "[Feature]: "
labels: ["enhancement"]
body:
  - type: textarea
    id: use_case
    attributes:
      label: Use case
      description: What problem does this solve?
    validations:
      required: true

  - type: textarea
    id: proposed_api
    attributes:
      label: Proposed API or behavior
      description: How would a developer use this feature?
    validations:
      required: false

  - type: textarea
    id: alternatives
    attributes:
      label: Alternatives considered
    validations:
      required: false
```

### PR Template Structure (D-04)

No emoji, no reference to `develop` branch, no old CONTRIBUTING.md link:

```markdown
## What changed and why
<!-- Describe your change and the motivation behind it. -->

## Checklist
- [ ] `./gradlew detekt` passes (zero issues)
- [ ] `./gradlew :kamper:modules:<name>:jvmTest` passes for every touched module
- [ ] No `TODO:` or `FIXME:` in changed files
- [ ] Public API changes are additive only (no removals or signature breaks)
```

### CONTRIBUTING.md Required Sections (D-01)

The document must cover these topics in order to be a standalone contributor reference:

1. **Prerequisites** — JDK 17 required; no Android device needed for the default workflow
2. **Clone and build** — `git clone`, `./gradlew :kamper:api:test` smoke test
3. **Development workflow** — `./gradlew :kamper:modules:<name>:jvmTest` (fast path), `./gradlew test` (full sweep), `./gradlew detekt` (zero-tolerance)
4. **Device-requiring tests** — `./gradlew connectedAndroidTest` requires a connected Android device or running emulator; do not run autonomously
5. **Module pattern overview** — 4-class structure (Info, Config, Watcher, Performance), INVALID sentinel, Builder/DEFAULT pattern; reference `kamper/modules/cpu/` as canonical example
6. **Commit format** — Conventional Commits: `<type>(<scope>): <description>`, allowed types and scopes, imperative mood, no emojis, no `resolves #N`, no trailing period
7. **Branch strategy** — `main` is the integration branch; no `develop` branch; squash merge for PRs
8. **PR process** — PR checklist matching the PR template (detekt, jvmTest, no TODO/FIXME, additive-only API)
9. **Code review** — what reviewers check; expected turnaround (single maintainer; be patient)

### README.md Required Changes (D-07, D-08, D-09)

**Section to remove:** The "Security Considerations" section (lines 494–535 of current README.md). This content was appropriate when SECURITY.md existed but becomes redundant — and the autoinitialization detail belongs in module-specific docs, not in the README.

**Installation block to replace:** Lines 92–120 (the GitHub Packages block) must be replaced with Maven Central coordinates. The `maven("https://maven.pkg.github.com/smellouk/kamper")` repository declaration must be removed entirely — Maven Central requires no `repositories {}` entry for most Gradle setups.

**New installation block (D-08):**

```kotlin
dependencies {
    val kamperVersion = "1.0.0"

    implementation("com.smellouk.kamper:engine:$kamperVersion")

    // Core metrics
    implementation("com.smellouk.kamper:cpu-module:$kamperVersion")
    implementation("com.smellouk.kamper:fps-module:$kamperVersion")
    implementation("com.smellouk.kamper:memory-module:$kamperVersion")
    implementation("com.smellouk.kamper:network-module:$kamperVersion")

    // Advanced metrics
    implementation("com.smellouk.kamper:jank-module:$kamperVersion")
    implementation("com.smellouk.kamper:gc-module:$kamperVersion")
    implementation("com.smellouk.kamper:thermal-module:$kamperVersion")
    implementation("com.smellouk.kamper:issues-module:$kamperVersion")

    // Android debug overlay (auto-init, debug builds only)
    debugImplementation("com.smellouk.kamper:ui-android:$kamperVersion")
}
```

Maven Central is the default repository in modern Gradle projects — no `repositories {}` block is needed for it.

**New Versioning section (D-09):**

```markdown
## Versioning

Kamper follows [semantic versioning](https://semver.org/):

- **Patch** (`1.0.x`) — bug fixes; no API changes
- **Minor** (`1.x.0`) — new modules or features; backward compatible
- **Major** (`x.0.0`) — breaking API changes; frozen for all v1.x releases

The latest release is always available on [GitHub Releases](https://github.com/smellouk/kamper/releases).
Changes are listed in [CHANGELOG.md](CHANGELOG.md).
```

Note: CHANGELOG.md currently exists but is empty (1 line). The README link to it is safe — release-please will populate it when the first release is cut on `main`. [VERIFIED: CHANGELOG.md wc -l = 0 (empty), .release-please-manifest.json]

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Structured issue input | Custom issue template with freeform Markdown | GitHub Forms `.yml` | Forms enforce required fields, add dropdowns, auto-apply labels — Markdown templates are easily skipped |
| Code of conduct | Write custom CoC prose | Contributor Covenant 2.1 | Universally recognized; placing a custom CoC has less credibility with potential contributors |
| Versioning prose | Custom versioning explanation | Mirror semver.org wording with Kamper context | Semver is well-known; contributors don't need a novel explanation |

---

## Common Pitfalls

### Pitfall 1: CI Badge Targeting Wrong Branch
**What goes wrong:** The README displays a badge for `pull-request.yml` which triggers on PRs to `develop`. On `main` the workflow never ran, so the badge shows "no status" or a misleading state.
**Why it happens:** The workflow `pull-request.yml` has `on: pull_request: branches: [develop]`. The git base branch per config.json is `develop`, so `main` will not have CI history for this workflow.
**How to avoid:** Either (a) skip the CI badge for this workflow entirely, or (b) add a push-to-main workflow before referencing it. The README already has four useful shields.io badges (license, release, issues, stars) — those are reliable. The CI badge is discretionary (per CONTEXT.md); omit it unless a push-to-main workflow is also created in this phase.
**Warning signs:** Badge shows "no status" permanently after publishing the README.

### Pitfall 2: SECURITY.md Content Leaking Into README
**What goes wrong:** The current README has a "Security Considerations" section (lines 494–535) that mirrors SECURITY.md content. Deleting SECURITY.md without also removing this README section leaves duplicate security content in the public README — and that section references `KamperUiInitProvider` internals that are more appropriate in module docs.
**Why it happens:** Both documents were written together during a prior phase and share content.
**How to avoid:** When removing SECURITY.md, simultaneously remove the "Security Considerations" section from README.md. [VERIFIED: README.md lines 494–535]

### Pitfall 3: SECURITY.md Has Appended Internal Audit Content
**What goes wrong:** SECURITY.md lines 102–162 contain the full Phase 14 internal security audit — threat IDs, open/closed dispositions, accepted risks. This is internal planning content, not a public security policy.
**Why it happens:** The audit was appended to SECURITY.md in a prior phase.
**How to avoid:** D-05 deletes SECURITY.md entirely, which resolves this automatically. Do not copy any content from SECURITY.md before deletion.
**Warning signs:** Any content with "Phase 14", "T-12-XX", "ASVS Level" — all internal.

### Pitfall 4: Stale `maven.pkg.github.com` Reference Remaining
**What goes wrong:** The current README installation block references `maven("https://maven.pkg.github.com/smellouk/kamper")` which requires GitHub authentication. External contributors who copy this block will get 401 errors.
**Why it happens:** The README predates Maven Central publication.
**How to avoid:** D-08 is explicit — remove the GitHub Packages `repositories {}` block entirely. Maven Central is the only canonical install path. [VERIFIED: README.md line 97]

### Pitfall 5: Old PR Template Conventions Remaining
**What goes wrong:** The existing `.github/PULL_REQUEST_TEMPLATE.md` references `develop` branch and old emoji-based commits. A partial update leaves contradictions.
**Why it happens:** The template is a full replacement (D-04), not a patch.
**How to avoid:** Overwrite the entire file. Do not preserve any existing content. [VERIFIED: .github/PULL_REQUEST_TEMPLATE.md — contains emoji headers, `develop` branch links, old CONTRIBUTING.md reference]

### Pitfall 6: CONTRIBUTING.md Diverges from CLAUDE.md on Commit Rules
**What goes wrong:** CONTRIBUTING.md and CLAUDE.md describe different commit scopes, types, or PR processes — contributors follow CONTRIBUTING.md and submit PRs that violate rules.
**Why it happens:** CLAUDE.md was updated in Phase 19; CONTRIBUTING.md was last updated in an earlier phase and reflects the old git-flow workflow.
**How to avoid:** Before finalizing CONTRIBUTING.md, cross-check every commit type, scope list, and PR rule against CLAUDE.md. They must match exactly. D-02 states they are independent but must agree. [VERIFIED: CLAUDE.md — canonical source]

---

## Code Examples

### GitHub Issue Form: Bug Report
Source: [GitHub Docs — Syntax for Issue Forms](https://docs.github.com/en/communities/using-templates-to-encourage-useful-issues-and-pull-requests/syntax-for-issue-forms)

```yaml
# .github/ISSUE_TEMPLATE/bug_report.yml
name: Bug Report
description: Report a reproducible bug in Kamper
title: "[Bug]: "
labels: ["bug", "triage"]
body:
  - type: markdown
    attributes:
      value: |
        Thanks for reporting a bug. Please search existing issues before submitting.
  - type: textarea
    id: description
    attributes:
      label: Description
    validations:
      required: true
  - type: dropdown
    id: platform
    attributes:
      label: Platform
      options:
        - Android
        - iOS
        - JVM
        - macOS
        - tvOS
        - JS
        - WasmJS
    validations:
      required: true
```

### CONTRIBUTING.md: Build Commands Section

```markdown
## Build and test

The fast development path uses the JVM target — no device required:

```shell
# Run tests for a single module (fast feedback)
./gradlew :kamper:modules:cpu:jvmTest

# Run all unit tests
./gradlew test

# Run API and engine tests
./gradlew :kamper:api:test :kamper:engine:test

# Static analysis (must pass before every commit — zero issues)
./gradlew detekt
```

> **Instrumented tests require a connected device.**
> `./gradlew connectedAndroidTest` requires an Android device or running emulator.
> Do not run this command in automated or headless environments.
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| GitHub Markdown issue templates (`.md`) | GitHub Forms (`.yml`) | ~2021 | Forms enforce required fields and structured input; Markdown templates are freeform and often ignored |
| Emoji commit messages | Conventional Commits | Phase 19 CLAUDE.md | Conventional Commits are machine-parseable by release-please; emoji commits break changelog generation |
| Snapshot install via GitHub Packages | Maven Central release artifacts | Phase 16 | External contributors no longer need GitHub token to add the dependency |
| `develop` as integration branch | `main` as integration branch | Phase 19 CLAUDE.md | Git-flow is outdated; trunk-based development on `main` is current practice |

**Deprecated/outdated (found in current files):**
- Old PR template: emoji section headers, `develop` branch links, lint command includes `lint` (not `detekt`), old CONTRIBUTING.md reference — full replacement per D-04.
- CONTRIBUTING.md: git-flow workflow, emoji commits, `#ISSUE_ID` prefix, `git rebase -t` instruction — full replacement per D-01.
- SECURITY.md: entire file including appended internal audit — delete per D-05.

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Maven Central requires no `repositories {}` entry in modern Gradle projects (it is included by default in Gradle 7+) | Architecture Patterns — Installation block | If a consuming project uses an older Gradle version or custom repo configuration, they may need to add `mavenCentral()` explicitly. Mitigate by adding a note: "Add `mavenCentral()` to your `repositories {}` if it is not already present." | [ASSUMED] |
| A2 | CHANGELOG.md will be populated by release-please on first release from main | Architecture Patterns | If the release-please config targets `develop` only (confirmed: it does), CHANGELOG.md may not auto-populate until the branch strategy changes. Low risk — README can say "see GitHub Releases" as the primary pointer. | [ASSUMED] |

---

## Open Questions (RESOLVED)

1. **CI badge for README (discretionary)**
   - What we know: `pull-request.yml` targets `develop`; no push-to-main workflow exists; the badge would show no status on `main`.
   - What's unclear: Should a lightweight push-to-main CI workflow be added as part of this phase to support the badge, or should the badge be omitted?
   - Recommendation: Omit the CI badge from README. The four existing shields.io badges (license, release, issues, stars) are sufficient. Creating a push-to-main workflow is out of scope for Phase 20 (documentation cleanup, not CI infrastructure).
   - **RESOLVED: Omit CI badge.** No push-to-main workflow exists; a badge would show no status on `main`. Discretionary item closed.

2. **README Security Considerations section**
   - What we know: Lines 494–535 of README.md duplicate SECURITY.md content, which is being deleted (D-05).
   - What's unclear: D-07 does not explicitly list "Security Considerations" as a section to preserve or remove in the new structure.
   - Recommendation: Remove the "Security Considerations" section from README when refreshing it per D-07. The content is implementation detail that belongs in module docs, not the top-level README.
   - **RESOLVED: Remove Security Considerations section.** D-05 deletes SECURITY.md; the README section mirrors that content and is removed in Plan 20-03 Task 1.

---

## Environment Availability

Step 2.6: SKIPPED — Phase 20 has no external tool dependencies. All deliverables are plain text and YAML files. No CLI tools, runtimes, databases, or external services are required beyond a text editor and git.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | N/A — documentation phase |
| Config file | N/A |
| Quick run command | N/A — manual verification only |
| Full suite command | `./gradlew test` (unchanged; no new code) |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| OSS-02a | CONTRIBUTING.md covers all D-01 topics | manual | — | ❌ Wave 0: file does not exist yet |
| OSS-02b | `.github/ISSUE_TEMPLATE/bug_report.yml` is valid GitHub Forms YAML | manual | — | ❌ Wave 0: file does not exist yet |
| OSS-02c | `.github/ISSUE_TEMPLATE/feature_request.yml` is valid GitHub Forms YAML | manual | — | ❌ Wave 0: file does not exist yet |
| OSS-02d | `.github/PULL_REQUEST_TEMPLATE.md` contains no emoji/develop/old refs | manual | `grep -c "emoji\|develop\|🚀\|🧪\|📄\|📷\|✅" .github/PULL_REQUEST_TEMPLATE.md` should return 0 | ❌ Wave 0: full replacement |
| OSS-02e | `SECURITY.md` is deleted | automated | `test ! -f SECURITY.md && echo PASS` | ❌ exists now — delete in Wave 0 |
| OSS-02f | `README.md` contains no `maven.pkg.github.com` reference | automated | `grep -c "maven.pkg.github.com" README.md` should return 0 | ❌ one instance at line 97 |
| OSS-02g | All public-facing files pass stale reference check | automated | see verification script below |

**Verification script for OSS-02g:**
```bash
# Run from repo root after implementation
echo "=== Stale references check ===" && \
grep -rn "develop" README.md CONTRIBUTING.md && echo "FAIL: develop branch reference" || echo "PASS: no develop ref" && \
grep -rn "maven.pkg.github.com" README.md CONTRIBUTING.md && echo "FAIL: GitHub Packages URL" || echo "PASS: no GPR URL" && \
grep -rn "TODO\|FIXME" README.md CONTRIBUTING.md .github/ && echo "FAIL: TODO/FIXME found" || echo "PASS: no TODO/FIXME" && \
test ! -f SECURITY.md && echo "PASS: SECURITY.md deleted" || echo "FAIL: SECURITY.md still exists"
```

### Sampling Rate
- **Per task commit:** manual review of created/modified file
- **Per wave merge:** run the verification script above
- **Phase gate:** verification script returns all PASS before `/gsd-verify-work`

### Wave 0 Gaps
- `.github/ISSUE_TEMPLATE/bug_report.yml` — covers OSS-02b (create in Wave 1)
- `.github/ISSUE_TEMPLATE/feature_request.yml` — covers OSS-02c (create in Wave 1)
- New `.github/PULL_REQUEST_TEMPLATE.md` — covers OSS-02d (replace in Wave 1)
- New `CONTRIBUTING.md` — covers OSS-02a (write in Wave 2)
- New `README.md` — covers OSS-02f (refresh in Wave 3)
- Delete `SECURITY.md` — covers OSS-02e (delete in Wave 3)

---

## Security Domain

Phase 20 is a documentation-only phase. The security relevance is:

### Applicable ASVS Categories

| ASVS Category | Applies | Note |
|---------------|---------|------|
| V2 Authentication | no | No auth code |
| V3 Session Management | no | No session code |
| V4 Access Control | no | No access control code |
| V5 Input Validation | no | No input processing |
| V6 Cryptography | no | No cryptographic code |

### Security-Relevant Content Handling

| Item | Risk | Action |
|------|------|--------|
| SECURITY.md Phase 14 audit (lines 102–162) | Internal planning content in a public file — exposes internal threat IDs, security assessment details | Delete file entirely per D-05 |
| README "Security Considerations" section | Duplicates SECURITY.md; auto-initialization internals | Remove section when refreshing README per D-07 |
| SECURITY.md public contact email (`sidali.mellouk@zattoo.com`) | Remains visible until file is deleted | No action beyond deletion |

No new security concerns are introduced by this phase.

---

## Sources

### Primary (HIGH confidence)
- [VERIFIED: .github/PULL_REQUEST_TEMPLATE.md] — content inspected; emoji headers, develop branch links, old commit format confirmed present
- [VERIFIED: README.md] — `maven.pkg.github.com` at line 97 confirmed; badge strip at lines 3–6 confirmed; Security Considerations section at lines 494–535 confirmed
- [VERIFIED: SECURITY.md] — Phase 14 audit appended at line 102; 162 total lines
- [VERIFIED: CONTRIBUTING.md] — git-flow workflow, emoji commits, outdated conventions confirmed
- [VERIFIED: build-logic/src/main/kotlin/KamperPublishPlugin.kt] — artifact ID derivation logic confirmed; group = `com.smellouk.kamper`
- [VERIFIED: gradle.properties + .release-please-manifest.json] — version `1.0.0` confirmed
- [VERIFIED: .github/workflows/pull-request.yml] — targets `develop` branch; no main-branch push workflow exists
- [VERIFIED: .planning/codebase/CONVENTIONS.md] — Conventional Commits types, scopes, Detekt rules
- [VERIFIED: CLAUDE.md] — authoritative build commands, commit format, module patterns, PR checklist

### Secondary (MEDIUM confidence)
- [CITED: docs.github.com/en/communities/using-templates-to-encourage-useful-issues-and-pull-requests/syntax-for-issue-forms] — GitHub Forms YAML schema: required fields (name, description, body), supported input types (markdown, input, textarea, dropdown, checkboxes, upload)

### Tertiary (LOW confidence)
- None

---

## Metadata

**Confidence breakdown:**
- File audit findings: HIGH — all files read directly from the repository
- GitHub Forms syntax: HIGH — verified against official GitHub documentation
- Artifact coordinates: HIGH — derived from build plugin source code
- CI badge guidance: HIGH — all workflows read and branch targets confirmed
- Contributor Covenant version: MEDIUM — confirmed current version is 2.1 from official site search

**Research date:** 2026-04-29
**Valid until:** 2026-05-29 (stable domain — documentation conventions change slowly)
