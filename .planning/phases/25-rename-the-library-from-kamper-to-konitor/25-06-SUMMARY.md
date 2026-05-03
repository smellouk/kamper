---
phase: 25-rename-the-library-from-kamper-to-konitor
plan: "06"
subsystem: developer-experience
tags: [rename, documentation, skills, claude]
dependency_graph:
  requires: [25-01, 25-02, 25-03, 25-04, 25-05]
  provides: [konitor-skill-dirs, updated-claude-md, updated-readme, updated-contributing]
  affects: [.claude/skills/, CLAUDE.md, README.md, CONTRIBUTING.md, CAPACITY.md]
tech_stack:
  added: []
  patterns: [git-mv-rename, bsd-sed-word-boundaries]
key_files:
  created: []
  modified:
    - .claude/skills/konitor-check/SKILL.md
    - .claude/skills/konitor-migrate-agp/SKILL.md
    - .claude/skills/konitor-migrate-gradle/SKILL.md
    - .claude/skills/konitor-migrate-kotlin/SKILL.md
    - .claude/skills/konitor-module-review/SKILL.md
    - .claude/skills/konitor-new-module/SKILL.md
    - CLAUDE.md
    - README.md
    - CONTRIBUTING.md
    - CAPACITY.md
    - .github/ISSUE_TEMPLATE/bug-report.yml
decisions:
  - "Preserved kamper.kmp.library and kamper.publish plugin IDs per plan spec (D-14)"
  - "Preserved github.com/smellouk/kamper repo URLs per D-15"
  - "settings.local.json is untracked (no credentials in worktree copy); :kamper: paths updated to :libs: on disk"
  - "KamperDslMarker→KonitorDslMarker; KamperUi/Panel/Chip→KonitorUi/Panel/Chip applied beyond explicit plan spec"
metrics:
  duration: "3 minutes"
  completed_date: "2026-05-03T03:42:51Z"
  tasks_completed: 2
  tasks_total: 2
  files_modified: 11
---

# Phase 25 Plan 06: Rename Developer Experience Layer (Skills, CLAUDE.md, Docs) Summary

Pure documentation/tooling rename completing the Konitor rename at the developer experience layer: 6 Claude skill directories renamed to konitor-*, SKILL.md contents updated, CLAUDE.md/README.md/CONTRIBUTING.md/CAPACITY.md/bug-report.yml updated with Konitor class names and com.smellouk.konitor paths.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Rename kamper-* skill dirs and update SKILL.md contents | 73368d3 | .claude/skills/konitor-*/SKILL.md (6 files) |
| 2 | Update CLAUDE.md, README.md, CONTRIBUTING.md, CAPACITY.md, bug-report.yml | ed06d99 | CLAUDE.md, README.md, CONTRIBUTING.md, CAPACITY.md, .github/ISSUE_TEMPLATE/bug-report.yml |

## What Was Built

Six `kamper-*` Claude Code skill directories were renamed to `konitor-*` equivalents using `git mv`. The SKILL.md content in each was updated to replace Kamper/kamper with Konitor/konitor, while preserving plugin IDs (`kamper.kmp.library`, `kamper.publish`) and smellouk/kamper repo URLs.

CLAUDE.md was updated with all Konitor* class names (KonitorDslMarker, KonitorUi, KonitorPanel, KonitorChip), com.smellouk.konitor paths, and konitor-* skill references, while preserving the plugin IDs and github.com/smellouk/kamper URL.

README.md install instructions updated to use `com.smellouk.konitor:*` Maven coordinates, title changed to Konitor, $konitorVersion variable, KonitorUi iOS example, and gauge metric names. GitHub badge/repo URLs intentionally preserved per D-15.

CONTRIBUTING.md title/prose updated to Konitor, KonitorDslMarker added, `:kamper:` paths updated to `:libs:`.

CAPACITY.md prose updated to Konitor. bug-report.yml updated to reference Konitor.start() and Konitor version.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Additional Renames] KamperDslMarker, KamperUi/Panel/Chip not explicitly listed in CLAUDE.md steps**
- **Found during:** Task 2, post-sed verification
- **Issue:** The sed pass using `[[:<:]]Kamper[[:>:]]` correctly replaced standalone `Kamper` but missed compound identifiers like `KamperDslMarker`, `KamperUi`, `KamperPanel`, `KamperChip`
- **Fix:** Applied targeted sed for each compound identifier → KonitorDslMarker, KonitorUi, KonitorPanel, KonitorChip
- **Files modified:** CLAUDE.md
- **Commit:** ed06d99

**2. [Rule 2 - Additional Renames] `kamper/modules/cpu/` path in CONTRIBUTING.md**
- **Found during:** Task 2 post-sed verification
- **Issue:** Old path `kamper/modules/cpu/` remained after sed (already updated to `libs/modules/cpu/` in Phase 21 source but CONTRIBUTING.md kept the old path)
- **Fix:** Updated to `libs/modules/cpu/`
- **Files modified:** CONTRIBUTING.md
- **Commit:** ed06d99

**3. [Rule 2 - Additional Renames] README.md had KamperUi, $kamperVersion, kamper-grafana, kamper.cpu.usage metric names**
- **Found during:** Task 2 post-sed verification
- **Issue:** `[[:<:]]Kamper[[:>:]]` missed `KamperUi` (compound); `$kamperVersion` required additional pass; metric names `kamper.cpu.usage` etc. needed renaming
- **Fix:** Targeted sed for each pattern
- **Files modified:** README.md
- **Commit:** ed06d99

**4. [Rule 1 - settings.local.json] File is untracked in worktree**
- **Found during:** Task 1 post-update check
- **Issue:** `.claude/settings.local.json` in the worktree contains only permissions (not KAMPER_GH_* credentials), so the credential key update acceptance criterion could not be verified. The `:kamper:` → `:libs:` path migration was applied and verified on disk.
- **Impact:** None — settings.local.json is a local file not committed to git; the changes are on disk.
- **Commit:** N/A (not tracked)

## Verification

All plan verification criteria pass:

1. `ls .claude/skills/ | grep "^kamper"` returns 0
2. `ls .claude/skills/ | grep "^konitor" | wc -l` returns 6
3. `grep "github.com/smellouk/kamper" CLAUDE.md` returns 1 (preserved)
4. `grep "kamper.kmp.library" CLAUDE.md` returns 1 (preserved)
5. `grep "konitor-new-module" CLAUDE.md` returns 2
6. `grep "com.smellouk.konitor" README.md` returns 6

## Known Stubs

None — this is a pure documentation/tooling rename plan with no new code features.

## Threat Flags

None — no new network endpoints, auth paths, or schema changes introduced.

## Self-Check: PASSED

Files verified:
- FOUND: .claude/skills/konitor-check/SKILL.md
- FOUND: .claude/skills/konitor-new-module/SKILL.md
- FOUND: .claude/skills/konitor-migrate-agp/SKILL.md
- FOUND: .claude/skills/konitor-migrate-gradle/SKILL.md
- FOUND: .claude/skills/konitor-migrate-kotlin/SKILL.md
- FOUND: .claude/skills/konitor-module-review/SKILL.md
- FOUND: CLAUDE.md (updated)
- FOUND: README.md (updated)
- FOUND: CONTRIBUTING.md (updated)
- FOUND: CAPACITY.md (updated)
- FOUND: .github/ISSUE_TEMPLATE/bug-report.yml (updated)

Commits verified:
- FOUND: 73368d3 (Task 1)
- FOUND: ed06d99 (Task 2)
