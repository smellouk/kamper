---
phase: 08-security-docs-scaling
plan: "05"
subsystem: api-correctness
tags: [code-review, builders, thread-safety, readme, recording-manager, gap-closure]
dependency_graph:
  requires: []
  provides:
    - correct-readme-issues-example
    - fresh-builder-per-module-call
    - synchronized-recording-buffer
  affects:
    - kamper/modules/issues
    - kamper/modules/cpu
    - kamper/modules/network
    - kamper/modules/fps
    - kamper/modules/jank
    - kamper/modules/gc
    - kamper/modules/thermal
    - kamper/engine
    - kamper/ui/android
tech_stack:
  patterns:
    - class Builder (fresh-instance) replacing object Builder (singleton)
    - synchronized(Any()) lock for concurrent buffer access
    - snapshot pattern: toList() under lock, serialize outside
key_files:
  modified:
    - README.md
    - kamper/modules/issues/src/commonMain/kotlin/com/smellouk/kamper/issues/IssuesConfig.kt
    - kamper/modules/cpu/src/commonMain/kotlin/com/smellouk/kamper/cpu/CpuConfig.kt
    - kamper/modules/network/src/commonMain/kotlin/com/smellouk/kamper/network/NetworkConfig.kt
    - kamper/modules/fps/src/commonMain/kotlin/com/smellouk/kamper/fps/FpsConfig.kt
    - kamper/modules/jank/src/commonMain/kotlin/com/smellouk/kamper/jank/JankConfig.kt
    - kamper/modules/gc/src/commonMain/kotlin/com/smellouk/kamper/gc/GcConfig.kt
    - kamper/modules/thermal/src/commonMain/kotlin/com/smellouk/kamper/thermal/ThermalConfig.kt
    - kamper/engine/src/commonMain/kotlin/com.smellouk.kamper/KamperConfig.kt
    - kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/RecordingManager.kt
    - "36 platform Module.kt / Kamper.kt files (cpu/network/fps/jank/gc/thermal/issues/engine)"
    - "4 commonTest files (CpuConfigBuilderTest, NetworkConfigBuilderTest, FpsConfigBuilderTest, KamperConfigBuilderTest)"
decisions:
  - Fresh Builder instance per call eliminates silent configuration bleed across sequential module installs
  - Snapshot pattern (toList() under lock) avoids holding bufferLock during slow protobuf serialization
  - stopRecording() deliberately left unlocked — MutableStateFlow is internally thread-safe
  - Pre-existing detekt MaxLineLength issues in IssuesConfig.kt (4 long build() lines) are out of scope
metrics:
  duration: "~15 minutes"
  completed: "2026-04-26T13:47:27Z"
  tasks_completed: 3
  tasks_total: 3
  files_modified: 51
---

# Phase 08 Plan 05: Code Review Gap Closure Summary

Closed all four critical code-review findings from `08-REVIEW.md` (CR-01 through CR-04): non-compiling README examples, singleton Builder state bleed, and unsynchronized recording buffer.

## Tasks Completed

### Task 1: CR-01 + CR-02 — Fix README IssuesModule and IssueInfo examples
**Commit:** adbe503

Replaced the broken Issues quick-start block (lines 272-291) in README.md with a correct example:
- CR-01: Rewrote `IssuesModule { slowSpanEnabled = ... }` to the real factory signature `IssuesModule(context = context, anr = AnrConfig(...), slowStart = SlowStartConfig(...)) { ... }` with sub-configs constructed via their data class constructors.
- CR-02: Fixed `info.severity`/`info.type`/`info.message` (nonexistent) to `val issue = info.issue; issue.severity/type/message` (real fields via the `Issue` wrapper).

All nonexistent identifiers removed: `slowSpanEnabled`, `slowSpanThresholdMs`, `droppedFramesEnabled`, `droppedFrameThresholdMs`, `droppedFrameConsecutiveThreshold`, `crashEnabled`, `memoryPressureEnabled`, `anrEnabled`, `slowStartEnabled`, `info.severity`, `info.type`, `info.message`.

### Task 2: CR-03 — Convert 15 object Builder to class Builder, update 47 call sites
**Commit:** 2325e09

**8 Config files converted** (15 total `object Builder` → `class Builder` declarations):
- `IssuesConfig.kt` — 8 builders (IssuesConfig, SlowSpanConfig, DroppedFramesConfig, CrashConfig, MemoryPressureConfig, AnrConfig, SlowStartConfig, StrictModeConfig)
- `CpuConfig.kt`, `NetworkConfig.kt`, `FpsConfig.kt`, `JankConfig.kt`, `GcConfig.kt`, `ThermalConfig.kt`, `KamperConfig.kt` — 1 builder each

**4 internal DSL helpers in IssuesConfig.kt** updated:
- `slowSpan = SlowSpanConfig.Builder().apply(block).build()`
- `droppedFrames = DroppedFramesConfig.Builder().apply(block).build()`
- `crash = CrashConfig.Builder().apply(block).build()`
- `memoryPressure = MemoryPressureConfig.Builder().apply(block).build()`

**36 platform Module/Kamper.kt call sites** updated via batch `perl -pi -e` replacement.

**4 commonTest files** updated manually (used `Builder.apply {` with a brace, not parenthesis, so escaped batch replace).

