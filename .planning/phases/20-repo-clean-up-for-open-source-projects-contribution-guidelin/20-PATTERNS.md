# Phase 20: Open Source Cleanup - Pattern Map

**Mapped:** 2026-04-29
**Files analyzed:** 6 new/modified files + 1 deleted file
**Analogs found:** 5 / 6 (1 is a net-new format with no codebase analog)

---

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|-------------------|------|-----------|----------------|---------------|
| `CONTRIBUTING.md` | doc | n/a | `CLAUDE.md` | content-source (all rules must be mirrored from here) |
| `.github/ISSUE_TEMPLATE/bug-report.yml` | config | n/a | none in repo | no analog — use RESEARCH.md GitHub Forms schema |
| `.github/ISSUE_TEMPLATE/feature-request.yml` | config | n/a | none in repo | no analog — use RESEARCH.md GitHub Forms schema |
| `.github/PULL_REQUEST_TEMPLATE.md` | config | n/a | `.github/PULL_REQUEST_TEMPLATE.md` (current) | replacement — current file is the anti-pattern |
| `README.md` | doc | n/a | `README.md` (current) | surgical replacement — keep structure, fix 4 problem areas |
| `SECURITY.md` | — | — | — | DELETE — no replacement |

---

## Pattern Assignments

### `CONTRIBUTING.md` (doc — full replacement)

**Analog source:** `CLAUDE.md` lines 1–226 (authoritative rules) and `.planning/codebase/CONVENTIONS.md` (naming, Detekt rules).

**Rule: CONTRIBUTING.md must agree with CLAUDE.md on every item below. Cross-check each before finalizing.**

**Build commands pattern** (mirror from `CLAUDE.md` lines 18–35):

```markdown
## Build and test

The fast development path uses the JVM target — no device required.

| Command | What it runs | When to use |
|---------|-------------|-------------|
| `./gradlew :kamper:modules:<name>:jvmTest` | Single module, JVM only | Fast feedback — primary path |
| `./gradlew :kamper:modules:<name>:test` | Single module, all unit tests | Includes androidUnitTest |
| `./gradlew :kamper:api:test` | API contracts | After modifying the api layer |
| `./gradlew :kamper:engine:test` | Engine | After modifying the engine layer |
| `./gradlew test` | All modules, all unit tests | Full pre-commit sweep |
| `./gradlew detekt` | Static analysis (zero-tolerance) | Before every commit |

> **Instrumented tests require a connected device.**
> `./gradlew connectedAndroidTest` requires an Android device or running emulator.
> Do not run this command in automated or headless environments.
```

**Commit format pattern** (mirror from `CLAUDE.md` lines 97–130):

```markdown
## Commit format

```
<type>(<scope>): <short description>
```

Allowed types: `feat`, `fix`, `chore`, `docs`, `test`, `refactor`

Allowed scopes: `cpu`, `fps`, `memory`, `network`, `issues`, `jank`, `gc`, `thermal`,
`engine`, `api`, `ui`, `build`, `deps`, `phase<N>`

Rules:
- Imperative mood, lowercase, no trailing period
- No emojis anywhere
- No `resolves #N` or `fixes #N` footers
```

**Module pattern overview** (mirror from `CLAUDE.md` lines 43–92):

```markdown
## Module pattern

Every Kamper performance module follows a strict 4-class structure:

| Class | Role | Visibility |
|-------|------|------------|
| `{Name}Info` | Data class implementing `Info`; the metric payload | `public` |
| `{Name}Config` | Data class implementing `Config`; holds `isEnabled`, `intervalInMs`, `logger` | `public` |
| `{Name}Watcher` | Coroutine polling loop extending `Watcher<{Name}Info>` | `internal` |
| `{Name}Performance` | Lifecycle container extending `Performance<...>` | `internal` |

Every `Info` subclass must have a companion `INVALID` constant.
The canonical reference module is `kamper/modules/cpu/`.
```

**PR checklist pattern** (mirror from `CLAUDE.md` lines 135–143):

```markdown
## PR checklist

