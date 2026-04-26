---
phase: 08-security-docs-scaling
plan: "04"
subsystem: security-docs
tags: [security, capacity, kdoc, documentation, gap-closure, roadmap-contract]
requirements: [SEC-01, DOC-02]

dependency_graph:
  requires: []
  provides:
    - SECURITY.md vulnerability disclosure policy
    - CAPACITY.md per-module scaling limits documentation
    - KDoc on all 15 public Config types in commonMain
  affects:
    - ROADMAP Phase 8 SC1 (SECURITY.md)
    - ROADMAP Phase 8 SC2 (KDoc on all public API types)
    - ROADMAP Phase 8 SC3 (CAPACITY.md)

tech_stack:
  added: []
  patterns:
    - KDoc @property annotations on public data class and class types
    - GitHub SECURITY.md auto-detection in Security tab
    - CAPACITY.md as canonical capacity reference document

key_files:
  created:
    - SECURITY.md
    - CAPACITY.md
  modified:
    - kamper/modules/issues/src/commonMain/kotlin/com/smellouk/kamper/issues/IssuesConfig.kt
    - kamper/modules/cpu/src/commonMain/kotlin/com/smellouk/kamper/cpu/CpuConfig.kt
    - kamper/modules/network/src/commonMain/kotlin/com/smellouk/kamper/network/NetworkConfig.kt
    - kamper/modules/fps/src/commonMain/kotlin/com/smellouk/kamper/fps/FpsConfig.kt
    - kamper/modules/jank/src/commonMain/kotlin/com/smellouk/kamper/jank/JankConfig.kt
    - kamper/modules/gc/src/commonMain/kotlin/com/smellouk/kamper/gc/GcConfig.kt
    - kamper/modules/thermal/src/commonMain/kotlin/com/smellouk/kamper/thermal/ThermalConfig.kt
    - kamper/engine/src/commonMain/kotlin/com.smellouk.kamper/KamperConfig.kt

decisions:
  - "SECURITY.md placed at repository root (not .planning/) so GitHub Security tab auto-detects it"
  - "KDoc added only as class-level blocks; no @property on Builder or companion object members per Detekt rules"
  - "CrashConfig.persistToDisk KDoc documents 'reserved for future release, currently has no effect' to be truthful (REVIEW.md WR-01 finding)"
  - "Pre-existing detekt MaxLineLength violations in IssuesConfig.kt Builder.build() methods not fixed — out of scope for KDoc-only task"

metrics:
  duration: "~30 minutes"
  completed: "2026-04-26T13:39:03Z"
  tasks_completed: 2
  files_created: 2
  files_modified: 8
---

# Phase 08 Plan 04: Security Policy, Capacity Limits & Config KDoc — Summary

SECURITY.md and CAPACITY.md created at repository root; 15 public Config types across 8 commonMain files gained class-level KDoc, satisfying the three ROADMAP Phase 8 gap-closure requirements (SEC-01, DOC-02).

## Commits

| Task | Commit | Description |
|------|--------|-------------|
| Task 1: SECURITY.md + CAPACITY.md | `bd985ef` | `docs(08-04): add SECURITY.md and CAPACITY.md at repository root` |
| Task 2: KDoc on 15 Config types | `ecc3936` | `docs(08-04): add class-level KDoc to all 15 public Config types in commonMain` |

## Files Created

| File | Lines | Purpose |
|------|-------|---------|
| `SECURITY.md` | 98 | Vulnerability disclosure policy: contact email, response timeline (5/10 business days), supported versions table, SharedPreferences/FLAG_DEBUGGABLE/CrashDetector security notes |
| `CAPACITY.md` | 86 | Documented scaling limits: IssuesModule (200 issues, FIFO, ~200 KB), RecordingManager (4200 samples, FIFO, ~100 KB), CPU ring buffer (< 50 KB), per-module summary table |

## Files Modified (KDoc additions only)

| File | KDoc blocks added | Types documented |
|------|-------------------|-----------------|
| `IssuesConfig.kt` | 8 | IssuesConfig, SlowSpanConfig, DroppedFramesConfig, CrashConfig, MemoryPressureConfig, AnrConfig, SlowStartConfig, StrictModeConfig |
| `CpuConfig.kt` | 1 | CpuConfig |
| `NetworkConfig.kt` | 1 | NetworkConfig |
| `FpsConfig.kt` | 1 | FpsConfig |
| `JankConfig.kt` | 1 | JankConfig |
| `GcConfig.kt` | 1 | GcConfig |
| `ThermalConfig.kt` | 1 | ThermalConfig |
| `KamperConfig.kt` | 1 | KamperConfig |

**Total:** 15 KDoc blocks across 8 files. No runtime logic changed. No imports modified.