**KDoc** in `KamperConfig.kt` updated to show `Builder()` constructor invocation.

Final counts verified:
- `grep -rn "object Builder" kamper --include='*.kt'` → 0 matches
- `grep -rn "\.Builder\.apply" kamper --include='*.kt'` → 0 matches  
- `grep -rE "\.Builder\(\)\.apply" kamper --include='*.kt'` → 47 matches

### Task 3: CR-04 — Synchronize RecordingManager.recordingBuffer under bufferLock
**Commit:** 02d69be

Replaced `RecordingManager.kt` (55 lines → 71 lines) with synchronized version:
- Added `private val bufferLock = Any()` as the lock object
- `record()`: both size-check+removeFirst and addLast+count-update wrapped in `synchronized(bufferLock)`
- `startRecording()`: clear+count-reset wrapped in `synchronized(bufferLock)`; `_isRecording.value = true` outside lock (MutableStateFlow is thread-safe)
- `exportTrace()`: snapshot via `synchronized(bufferLock) { recordingBuffer.toList() }`, then `PerfettoExporter.export(snapshot)` outside lock
- `exportTraceToFile()`: same snapshot pattern; `PerfettoExporter.exportToFile(snapshot, out)` outside lock
- `clearRecording()`: clear+count-reset wrapped; `_isRecording.value = false` outside lock
- `stopRecording()`: unchanged — no buffer access, no lock needed

## Verification Results

- `grep -c "slowSpanEnabled\|info\.severity" README.md` → 0
- `grep -rn "object Builder" kamper --include='*.kt'` → 0 matches
- `grep -rn "\.Builder\.apply" kamper --include='*.kt'` → 0 matches
- `grep -rE "\.Builder\(\)\.apply" kamper --include='*.kt'` → 47 matches (exceeds required 44)
- `grep -c "synchronized(bufferLock)" RecordingManager.kt` → 5
- `./gradlew :kamper:modules:issues:compileDebugKotlinAndroid` → exit 0
- `./gradlew :kamper:modules:cpu:compileDebugKotlinAndroid :kamper:modules:network:compileDebugKotlinAndroid :kamper:modules:fps:compileDebugKotlinAndroid :kamper:modules:jank:compileDebugKotlinAndroid :kamper:modules:gc:compileDebugKotlinAndroid :kamper:modules:thermal:compileDebugKotlinAndroid` → exit 0
- `./gradlew :kamper:engine:compileDebugKotlinAndroid` → exit 0
- `./gradlew :kamper:modules:issues:compileKotlinJvm :kamper:modules:cpu:compileKotlinJvm :kamper:modules:network:compileKotlinJvm :kamper:modules:fps:compileKotlinJvm :kamper:engine:compileKotlinJvm` → exit 0
- `./gradlew :kamper:ui:android:compileDebugKotlinAndroid` → exit 0
- `CpuConfigBuilderTest`, `NetworkConfigBuilderTest`, `FpsConfigBuilderTest`, `KamperConfigBuilderTest` → all PASS
- `IssuesWatcherTest` (6 tests) → all PASS
- Detekt: no new violations introduced (109 weighted issues before and after — all pre-existing)

## Wave Ordering Note

Plan 04 and Plan 05 both modify the 8 Config files (IssuesConfig.kt and the 6 other Config files). Plan 04 adds KDoc above class declarations; Plan 05 changes `object Builder` to `class Builder`. In this wave execution, Plan 05 ran without Plan 04 changes present — but the files are KDoc-clean per prior wave work (Plan 04 was wave 1). The only overlap was the Config files: Plan 04 KDoc sits above data class declarations, Plan 05 only modifies lines containing `object Builder` inside the companion scope. No merge conflicts occurred.

## Deviations from Plan

### Auto-fixed Issues

None. The mechanical edit set was complete as specified. The only deviation was the scope of the `perl -pi -e` batch replacement: it correctly skipped `Builder.apply {` forms (with brace) since those require explicit `()` insertion — the test files were updated manually as a separate step.

### Pre-existing Issues Not Fixed (Out of Scope)

- `CpuInfoUnsupportedTest` fails with `NotImplementedError: implement in Plan 07-01 Task 3` — pre-existing stub, not caused by Builder refactor.
- 109 detekt MaxLineLength warnings in IssuesConfig.kt `build()` functions — pre-existing, not caused by `object` → `class` change.

## Known Stubs

None. All plan goals fully wired and functional.

## Threat Flags

None. No new network endpoints, auth paths, or trust-boundary changes introduced.

## CR Closure Summary

| Finding | Status | Task |
|---------|--------|------|
| CR-01: README IssuesModule example uses nonexistent properties | Closed | Task 1 |
| CR-02: README IssueInfo listener uses nonexistent direct fields | Closed | Task 1 |
| CR-03: object Builder singleton causes configuration bleed | Closed | Task 2 |
| CR-04: RecordingManager.recordingBuffer unsynchronized | Closed | Task 3 |

## Self-Check: PASSED

- README.md: found, changed sections verified
- IssuesConfig.kt: 8 class Builder declarations confirmed
- RecordingManager.kt: bufferLock + 5 synchronized blocks confirmed
- Commits: adbe503, 2325e09, 02d69be all present in git log