Before opening a pull request:

- [ ] `./gradlew detekt` passes (zero issues)
- [ ] `./gradlew :kamper:modules:<name>:jvmTest` passes for every touched module
- [ ] No `TODO:` or `FIXME:` comments in changed files
- [ ] Public API changes are additive only (no removals or signature breaks)
```

**Branch strategy** (mirror from `CLAUDE.md` lines 131–134 and commit section):

```markdown
## Branch strategy

- `main` is the integration branch. There is no `develop` branch.
- PRs are squash-merged. Clean your commit history before requesting review.
```

---

### `.github/ISSUE_TEMPLATE/bug_report.yml` (config — new file, no codebase analog)

**No analog in codebase.** Use the GitHub Forms schema from RESEARCH.md directly.

**Full template pattern** (from RESEARCH.md lines 173–246):

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
        Before opening, please search existing issues.

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

---

### `.github/ISSUE_TEMPLATE/feature_request.yml` (config — new file, no codebase analog)

**No analog in codebase.** Use the GitHub Forms schema from RESEARCH.md directly.

**Full template pattern** (from RESEARCH.md lines 250–278):

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

---

### `.github/PULL_REQUEST_TEMPLATE.md` (config — full replacement)

**Anti-pattern reference** (current file `.github/PULL_REQUEST_TEMPLATE.md` lines 1–28):
The current file must NOT be preserved. Its violations to avoid:
- Emoji section headers (`## 🚀 Description`, `## 🧪 How Has This Been Tested?`, `## ✅ Checklist`, `## 📷 Screenshots`)
- Reference to `develop` branch (lines 19, 23): `blob/develop/CONTRIBUTING.md`, `blob/develop/README.md`
- Checklist items irrelevant to Kamper's PR process: "I have used and tested this on my device(s)"
- Missing additive-only API requirement

**New template pattern** (from RESEARCH.md lines 283–293):

```markdown
## What changed and why
<!-- Describe your change and the motivation behind it. -->

## Checklist
- [ ] `./gradlew detekt` passes (zero issues)
- [ ] `./gradlew :kamper:modules:<name>:jvmTest` passes for every touched module
- [ ] No `TODO:` or `FIXME:` in changed files
- [ ] Public API changes are additive only (no removals or signature breaks)
```

---

### `README.md` (doc — surgical replacement, not full rewrite)

**Analog:** Current `README.md`. The file has a good structure and rich content — only four areas require changes.

**What to KEEP** (read current file to identify — these sections are already correct):
- Lines 1–6: badge strip (license, release, issues, stars) — keep as-is
- Lines 7–88: tagline, Kamper UI section, platform support matrix — keep as-is
- Lines 122–299: Quick start, Modules section — keep as-is
- Lines 300–492: Kamper UI detailed section, Demos, Lifecycle, Service Integrations — keep as-is
- Lines 537–586: How-tos, Contributing, Acknowledgements, License — keep as-is

**Change 1 — Installation block** (current lines 91–120, replace entirely):

Current anti-pattern (line 93 + 96–98):
```
Add the GitHub Packages repository, then pull...
repositories {
    maven("https://maven.pkg.github.com/smellouk/kamper")
}
```

Replacement (from RESEARCH.md lines 315–340):
```markdown
## Installation

Add the engine and whichever modules you need:

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

> If `mavenCentral()` is not already present in your `repositories {}` block, add it.
```

**Change 2 — Delete Security Considerations section** (current lines 495–534):

The entire section from `## Security Considerations` through the closing `---` before `## How-tos` must be removed. This resolves Pitfall 2 from RESEARCH.md.

**Change 3 — Add Versioning section** (insert before `## Contributing`, currently around line 557):

New section (from RESEARCH.md lines 344–355):
```markdown
## Versioning

Kamper follows [semantic versioning](https://semver.org/):

- **Patch** (`1.0.x`) — bug fixes; no API changes
- **Minor** (`1.x.0`) — new modules or features; backward compatible
- **Major** (`x.0.0`) — breaking API changes; frozen for all v1.x releases

The latest release is always available on [GitHub Releases](https://github.com/smellouk/kamper/releases).
Changes are listed in [CHANGELOG.md](CHANGELOG.md).
```

