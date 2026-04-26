---
phase: 07
plan: 01
subsystem: kamper-ui-compose
tags:
  - compose
  - refactor
  - kotlin-multiplatform
  - internal-api
dependency_graph:
  requires:
    - "06-01: KamperUiRepository refactor (PreferencesStore/SettingsRepository/RecordingManager)"
  provides:
    - "PanelComponents.kt: 16 internal composables + INTERVAL_OPTIONS as stable shared API"
    - "Wave 2 unblocked: Plans 07-02..07-05 can reference PanelComponents API"
  affects:
    - "KamperPanel.kt: private composables removed, now resolved via PanelComponents.kt"
    - "IssuesTab.kt: IssueDetailDialog/DetailField private funs removed"
    - "PerfettoTab.kt: RecordingBadge/GuideStep/StepBadge private funs removed"
tech_stack:
  added: []
  patterns:
    - "internal visibility for shared composables (not private) across package files"
    - "Inline helper logic to avoid cross-file private function calls"
key_files:
  created:
    - "kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/PanelComponents.kt"
  modified:
    - "kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/KamperPanel.kt"
    - "kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/IssuesTab.kt"
    - "kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/PerfettoTab.kt"
decisions:
  - "Kotlin does not allow two package-level declarations (val or fun) with the same name even with different visibility — private+internal same-name funs conflict at package level, so originals were moved not copied"
  - "IssueDetailDialog helpers (TypeChip, severityColor, fmtTime) inlined directly into PanelComponents.kt body to avoid cross-file private calls"
  - "RecordingBadge preserved with 3-parameter signature (isRecording, sampleCount, maxRecordingSamples) matching actual PerfettoTab.kt source"
metrics:
  duration: "7 minutes"
  completed: "2026-04-26"
  tasks_completed: 4
  tasks_total: 4
  files_created: 1
  files_modified: 3
requirements:
  - DEBT-02
---

# Phase 07 Plan 01: PanelComponents.kt — Shared Composables Extraction Summary

## One-liner

Created `PanelComponents.kt` with 16 `internal` composables (11 from KamperPanel, 2 from IssuesTab, 3 from PerfettoTab) plus `INTERVAL_OPTIONS`, establishing the stable shared API for the KamperPanel refactor wave.

## What Was Built

A new file `PanelComponents.kt` (831 lines) in `com.smellouk.kamper.ui.compose` containing:

| # | Composable | Source |
|---|-----------|--------|
| 1 | `internal val INTERVAL_OPTIONS` | KamperPanel.kt |
| 2 | `internal fun ThemeToggle` | KamperPanel.kt |
| 3 | `internal fun ThemeSegment` | KamperPanel.kt |
| 4 | `internal fun PanelTab` | KamperPanel.kt |
| 5 | `internal fun DetectorCard` | KamperPanel.kt |
| 6 | `internal fun EngineSection` | KamperPanel.kt |
| 7 | `internal fun EngineButton` | KamperPanel.kt |
| 8 | `internal fun ModuleCard` | KamperPanel.kt |
| 9 | `internal fun SectionLabel` | KamperPanel.kt |
| 10 | `internal fun KamperSwitch` | KamperPanel.kt |
| 11 | `internal fun <T> OptionRow` | KamperPanel.kt |
| 12 | `internal fun MetricCard` | KamperPanel.kt |
| 13 | `internal fun IssueDetailDialog` | IssuesTab.kt (with inlined helpers) |
| 14 | `internal fun DetailField` | IssuesTab.kt |
| 15 | `internal fun RecordingBadge` | PerfettoTab.kt |
| 16 | `internal fun GuideStep` | PerfettoTab.kt |
| 17 | `internal fun StepBadge` | PerfettoTab.kt |

(16 internal fun composables + 1 internal val = 17 symbols)

## Compile Result

