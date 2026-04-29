# Phase 20: Open Source Cleanup - Context

**Gathered:** 2026-04-29
**Updated:** 2026-04-29
**Status:** Executed — addendum adds README renovation decisions deferred to Phase 21

<domain>
## Phase Boundary

Make Kamper welcoming and legible to external contributors: full CONTRIBUTING.md rewrite, GitHub issue and PR templates, README.md refresh, deletion of SECURITY.md, and a sweep of all public-facing files for stale internal references.

**Execution note:** Phase 20 was executed with surgical README edits (3 localized changes) rather than the full structural renovation specified in D-07. The full renovation is deferred to Phase 21 per this addendum discussion.

</domain>

<decisions>
## Implementation Decisions

### CONTRIBUTING.md
- **D-01:** Full contributor guide — standalone document covering: setup (JDK 17, Gradle wrapper), how to build and run tests (`./gradlew jvmTest`, detekt), module patterns overview (4-class structure, INVALID sentinel), Conventional Commits format (types, scopes, no emojis, no `resolves #N`), branch naming (`main` is integration branch, no `develop`), PR process (squash merge), code review expectations, and device requirement note for instrumented tests.
- **D-02:** CONTRIBUTING.md is authoritative for **humans**; CLAUDE.md is authoritative for **Claude Code agents**. They share the same rules but are completely independent documents — no cross-reference required between them.

### GitHub Templates
- **D-03:** Create both issue templates in `.github/ISSUE_TEMPLATE/`: bug report (reproduction steps, expected vs actual, platform, Kamper version, module) and feature request (use case, proposed API or behavior). Use `.yml` format for structured GitHub Forms.
- **D-04:** Replace the existing `.github/PULL_REQUEST_TEMPLATE.md` entirely. New template: description of what changed and why, checklist (detekt passes, jvmTest passes, no TODO/FIXME in changed files, public API changes are additive only).

### Public File Audit
- **D-05:** Delete `SECURITY.md` entirely — it is not needed.
- **D-06:** Audit all public-facing files for stale internal content: `README.md`, `.github/` directory (workflows, templates), any docs accessible from the repo root. Strip stale file:line references, internal snapshot URLs, and outdated version references.

### README.md Refresh (Phase 20 — surgical, executed)
- **D-07:** Phase 20 plan 03 applied three surgical edits to README.md: (1) replaced GitHub Packages install block with Maven Central coordinates, (2) deleted the "Security Considerations" section, (3) added a Versioning section. The D-07 full structural renovation was NOT executed — see D-10 through D-15 below for the full renovation spec deferred to Phase 21.
- **D-08:** Maven Central coordinates are the canonical install path. Remove any `maven.pkg.github.com` snapshot references. *(Executed in Phase 20.)*

### Release / Versioning Docs
- **D-09:** README includes a "Versioning" section: semantic versioning explanation in the context of Kamper (patch = bug fixes, minor = new modules or features, major = breaking API changes, which are frozen until post-v1.0), how to find the latest version (GitHub Releases), link to CHANGELOG. *(Executed in Phase 20.)*

### README.md Full Renovation (Phase 21 — not yet executed)
- **D-10:** Full structural rewrite of README.md in Phase 21. Not surgical — rebuild from scratch following the section order below. Existing content is reorganized, not discarded.
- **D-11:** Final section order:
  1. Badge strip (license, release, issues, stars)
  2. Project tagline + one-paragraph description
  3. Modules table (all 8 modules — name, description, platform support)
  4. Quick start (Maven Central coordinates, engine install, one module install, one listener example) — Lifecycle condensed into this section
  5. Kamper UI (with screenshots/GIFs — flagship feature)
  6. Service integrations (Sentry, Firebase Crashlytics, OpenTelemetry — current depth per D-13)
  7. Platform matrix
  8. Versioning
  9. Contributing + Links (CONTRIBUTING.md, CLAUDE.md, GitHub Releases)
  10. Acknowledgements
  11. License
- **D-12:** Sections to remove entirely: `## Demos`, `## How-tos` (belong in docs/wiki, not README), duplicate `## Kamper UI — Android debug overlay` section. Acknowledgements stays (footer position).
- **D-13:** Integrations section keeps current depth — dependency coordinate, DSL options table, platform support note per integration. All three integrations documented: Sentry, Firebase Crashlytics, OpenTelemetry. Do not shorten to snippet-only; the current detail level is the right call.
- **D-14:** Screenshots and GIFs are preserved in the rewrite. Kamper UI section retains its visual demo content.
- **D-15:** Phase 21 bundles the README full renovation with the `:kamper:` → `:libs:` rename sweep. This avoids two README PRs and ensures the rewritten README has correct final paths from day one.