**Change 4 — No CI badge addition** (from RESEARCH.md lines 500–503):
The `pull-request.yml` workflow targets the `develop` branch (verified: `.github/workflows/pull-request.yml` line 8). No push-to-main workflow exists. Omit CI badge — the four existing shields.io badges (license, release, issues, stars) at lines 3–6 are reliable and sufficient.

---

### `SECURITY.md` (DELETE — no new file)

**Action:** Delete the file entirely per D-05. No content should be copied or archived.

**Warning:** SECURITY.md lines 102–162 contain internal Phase 14 security audit data (threat IDs, ASVS levels, accepted risks). This content must not appear in any public-facing file. Deletion eliminates it.

---

## Shared Patterns

### Conventional Commits Agreement
**Source:** `CLAUDE.md` (lines 88–143) — authoritative.
**Apply to:** `CONTRIBUTING.md`.
**Rule:** Every type (`feat`, `fix`, `chore`, `docs`, `test`, `refactor`), every scope (`cpu`, `fps`, `memory`, `network`, `issues`, `jank`, `gc`, `thermal`, `engine`, `api`, `ui`, `build`, `deps`, `phase<N>`), and all constraints (no emojis, no `resolves #N`, imperative mood, lowercase, no trailing period) must be transcribed verbatim into CONTRIBUTING.md. No divergence is permitted.

### PR Checklist Agreement
**Source:** `CLAUDE.md` lines 135–143.
**Apply to:** `CONTRIBUTING.md` PR process section AND `.github/PULL_REQUEST_TEMPLATE.md`.
**Both files must carry identical checklist items:**
1. `./gradlew detekt` passes (zero issues)
2. `./gradlew :kamper:modules:<name>:jvmTest` passes for every touched module
3. No `TODO:` or `FIXME:` in changed files
4. Public API changes are additive only

### Maven Central Artifact IDs
**Source:** RESEARCH.md lines 106–121 (verified from `build-logic/src/main/kotlin/KamperPublishPlugin.kt`).
**Apply to:** `README.md` installation block and `CONTRIBUTING.md` quick-start reference.
**Group:** `com.smellouk.kamper` for all artifacts.
**Version:** `1.0.0` (verified in `gradle.properties`).
**Artifact ID pattern:** `{name}-module` for modules, `{name}-integration` for integrations, bare name (`engine`, `bom`) for infrastructure.

### No Stale References Policy
**Apply to:** All files created or modified in this phase.
**Forbidden strings (verified from current files):**
- `maven.pkg.github.com` — GitHub Packages URL; remove entirely
- `develop` — stale integration branch name; replace with `main`
- `TODO:` / `FIXME:` — forbidden by Detekt; never introduce
- Any emoji characters in commit message examples
- `resolves #N` / `fixes #N` in commit footer examples
- `lint` as a standalone Gradle task (the CI workflow uses this; CONTRIBUTING.md should reference `detekt` only)

---

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| `.github/ISSUE_TEMPLATE/bug_report.yml` | config | n/a | GitHub Forms YAML format; no existing issue template directory in this repo |
| `.github/ISSUE_TEMPLATE/feature_request.yml` | config | n/a | GitHub Forms YAML format; no existing issue template directory in this repo |

For these two files, use the schema in RESEARCH.md lines 155–168 (field types and required/optional attributes) as the authoritative pattern.

---

## Metadata

**Analog search scope:** `/Users/smellouk/Developer/git/kamper/.github/`, `/Users/smellouk/Developer/git/kamper/CONTRIBUTING.md`, `/Users/smellouk/Developer/git/kamper/README.md`, `/Users/smellouk/Developer/git/kamper/CLAUDE.md`, `/Users/smellouk/Developer/git/kamper/.planning/codebase/CONVENTIONS.md`
**Files scanned:** 8
**Pattern extraction date:** 2026-04-29
