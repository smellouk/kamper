# Phase 21: Monorepo Structure & Clean Up - Context

**Gathered:** 2026-04-29
**Updated:** 2026-04-29
**Status:** Ready for planning

<domain>
## Phase Boundary

Reorganize the physical monorepo layout by renaming `kamper/` to `libs/`, updating all Gradle project paths to match (`:kamper:*` → `:libs:*`), sweeping every reference to the old paths across build scripts and docs, cleaning up `build-logic/` convention plugins, updating release automation config and the Podspec, and removing any stale/deprecated files discovered during the sweep. Additionally, perform a full structural rewrite of `README.md` (deferred from Phase 20) so the renovated README ships with correct `libs/` paths from day one. Published artifact coordinates (`com.smellouk.kamper:*`) are unchanged — this is a structural refactor, not an API or publishing change.

</domain>

<decisions>
## Implementation Decisions

### Rename Target
- **D-01:** `kamper/` top-level folder is renamed to `libs/`.
- **D-02:** Gradle project paths follow the filesystem. `:kamper:api` → `:libs:api`, `:kamper:engine` → `:libs:engine`, `:kamper:modules:cpu` → `:libs:modules:cpu`, `:kamper:integrations:sentry` → `:libs:integrations:sentry`, `:kamper:ui:kmm` → `:libs:ui:kmm`, etc. `settings.gradle.kts` `include()` calls and all `project(":kamper:*")` references in build scripts are updated to match.

### Rename Scope
- **D-03:** Full reference sweep — every occurrence of `:kamper:*` or `kamper/` paths in the following files is updated: `settings.gradle.kts`, all `build.gradle.kts` files, `CLAUDE.md`, `README.md`, `Kamper.podspec`, `.planning/` docs that name module paths.
- **D-04:** `build-logic/` cleanup is in scope — review convention plugin code for any hardcoded `:kamper:*` project path references and update them to `:libs:*`.
- **D-05:** `demos/` stays as-is — no rename.

### Artifact Coordinates
- **D-06:** Published Maven Central coordinates stay as `com.smellouk.kamper:*` (engine, cpu-module, fps-module, etc.). The rename is filesystem + Gradle project paths only. Artifact IDs are defined explicitly in publishing configuration, not derived from the Gradle project path. Zero breaking change for library consumers. This is consistent with ADR-004 (API freeze for v1.0).

### Additional Cleanup
- **D-07:** `.release-please-manifest.json` and `release-please-config.json` (if present) are updated to reflect `libs/*` component paths.
- **D-08:** `Kamper.podspec` source path references are updated from `kamper/` to `libs/`.
- **D-09:** Identify and remove deprecated or stale files found during the rename sweep — old TODO stubs, leftover scaffolding, empty stubs, etc. Confirmed stale-file fix: `.planning/codebase/STRUCTURE.md` references `kamper/publish.gradle.kts` (non-existent); replace with `build-logic/src/main/kotlin/KamperPublishPlugin.kt`.

### OSS Structure Validation
- **D-10 (research input):** Research top-level layout conventions for published KMP library monorepos (e.g., kotlinx, ktor, koin) to validate the `libs/` rename choice and confirm the overall root-level layout. Research input only — no additional structural changes are added to the phase based on this research unless they are trivially in scope of the rename.

### README Full Renovation (deferred from Phase 20)
- **D-11:** Full structural rewrite of `README.md` in Phase 21. Not surgical — rebuild from scratch following the section order in D-12. Existing content is reorganized, not discarded.
- **D-12:** Final section order:
  1. Badge strip (license, release, issues, stars; CI badge if a suitable GitHub Actions workflow exists)
  2. Project tagline + one-paragraph description
  3. Modules table — 3 columns: Name, Description, Platforms (all 8 modules)
  4. Quick start: Maven Central coordinates, engine install, one module install (`CpuModule`), one listener example — 3 code blocks (~15 lines total); Lifecycle condensed here
  5. Kamper UI (with screenshots/GIFs — flagship feature)
  6. Service integrations (Sentry, Firebase Crashlytics, OpenTelemetry — full depth per D-14)
  7. Platform matrix
  8. Versioning
  9. Contributing + Links: CONTRIBUTING.md, CLAUDE.md, GitHub Releases
  10. Acknowledgements
  11. License
