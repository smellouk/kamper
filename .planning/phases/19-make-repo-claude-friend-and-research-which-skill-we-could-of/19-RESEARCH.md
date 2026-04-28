# Phase 19: Claude-Friendly Repo — Research

**Researched:** 2026-04-28
**Domain:** Developer tooling — CLAUDE.md authoring, Claude Code project skills format, KMP module scaffolding
**Confidence:** HIGH

---

## Summary

Phase 19 creates two categories of artifacts: a root-level `CLAUDE.md` that gives AI agents and contributors a self-contained reference for the Kamper project, and three Claude Code project skills under `.claude/skills/` that encode Kamper-specific workflows. No new library code is written; this phase is purely documentation and developer-tooling authoring.

All decisions are locked in CONTEXT.md (D-01 through D-11). Research confirms the locked choices are coherent with how Claude Code discovers and applies project-level instructions, and provides the exact technical content the planner needs to write detailed task actions.

The key planning risk is completeness: CLAUDE.md and the skills derive their value from accuracy. Every factual claim about Gradle commands, source set names, and class patterns must match the codebase as it exists after Phase 18. The canonical reference is `kamper/modules/cpu/` — every scaffolding detail in `kamper-new-module` must match it exactly.

**Primary recommendation:** Author CLAUDE.md first (it is the authoritative source the skills will reference), then author the three SKILL.md files in dependency order: `kamper-check` (command reference, reuses .claude/settings.json allowlist), `kamper-new-module` (scaffolding template, depends on command knowledge), `kamper-module-review` (review checklist, depends on knowing what a complete module looks like).

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** Rich self-contained guide — not a thin pointer doc. All critical information is inlined so agents and contributors can act without exploring other files.
- **D-02:** Include all four sections: Build & test commands, Module patterns, Commit & PR rules, Architecture & key files.
- **D-03:** Build & test section must document what NOT to run (e.g. Android instrumented tests require a physical device/emulator), and prefer `jvmTest` for fast feedback.
- **D-04:** Commit & PR rules section replaces the outdated CONTRIBUTING.md content (old git-flow, emoji commits, develop branch are all superseded by Conventional Commits from Phase 1). CLAUDE.md is the authoritative source for commit conventions until Phase 20 rewrites CONTRIBUTING.md.
- **D-05:** Audience is both maintainer and external contributors — tone is welcoming, technical precision is maintained.
- **D-06:** Define three skills: `kamper-new-module`, `kamper-check`, `kamper-module-review`.
- **D-07:** `kamper-new-module` scaffolds a full module skeleton — all 8 platform source sets, pre-filled `build.gradle.kts`, Info/Config/Watcher/Performance class stubs, INVALID sentinel, DEFAULT config, commonTest stubs, and the `settings.gradle.kts` registration line. Ready to compile on creation.
- **D-08:** `kamper-check` documents the right Gradle commands per scope: fast JVM-only check (`:kamper:modules:<name>:jvmTest`), full module build, detekt, lint. Prevents Claude from guessing or attempting Android instrumented tests without a device.
- **D-09:** `kamper-module-review` reviews module code against Kamper conventions: naming patterns, expect/actual completeness across all 8 platforms, INVALID sentinel presence, Builder/DEFAULT pattern, test coverage, and safety rule (all platform calls wrapped in try/catch).
- **D-10:** Skills live in `.claude/skills/` (project-local). Automatically picked up when Claude Code runs in this repo.
- **D-11:** Each skill is a SKILL.md file in `.claude/skills/<skill-name>/SKILL.md` following the Claude Code project skill convention.

### Claude's Discretion
- Exact wording and structure of CLAUDE.md sections — the content requirements are locked, the prose style is flexible.
- Whether to add a brief "Quick Start" section at the top of CLAUDE.md (orient new users before the detailed sections).
- Depth of the architecture layer diagram — a textual overview is sufficient, no need for a formal diagram tool.

