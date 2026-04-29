---
phase: 21
plan: "03"
subsystem: docs
tags: [monorepo, refactor, docs, claude-skills, allowlist]
dependency_graph:
  requires: [21-01, 21-02]
  provides: [libs-rename-docs-sweep, allowlist-coherence, skills-updated]
  affects: [CLAUDE.md, .claude/settings.json, .claude/skills, .planning/codebase/STRUCTURE.md, CAPACITY.md]
tech_stack:
  added: []
  patterns: [documentation-sweep, allowlist-coherence, d09-stale-cleanup]
key_files:
  created: []
  modified:
    - CLAUDE.md
    - .claude/settings.json
    - .claude/skills/kamper-check/SKILL.md
    - .claude/skills/kamper-new-module/SKILL.md
    - .claude/skills/kamper-module-review/SKILL.md
    - CAPACITY.md
    - .planning/codebase/STRUCTURE.md
decisions:
  - "Leave kamper/ as repository root label in STRUCTURE.md directory tree (the repo is named kamper)"
  - "ROADMAP.md kamper/ references are historical phase narrative text, not functional paths"
  - "Kotlin package paths (com/smellouk/kamper/) are not changed — package namespace is frozen per ADR-004"
  - "settings.local.json updated on disk but not committed (gitignored session file)"
  - "Gitignored planning files (ARCHITECTURE.md, CONVENTIONS.md, ADRs, etc.) updated in main repo working directory"
metrics:
  duration: "10 minutes"
  completed: "2026-04-29"
  tasks_completed: 2
  tasks_total: 2
  files_modified: 7
---

# Phase 21 Plan 03: Documentation and Skills Sweep for libs/ Rename — Summary

Documentation, Claude Code allowlist, and all 3 project skills updated to reference `libs/` paths and `:libs:` Gradle project paths. D-09 stale-file cleanup completed. Allowlist coherence verified.

## What Was Done

### Task 1: CLAUDE.md, settings.json, settings.local.json, 3 SKILL.md files

**CLAUDE.md** (~15 occurrences updated):
- Build and Test table: all `:kamper:modules:<name>:jvmTest/test` commands → `:libs:modules:<name>:jvmTest/test`
- Quick Start smoke test: `:kamper:api:test :kamper:engine:test` → `:libs:api:test :libs:engine:test`
- PR Checklist: `:kamper:modules:<name>:jvmTest` → `:libs:modules:<name>:jvmTest`
- Module Patterns / Canonical Reference section: `kamper/modules/cpu/` → `libs/modules/cpu/`
- Build template: `project(":kamper:api")` → `project(":libs:api")`
- Settings registration template: `include(":kamper:modules:{name}")` → `include(":libs:modules:{name}")`
- 4-Layer Model architecture diagram: `kamper/api/`, `kamper/engine/`, `kamper/modules/`, `kamper/ui/android/` → `libs/*`
- Key Files table: `kamper/api/src/...`, `kamper/engine/src/...`, `kamper/modules/cpu/` → `libs/*`
- Added module path prefix note clarifying `:libs:` prefix
- Preserved: plugin IDs (`kamper.kmp.library`, `kamper.publish`), Maven coordinates, GitHub URLs, skill folder names

**.claude/settings.json** (2 substitutions in allow array):
- `Bash(./gradlew :kamper:api:test:*)` → `Bash(./gradlew :libs:api:test:*)`
- `Bash(./gradlew :kamper:engine:test:*)` → `Bash(./gradlew :libs:engine:test:*)`

**.claude/settings.local.json** (35 entries swept):
- All 35 `:kamper:` occurrences replaced with `:libs:` (session-local file, updated on disk, not committed)
- Entries for `:samples:*` and absolute paths were unaffected

**kamper-check/SKILL.md** (~8 occurrences updated):
- Command table: all `./gradlew :kamper:*` → `./gradlew :libs:*`
- Settings allowlist references updated
- The skill name `kamper-check` itself preserved

**kamper-new-module/SKILL.md** (~10 occurrences updated):
- Directory tree: `kamper/modules/{name}/` → `libs/modules/{name}/`
- settings.gradle.kts registration: `include(":kamper:modules:{name}")` → `include(":libs:modules:{name}")`
- Verification command: `:kamper:modules:{name}:` → `:libs:modules:{name}:`
- Dependency reference: `project(":kamper:api")` → `project(":libs:api")`
- Canonical reference: `kamper/modules/cpu/` → `libs/modules/cpu/`
- Skill name `kamper-new-module` preserved

**kamper-module-review/SKILL.md** (~8 occurrences updated):
- Module directory validation path: `kamper/modules/<name>/` → `libs/modules/<name>/`
- Expect/actual path listing: all `kamper/modules/<name>/src/` → `libs/modules/<name>/src/`
- Gradle command: `:kamper:modules:<name>:jvmTest` → `:libs:modules:<name>:jvmTest`
- Reference checklist: `kamper/modules/` → `libs/modules/`
- `include(":kamper:modules:{name}")` → `include(":libs:modules:{name}")`
- Source files section: `kamper/modules/cpu/` → `libs/modules/cpu/`
- Skill name `kamper-module-review` preserved

