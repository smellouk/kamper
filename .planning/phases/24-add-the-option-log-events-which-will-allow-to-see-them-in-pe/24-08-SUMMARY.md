---
phase: 24
plan: 08
subsystem: ui, engine
tags: [perfetto, events, proto-encoding, tdd, wave-3]
dependency_graph:
  requires:
    - 24-04 (Engine event API: drainEvents, EventRecord)
  provides:
    - Tracks.EVENTS = 9 constant (named event track, NOT in ALL)
    - PerfettoExporter.export(samples, events) updated signature
    - EVENTS track descriptor without counter{} (D-18)
    - TYPE_INSTANT=3 packet encoding (D-16)
    - TYPE_SLICE_BEGIN=1 + TYPE_SLICE_END=2 pair encoding (D-17)
    - RecordingManager.exportTrace(events) / exportTraceToFile(out, events)
    - KamperUiRepository.exportTrace() drains Engine events at export time
  affects:
    - libs/ui/kmm (Perfetto export pipeline)
    - libs/engine (EventRecord public, drainEvents public)
tech_stack:
  added:
    - ProtoWalker test helper (inline in PerfettoExporterEventTest)
    - TRACK_EVENT_NAME_FIELD = 23 constant
    - TRACK_EVENT_TYPE_SLICE_BEGIN/END/INSTANT constants
  patterns:
    - ProtoWriter extension for named event tracks vs counter tracks
    - LongMethod extraction: writeEventPackets + writeEventRecord helpers
    - Default-empty events parameter for backward compatibility (D-20 Option B)
key_files:
  created: []
  modified:
    - libs/ui/kmm/src/commonMain/kotlin/com/smellouk/kamper/ui/RecordedSample.kt
    - libs/ui/kmm/src/androidMain/kotlin/com/smellouk/kamper/ui/PerfettoExporter.kt
    - libs/ui/kmm/src/androidMain/kotlin/com/smellouk/kamper/ui/RecordingManager.kt
    - libs/ui/kmm/src/androidMain/kotlin/com/smellouk/kamper/ui/KamperUiRepository.kt
    - libs/ui/kmm/src/androidUnitTest/kotlin/com/smellouk/kamper/ui/PerfettoExporterEventTest.kt
    - libs/engine/src/commonMain/kotlin/com.smellouk.kamper/EventRecord.kt
    - libs/engine/src/commonMain/kotlin/com.smellouk.kamper/Engine.kt
decisions:
  - Tracks.EVENTS = 9 (not 8 as planned): Phase 23 allocated GPU=8; EVENTS uses next available slot
  - EventRecord made public (was internal): cross-module access from ui:kmm requires it
  - Engine.drainEvents() made public (was @PublishedApi internal): consistent with EventRecord visibility
  - ProtoWalker test helper: inline in test file, not a separate class; reads proto varint/length-delimited messages
  - "@file:Suppress(MagicNumber)" on test file: androidUnitTest not in detekt.yml excludes list; field numbers are protocol-defined constants
  - writeTrace refactored into writeEventPackets + writeEventRecord to satisfy LongMethod (69>60) detekt rule
metrics:
  duration: ~25m
  completed_date: "2026-05-02T18:41:00Z"
  tasks_completed: 3
  tasks_total: 3
  files_changed: 7
---

# Phase 24 Plan 08: Perfetto Event Encoding Summary

Wire custom events (D-16..D-21) into the Perfetto trace export pipeline: EVENTS track descriptor without counter{}, TYPE_INSTANT markers, TYPE_SLICE_BEGIN/END colored bars, Engine drain at export time — all 5 PerfettoExporterEventTest assertions passing with zero new detekt violations in modified files.

## What Was Built

### Task 1: Tracks.EVENTS Constant

`RecordedSample.kt` gained `const val EVENTS = 9` after `GPU = 8`.

**Deviation:** The plan specified EVENTS=8, but Phase 23 had already allocated 8 to GPU. EVENTS uses the next available UUID (9). The constant is NOT added to `Tracks.ALL` — the EVENTS track uses a different descriptor and encoding than the 8 existing counter tracks.

### Task 2: PerfettoExporter — Events Track Encoding + 5 Tests

