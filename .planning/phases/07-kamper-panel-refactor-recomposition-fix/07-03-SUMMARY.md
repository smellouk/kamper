---
phase: 07
plan: "03"
subsystem: kamper-ui-compose
tags:
  - compose
  - refactor
  - performance
  - derivedstateof
dependency_graph:
  requires:
    - "07-01: PanelComponents.kt created with 16 internal composables"
  provides:
    - "ActivityTab.kt: per-metric derivedStateOf wrappers (PERF-03)"
    - "SettingsTab.kt: extracted SettingsContent + co-located IssuesSubConfig (DEBT-02, D-05)"
  affects:
    - "KamperPanel.kt: ActivityTab/SettingsTab are new call targets (Plan 07-05 will wire them)"
tech_stack:
  added: []
  patterns:
    - "remember { derivedStateOf { ... } } per metric field for scoped recomposition (PERF-03)"
    - "Tab composables receive StateFlow directly (ActivityTab) or already-collected values (SettingsTab)"
    - "Private helper co-located in same file (IssuesSubConfig in SettingsTab.kt)"
key_files:
  created:
    - "kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/ActivityTab.kt"
    - "kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/SettingsTab.kt"
  modified: []
decisions:
  - "ActivityTab.kt receives StateFlow directly and collects internally so derivedStateOf is scoped to its recomposition pipeline (D-04, D-06)"
  - "SettingsTab.kt receives already-collected s: KamperUiState + cfg: KamperUiSettings (not StateFlows) per D-05"
  - "IssuesSubConfig co-located in SettingsTab.kt as private fun because it is only called from SettingsTab and the 236-line file stays well under 300-line budget (D-05, Claude discretion)"
  - "gcCountDelta is Long in KamperUiState — val gcCountDelta by remember { derivedStateOf { s.gcCountDelta } } infers Long type correctly; fraction division (gcCountDelta / 10f) works without cast"
  - "KamperPanel.kt left unchanged per plan — private fun ActivityContent, SettingsContent, IssuesSubConfig still present until Plan 07-05 cleans them up"
metrics:
  duration: "4 minutes"
  completed: "2026-04-26"
  tasks_completed: 2
  tasks_total: 2
  files_created: 2
  files_modified: 0
requirements:
  - DEBT-02
  - PERF-03
---

# Phase 07 Plan 03: ActivityTab.kt and SettingsTab.kt Creation Summary

## One-liner

Created `ActivityTab.kt` (16 `derivedStateOf` wrappers, PERF-03) and `SettingsTab.kt` (8 modules + 6 detectors + co-located `IssuesSubConfig`, DEBT-02) as new composable tab files under 300 lines each.

## What Was Built

### ActivityTab.kt (141 lines)

`internal fun ActivityTab(state: StateFlow<KamperUiState>, settings: StateFlow<KamperUiSettings>)` collecting both StateFlows internally and wrapping every one of the 16 `KamperUiState` metric fields in `remember { derivedStateOf { ... } }`:

| Derived Val | State Field | Used By |
|-------------|-------------|---------|
| `cpuPercent` | `s.cpuPercent` | CPU MetricCard fraction + label |
| `cpuHistory` | `s.cpuHistory` | CPU MetricCard sparkline |
| `fps` | `s.fps` | FPS MetricCard label |
| `fpsPeak` | `s.fpsPeak` | FPS MetricCard extra |
| `fpsLow` | `s.fpsLow` | FPS MetricCard extra |
| `fpsHistory` | `s.fpsHistory` | FPS MetricCard sparkline |
| `memoryUsedMb` | `s.memoryUsedMb` | Memory MetricCard |
| `memoryHistory` | `s.memoryHistory` | Memory MetricCard sparkline |
| `downloadMbps` | `s.downloadMbps` | Network MetricCard |
| `downloadHistory` | `s.downloadHistory` | Network MetricCard sparkline |
| `jankDropped` | `s.jankDroppedFrames` | Jank MetricCard |
| `jankRatio` | `s.jankRatio` | Jank MetricCard |
| `gcCountDelta` | `s.gcCountDelta` (Long) | GC MetricCard |
| `gcPauseDelta` | `s.gcPauseMsDelta` | GC MetricCard |
| `thermalState` | `s.thermalState` | Thermal MetricCard |
| `isThrottling` | `s.isThrottling` | Thermal MetricCard color/extra |

7 MetricCard calls (CPU, FPS, Memory, Network, Jank, GC, Thermal).

### SettingsTab.kt (236 lines)

`internal fun SettingsTab(s: KamperUiState, cfg: KamperUiSettings, onSettingsChange, onStartEngine, onStopEngine, onRestartEngine)` with:
- 1 EngineSection call
- 8 ModuleCard calls (CPU, FPS, Memory, Network, Issues, Jank, GC, Thermal)
- `private fun IssuesSubConfig` co-located in same file with 6 DetectorCard calls (Slow Span, Dropped Frames, Crash, Memory Pressure, ANR, Slow Start)
- Zero `derivedStateOf` usage (D-07 compliant)

## Verification Results

| Check | Result |
|-------|--------|
| `remember { derivedStateOf {` count in ActivityTab.kt | 16 (code) + 1 (comment) = 17 grep hits |
| MetricCard calls in ActivityTab.kt | 7 |
| ActivityTab.kt line count | 141 (< 300 OK) |
| ModuleCard calls in SettingsTab.kt | 8 |
| DetectorCard calls in SettingsTab.kt | 6 |
| `private fun IssuesSubConfig` in SettingsTab.kt | 1 |
| `derivedStateOf` in SettingsTab.kt | 0 |
| SettingsTab.kt line count | 236 (< 300 OK) |
| KamperPanel.kt changed | No (git diff shows 0 lines) |
| `./gradlew :kamper:ui:android:compileDebugKotlinAndroid` | EXIT 0 (BUILD SUCCESSFUL) |

## Deviations from Plan

None — plan executed exactly as written. The `gcCountDelta` field type is `Long` (not `Int` as the plan template implied), which works correctly since Kotlin's Long division by `10f` produces Float without any cast needed. No behavior change.

## Known Stubs

None. Both composables are fully functional with complete logic sourced verbatim from `KamperPanel.kt` existing private implementations.

## Threat Flags

None. Pure internal Compose refactor within `com.smellouk.kamper.ui.compose` package. No new network endpoints, auth paths, or trust boundaries introduced.

## Self-Check: PASSED

**Files created:**
- FOUND: `kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/ActivityTab.kt`
- FOUND: `kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/SettingsTab.kt`

**Commits verified:**
- FOUND: 181989a — feat(07-03): ActivityTab.kt
- FOUND: e3c294a — feat(07-03): SettingsTab.kt
