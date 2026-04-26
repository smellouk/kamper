---
phase: 08-security-docs-scaling
plan: 03
subsystem: ui
tags: [perfetto, gzip, streaming, protobuf, android, kmp, expect-actual, file-io, scaling]

# Dependency graph
requires:
  - phase: 06-kamperuirepository-refactor-settings-tests
    provides: RecordingManager with recordingBuffer, PerfettoExporter.export() API

provides:
  - PerfettoExporter.exportToFile(samples, out: OutputStream) — streaming gzip-compressed Perfetto export (androidMain)
  - RecordingManager.exportTraceToFile(out: OutputStream) — delegates to PerfettoExporter.exportToFile
  - KamperUiRepository.exportTraceToFile(out: OutputStream) — Android-only, not in expect class
  - KamperPanelActivity.sharePerfettoTrace() — opens .perfetto-trace.gz in cacheDir, streams via Intent

affects: [09-missing-features, 10-test-coverage, 16-release-automation]

# Tech tracking
tech-stack:
  added: [java.io.GZIPOutputStream, java.io.ByteArrayOutputStream, java.io.FileOutputStream]
  patterns:
    - Streaming ProtoWriter with OutputStream constructor — outer writes stream directly, inner sub-messages buffer to ByteArrayOutputStream for length-prefix
    - Android-only method added to actual class without expect declaration — keeps commonMain free of java.io.*
    - PerfettoExporter moved from commonMain to androidMain to isolate JVM-specific APIs from Apple KMP targets

key-files:
  created:
    - kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/PerfettoExporter.kt
  modified:
    - kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/RecordingManager.kt
    - kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/KamperUiRepository.kt
    - kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/KamperPanelActivity.kt
    - kamper/ui/android/src/appleMain/kotlin/com/smellouk/kamper/ui/RecordingManager.kt

key-decisions:
  - "PerfettoExporter moved from commonMain to androidMain — java.io.* is unavailable on Apple KMP targets (compileKotlinIosArm64 confirmed failure); this was the plan's anticipated fallback"
  - "Apple RecordingManager.exportTrace() stubs to ByteArray(0) directly — removes the now-androidMain-only PerfettoExporter reference from Apple source set"
  - "exportTraceToFile placed on Android actual class only (no expect declaration) — keeps commonMain/KamperUiRepository.kt pristine"
  - "Empty-buffer guard uses recordingSampleCount.value == 0 before opening FileOutputStream — no zero-byte cache files created (T-06-16)"

patterns-established:
  - "Platform-specific JVM/Android APIs belong in androidMain, not commonMain — even when Apple stub is a no-op"
  - "Streaming ProtoWriter: outer loop writes to OutputStream directly; message() blocks buffer to ByteArrayOutputStream for length-prefix only"
  - "GZIPOutputStream(..).use { } is mandatory — .use ensures gzip footer (CRC32 + ISIZE) is written"

requirements-completed: [SCALE-02]

# Metrics
duration: 5min
completed: 2026-04-26
---

# Phase 08 Plan 03: Perfetto Streaming Gzip Export Summary

**Streaming gzip-compressed Perfetto trace export using ProtoWriter(OutputStream) + GZIPOutputStream, eliminating the 3MB transient ByteArray allocation for 10-minute recordings**

## Performance

- **Duration:** 5 min
- **Started:** 2026-04-26T12:42:23Z
- **Completed:** 2026-04-26T12:48:20Z
- **Tasks:** 2 auto + 1 checkpoint (auto-approved)
- **Files modified:** 5

## Accomplishments

- Refactored ProtoWriter from buffer-based (`mutableListOf<Byte>()`) to streaming (OutputStream constructor) — eliminates ~3MB boxed ArrayList allocation for 4200-sample recordings
- Added `PerfettoExporter.exportToFile(samples, out)` wrapping caller stream in `GZIPOutputStream(out.buffered()).use { }` — gzip footer guaranteed via `.use {}`
- Moved PerfettoExporter from commonMain to androidMain (java.io.* unavailable on Apple KMP targets) — confirmed by compileKotlinIosArm64 failure
- Updated KamperPanelActivity to stream directly to `.perfetto-trace.gz` in cacheDir with `application/gzip` MIME type
- Preserved backward-compatible `PerfettoExporter.export(): ByteArray` for in-memory callers

## Task Commits

Each task was committed atomically:

1. **Task 1: Streaming ProtoWriter + gzip PerfettoExporter** - `9297002` (feat)
2. **Task 2: Android exportTraceToFile + streaming KamperPanelActivity** - `e3ee98f` (feat)
3. **Task 3: Manual gzip-validity verification** - auto-approved checkpoint (skip_checkpoints: true)

## Files Created/Modified

- `kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/PerfettoExporter.kt` — MOVED from commonMain; now contains streaming ProtoWriter + exportToFile() with GZIPOutputStream
- `kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/RecordingManager.kt` — added `exportTraceToFile(out: OutputStream)` delegating to PerfettoExporter.exportToFile
- `kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/KamperUiRepository.kt` — added `fun exportTraceToFile(out: OutputStream)` (Android-only, no expect declaration)
- `kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/KamperPanelActivity.kt` — replaced `sharePerfettoTrace(bytes)` with `sharePerfettoTrace()` using FileOutputStream + .perfetto-trace.gz
- `kamper/ui/android/src/appleMain/kotlin/com/smellouk/kamper/ui/RecordingManager.kt` — stubbed `exportTrace()` as `ByteArray(0)` (was calling PerfettoExporter which moved to androidMain)
- `kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/PerfettoExporter.kt` — DELETED (moved to androidMain)