**Commit:** `b968247` — `docs(21): sweep CLAUDE.md, settings.json, and 3 SKILL.md files for libs/ rename`

---

### Task 2: CAPACITY.md, STRUCTURE.md, and gitignored planning files

**CAPACITY.md** (1 occurrence):
- `kamper/modules/cpu/src/commonMain/kotlin/...` → `libs/modules/cpu/src/commonMain/kotlin/...`

**.planning/codebase/STRUCTURE.md** (D-09 cleanup + path sweep):
- Directory tree: `├── kamper/` library subdirectory → `├── libs/`
- Source set layouts: all `kamper/modules/<name>/src/`, `kamper/engine/src/`, `kamper/api/src/`, `kamper/ui/android/src/` → `libs/*`
- Directory Purposes section: `kamper/api/`, `kamper/engine/`, `kamper/modules/`, `kamper/ui/android/`, `kamper/xcframework/` → `libs/*`
- Testing paths: `kamper/modules/{name}/src/commonTest/`, `kamper/api/src/commonTest/`, etc. → `libs/*`
- Where to Add New Code: `kamper/modules/xxx/`, `:kamper:modules:xxx` → `libs/*`, `:libs:modules:xxx`
- **D-09 fix:** `kamper/publish.gradle.kts` (non-existent stale reference) replaced with `build-logic/src/main/kotlin/KamperPublishPlugin.kt` with updated description noting it is a Kotlin convention plugin applied via `id("kamper.publish")`

**Gitignored planning files updated (on disk, not committed):**
All 7 `.planning/codebase/` files (ARCHITECTURE.md, CONVENTIONS.md, CONCERNS.md, INTEGRATIONS.md, PRINCIPLES.md, STACK.md, TESTING.md) + all 4 ADR files + v1.0-REQUIREMENTS.md + v1.0-ROADMAP.md updated with same substitution rule. Kotlin package paths (`com/smellouk/kamper/`) were explicitly preserved (not changed — package namespace is frozen).

**README.md:** No `kamper/` filesystem path references found (already clean post Wave 1/2 or never present).

**ROADMAP.md:** No `kamper/modules/` references. Remaining `kamper/` occurrences are historical phase narrative text accurately describing the Phase 21 rename task — intentionally preserved.

**Commit:** `6481f9e` — `docs(21): sweep docs and planning files for libs/ rename, fix D-09 stale reference`

---

## Verification Results

### 1. Allowlist coherence

```
grep 'Bash(./gradlew :libs:api:test:\*)' .claude/settings.json  → 1 match PASS
grep 'Bash(./gradlew :libs:engine:test:\*)' .claude/settings.json → 1 match PASS
grep ':kamper:' .claude/settings.json                            → 0 matches PASS
```

### 2. CLAUDE.md and skills swept clean

```
grep ':kamper:\|kamper/' CLAUDE.md .claude/skills/*/SKILL.md | [whitelist filter] → 0 matches PASS
```

### 3. STRUCTURE.md D-09 cleanup

```
grep 'publish.gradle.kts' .planning/codebase/STRUCTURE.md → 0 matches PASS
grep 'KamperPublishPlugin' .planning/codebase/STRUCTURE.md → 3 matches PASS
```

### 4. Sample Gradle command runs without prompt

The allowlist entries `Bash(./gradlew :libs:api:test:*)` and `Bash(./gradlew :libs:engine:test:*)` are now in `.claude/settings.json`. The renamed commands `:libs:api:test` and `:libs:engine:test` will not trigger permission prompts in future Claude sessions.

---

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] sed command incorrectly modified Kotlin package paths**
- **Found during:** Task 2 execution
- **Issue:** The initial `sed` substitution for filesystem paths matched inside Kotlin package paths (e.g., `com/smellouk/kamper/api/InfoRepository.kt` was changed to `com/smellouk/libs/api/InfoRepository.kt`)
- **Fix:** Applied a correction pass using Python to restore all `com/smellouk/libs/` occurrences back to `com/smellouk/kamper/` across all affected planning files (PRINCIPLES.md, ARCHITECTURE.md, CONVENTIONS.md, TESTING.md, CONCERNS.md, adr-003-listener-pattern.md)
- **Files modified:** 6 gitignored planning files (corrected on disk)
- **Commit:** Not separately committed (corrections applied inline during Task 2)

---

## Known Stubs

None. All documentation references are complete and accurate for the libs/ rename.

---

## Threat Flags

None. No new security-relevant surface was introduced. All changes are documentation and allowlist updates.

---

## Self-Check: PASSED

- CLAUDE.md clean of unwhitelisted kamper/ paths: PASS
- settings.json has :libs:api:test and :libs:engine:test entries: PASS
- STRUCTURE.md has no publish.gradle.kts: PASS
- STRUCTURE.md has KamperPublishPlugin: PASS (3 occurrences)
- Task 1 commit exists (b968247): PASS
- Task 2 commit exists (6481f9e): PASS
