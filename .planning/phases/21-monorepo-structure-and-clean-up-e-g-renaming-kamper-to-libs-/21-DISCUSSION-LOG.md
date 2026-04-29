# Phase 21: Monorepo Structure & Clean Up - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-29
**Updated:** 2026-04-29
**Phase:** 21-monorepo-structure-and-clean-up-e-g-renaming-kamper-to-libs-
**Areas discussed:** Rename target, Rename scope, Artifact coordinates, Additional cleanup, OSS structure recommendations, README renovation, Plan coverage gap

---

## Rename Target

| Option | Description | Selected |
|--------|-------------|----------|
| `libs/` | Standard monorepo convention — signals 'these are library modules'. Matches kotlinx, ktor, and other KMP monorepos. | ✓ |
| `core/` | Foundational/core modules semantic, less common in multi-module library monorepos. | |
| Keep `kamper/` | No rename — just clean up other structural issues. | |

**User's choice:** `libs/`
**Notes:** None — straightforward preference for ecosystem-standard naming.

---

| Option | Description | Selected |
|--------|-------------|----------|
| Yes — paths match filesystem | Full consistency: `libs/api` maps to `:libs:api`. All settings.gradle.kts and build.gradle.kts references need updating. | ✓ |
| No — keep `:kamper:*` project paths | Less churn: only filesystem moves, Gradle identifiers stay as `:kamper:*`. Creates permanent mismatch. | |

**User's choice:** Gradle project paths follow the filesystem.

---

## Rename Scope

| Option | Description | Selected |
|--------|-------------|----------|
| Full reference sweep | Update all `:kamper:*` references everywhere — settings.gradle.kts, build.gradle.kts, CLAUDE.md, README.md, Kamper.podspec, .planning/ docs. | ✓ |
| `demos/` restructure | Rename or reorganize demos/ folder. | |
| `build-logic/` cleanup | Review convention plugins for hardcoded `:kamper:*` references. | ✓ |
| Just folder + settings.gradle.kts | Minimal scope — rename and update settings only. | |

**User's choice:** Full reference sweep + build-logic cleanup.

---

| Option | Description | Selected |
|--------|-------------|----------|
| Keep `demos/` as-is | Clear enough; changing adds churn with no gain. | ✓ |
| Rename to `samples/` | More common in Kotlin/KMP projects. | |
| Rename to `examples/` | Most self-explanatory for OSS newcomers. | |

**User's choice:** `demos/` stays unchanged.

---

## Artifact Coordinates

| Option | Description | Selected |
|--------|-------------|----------|
| No — artifact IDs stay the same | Rename is filesystem + Gradle paths only. `com.smellouk.kamper:*` stays. Zero breaking change. | ✓ |
| Yes — align artifact IDs with new structure | Breaking change for all existing consumers; incompatible with ADR-004 API freeze for v1.0. | |

**User's choice:** Artifact IDs stay as `com.smellouk.kamper:*`.

---

## Additional Cleanup

| Option | Description | Selected |
|--------|-------------|----------|
| Release-please manifest | Update `.release-please-manifest.json` and `release-please-config.json` to `libs/*` paths. | ✓ |
| `Kamper.podspec` paths | Update source path references from `kamper/` to `libs/`. | ✓ |
| Deprecated / stale files | Remove files that became stale after prior phases. | ✓ |
| You decide | Claude handles any structural loose ends during the sweep. | ✓ |

**User's choice:** All four — comprehensive cleanup during the rename sweep.

---

## OSS Structure Recommendations

| Option | Description | Selected |
|--------|-------------|----------|
| Top-level layout conventions | Research how published KMP library monorepos organize their root layout. | ✓ |
| `docs/` directory | Should there be a top-level docs/ folder? | |
| Module grouping strategy | Review libs/modules/ vs libs/integrations/ grouping. | |
| Just research — inform the rename | Use OSS conventions as validation input, don't add structural work. | ✓ |

**User's choice:** Research top-level KMP monorepo conventions to validate the `libs/` rename. Research-only — no new structural work added to the phase.

---

---

## README renovation (update session 2026-04-29)

### CI badge

| Option | Description | Selected |
|--------|-------------|----------|
| Yes — include if workflow exists | Check .github/workflows/ for a build/test workflow with a status badge URL and include it | ✓ |
| No — skip CI badge | Just license, release, issues, stars badges | |
| You decide | Claude checks and decides | |

**User's choice:** Include CI badge if a suitable workflow exists.

---

### Modules table columns

| Option | Description | Selected |
|--------|-------------|----------|
| Name + Description + Platforms | 3 columns: module name, what it measures, supported platforms | ✓ |
| Name + Description only | 2-column table, platform detail in platform matrix section | |
| You decide | Claude picks the layout | |

**User's choice:** 3 columns (Name, Description, Platforms) for all 8 modules.

---

### Quick start depth

| Option | Description | Selected |
|--------|-------------|----------|
| Engine install + one module + one listener | Three code blocks: Gradle dep, Kamper.install(CpuModule), addListener { cpu -> ... } | ✓ |
| Engine install only | Minimal — just Gradle dep and Kamper.install() | |
| You decide | Claude picks the depth | |

**User's choice:** Full quick start flow — engine install, one module, one listener (~15 lines total).

---

### Contributing & Links section

| Option | Description | Selected |
|--------|-------------|----------|
| CONTRIBUTING.md + CLAUDE.md + GitHub Releases | Three links including Claude/contributor reference | ✓ |
| CONTRIBUTING.md + GitHub Releases only | CLAUDE.md excluded as it's Claude-agent-focused | |
| You decide | Claude decides what's appropriate | |

**User's choice:** All three links — CONTRIBUTING.md, CLAUDE.md, GitHub Releases.

---

## Plan coverage gap (update session 2026-04-29)

### How to handle the 21-03 README gap

| Option | Description | Selected |
|--------|-------------|----------|
| Update 21-03 to add README rewrite task | Add Task 3 to 21-03 with full renovation spec | |
| Create new 21-04 plan for README renovation | Keep 21-03 focused on doc sweep; README rewrite in Wave 4 | ✓ |
| You decide | Claude picks the approach | |

**User's choice:** New Wave 4 plan (21-04) for README renovation only.

---

### 21-04 dependency

| Option | Description | Selected |
|--------|-------------|----------|
| Depends on 21-03 | README rewrite happens after full doc sweep so all paths are correct | ✓ |
| Depends on 21-01 only | README rewrite in parallel with CI/release and doc-sweep waves | |

**User's choice:** 21-04 depends on 21-03.

---

### 21-04 commit strategy

| Option | Description | Selected |
|--------|-------------|----------|
| Standalone commit | `docs(21): full README renovation` — standalone, easily reviewable | ✓ |
| Squash into 21-03 commit | Fewer commits; README rewrite and doc sweep land together | |

**User's choice:** Standalone commit `docs(21): full README renovation`.

---

## Claude's Discretion

- Structural loose ends found during the sweep (CI workflows, .idea/ configs, Gradle wrapper properties referencing old paths) — clean up inline.
- Whether to add a `## Monorepo Structure` section to CLAUDE.md after the rename.
- Order of git operations (git mv vs manual edits) for a clean history.
- CI badge: Claude checks `.github/workflows/` for a suitable workflow and includes badge URL if found.

## Deferred Ideas

- **docs/ directory** — top-level structured documentation folder; possible follow-on phase.
- **Module grouping reorganization** — flattening libs/modules/ and libs/integrations/; out of scope here.
- **Per-module sub-CLAUDE.md files** — already deferred from Phase 19.