## Files NOT Modified Despite Being in frontmatter `files_modified`

- `kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/KamperUiRepository.kt` — unchanged (expect class stays pristine; new method is Android-only, not in expect)
- `kamper/ui/android/src/appleMain/kotlin/com/smellouk/kamper/ui/KamperUiRepository.kt` — unchanged (Apple gets no new method; Perfetto export is Android-only)

These files were listed as a precaution in the plan frontmatter. The design decision locked in the plan's `<interfaces>` section correctly predicted they would not need modification.

## Decisions Made

- PerfettoExporter moved to androidMain (not commonMain) because java.io.* is unavailable on Apple Kotlin/Native targets. This was the anticipated fallback in the plan's constraint section.
- Apple RecordingManager.exportTrace() was updated to return ByteArray(0) directly (removing the now-Android-only PerfettoExporter reference). This is consistent with the Apple KamperUiRepository already being a Perfetto stub.
- exportTraceToFile is on the Android actual class only (not in commonMain expect). Keeps commonMain free of JVM-specific APIs.
- Empty-buffer guard placed before FileOutputStream is opened — no zero-byte files created when recording hasn't started.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Moved PerfettoExporter from commonMain to androidMain**
- **Found during:** Task 1 (after initial write to commonMain)
- **Issue:** `compileKotlinIosArm64` failed with "Unresolved reference 'java'" — java.io.* not available on Apple KMP targets. The plan explicitly anticipated this in the `<interfaces>` constraint section and documented the resolution.
- **Fix:** Deleted `commonMain/PerfettoExporter.kt`, created `androidMain/PerfettoExporter.kt` with full streaming implementation. Updated `appleMain/RecordingManager.kt` to stub `exportTrace()` as `ByteArray(0)` directly.
- **Files modified:** commonMain/PerfettoExporter.kt (deleted), androidMain/PerfettoExporter.kt (created), appleMain/RecordingManager.kt (stubbed)
- **Verification:** `./gradlew :kamper:ui:android:compileDebugKotlinAndroid :kamper:ui:android:compileKotlinIosArm64 -q` — both pass
- **Committed in:** `9297002` (Task 1 commit)

**2. [Rule 2 - Architecture] exportTraceToFile added through RecordingManager chain**
- **Found during:** Task 2
- **Issue:** Plan said to add `exportTraceToFile` directly to `KamperUiRepository` with access to `recordingBuffer`, but the actual Android `KamperUiRepository` delegates to `RecordingManager` which owns `recordingBuffer` (private). The plan was written against an older snapshot where `recordingBuffer` was in `KamperUiRepository` directly.
- **Fix:** Added `exportTraceToFile(out)` to `RecordingManager` first (it owns the buffer), then surfaced it via `KamperUiRepository.exportTraceToFile(out)`. The call chain is: `KamperPanelActivity -> KamperUiRepository.exportTraceToFile -> RecordingManager.exportTraceToFile -> PerfettoExporter.exportToFile -> GZIPOutputStream -> file`.
- **Files modified:** androidMain/RecordingManager.kt, androidMain/KamperUiRepository.kt
- **Verification:** `grep -q "recordingManager.exportTraceToFile(out)"` confirms the delegation chain
- **Committed in:** `e3ee98f` (Task 2 commit)

---

**Total deviations:** 2 auto-fixed (1 blocking — Java IO not in KMP commonMain, 1 architecture — delegation chain through RecordingManager)
**Impact on plan:** Both auto-fixes necessary for correctness and consistent with the plan's explicitly documented fallback paths. No scope creep.

## Issues Encountered

- The plan's `<interfaces>` section showed `KamperUiRepository` directly owning `recordingBuffer` — the Phase 06 refactor had moved this to `RecordingManager`. The deviation was handled automatically by tracing the delegation chain.

## Known Stubs

- `appleMain/RecordingManager.exportTrace()` returns `ByteArray(0)` — intentional stub for Apple platform where Perfetto export is not implemented. This was already the effective behavior (Apple `KamperUiRepository.exportTrace()` returned `ByteArray(0)` before). The stub is correct.

## Threat Flags

None. The implementation matches the threat model:
- T-06-12 (gzip corruption): mitigated by `GZIPOutputStream(..).use { }` — footer guaranteed
- T-06-15 (Apple side invocation): mitigated at compile time — `exportTraceToFile` absent from expect class and Apple actual
- T-06-16 (empty buffer file): mitigated by `recordingSampleCount.value == 0` early return

## User Setup Required

None — no external service configuration required. The feature is wired through the existing FileProvider configuration (unchanged from pre-refactor).

## Next Phase Readiness

- SCALE-02 streaming export complete — ready for Phase 09 missing features work
- `PerfettoExporter.export(): ByteArray` preserved for any in-memory callers (no breaking changes)
- Both Android and Apple targets compile cleanly
- Manual `gunzip -t` verification was auto-approved by orchestrator (skip_checkpoints: true); recommended for next device test cycle

---
*Phase: 08-security-docs-scaling*
*Completed: 2026-04-26*
