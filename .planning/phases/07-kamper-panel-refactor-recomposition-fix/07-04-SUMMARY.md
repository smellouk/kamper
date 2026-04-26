---
phase: 07-kamper-panel-refactor-recomposition-fix
plan: "04"
subsystem: kamper-ui-compose
tags:
  - compose
  - refactor
  - line-budget
  - cleanup
dependency_graph:
  requires:
    - "07-01 (moved IssueDetailDialog/DetailField/RecordingBadge/GuideStep/StepBadge to PanelComponents.kt)"
  provides:
    - "IssuesTab.kt: clean, minimal imports, 224 lines"
    - "PerfettoTab.kt: clean, minimal imports, 192 lines"
  affects:
    - "kamper/ui/android compile unit"
tech_stack:
  added: []
  patterns:
    - "Internal visibility resolution: private composables replaced by same-package internal versions in PanelComponents.kt"
key_files:
  modified:
    - kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/IssuesTab.kt
    - kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/PerfettoTab.kt
decisions:
  - "Both files were already trimmed by Plan 01 (Wave 1) — this plan verified and cleaned up orphaned imports"
  - "FontFamily import kept in both files (still used: IssueRow line 149 in IssuesTab.kt, URL_UI display in PerfettoTab.kt)"
metrics:
  duration: "~5 minutes"
  completed: "2026-04-26"
  tasks_completed: 2
  files_modified: 2
requirements:
  - DEBT-02
---

# Phase 07 Plan 04: IssuesTab + PerfettoTab Trim — Summary

**One-liner:** Removed orphaned imports from IssuesTab.kt (5 imports) and PerfettoTab.kt (13 imports) after Wave 1 moved private composables to PanelComponents.kt; both files compile and are under the 300-line cap.

## What Was Done

Plan 01 (Wave 1) had already moved the private composables out of both files and added them as `internal` declarations in `PanelComponents.kt`. This plan verified that state and cleaned up the resulting orphaned import statements.

### IssuesTab.kt

**Before:** 230 lines, 5 unused imports from the removed `IssueDetailDialog` and `DetailField` composables.

**Removed imports:**
- `androidx.compose.material3.AlertDialog`
- `androidx.compose.material3.TextButton`
- `androidx.compose.foundation.rememberScrollState`
- `androidx.compose.foundation.layout.heightIn`
- `androidx.compose.foundation.verticalScroll`

**After:** 224 lines. Kept `FontFamily` (used by `IssueRow` for timestamp display).

**Call site preserved:** `IssueDetailDialog(issue = issue, onDismiss = { selected = null })` at line 88 resolves to `internal fun IssueDetailDialog` in `PanelComponents.kt`.

### PerfettoTab.kt

**Before:** 206 lines, 13 unused imports from the removed `RecordingBadge`, `GuideStep`, and `StepBadge` composables.

**Removed imports:**
- `androidx.compose.animation.AnimatedContent`
- `androidx.compose.animation.fadeIn`
- `androidx.compose.animation.fadeOut`
- `androidx.compose.animation.togetherWith`
- `androidx.compose.foundation.layout.size`
- `androidx.compose.foundation.shape.CircleShape`
- `androidx.compose.runtime.LaunchedEffect`
- `androidx.compose.runtime.getValue`
- `androidx.compose.runtime.mutableStateOf`
- `androidx.compose.runtime.setValue`
- `androidx.compose.ui.platform.LocalClipboardManager`
- `androidx.compose.ui.text.AnnotatedString`
- `kotlinx.coroutines.delay`

**After:** 192 lines. Kept `FontFamily` (used for `URL_UI` monospace display). `internal expect val showAdbGuide` preserved.

**Call sites preserved:**
- `RecordingBadge(isRecording, sampleCount, maxRecordingSamples)` → resolves to `internal fun RecordingBadge` in PanelComponents.kt
- `GuideStep(number, label, cmd)` (2 calls) → resolves to `internal fun GuideStep` in PanelComponents.kt
- `StepBadge("3")` → resolves to `internal fun StepBadge` in PanelComponents.kt

## Verification

| Check | Result |
|-------|--------|
| `wc -l IssuesTab.kt` | 224 (OK < 300) |
| `wc -l PerfettoTab.kt` | 192 (OK < 300) |
| `private fun IssueDetailDialog` in IssuesTab.kt | 0 (removed by Plan 01) |
| `private fun DetailField` in IssuesTab.kt | 0 (removed by Plan 01) |
| `IssueDetailDialog(` call site in IssuesTab.kt | 1 (preserved) |
| `private fun RecordingBadge` in PerfettoTab.kt | 0 (removed by Plan 01) |
| `private fun GuideStep` in PerfettoTab.kt | 0 (removed by Plan 01) |
| `private fun StepBadge` in PerfettoTab.kt | 0 (removed by Plan 01) |
| `RecordingBadge(` call site in PerfettoTab.kt | 1 (preserved) |
| `GuideStep(` call sites in PerfettoTab.kt | 2 (preserved) |
| `internal expect val showAdbGuide` | 1 (preserved) |
| `./gradlew :kamper:ui:android:compileDebugKotlinAndroid` | BUILD SUCCESSFUL |

## Commits

| Task | Commit | Description |
|------|--------|-------------|
| Task 1+2 | 641b8da | refactor(07-04): remove unused imports from IssuesTab.kt and PerfettoTab.kt |

## Deviations from Plan

The plan described Tasks 1 and 2 as requiring deletion of `IssueDetailDialog`, `DetailField`, `RecordingBadge`, `GuideStep`, and `StepBadge` function bodies from the respective files. However, Plan 01 (Wave 1) had already performed this deletion as a documented deviation (Kotlin doesn't allow same-named declarations with different visibility in the same package, so Plan 01 moved rather than copied). 

**Actual work done:** Verified the moved state was correct, confirmed all call sites resolve properly, and removed the orphaned imports that remained after Plan 01's moves. The plan's core objective (both files under 300 lines, compiling, with call sites resolving via PanelComponents.kt) was fully achieved.

## Known Stubs

None — all composable call sites are fully wired to internal implementations in PanelComponents.kt.

## Threat Flags

None — pure import cleanup, no new trust boundaries introduced.

## Self-Check: PASSED

- IssuesTab.kt: FOUND at expected path, 224 lines
- PerfettoTab.kt: FOUND at expected path, 192 lines  
- Commit 641b8da: EXISTS in git log
- Build: SUCCESSFUL