### Deferred Ideas (OUT OF SCOPE)
- CONTRIBUTING.md rewrite — explicitly Phase 20 scope
- GitHub issue and PR templates — Phase 20 scope
- Per-module sub-CLAUDE.md files — discussed and deferred; root CLAUDE.md is sufficient for v1.0
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| OSS-01 | Make the repo Claude-friendly — CLAUDE.md exists with accurate project context; at least 1 Claude skill defined for Kamper-specific workflows | Sections below provide the exact content for CLAUDE.md and the three skill files, derived directly from codebase analysis |
</phase_requirements>

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| CLAUDE.md authoring | Repository root (static doc) | — | Root-level file consumed by Claude Code automatically |
| Skills discovery | `.claude/skills/` (project-local) | — | Claude Code reads `.claude/` directory when running in repo |
| Gradle command documentation | Build system knowledge | `.claude/settings.json` | Allowlist in settings.json is the ground truth for permitted commands |
| Module scaffolding template | `kamper/modules/cpu/` as canonical | `build-logic/KmpLibraryPlugin.kt` | CPU module is the most complete; KmpLibraryPlugin defines target list |

---

## Standard Stack

This phase produces no new library dependencies. The artifacts are:

| Artifact | Location | Format |
|----------|----------|--------|
| `CLAUDE.md` | `/CLAUDE.md` (repo root) | Markdown |
| `kamper-new-module` skill | `.claude/skills/kamper-new-module/SKILL.md` | Markdown |
| `kamper-check` skill | `.claude/skills/kamper-check/SKILL.md` | Markdown |
| `kamper-module-review` skill | `.claude/skills/kamper-module-review/SKILL.md` | Markdown |

No `npm install` or Gradle dependency changes are needed.

---

## Architecture Patterns

### Claude Code — How CLAUDE.md Is Consumed

`CLAUDE.md` at the repository root is the standard location. Claude Code reads it automatically when launched in the working directory. [ASSUMED — based on Claude Code documentation patterns; behavior confirmed by the GSD system in this repo itself]

**Critical content requirements for CLAUDE.md (from D-01 through D-05):**

1. **Build & test commands** — what to run, what NOT to run, and why
2. **Module patterns** — the 4-class structure, expect/actual, INVALID, Builder/DEFAULT
3. **Commit & PR rules** — Conventional Commits, scopes, no emojis, no `resolves #N`
4. **Architecture & key files** — 4-layer model, key entry points, .planning/ reference

### Claude Code Project Skills — Format