- **D-13:** Sections to remove entirely: `## Demos`, `## How-tos` (belong in docs/wiki, not README), duplicate `## Kamper UI — Android debug overlay` section. Acknowledgements stays (footer position).
- **D-14:** Integrations section keeps current depth — dependency coordinate, DSL options table, platform support note per integration. All three integrations documented: Sentry, Firebase Crashlytics, OpenTelemetry. Do not shorten to snippet-only; the current detail level is the right call.
- **D-15:** Screenshots and GIFs are preserved in the rewrite. Kamper UI section retains its visual demo content.
- **D-16:** Phase 21 bundles the README full renovation with the `:kamper:` → `:libs:` rename sweep so the rewritten README has correct final paths from day one (D-16 = Phase 20's D-15).

### Plan Structure
- **D-17:** README renovation is a separate Wave 4 plan (21-04). Plan 21-03 keeps its scope (23 files, path sweep + doc update only — README.md treatment in 21-03 is path-only, not a rewrite). Plan 21-04 depends on 21-03. Standalone commit: `docs(21): full README renovation`.

### Claude's Discretion
- Any structural loose ends discovered during the rename sweep (e.g., stray `kamper/` references in CI workflow files, `.idea/` run configs, or Gradle wrapper properties) — clean up inline rather than flagging each one.
- Whether to add a `## Monorepo Structure` section to CLAUDE.md documenting the new layout after the rename completes (CLAUDE.md Task 1 in 21-03 already has this as a discretionary item).
- Exact order of operations for the rename (git mv vs manual file edits) — choose whatever produces the cleanest git history with no broken intermediate states.
- CI badge: include in README badge strip if a suitable GitHub Actions build/test workflow exists and produces a status badge URL.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Build System (primary rename targets)
- `settings.gradle.kts` — all `include(":kamper:*")` calls; this file defines the Gradle project graph
- `build.gradle.kts` (root) — may reference `:kamper:*` project paths
- `kamper/` (entire directory tree) — source of truth for what gets renamed; enumerate all subdirectories before renaming
- `build-logic/src/main/kotlin/` — convention plugins; check for hardcoded `:kamper:*` references

### Publishing & Release Automation
- `.release-please-manifest.json` — component paths for release-please
- `gradle.properties` — check for any `kamper.`-prefixed properties that reference paths
- `.github/workflows/` — CI workflow files; check for hardcoded `kamper/` directory references; also check for CI status badge URL (needed for D-12 badge strip)
- `Kamper.podspec` — iOS/macOS CocoaPods spec; references source paths inside the library dir

### Documentation (reference sweep targets)
- `CLAUDE.md` — extensive `:kamper:*` and `kamper/` references in Build & Test table, Architecture & Key Files section, Module Patterns section
- `README.md` — full structural rewrite per D-11–D-16; also path sweep for `libs/` after Wave 1
- `.planning/codebase/STRUCTURE.md` — documents build layout; will need updating post-rename; also stale `kamper/publish.gradle.kts` reference (D-09 fix)
- `.planning/ROADMAP.md` — phase descriptions reference `kamper/modules/` paths

### README Renovation Source Material (for 21-04)
- `README.md` (current, 554 lines) — existing content to reorganize; do not discard; screenshots/GIFs must be preserved
- `kamper/integrations/` (pre-rename) / `libs/integrations/` (post-rename) — three integration dirs: firebase, opentelemetry, sentry; all three documented in integrations section at full depth
- `kamper/modules/` (pre-rename) / `libs/modules/` (post-rename) — 8 module directories: cpu, fps, memory, network, jank, gc, thermal, issues; all 8 in modules table
- `.github/workflows/` — check for CI badge URL for badge strip (D-12 step 1)
- `CONTRIBUTING.md` — link target for Contributing section (D-12 step 9)

### API Stability (constraint)
- `.planning/codebase/adr/ADR-004.md` (or equivalent) — API freeze for v1.0; confirms artifact ID changes are out of scope

### Canonical Reference Module (do NOT move its role)
- `libs/modules/cpu/` (post-rename) — remains the canonical reference module per CLAUDE.md; all documentation references to it should be updated to the new path

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `settings.gradle.kts` — already uses clean `include(":kamper:...")` syntax; updating is a mechanical find-and-replace of the prefix
- `build-logic/` convention plugins — apply to modules via plugin IDs (e.g., `id("kamper.kmp.library")`); plugin IDs themselves are user-facing strings and should NOT be renamed (would break any external consumers applying them)

### Established Patterns
- **Gradle project path convention:** All library modules follow `:kamper:<layer>` or `:kamper:modules:<name>` or `:kamper:integrations:<name>` — a consistent two-to-three level hierarchy that maps directly to the filesystem. The rename preserves this hierarchy under `:libs:`.
- **`project(":kamper:*")` dependencies:** Used in `commonMain` and platform source set dependency blocks. These are the most numerous reference sites after `settings.gradle.kts`.
- **Artifact ID independence:** Publishing config in each module's `build.gradle.kts` declares `artifactId` explicitly — it is not derived from the Gradle project name. This is the key fact that makes the filesystem rename safe.

### Integration Points
- `demos/android/build.gradle.kts`, `demos/jvm/build.gradle.kts`, etc. — demo apps depend on `:kamper:engine` and specific modules via `project()` references; these all need updating.
- `demos/react-native/android/` — composite-included RN demo; check for any `:kamper:*` references in its settings or build files.
- `kamper/xcframework/` — XCFramework assembly module; may have platform-specific path logic referencing the parent directory name.

</code_context>

<specifics>
## Specific Ideas

- The rename is `kamper/` → `libs/` at the filesystem level, with Gradle project paths following (`:kamper:*` → `:libs:*`) and artifact IDs staying unchanged (`com.smellouk.kamper:*`).
- Plugin IDs in `build-logic/` (e.g., `kamper.kmp.library`, `kamper.publish`, `kamper.android.config`) keep the `kamper.` prefix — these are user-visible plugin IDs and changing them would break external consumers.
- `demos/` is intentionally left as-is — no rename needed.
- README quick start: 3 code blocks — Gradle dep, `Kamper.install(CpuModule)`, `addListener { cpu -> ... }`. Engine install + one module + one listener is the right depth for a first-time visitor.
- README contributing section: three links — `CONTRIBUTING.md`, `CLAUDE.md`, GitHub Releases. CLAUDE.md is intentionally included despite being Claude-agent-focused; it also serves as the contributor reference for commit conventions and build commands.
- README integrations section: keep current depth (dependency coordinate, DSL table, platform support note) for all three integrations. Do not shorten.
- README Kamper UI section: screenshots and GIFs stay; this is the visual flagship.
- 21-04 plan is Wave 4, depends on 21-03, standalone commit `docs(21): full README renovation`.

</specifics>

<deferred>
## Deferred Ideas

- **docs/ directory** — structured top-level docs folder (architecture, API reference, contributing). Not in scope here; could be a follow-on.
- **Module grouping reorganization** — flattening `libs/modules/` and `libs/integrations/` into a single level. Out of scope for this phase.
- **Per-module sub-CLAUDE.md files** — already deferred from Phase 19.

</deferred>

---

*Phase: 21-monorepo-structure-and-clean-up-e-g-renaming-kamper-to-libs-*
*Context gathered: 2026-04-29*
*Context updated: 2026-04-29 — added README renovation decisions (D-11–D-17) deferred from Phase 20; added 21-04 plan structure decision*
