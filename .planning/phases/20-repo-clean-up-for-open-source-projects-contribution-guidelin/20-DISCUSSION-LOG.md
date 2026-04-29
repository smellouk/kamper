# Phase 20: Open Source Cleanup - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-29
**Phase:** 20-repo-clean-up-for-open-source-projects-contribution-guidelin
**Areas discussed:** CONTRIBUTING.md depth, CLAUDE.md relationship, Issue templates, Internal reference audit, README refresh, Release / versioning docs

---

## CONTRIBUTING.md Depth

| Option | Description | Selected |
|--------|-------------|----------|
| Full contributor guide | Setup, build, module patterns, commit format, PR process — standalone for external contributors | ✓ |
| Conventions-only | Just corrected git conventions, defer to CLAUDE.md for everything else | |
| CLAUDE.md wrapper | Thin doc linking to CLAUDE.md | |

**User's choice:** Full contributor guide
**Notes:** No additional notes.

---

## CLAUDE.md Relationship

| Option | Description | Selected |
|--------|-------------|----------|
| CONTRIBUTING.md authoritative for humans, CLAUDE.md for Claude | Independent docs, same rules, no cross-reference | ✓ |
| CONTRIBUTING.md defers to CLAUDE.md | DRY but CONTRIBUTING.md reads as incomplete | |
| CLAUDE.md references CONTRIBUTING.md | Each self-contained with a pointer | |

**User's choice:** CONTRIBUTING.md is authoritative for humans, CLAUDE.md for Claude (independent docs).
**Notes:** No cross-reference required between them.

---

## Issue Templates

| Option | Description | Selected |
|--------|-------------|----------|
| Bug report + Feature request | Standard two-template setup | ✓ |
| Bug report only | Library users mainly report bugs | |
| Bug + Feature + Question | Third template for support questions | |

**User's choice:** Bug report + Feature request

---

## PR Template

| Option | Description | Selected |
|--------|-------------|----------|
| Replace it | New template with detekt/jvmTest checklist | ✓ |
| Keep as-is | Leave existing template | |
| Review first | Update only outdated parts | |

**User's choice:** Replace entirely.

---

## Internal Reference Audit

| Option | Description | Selected |
|--------|-------------|----------|
| Remove stale path references, keep threat table | Strip file:line refs from SECURITY.md | |
| Leave SECURITY.md as-is | Don't touch it | |
| Full SECURITY.md audit | Verify every path, fix or remove stale refs | |

**User's choice:** "remove the file it's not needed" — delete SECURITY.md entirely.

---

## Audit Scope

| Option | Description | Selected |
|--------|-------------|----------|
| README.md + CONTRIBUTING.md only | Targeted sweep | |
| All public files | README, workflows, all .github/ files | ✓ |
| No audit needed | Skip sweep | |

**User's choice:** All public files + improve README comprehensively.

---

## README Structure

| Option | Description | Selected |
|--------|-------------|----------|
| Badge strip + tagline + modules table + quickstart + integrations + platform matrix + links | Standard OSS library structure | ✓ |
| Keep current structure, expand content | Lower disruption | |
| Claude decides | Based on similar KMP libraries | |

**User's choice:** Full refresh with explicit structure (badge strip → tagline → modules table → quickstart → integrations → platform matrix → versioning → links).

---

## Release / Versioning Docs

| Option | Description | Selected |
|--------|-------------|----------|
| Maven Central coordinates + changelog link | Minimal release info | |
| Full release process docs | Semver explanation, how to pick version, how to watch for releases | ✓ |
| Nothing | GitHub Releases page is enough | |

**User's choice:** Full release process docs in README versioning section.

---

## Claude's Discretion

- Whether to add CODE_OF_CONDUCT.md (Contributor Covenant)
- License attribution in README footer
- CI badge presence in README
- Exact prose and section ordering within CONTRIBUTING.md

## Deferred Ideas

- Monorepo structure cleanup (`kamper/` → `libs/` rename) — structural refactor, own phase
- Per-module sub-CLAUDE.md files — deferred from Phase 19

---

## Addendum Discussion — 2026-04-29

> Post-execution addendum: Phase 20's plan 03 applied surgical README edits rather than the full
> structural renovation specified in D-07. This session captures the full renovation decisions
> for delegation to Phase 21.

**Areas discussed:** Renovation scope, Where it belongs, Integration representation, Section ordering

---

## Renovation Scope (addendum)

| Option | Description | Selected |
|--------|-------------|----------|
| Full structural rewrite | Rebuild README from scratch following D-07 structure. Existing content reorganized, not discarded. | ✓ |
| Targeted improvements only | Keep current structure, fix specific weak sections. | |
| You decide | Claude judges what the README needs. | |

**Done bar (all four selected):**
- Matches D-07 section order exactly ✓
- Integrations get a real section ✓
- Screenshots / GIFs preserved ✓
- All stale content removed ✓

---

## Where It Belongs (addendum)

| Option | Description | Selected |
|--------|-------------|----------|
| Phase 21 | Already touches README for :kamper: → :libs: rename. Bundles rewrite, avoids two README PRs. | ✓ |
| New dedicated phase | Phase 22 specifically for README renovation. | |

**User's choice:** Phase 21

---

## Integration Representation (addendum)

| Option | Description | Selected |
|--------|-------------|----------|
| Current depth preserved | Keep dependency, DSL table, platform support per integration. Already high quality. | ✓ |
| Compact — one snippet each | Shorten to brief setup snippet + link to docs. | |
| Promote to top section | More prominent placement near top. | |

**OTel status:** exists — include all three integrations (Sentry, Firebase, OpenTelemetry).

---

## Section Ordering (addendum)

**Sections not in D-07:**

| Option | Description | Selected |
|--------|-------------|----------|
| Kamper UI stays, others cut | Keep screenshots. Cut Demos/How-tos. Condense Lifecycle into Quick start. | ✓ |
| Keep all sections, reorder | Preserve everything, reorder around D-07. | |
| You decide | Claude judges which sections earn their place. | |

**Kamper UI placement:**

| Option | Description | Selected |
|--------|-------------|----------|
| After Quick start | badges → tagline → modules → quick start → Kamper UI → integrations... | ✓ |
| Second — right after badges | Lead with visual. Risky: hides install. | |
| Near the bottom | Detail section. | |

**Final locked section order:**
1. Badge strip → 2. Tagline → 3. Modules table → 4. Quick start (Lifecycle condensed) →
5. Kamper UI (screenshots) → 6. Integrations (Sentry, Firebase, OTel — full depth) →
7. Platform matrix → 8. Versioning → 9. Contributing + Links → 10. Acknowledgements → 11. License