Skills live in `.claude/skills/<skill-name>/SKILL.md`. Claude Code discovers them automatically when the working directory contains a `.claude/` folder. [ASSUMED — confirmed consistent with GSD's own `.claude/get-shit-done/` conventions used in this session]

The `.claude/settings.json` already exists and grants git and file-reading permissions. Skills do NOT modify `settings.json`; they are purely markdown instruction files.

**SKILL.md structure (verified pattern from GSD system reference):** [ASSUMED — derived from observed GSD skill conventions]

```markdown
# Skill Name

## Purpose
One sentence: what this skill does.

## When to Use
Conditions that should trigger this skill.

## Steps
Numbered, actionable steps the agent executes.

## Reference
Key files, commands, or patterns the agent must know.
```

No schema validation — SKILL.md files are freeform markdown. The skill name comes from the directory name, not a field inside the file.

### KMP Module Scaffolding — Complete File Tree

Verified from `kamper/modules/cpu/src/` directory listing. [VERIFIED: codebase grep]

A new module named `{name}` (e.g. `thermal2`) requires these files:

```
kamper/modules/{name}/
├── build.gradle.kts
└── src/
    ├── commonMain/kotlin/com/smellouk/kamper/{name}/
    │   ├── {Name}Info.kt
    │   ├── {Name}Config.kt
    │   ├── {Name}Watcher.kt
    │   ├── {Name}Performance.kt
    │   ├── Module.kt                      (expect val {Name}Module)
    │   └── repository/
    │       ├── {Name}InfoDto.kt
    │       ├── {Name}InfoMapper.kt
    │       └── {Name}InfoRepository.kt
    ├── commonTest/kotlin/com/smellouk/kamper/{name}/
    │   ├── {Name}ConfigBuilderTest.kt
    │   └── repository/
    │       └── {Name}InfoMapperTest.kt
    ├── androidMain/kotlin/com/smellouk/kamper/{name}/
    │   ├── Module.kt                      (actual val {Name}Module)
    │   └── repository/
    │       ├── {Name}InfoRepositoryImpl.kt
    │       └── source/
    │           └── Android{Name}InfoSource.kt
    ├── androidTest/kotlin/com/smellouk/kamper/{name}/
    │   └── repository/
    │       └── {Name}InfoRepositoryImplTest.kt
    ├── iosMain/kotlin/com/smellouk/kamper/{name}/
    │   └── Module.kt                      (actual — typically no-op or iOS API)
    ├── jvmMain/kotlin/com/smellouk/kamper/{name}/
    │   └── Module.kt                      (actual — JVM source or no-op)
    ├── jvmTest/kotlin/com/smellouk/kamper/{name}/
    │   └── (optional — JVM-specific source tests)
    ├── macosMain/kotlin/com/smellouk/kamper/{name}/
    │   └── Module.kt                      (actual — no-op or macOS API)
    ├── tvosMain/kotlin/com/smellouk/kamper/{name}/
    │   └── Module.kt                      (actual — typically no-op)
    ├── jsMain/kotlin/com/smellouk/kamper/{name}/
    │   └── Module.kt                      (actual — no-op)
    └── wasmJsMain/kotlin/com/smellouk/kamper/{name}/
        └── Module.kt                      (actual — no-op)
```

**Registration line in `settings.gradle.kts`:**
```kotlin
include(":kamper:modules:{name}")
```
Insert after line 59 (`:kamper:modules:thermal`). [VERIFIED: codebase grep]

### Canonical Code Patterns for SKILL.md Content

The following patterns must appear verbatim (or with `{Name}` substituted) in `kamper-new-module`:

**`build.gradle.kts` for a new module:** [VERIFIED: kamper/modules/cpu/build.gradle.kts]
```kotlin
plugins {
    id("kamper.kmp.library")
    id("kamper.publish")
}

android {
    namespace = "com.smellouk.kamper.{name}"
    buildFeatures { buildConfig = true }
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":kamper:api"))
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.android)
                implementation(libs.androidx.annotation)
            }
        }
    }
}
```

**`{Name}Info.kt` stub:** [VERIFIED: CONVENTIONS.md + ARCHITECTURE.md]
```kotlin
package com.smellouk.kamper.{name}

import com.smellouk.kamper.api.Info

data class {Name}Info(val value: Double) : Info {
    companion object {
        val INVALID = {Name}Info(-1.0)
    }
}
```

**`{Name}Config.kt` stub:** [VERIFIED: CONVENTIONS.md]
```kotlin
package com.smellouk.kamper.{name}

import com.smellouk.kamper.api.Config
import com.smellouk.kamper.api.KamperDslMarker
import com.smellouk.kamper.api.Logger

data class {Name}Config(
    override val isEnabled: Boolean,
    override val intervalInMs: Long,
    val logger: Logger
) : Config {
    @KamperDslMarker
    object Builder {
        var isEnabled: Boolean = false
        var intervalInMs: Long = 1000L
        var logger: Logger = Logger.EMPTY
        fun build() = {Name}Config(isEnabled, intervalInMs, logger)
    }

    companion object {
        val DEFAULT = Builder.build()
    }
}
```

**`commonMain/Module.kt` (expect):** [VERIFIED: ARCHITECTURE.md]
```kotlin
package com.smellouk.kamper.{name}

import com.smellouk.kamper.api.PerformanceModule

expect val {Name}Module: PerformanceModule<{Name}Config, {Name}Info>
```

**`{platform}Main/Module.kt` (actual, no-op platforms):** [VERIFIED: ARCHITECTURE.md pattern]
```kotlin
package com.smellouk.kamper.{name}

import com.smellouk.kamper.api.PerformanceModule

actual val {Name}Module: PerformanceModule<{Name}Config, {Name}Info>
    get() = PerformanceModule(
        config = {Name}Config.DEFAULT,
        performance = {Name}Performance(
            watcher = {Name}Watcher(
                defaultDispatcher = kotlinx.coroutines.Dispatchers.Default,
                mainDispatcher = kotlinx.coroutines.Dispatchers.Main,
                repository = {Name}InfoRepositoryImpl({Name}InfoSource()),
                logger = {Name}Config.DEFAULT.logger
            ),
            logger = {Name}Config.DEFAULT.logger
        )
    )
```

### Gradle Command Reference for CLAUDE.md and `kamper-check`

[VERIFIED: .planning/codebase/TESTING.md + .claude/settings.json]

| Command | Scope | Device needed? | When to use |
|---------|-------|----------------|-------------|
| `./gradlew :kamper:modules:<name>:jvmTest` | Single module JVM | No | Fast feedback — primary path for Claude agents |
| `./gradlew :kamper:modules:<name>:test` | Single module all unit tests | No | Includes androidUnitTest (MockK, JVM) |
| `./gradlew :kamper:api:test` | API contracts | No | After modifying api layer |
| `./gradlew :kamper:engine:test` | Engine | No | After modifying engine layer |
| `./gradlew test` | All modules all unit tests | No | Full pre-commit sweep |
| `./gradlew detekt` | Static analysis | No | Before every commit (zero-tolerance) |
| `./gradlew connectedAndroidTest` | Instrumented (on-device) | YES — emulator or physical | Never run in CI without device; skip in Claude sessions |
| `./gradlew assembleXCFramework` | iOS/macOS XCFramework | No (compiles on host) | Only when publishing iOS artifacts |

**Gradle NOT in .claude/settings.json allowlist:** `./gradlew` commands that invoke Gradle are NOT currently in the allowlist (`settings.json` only permits `git`, `ls`, `cat`, `grep`, `mkdir`, `wc`, `head`, `tail`, `sort`, `tr`, `echo`, `date`). This means Claude Code agents cannot run Gradle commands without a permission prompt unless the planner adds them to the allowlist. [VERIFIED: .claude/settings.json]

This is a critical finding: the `kamper-check` skill should document the commands for human use, AND the planner should consider whether to add `Bash(./gradlew *:test:*)` and `Bash(./gradlew detekt:*)` etc. to the allowlist in settings.json.

### CLAUDE.md Architecture Section — Key Facts

These go into the architecture section verbatim:

**4-layer model:** [VERIFIED: ARCHITECTURE.md]
```
Layer 1 — kamper/api/        Shared contracts (Info, Config, Watcher, Performance, Logger)
Layer 2 — kamper/engine/     Engine orchestrator + Kamper platform singleton
Layer 3 — kamper/modules/    8 metric modules (cpu, fps, memory, network, issues, jank, gc, thermal)
Layer 4 — kamper/ui/android/ Optional Compose overlay panel
```

**Key files for contributors:** [VERIFIED: codebase]
- `kamper/api/src/commonMain/kotlin/com/smellouk/kamper/api/Watcher.kt` — polling engine shared by all modules
- `kamper/engine/src/commonMain/kotlin/com.smellouk.kamper/Engine.kt` — module registry
- `kamper/modules/cpu/` — canonical reference for all module patterns
- `build-logic/src/main/kotlin/KmpLibraryPlugin.kt` — convention plugin all KMP modules apply
- `settings.gradle.kts` — new modules must be registered here
- `.planning/` — GSD planning artifacts, phase docs, ADRs (not shipped in library)

**ADRs to mention in CLAUDE.md architecture section:** [VERIFIED: .planning/codebase/adr/ listing]
- ADR-001: Plugin architecture (manual install pattern)
- ADR-002: KMP expect/actual pattern
- ADR-003: Listener pattern
- ADR-004: No breaking changes (API freeze)

### Commit Convention for CLAUDE.md (D-04)

The following replaces the outdated CONTRIBUTING.md content as the authoritative source until Phase 20:

[VERIFIED: CONVENTIONS.md + CONTEXT.md]

```
Format:  <type>(<scope>): <short description>

Types:   feat | fix | chore | docs | test | refactor
Scopes:  cpu | fps | memory | network | issues | jank | gc | thermal | engine | api | ui | build | deps | phase<N>

Examples:
  feat(cpu): add thermal throttle detection
  fix(engine): guard against duplicate module install
  chore(deps): bump coroutines to 1.10.1
  docs(api): update Watcher KDoc

Rules:
  - No emojis
  - No "resolves #N" footers
  - No develop branch — main is the integration branch
  - Squash merge when merging executor worktree branches
  - Description is imperative mood, lowercase, no trailing period
```

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Skill discovery mechanism | Custom plugin loader | `.claude/skills/` directory convention | Claude Code reads this path automatically |
| Gradle command documentation | Guessing from memory | `.claude/settings.json` allowlist + TESTING.md | Ground truth for permitted and safe commands |
| Module file templates | Invent structure | `kamper/modules/cpu/` as canonical | CPU is verified complete with all 8 platforms |
| Convention plugin config | Re-document KmpLibraryPlugin | Reference `build-logic/src/main/kotlin/KmpLibraryPlugin.kt` | Plugin is the authoritative target list |

---

## Common Pitfalls

### Pitfall 1: Stale or Invented Gradle Commands
**What goes wrong:** CLAUDE.md or `kamper-check` documents a Gradle task path that doesn't exist (e.g. `:kamper:modules:cpu:check` instead of `:kamper:modules:cpu:test`).
**Why it happens:** Training data or guessing, not reading TESTING.md.
**How to avoid:** Every command in CLAUDE.md and skills must come from TESTING.md or verified codebase grep. The `test` task is the right unit-test entry point; `jvmTest` is the fast subset.
**Warning signs:** A command that produces "Task not found" when run.

### Pitfall 2: Wrong Platform Source Set Count
**What goes wrong:** `kamper-new-module` scaffolds 7 platforms instead of 8, or names them wrong (e.g. `tvosMain` vs `tvOSMain`).
**Why it happens:** Counting from memory instead of reading the actual directory listing.
**How to avoid:** Source set names are verified from `kamper/modules/cpu/src/` listing: `androidMain`, `androidTest`, `commonMain`, `commonTest`, `iosMain`, `jsMain`, `jvmMain`, `jvmTest`, `macosMain`, `tvosMain`, `wasmJsMain`. That is 8 platform main source sets (android, ios, jvm, macos, tvos, js, wasmJs + common).
**Warning signs:** KMP build failure: "no actual for expect val XxxModule".

### Pitfall 3: Gradle Commands Not in Allowlist
**What goes wrong:** `kamper-check` skill tells Claude to run `./gradlew test` but Claude Code blocks it because `Bash(./gradlew:*)` is not in `.claude/settings.json`.
**Why it happens:** Skills document commands without checking settings.json allowlist.
**How to avoid:** Either (a) skills note that Gradle requires a permission prompt and humans must run them, OR (b) the planner adds relevant `Bash(./gradlew *:*)` entries to `settings.json` as part of this phase.
**Warning signs:** Claude Code displays a permission denied message when trying to run Gradle.

### Pitfall 4: CLAUDE.md Describing Outdated Patterns
**What goes wrong:** CLAUDE.md describes git-flow (develop branch, feature branches) instead of trunk-based development on main.
**Why it happens:** CONTRIBUTING.md still has old content; Phase 19 must explicitly override it.
**How to avoid:** D-04 is locked: CLAUDE.md is authoritative. The commit convention section above is the correct content.
**Warning signs:** External contributor opens a PR targeting `develop` (which no longer exists).

### Pitfall 5: Skills Referencing Files That Don't Exist Yet
**What goes wrong:** `kamper-module-review` references a pattern file that is itself part of a future phase.
**Why it happens:** Writing skills before verifying the codebase state.
**How to avoid:** All references in skills must be to files that exist after Phase 18 completes. No forward references.

---

## Code Examples

### SKILL.md Invocation Pattern (how Claude uses it)
[ASSUMED — consistent with GSD skill conventions observed in this session]

When a user types `/kamper-new-module thermal2`, Claude Code:
1. Finds `.claude/skills/kamper-new-module/SKILL.md`
2. Reads the skill content as system-level instructions
3. Executes the steps (create directories, write files, update settings.gradle.kts)
4. Reports completion

The skill name in the slash command matches the directory name exactly.

### `kamper-module-review` Checklist Content

The review skill must check all of these (verified from CONVENTIONS.md + ARCHITECTURE.md):

```markdown
## Convention Checklist

### Naming
- [ ] Module directory: `kamper/modules/{name}/`
- [ ] Package: `com.smellouk.kamper.{name}`
- [ ] Info class: `{Name}Info` (data class, implements `Info`)
- [ ] Config class: `{Name}Config` (data class, implements `Config`)
- [ ] Watcher class: `{Name}Watcher` (internal, extends `Watcher<{Name}Info>`)
- [ ] Performance class: `{Name}Performance` (internal, extends `Performance<...>`)

### Completeness
- [ ] `{Name}Info.companion.INVALID` exists
- [ ] `{Name}Config.Builder` object with `build()` function
- [ ] `{Name}Config.DEFAULT` companion val
- [ ] `expect val {Name}Module` in `commonMain/Module.kt`
- [ ] `actual val {Name}Module` in ALL 8 platform source sets: androidMain, iosMain, jvmMain, macosMain, tvosMain, jsMain, wasmJsMain
- [ ] Registered in `settings.gradle.kts`

### Safety
- [ ] All platform calls in `XxxInfoSource` wrapped in `try/catch`
- [ ] Watcher loop uses `while (isActive)`, not `while (true)`
- [ ] No `println()` in production code — uses `logger.log()`
- [ ] No `TODO:` or `FIXME:` comments (Detekt will fail)

### Testing
- [ ] `{Name}ConfigBuilderTest` in `commonTest`
- [ ] `{Name}InfoMapperTest` in `commonTest`
- [ ] Platform source test in `androidTest` or `jvmTest`
```

---

## State of the Art

| Old Approach | Current Approach | Impact |
|--------------|------------------|--------|
| CONTRIBUTING.md as commit guide | CLAUDE.md as authoritative source (D-04) | CONTRIBUTING.md is outdated; ignore it until Phase 20 |
| No project skills | `.claude/skills/` with 3 Kamper skills | Claude Code agents get Kamper-specific workflow knowledge |
| No CLAUDE.md | Root CLAUDE.md with 4 sections | Agents and contributors have accurate project context |

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | CLAUDE.md at repo root is automatically read by Claude Code | Architecture Patterns | Low — Claude Code reads CLAUDE.md by convention; this is the established pattern used throughout this GSD session |
| A2 | `.claude/skills/<name>/SKILL.md` is the correct skill file path and Claude Code discovers it automatically | Architecture Patterns | Medium — if path or naming convention differs, skills won't be found; planner should verify with user or Claude Code docs |
| A3 | Slash command `/kamper-new-module` maps to `.claude/skills/kamper-new-module/SKILL.md` by directory name | Code Examples | Medium — same risk as A2 |

---

## Open Questions (RESOLVED)

1. **Gradle allowlist in settings.json**
   - What we know: `.claude/settings.json` does not currently permit `./gradlew` commands.
   - What's unclear: Should Phase 19 add `Bash(./gradlew *:test:*)` and `Bash(./gradlew detekt:*)` to the allowlist so Claude Code agents can run checks autonomously?
   - Recommendation: Add a sub-task to Wave 1 that extends `.claude/settings.json` with the safe Gradle commands. Specifically: `Bash(./gradlew *:jvmTest:*)`, `Bash(./gradlew *:test:*)`, `Bash(./gradlew detekt:*)`. Exclude `connectedAndroidTest`.

2. **Quick Start section in CLAUDE.md**
   - What we know: D-05 says audience is both maintainer and external contributor; Claude's Discretion allows adding a Quick Start section.
   - What's unclear: How brief should Quick Start be to not duplicate the four required sections?
   - Recommendation: Add a 5-line Quick Start at the top covering: clone, JDK 17 requirement, `./gradlew test` as smoke test, and pointer to the four sections below.

---

## Environment Availability

Step 2.6: SKIPPED — Phase 19 produces only markdown files. No external tools, runtimes, or services are needed beyond `git` and a text editor. The `.claude/skills/` directory structure is created with `mkdir` (already in the settings.json allowlist).

---

## Validation Architecture

Phase 19 produces no production code. There are no automated tests to write. Success criteria are human-verified:

1. `CLAUDE.md` exists at repo root — `ls /CLAUDE.md`
2. Three skill files exist — `ls .claude/skills/*/SKILL.md`
3. CLAUDE.md contains all four required sections (Build & test commands, Module patterns, Commit & PR rules, Architecture & key files)
4. `kamper-new-module` SKILL.md references all 8 platform source sets by name
5. `kamper-check` SKILL.md lists jvmTest as the fast path and flags connectedAndroidTest as device-required
6. `kamper-module-review` SKILL.md includes the INVALID sentinel check and 8-platform completeness check

**Wave 0 gaps:** None — no test framework needed for this phase.

---

## Security Domain

Phase 19 creates only markdown documentation files. No authentication, session management, input validation, cryptography, or access control is involved. Security domain is not applicable.

---

## Sources

### Primary (HIGH confidence)
- `kamper/modules/cpu/src/` — [VERIFIED] directory listing confirming all 8 platform source set names
- `kamper/modules/cpu/build.gradle.kts` — [VERIFIED] canonical build file pattern
- `build-logic/src/main/kotlin/KmpLibraryPlugin.kt` — [VERIFIED] all 10 KMP targets (8 platform main sets)
- `.planning/codebase/CONVENTIONS.md` — [VERIFIED] naming patterns, INVALID sentinel, Builder/DEFAULT
- `.planning/codebase/ARCHITECTURE.md` — [VERIFIED] 4-layer model, expect/actual step-by-step guide
- `.planning/codebase/TESTING.md` — [VERIFIED] Gradle command reference, test runner mapping
- `.planning/codebase/STACK.md` — [VERIFIED] versions, targets, dependencies
- `.claude/settings.json` — [VERIFIED] current allowlist (Gradle NOT included)
- `settings.gradle.kts` — [VERIFIED] module registration lines 52-59

### Secondary (MEDIUM confidence)
- `.planning/codebase/adr/` — [VERIFIED listing] ADR-001 through ADR-004 exist and must be referenced in CLAUDE.md

### Tertiary (ASSUMED — flagged)
- Claude Code SKILL.md format and discovery path — [ASSUMED] consistent with GSD conventions observed in this session; see Assumptions Log A2/A3

---

## Metadata

**Confidence breakdown:**
- CLAUDE.md content: HIGH — all content derived directly from verified codebase files
- Skill format: MEDIUM — path convention assumed from GSD system conventions; functionally proven in this session
- Gradle command reference: HIGH — verified from TESTING.md
- Scaffolding template: HIGH — verified from cpu module directory listing and build file

**Research date:** 2026-04-28
**Valid until:** 2026-05-28 (stable — no library code changes in this phase)