**PerfettoExporter.kt** changes:
- Import: `com.smellouk.kamper.EventRecord`
- `export(samples, events = emptyList())` and `exportToFile(samples, events = emptyList(), out)` new signatures
- New constants: `TRACK_EVENT_TYPE_SLICE_BEGIN=1`, `TRACK_EVENT_TYPE_SLICE_END=2`, `TRACK_EVENT_TYPE_INSTANT=3`, `TRACK_EVENT_NAME_FIELD=23`
- `writeEventPackets(root, events)` — emits EVENTS track descriptor (D-18: no `counter{}` sub-message) + iterates events
- `writeEventRecord(root, event)` — emits TYPE_INSTANT or TYPE_SLICE_BEGIN+END pair; name (field 23) on INSTANT and BEGIN only (Pitfall 4 guarded)
- `writeTrace` refactored to call `writeEventPackets` when events non-empty (fixes LongMethod 69->~30 lines)

**Engine changes (cross-module access fix):**
- `EventRecord`: `internal` → `public` — required for `PerfettoExporter` import from `libs/ui/kmm`
- `Engine.drainEvents()`: `@PublishedApi internal` → `public` — consistent with EventRecord visibility change

**PerfettoExporterEventTest.kt** — 5 stubs replaced with real assertions:
- Test strategy: `ProtoWalker` helper inline in test file — reads varint and length-delimited proto bytes, returns `List<Pair<fieldNum, value>>` for each message level
- `exportEmitsEventsTrackDescriptorWithoutCounter` — finds track_descriptor with uuid=9, name="Custom Events", no field 8 (counter)
- `exportEmitsTypeInstantPacketForInstantEvent` — finds packet with track_event type=3, track_uuid=9, name field 23 = "purchase"
- `exportEmitsTypeSliceBeginAndEndForDurationEvent` — finds BEGIN (type=1) at startNs with name, END (type=2) at startNs+durationNs
- `exportEmitsTrackEventNameOnBeginAndInstantOnly` — verifies name field 23 on TYPE_INSTANT + TYPE_SLICE_BEGIN, absent on TYPE_SLICE_END
- `exportWithEmptyEventsListProducesNoEventsTrack` — asserts "Custom Events" string absent and no track_descriptor with uuid=9

### Task 3: RecordingManager + KamperUiRepository Wire-Up

**RecordingManager.kt:**
- Import: `com.smellouk.kamper.EventRecord`
- `exportTrace(events: List<EventRecord> = emptyList())` — passes events to `PerfettoExporter.export(snapshot, events)`
- `exportTraceToFile(out, events = emptyList())` — passes events to `PerfettoExporter.exportToFile(snapshot, events, out)`

**KamperUiRepository.kt:**
- Import: `com.smellouk.kamper.Kamper`
- `exportTrace()` — drains `Kamper.drainEvents()` then calls `recordingManager.exportTrace(events)`
- `exportTraceToFile(out)` — same drain+forward pattern

## Pitfall Guards

| Pitfall | Guard |
|---------|-------|
| Pitfall 3: counter{} sub-message on EVENTS track | `writeEventPackets` omits `message(8) {}`; guarded by `exportEmitsEventsTrackDescriptorWithoutCounter` test |
| Pitfall 4: name on TYPE_SLICE_END | `writeEventRecord` only calls `string(TRACK_EVENT_NAME_FIELD, event.name)` on INSTANT and BEGIN; guarded by `exportEmitsTrackEventNameOnBeginAndInstantOnly` test |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Tracks.EVENTS = 8 conflicts with GPU = 8 from Phase 23**
- **Found during:** Task 1 — RecordedSample.kt already had `const val GPU = 8`
- **Issue:** Plan pre-dated Phase 23 (GPU module added GPU=8 to Tracks); assigning EVENTS=8 would create a UUID collision in Perfetto traces
- **Fix:** `EVENTS = 9` (next available slot after GPU=8); UUID collision guarded by Pitfall 2 in RESEARCH
- **Files modified:** `RecordedSample.kt`
- **Commit:** 1c6b3be

**2. [Rule 1 - Bug] EventRecord internal visibility blocks cross-module import**
- **Found during:** Task 2 compilation — "Cannot access 'data class EventRecord : Any': it is internal in file"
- **Issue:** `EventRecord` was `internal` in `libs/engine` — cannot be imported in `libs/ui/kmm` PerfettoExporter
- **Fix:** Made `EventRecord` public; updated `Engine.drainEvents()` from `@PublishedApi internal` to `public` for consistency
- **Files modified:** `EventRecord.kt`, `Engine.kt`
- **Commit:** e5a94ec

**3. [Rule 1 - Bug] PerfettoExporter.exportToFile signature change breaks existing RecordingManager call**
- **Found during:** Task 2 — changed `exportToFile(samples, out)` to `exportToFile(samples, events, out)` with events before out
- **Issue:** RecordingManager's existing call `PerfettoExporter.exportToFile(snapshot, out)` mismatched new signature
- **Fix:** Updated RecordingManager to use named parameter then full Task 3 rewrite
- **Files modified:** `RecordingManager.kt`
- **Commit:** bcc8065