## ROADMAP Phase 8 Acceptance Criteria Status

| Success Criterion | Status | Evidence |
|-------------------|--------|----------|
| SC1: SECURITY.md with vulnerability reporting policy | SATISFIED | `test -f SECURITY.md` exits 0; contains `sidali.mellouk@zattoo.com`, `## Reporting a Vulnerability`, `## Response Timeline`, `## Supported Versions`, `## Security-Relevant Configuration Notes` |
| SC2: All public API Config types have KDoc documentation | SATISFIED | 15/15 public Config types have class-level KDoc (`/**` opener); IssuesConfig.kt has 8 KDoc blocks counted by `grep -c "^/\*\*$"` |
| SC3: CAPACITY.md documents known scaling limits per module | SATISFIED | `test -f CAPACITY.md` exits 0; contains `## IssuesModule`, `## RecordingManager`, `maxStoredIssues`, `DEFAULT_MAX_RECORDING_SAMPLES`, `4_200`, `200 KB`, `100 KB`, `FIFO` |

VERIFICATION.md Gap 1 (SECURITY.md), Gap 2 (KDoc), and Gap 3 (CAPACITY.md) are all closed by this plan.

## Build Verification

All 8 affected modules compile clean after KDoc additions:

```
./gradlew :kamper:modules:issues:compileDebugKotlinAndroid \
  :kamper:modules:cpu:compileDebugKotlinAndroid \
  :kamper:modules:network:compileDebugKotlinAndroid \
  :kamper:modules:fps:compileDebugKotlinAndroid \
  :kamper:modules:jank:compileDebugKotlinAndroid \
  :kamper:modules:gc:compileDebugKotlinAndroid \
  :kamper:modules:thermal:compileDebugKotlinAndroid \
  :kamper:engine:compileDebugKotlinAndroid -q
EXIT: 0
```

## What Was NOT Done (By Design)

- `DroppedIssueEvent.kt` — untouched. Plan 02 already added class-level KDoc; the plan explicitly forbids re-editing it. Git diff confirms 0 lines changed.
- `object Builder { ... }` blocks inside each Config — not given KDoc. Detekt rule `CommentOverPrivateProperty` / `CommentOverPrivateFunction` constrains KDoc to public members; Builder singletons are also scheduled for refactor to `class Builder` in plan 08-05.
- `companion object { ... }` blocks — not given KDoc per plan constraints and Kotlin idiom.
- No new dependencies added. `androidx.security:security-crypto` is only referenced in SECURITY.md as an optional consumer-side dependency; it was NOT added to `buildSrc/src/main/kotlin/Libs.kt`.
- No Builder structure changes — deferred to plan 08-05.
- No code logic changes of any kind — this was a documentation-only plan.

## Deviations from Plan

### Pre-existing Detekt Violations (Out of Scope)

The root `./gradlew detekt` run reveals 4 pre-existing `MaxLineLength` violations in `IssuesConfig.kt` at the `Builder.build()` one-liner methods (lines 73, 109, 126, 162 in the original file, shifted by KDoc insertion). These violations existed before this plan and are not caused by any KDoc line we added. They were not fixed because:

1. The plan explicitly states "KDoc-only changes" and no runtime/code modifications.
2. These lines are in `object Builder` blocks scheduled for refactor in plan 08-05.
3. Fixing them would require either reformatting the build() calls (a non-KDoc change) or modifying the detekt config (out of scope).

These are logged here for the deferred-items tracking. The detekt failures are pre-existing (109 weighted issues total before this plan).

## Known Stubs

None. Both SECURITY.md and CAPACITY.md contain real data from the codebase (verified `maxStoredIssues = 200`, `DEFAULT_MAX_RECORDING_SAMPLES = 4_200`). All KDoc descriptions accurately reflect the current implementation.

## Threat Flags

No new security surface introduced. This plan adds only documentation files and KDoc comments. The SECURITY.md itself is the security intake channel — its contact address (`sidali.mellouk@zattoo.com`) is verified as the project maintainer address per CLAUDE.md user email.

## Self-Check: PASSED

- `test -f SECURITY.md` → found
- `test -f CAPACITY.md` → found
- `git log --oneline | grep bd985ef` → found: `bd985ef docs(08-04): add SECURITY.md and CAPACITY.md at repository root`
- `git log --oneline | grep ecc3936` → found: `ecc3936 docs(08-04): add class-level KDoc to all 15 public Config types in commonMain`
- All 8 Config files contain `^/**$` pattern
- DroppedIssueEvent.kt: `git diff -- .../DroppedIssueEvent.kt | wc -l` = 0
- `./gradlew :kamper:modules:issues:compileDebugKotlinAndroid :kamper:engine:compileDebugKotlinAndroid -q` → EXIT: 0