### Claude's Discretion
- Whether to add `CODE_OF_CONDUCT.md` (Contributor Covenant) — standard OSS practice; include if it fits naturally.
- License attribution line in README footer.
- CI badge in README — check if GitHub Actions are wired to produce a status badge; include if available.
- Exact prose and section ordering within CONTRIBUTING.md, subject to D-01 content requirements.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Existing public-facing files to replace or audit
- `CONTRIBUTING.md` — current file (outdated git-flow + emoji conventions); full replacement *(executed Phase 20)*
- `README.md` — current file (554 lines, surgical fixes applied); full structural rewrite in Phase 21 per D-10–D-15
- `.github/PULL_REQUEST_TEMPLATE.md` — current file; full replacement per D-04 *(executed Phase 20)*
- `SECURITY.md` — deleted per D-05 *(executed Phase 20)*

### Convention sources (inform CONTRIBUTING.md content)
- `CLAUDE.md` — authoritative Conventional Commits format, build commands, module patterns, PR checklist; CONTRIBUTING.md must agree with all rules here
- `.planning/codebase/CONVENTIONS.md` — naming patterns, Detekt rules, code style
- `.planning/codebase/TESTING.md` — what runs on JVM vs device (informs device-requirement note)

### Release automation (inform README versioning section)
- `.github/workflows/` — check which workflows exist and what triggers releases
- `CHANGELOG.md` (if exists) — link from README

### Module inventory (inform README modules table)
- `kamper/modules/` — all 8 module directories: cpu, fps, memory, network, jank, gc, thermal, issues
- `kamper/integrations/` — three integration modules: firebase, opentelemetry, sentry

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `.github/PULL_REQUEST_TEMPLATE.md` — exists but outdated; use as reference for what NOT to replicate
- `CLAUDE.md` — source of truth for commit types, scopes, PR checklist items to mirror in CONTRIBUTING.md

### Established Patterns
- **Conventional Commits:** `feat/fix/chore/docs/test/refactor` + scope (`cpu`, `fps`, `engine`, `api`, etc.), imperative mood, no emojis, no `resolves #N`. CONTRIBUTING.md must match exactly.
- **`main` branch:** No `develop`. Squash merge is the PR merge strategy.
- **JDK 17 required:** Must be stated in CONTRIBUTING.md setup section.
- **Device guard:** `connectedAndroidTest` requires a connected device — CONTRIBUTING.md must warn contributors who want to run instrumented tests.

### Integration Points
- README quick-start block must use correct Maven Central group/artifact IDs (`com.smellouk.kamper:engine`, `com.smellouk.kamper:cpu-module`, etc.) — verify against published artifacts or `build.gradle.kts` publishing config.
- README integrations section covers: `kamper/integrations/firebase`, `kamper/integrations/opentelemetry`, `kamper/integrations/sentry` — all three must be documented in Phase 21 rewrite.

</code_context>

<specifics>
## Specific Ideas

- README structure is explicit per D-11: badges → tagline → modules table → quickstart → Kamper UI → integrations → platform matrix → versioning → links → acknowledgements → license.
- CONTRIBUTING.md is a standalone doc — a contributor should be able to follow it without opening CLAUDE.md.
- SECURITY.md is deleted, not archived. *(Executed.)*
- Issue templates use GitHub Forms (`.yml`) for structured input. *(Executed.)*
- README integrations section: keep current depth (dependency, DSL table, platform support per integration). Do not shorten.
- Kamper UI section must keep screenshots/GIFs — they are the visual flagship of the project.

</specifics>

<deferred>
## Deferred Ideas

- **README full structural renovation** — deferred from Phase 20 execution to Phase 21. Decisions D-10 through D-15 fully specify the rewrite. Phase 21 bundles it with the monorepo rename so the rewritten README has correct paths from day one.
- **Monorepo structure cleanup** (`kamper/` folder rename to `libs/`) — structural refactor affecting all module paths, build scripts, and published artifact coordinates. Phase 21.
- **Per-module sub-CLAUDE.md files** — deferred from Phase 19 discussion; root CLAUDE.md is sufficient for v1.0.

</deferred>

---

*Phase: 20-repo-clean-up-for-open-source-projects-contribution-guidelin*
*Context gathered: 2026-04-29*
*Context updated: 2026-04-29 — addendum: README full renovation decisions deferred to Phase 21*