**4. [Rule 1 - Bug] LongMethod detekt violation in writeTrace**
- **Found during:** Task 2 post-commit detekt check — `writeTrace` 69 lines > 60 max
- **Fix:** Extracted `writeEventPackets()` and `writeEventRecord()` private helpers; `writeTrace` reduces to ~30 lines
- **Files modified:** `PerfettoExporter.kt`
- **Commit:** e5a94ec

**5. [Rule 1 - Bug] MagicNumber detekt violations in PerfettoExporter (field 23)**
- **Found during:** Task 2 post-commit detekt check
- **Fix:** Added `TRACK_EVENT_NAME_FIELD = 23` constant; replaced literal `23` usages
- **Files modified:** `PerfettoExporter.kt`
- **Commit:** e5a94ec

**6. [Rule 1 - Bug] MagicNumber detekt violations in PerfettoExporterEventTest.kt**
- **Found during:** Task 2 detekt check — `androidUnitTest` source set not in detekt.yml exclusion list (excludes list covers `**/androidTest/**` but not `**/androidUnitTest/**`)
- **Fix:** Added `@file:Suppress("MagicNumber")` to test file; proto field numbers are protocol-defined constants, not arbitrary magic values
- **Files modified:** `PerfettoExporterEventTest.kt`
- **Commit:** e5a94ec

### Deferred Items (Pre-Existing, Out-of-Scope)

- `libs/modules/thermal/src/jvmMain/ThermalInfoRepositoryImpl.kt` — 5 `NoMultipleSpaces` violations (pre-existing from Phase 23)
- `libs/modules/thermal/src/{ios,tvos}Main/ThermalInfoRepositoryImpl.kt` — 2 `MagicNumber` violations (pre-existing from Phase 23)
- `libs/modules/{gpu,cpu}/...macosMain` and `libs/modules/thermal/...{ios,tvos,macos}Main` native compilation failures (pre-existing platform klib issues)

These 7 pre-existing detekt violations cause `./gradlew detekt` to fail with 7 weighted issues, but none are in files touched by Plan 08.

## Verification

| Check | Result |
|-------|--------|
| `./gradlew :libs:ui:kmm:testDebugUnitTest --tests "...PerfettoExporterEventTest"` | 5/5 passing |
| `./gradlew :libs:ui:kmm:testDebugUnitTest` | 42 passing, 0 failing |
| `./gradlew :libs:engine:jvmTest` | 41 passing, 0 failing |
| `./gradlew :libs:ui:kmm:assembleDebug` | BUILD SUCCESSFUL |
| `./gradlew detekt` (my files) | 0 new violations; 7 pre-existing thermal violations |
| `grep -c '@Ignore' PerfettoExporterEventTest.kt` | 0 |

## Self-Check: PASSED

- [x] `libs/ui/kmm/src/commonMain/.../RecordedSample.kt` — contains `const val EVENTS = 9`, NOT in ALL — VERIFIED
- [x] `libs/ui/kmm/src/androidMain/.../PerfettoExporter.kt` — contains `EventRecord` import, `events: List<EventRecord>`, `= emptyList()`, `Custom Events`, `Tracks.EVENTS`, `TRACK_EVENT_NAME_FIELD` — VERIFIED
- [x] `libs/ui/kmm/src/androidMain/.../RecordingManager.kt` — contains `EventRecord` import, `events: List<EventRecord>`, `PerfettoExporter.export(snapshot, events)` — VERIFIED
- [x] `libs/ui/kmm/src/androidMain/.../KamperUiRepository.kt` — contains `Kamper` import, `drainEvents()` (2 calls), `recordingManager.exportTrace(events)` — VERIFIED
- [x] `libs/ui/kmm/src/androidUnitTest/.../PerfettoExporterEventTest.kt` — 0 `@Ignore` annotations, 5 test functions present — VERIFIED
- [x] `libs/engine/src/commonMain/.../EventRecord.kt` — `data class EventRecord` (public) — VERIFIED
- [x] `libs/engine/src/commonMain/.../Engine.kt` — `fun drainEvents()` (public) — VERIFIED
- [x] Commits 1c6b3be, e5a94ec, bcc8065 exist in git log — VERIFIED
- [x] All 5 PerfettoExporterEventTest tests pass — VERIFIED
- [x] 42 total UI tests pass — VERIFIED
- [x] 41 engine tests pass — VERIFIED