`./gradlew :kamper:ui:android:compileDebugKotlinAndroid` exits 0 after every task and at final verification.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Kotlin package-level name conflict between private and internal declarations**

- **Found during:** Task 1 (INTERVAL_OPTIONS), Task 2 (all 11 composables), Task 3 (IssueDetailDialog/DetailField), Task 4 (RecordingBadge/GuideStep/StepBadge)
- **Issue:** The plan assumed that `private fun Foo` in `KamperPanel.kt` and `internal fun Foo` in `PanelComponents.kt` can coexist in the same package because "private is file-scoped." This is incorrect: Kotlin considers both as package-level declarations with the same name — the compiler reports "Conflicting overloads/declarations" regardless of visibility modifier.
- **Fix:** Moved (not copied) private composables from source files. Once moved, call sites in the original files resolve to the `internal` versions in PanelComponents.kt (same package), which is exactly the desired behavior. No semantics changed.
- **Files modified:** KamperPanel.kt, IssuesTab.kt, PerfettoTab.kt
- **Commits:** f0950d5 (Task 1), 65fd635 (Task 2), 70ee053 (Task 3), a1c85ba (Task 4)

**2. [Rule 1 - Bug] RecordingBadge has 3 parameters, not 2 as stated in plan interfaces section**

- **Found during:** Task 4
- **Issue:** The plan's `<interfaces>` section listed `RecordingBadge(isRecording: Boolean, sampleCount: Int)` with 2 params. The actual PerfettoTab.kt source has `RecordingBadge(isRecording: Boolean, sampleCount: Int, maxRecordingSamples: Int)` — 3 params.
- **Fix:** Copied verbatim from source (3 params) as directed by the plan's "copied verbatim" instruction. The inlined PanelComponents.kt version matches the PerfettoTab.kt call site.
- **Files modified:** PanelComponents.kt

**3. [Plan deviation] Line count exceeds expected 350-500 range**

- **Actual:** 831 lines
- **Expected:** 350-500 lines (plan's acceptance criteria)
- **Reason:** The plan's line estimate did not account for: (a) IssueDetailDialog with fully inlined TypeChip/severityColor/fmtTime helpers adding ~80 lines vs expected compact copy, (b) section comments between composables, (c) RecordingBadge having 3 params and matching the full original body including the warning logic.
- **Impact:** None — the file is a correct shared primitives file; the CONTEXT.md explicitly notes "PanelComponents.kt is expected to be the largest at ~400 lines — that is acceptable since it is a shared primitives file." The 831 line count reflects accurate inlining. No functionality is missing or incorrect.

## Source Files Status

| File | Changed? | Reason |
|------|----------|--------|
| `KamperPanel.kt` | Yes — private composables removed | Kotlin name conflict resolution (Rule 1 fix) |
| `IssuesTab.kt` | Yes — IssueDetailDialog/DetailField removed | Kotlin name conflict resolution (Rule 1 fix) |
| `PerfettoTab.kt` | Yes — RecordingBadge/GuideStep/StepBadge removed | Kotlin name conflict resolution (Rule 1 fix) |

All source files continue to compile correctly. Their call sites now resolve to the `internal` composables in PanelComponents.kt via Kotlin's package-level name resolution.

## Known Stubs

None. All composables are functional with complete logic.

## Threat Flags

None. This plan is a pure code-reorganisation refactor within the `internal` module scope. No new trust boundaries, network endpoints, auth paths, or persistence introduced.

## Self-Check: PASSED

**Files created:**
- FOUND: `kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/PanelComponents.kt`
- FOUND: `.planning/phases/07-kamper-panel-refactor-recomposition-fix/07-01-SUMMARY.md`

**Commits verified:**
- FOUND: f0950d5 — Task 1 scaffold
- FOUND: 65fd635 — Task 2 composables from KamperPanel
- FOUND: 70ee053 — Task 3 composables from IssuesTab
- FOUND: a1c85ba — Task 4 composables from PerfettoTab
