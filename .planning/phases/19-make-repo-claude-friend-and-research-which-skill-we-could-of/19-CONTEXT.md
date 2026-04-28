# Phase 19: Claude-Friendly Repo - Context

**Gathered:** 2026-04-29
**Status:** Ready for planning

<domain>
## Phase Boundary

Create CLAUDE.md at the repo root with accurate, comprehensive project context, and define three project-specific Claude skills in `.claude/skills/`. CONTRIBUTING.md and GitHub issue/PR templates are out of scope (Phase 20).

</domain>

<decisions>
## Implementation Decisions

### CLAUDE.md Strategy
- **D-01:** Rich self-contained guide — not a thin pointer doc. All critical information is inlined so agents and contributors can act without exploring other files.
- **D-02:** Include all four sections: Build & test commands, Module patterns, Commit & PR rules, Architecture & key files.
- **D-03:** Build & test section must document what NOT to run (e.g. Android instrumented tests require a physical device/emulator), and prefer `jvmTest` for fast feedback.
- **D-04:** Commit & PR rules section replaces the outdated CONTRIBUTING.md content (old git-flow, emoji commits, develop branch are all superseded by Conventional Commits from Phase 1). CLAUDE.md is the authoritative source for commit conventions until Phase 20 rewrites CONTRIBUTING.md.
- **D-05:** Audience is both maintainer and external contributors — tone is welcoming, technical precision is maintained.

### Skill Selection & Scope
- **D-06:** Define three skills: `kamper-new-module`, `kamper-check`, `kamper-module-review`.
- **D-07:** `kamper-new-module` scaffolds a **full module skeleton** — all 8 platform source sets, pre-filled `build.gradle.kts`, Info/Config/Watcher/Performance class stubs, INVALID sentinel, DEFAULT config, commonTest stubs, and the `settings.gradle.kts` registration line. Ready to compile on creation.
- **D-08:** `kamper-check` documents the right Gradle commands per scope: fast JVM-only check (`:kamper:modules:<name>:jvmTest`), full module build, detekt, lint. Prevents Claude from guessing or attempting Android instrumented tests without a device.
- **D-09:** `kamper-module-review` reviews module code against Kamper conventions: naming patterns, expect/actual completeness across all 8 platforms, INVALID sentinel presence, Builder/DEFAULT pattern, test coverage, and safety rule (all platform calls wrapped in try/catch).

### Skill Placement & Format
- **D-10:** Skills live in `.claude/skills/` (project-local). Automatically picked up when Claude Code runs in this repo.
- **D-11:** Each skill is a SKILL.md file in `.claude/skills/<skill-name>/SKILL.md` following the Claude Code project skill convention.

### Claude's Discretion
- Exact wording and structure of CLAUDE.md sections — the content requirements are locked, the prose style is flexible.
- Whether to add a brief "Quick Start" section at the top of CLAUDE.md (orient new users before the detailed sections).
- Depth of the architecture layer diagram — a textual overview is sufficient, no need for a formal diagram tool.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Module Architecture & Patterns
- `.planning/codebase/STRUCTURE.md` — KMP source set layout, directory conventions, module graph
- `.planning/codebase/CONVENTIONS.md` — Naming patterns, code style, Detekt rules, ubiquitous language
- `.planning/codebase/STACK.md` — Full tech stack, KMP targets (all 8), Gradle versions, test frameworks
- `.planning/codebase/ARCHITECTURE.md` — Layer architecture (api → engine → modules → ui)

### Testing Approach
- `.planning/codebase/TESTING.md` — Test conventions, what runs on JVM vs device, Mokkery usage

### Architecture Decision Records (ADRs)
- `.planning/codebase/adr/` — ADR directory; reference relevant ADRs in CLAUDE.md architecture section (especially ADR-001 manual install, ADR-003 listener pattern, ADR-004 API freeze)

### Build System
- `build-logic/src/main/kotlin/KmpLibraryPlugin.kt` — Convention plugin all KMP modules use; reference for what `build.gradle.kts` needs in a new module
- `kamper/modules/cpu/build.gradle.kts` — Canonical example for a full KMP module build file
- `settings.gradle.kts` — Where new modules must be registered

### Example Module (canonical reference for scaffolding)
- `kamper/modules/cpu/` — The most complete and well-tested module; use as the scaffolding template

### Skills Format
- `.claude/settings.json` — Existing project-level Claude Code settings (permissions already defined here)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `kamper/modules/cpu/` — Full 8-platform module; use as the template for `kamper-new-module` scaffolding
- `.claude/settings.json` — Already has Gradle permission allowlist; the `kamper-check` skill should reference these exact commands
- `build-logic/src/main/kotlin/KmpLibraryPlugin.kt` — Convention plugin; `build.gradle.kts` for new modules applies this plugin

### Established Patterns
- **KMP expect/actual:** Every non-trivial platform behavior has an `expect` in `commonMain` and per-platform `actual` implementations. The `kamper-new-module` skill must scaffold all 8: `androidMain`, `iosMain`, `jvmMain`, `macosMain`, `jsMain`, `wasmJsMain`, `tvosMain`, `commonMain`.
- **Info/Config/Watcher/Performance hierarchy:** Every module follows this exact 4-class structure. Deviation is a convention violation.
- **INVALID sentinel:** Every `Info` subclass has a companion `INVALID` constant. `kamper-module-review` must check this.
- **Safety rule:** All platform-specific calls wrapped in try/catch — bugs must never crash the host app.
- **Conventional Commits:** `feat/fix/chore/docs/test` + scope (e.g. `feat(cpu):`, `fix(engine):`), no emojis, no `resolves #N` footers. CLAUDE.md is the authoritative source.

### Integration Points
- New modules register in `settings.gradle.kts` — `kamper-new-module` must emit the registration line
- New modules depend on `:kamper:api` — scaffold must include this in `build.gradle.kts`
- All new modules use `id("com.smellouk.kmp.library")` convention plugin from `build-logic`

</code_context>

<specifics>
## Specific Ideas

- `kamper-new-module` takes the module name as an argument (e.g. `/kamper-new-module thermal2`) and scaffolds the entire directory tree
- `kamper-check` should distinguish fast path (JVM tests only, no device needed) from full path (Android instrumented, requires device)
- CLAUDE.md should mention the GSD planning workflow in `.planning/` so contributors understand the project management structure

</specifics>

<deferred>
## Deferred Ideas

- CONTRIBUTING.md rewrite — explicitly Phase 20 scope
- GitHub issue and PR templates — Phase 20 scope
- Per-module sub-CLAUDE.md files — discussed and deferred; root CLAUDE.md is sufficient for v1.0

</deferred>

---

*Phase: 19-make-repo-claude-friend-and-research-which-skill-we-could-of*
*Context gathered: 2026-04-29*
