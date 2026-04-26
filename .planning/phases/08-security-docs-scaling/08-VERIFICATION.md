---
phase: 08-security-docs-scaling
verified: 2026-04-26T15:30:00Z
status: human_needed
score: 3/3 must-haves verified
overrides_applied: 0
re_verification:
  previous_status: gaps_found
  previous_score: 0/3
  gaps_closed:
    - "SECURITY.md exists with vulnerability reporting policy — created at repo root by 08-04 (commit bd985ef)"
    - "All public API types have KDoc documentation — 15 Config types documented by 08-04 (commit ecc3936)"
    - "CAPACITY.md documents known scaling limits per module — created at repo root by 08-04 (commit bd985ef)"
    - "README IssuesModule example corrected (CR-01) — 08-05 (commit adbe503)"
    - "README IssueInfo listener example corrected (CR-02) — 08-05 (commit adbe503)"
    - "All 15 object Builder converted to class Builder (CR-03) — 08-05 (commit 2325e09)"
    - "RecordingManager.recordingBuffer synchronized under bufferLock (CR-04) — 08-05 (commit 02d69be)"
  gaps_remaining: []
  regressions: []
deferred: []
human_verification:
  - test: "Manual Perfetto gzip-validity verification (SCALE-02)"
    expected: "gunzip -t kamper_*.perfetto-trace.gz exits 0; file extension is .perfetto-trace.gz; share sheet appears with application/gzip MIME type; extracted file is non-empty"
    why_human: "Requires Android device/emulator with FileProvider configured; Plan 03 Task 3 was auto-approved (skip_checkpoints: true) without a real device run. The streaming code path (GZIPOutputStream + .use{} + PerfettoExporter.exportToFile) is wired correctly in code, but the end-to-end gzip validity can only be confirmed on a physical device."
---

# Phase 8: Security, Docs & Scaling Verification Report

**Phase Goal:** Document Kamper's security model, implement missing capacity controls, and ship streaming Perfetto export — close all SEC-01/DOC-02 gaps, wire IssuesConfig.maxStoredIssues, and fix the code review findings from 08-REVIEW.md.
**Verified:** 2026-04-26T15:30:00Z
**Status:** human_needed
**Re-verification:** Yes — after gap closure plans 08-04 and 08-05

## Goal Achievement

### Observable Truths (ROADMAP Phase 8 Success Criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | SECURITY.md exists with vulnerability reporting policy | VERIFIED | `test -f SECURITY.md` exits 0; contains `sidali.mellouk@zattoo.com`, `## Reporting a Vulnerability`, `## Response Timeline`, `## Supported Versions`, `## Security-Relevant Configuration Notes`, `kamper_ui_prefs`, `FLAG_DEBUGGABLE`, `tools:replace="android:enabled"`, `EncryptedSharedPreferences` |
| 2 | All public API types have KDoc documentation | VERIFIED | 15/15 public Config types have class-level KDoc: IssuesConfig + 7 sub-configs (IssuesConfig.kt has 8 `/**` openers), CpuConfig, NetworkConfig, FpsConfig, JankConfig, GcConfig, ThermalConfig, KamperConfig — each confirmed by content grep |
| 3 | CAPACITY.md documents known scaling limits per module | VERIFIED | `test -f CAPACITY.md` exits 0; contains `## IssuesModule`, `## RecordingManager`, `maxStoredIssues`, `DEFAULT_MAX_RECORDING_SAMPLES`, `4_200`, `200 KB`, `100 KB`, `FIFO` |

**Score:** 3/3 truths verified

### Additional Must-Haves from Plan Frontmatter (Phase Goal Context)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 4 | IssuesConfig.maxStoredIssues wired into FIFO cap enforcement (SCALE-01) | VERIFIED | `IssuesWatcher.kt`: `ArrayDeque<Issue>` accumulator, `IssuesLock.withLock { accumulator.removeFirst(); DroppedIssueEvent(...) }` pattern; 7 platform Module.kt files pass `config = config`; 6 IssuesWatcherTest tests pass |
| 5 | DroppedIssueEvent callback delivered to consumers on cap overflow (SCALE-01) | VERIFIED | `droppedEvent?.let { config.onDroppedIssue?.invoke(it) }` outside lock in IssuesWatcher.kt; IssuesConfig contains `val onDroppedIssue: ((DroppedIssueEvent) -> Unit)? = null` |
| 6 | PerfettoExporter streams to GZIPOutputStream without full ByteArray buffering (SCALE-02) | VERIFIED | `androidMain/PerfettoExporter.kt` exists; `GZIPOutputStream(out.buffered()).use` present; no `mutableListOf<Byte>`; commonMain version correctly removed |
| 7 | KamperPanelActivity shares `.perfetto-trace.gz` file via streaming path (SCALE-02) | VERIFIED | `.perfetto-trace.gz` in filename, `FileOutputStream(file).use { fos -> repo.exportTraceToFile(fos) }`, `application/gzip` MIME type, `recordingSampleCount.value == 0` early-exit guard |
| 8 | README IssuesModule example compiles (CR-01 closed) | VERIFIED | No nonexistent properties (`slowSpanEnabled`, `crashEnabled`, etc.) found in README; `context   = context`, `anr       = AnrConfig(isEnabled = true)`, `val issue = info.issue` all present |
| 9 | README IssueInfo listener uses `info.issue.X` not `info.X` (CR-02 closed) | VERIFIED | `val issue = info.issue` present; `grep -E "info\.(severity\|type\|message)" README.md` returns 0 matches |
| 10 | Every public Config Builder is `class Builder` not `object Builder` (CR-03 closed) | VERIFIED | `grep -rn "object Builder" kamper --include='*.kt' \| grep -v "//"` returns 0; `class Builder` count = 16; `Builder().apply` count = 47 |
| 11 | RecordingManager.recordingBuffer synchronized under `bufferLock` (CR-04 closed) | VERIFIED | `private val bufferLock = Any()` present; 5 `synchronized(bufferLock)` occurrences; snapshot pattern in `exportTrace`/`exportTraceToFile` confirmed |

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `SECURITY.md` | Vulnerability disclosure policy at repo root | VERIFIED | 98 lines; all required sections present |
| `CAPACITY.md` | Documented scaling limits per module | VERIFIED | 86 lines; IssuesModule + RecordingManager + CPU ring buffer entries |
| `kamper/modules/issues/src/commonMain/kotlin/com/smellouk/kamper/issues/IssuesConfig.kt` | KDoc on IssuesConfig + 7 sub-config data classes | VERIFIED | 8 `/**` openers; all 8 KDoc content strings confirmed |
| `kamper/modules/cpu/src/commonMain/kotlin/com/smellouk/kamper/cpu/CpuConfig.kt` | KDoc on CpuConfig | VERIFIED | Contains `* Configuration for the CPU monitoring module` |
| `kamper/modules/network/src/commonMain/kotlin/com/smellouk/kamper/network/NetworkConfig.kt` | KDoc on NetworkConfig | VERIFIED | Contains `* Configuration for the network monitoring module` |
| `kamper/modules/fps/src/commonMain/kotlin/com/smellouk/kamper/fps/FpsConfig.kt` | KDoc on FpsConfig | VERIFIED | Contains `* Configuration for the FPS monitoring module` |
| `kamper/modules/jank/src/commonMain/kotlin/com/smellouk/kamper/jank/JankConfig.kt` | KDoc on JankConfig | VERIFIED | Contains `* Configuration for the jank detection module` |
| `kamper/modules/gc/src/commonMain/kotlin/com/smellouk/kamper/gc/GcConfig.kt` | KDoc on GcConfig | VERIFIED | Contains `* Configuration for the garbage collection (GC) monitoring module` |
| `kamper/modules/thermal/src/commonMain/kotlin/com/smellouk/kamper/thermal/ThermalConfig.kt` | KDoc on ThermalConfig | VERIFIED | Contains `* Configuration for the thermal monitoring module` |
| `kamper/engine/src/commonMain/kotlin/com.smellouk.kamper/KamperConfig.kt` | KDoc on KamperConfig | VERIFIED | Contains `* Top-level configuration for the Kamper engine` |
| `kamper/modules/issues/src/commonMain/kotlin/com/smellouk/kamper/issues/DroppedIssueEvent.kt` | data class DroppedIssueEvent (unchanged) | VERIFIED | KDoc present from Plan 02; `data class DroppedIssueEvent(val droppedIssue: Issue, val totalDropped: Int)` |
| `kamper/modules/issues/src/commonMain/kotlin/com/smellouk/kamper/issues/IssuesWatcher.kt` | FIFO cap enforcement with IssuesLock | VERIFIED | `ArrayDeque<Issue>`, `IssuesLock.withLock`, FIFO `removeFirst()`, callback outside lock |
| `kamper/modules/issues/src/commonMain/kotlin/com/smellouk/kamper/issues/IssuesLock.kt` | expect/actual lock for KMP | VERIFIED | 8 files: 1 expect + 7 actuals (android, jvm, ios, macos, tvos, js, wasmJs) |
| `kamper/modules/issues/src/commonTest/kotlin/com/smellouk/kamper/issues/IssuesWatcherTest.kt` | 6 unit tests for cap enforcement | VERIFIED | 6 `@Test` functions; FakeIssueDetector helper |
| `kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/PerfettoExporter.kt` | Streaming ProtoWriter + exportToFile + GZIPOutputStream | VERIFIED | In androidMain (moved from commonMain); `GZIPOutputStream(out.buffered()).use`; no `mutableListOf<Byte>` |
| `kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/RecordingManager.kt` | bufferLock + 5 synchronized blocks | VERIFIED | `private val bufferLock = Any()`; 5 `synchronized(bufferLock)` occurrences; snapshot pattern outside lock |
| `kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/KamperUiRepository.kt` | exportTraceToFile (Android-only) | VERIFIED | `fun exportTraceToFile(out: OutputStream)` delegates to `recordingManager.exportTraceToFile(out)` |
| `kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/KamperPanelActivity.kt` | sharePerfettoTrace() streaming gzip | VERIFIED | `.perfetto-trace.gz`, `FileOutputStream(file).use`, `repo.exportTraceToFile(fos)`, `application/gzip` |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `CrashDetector.start()` | `android.util.Log.w` | `if (previousHandler != null) { val handlerName = ...; Log.w(TAG, "CrashDetector: replacing existing UncaughtExceptionHandler: $handlerName") }` | WIRED | Confirmed in CrashDetector.kt androidMain |
| `IssueDetector.start lambda` | `IssuesConfig.onDroppedIssue` | `droppedEvent?.let { config.onDroppedIssue?.invoke(it) }` after `lock.withLock` closes | WIRED | Callback-outside-lock discipline confirmed in IssuesWatcher.kt line 38 |
| `IssuesModule(...)` | `IssuesWatcher(config = config)` | `IssuesConfig.Builder().apply(builder).build()` + `IssuesWatcher(detectors = ..., config = config)` in all 7 platform Module.kt | WIRED | All 7 platforms confirmed |
| `KamperPanelActivity.sharePerfettoTrace()` | `GZIPOutputStream → .perfetto-trace.gz` | `FileOutputStream(file).use { fos -> repo.exportTraceToFile(fos) }` → `recordingManager.exportTraceToFile(out)` → `PerfettoExporter.exportToFile(snapshot, out)` → `GZIPOutputStream(out.buffered()).use` | WIRED | Full chain confirmed; early-exit guard `recordingSampleCount.value == 0` present |
| `README.md IssuesModule example` | Real public API | `IssuesModule(context = context, anr = AnrConfig(...), slowStart = SlowStartConfig(...)) { slowSpan = SlowSpanConfig(...) ... }` + `val issue = info.issue` | WIRED | All nonexistent identifiers removed |
| `Config.Builder().apply` | Fresh instance per call | `object Builder` → `class Builder` in 8 files; all 47 call sites use `Builder().apply(...)` | WIRED | `grep -rn "object Builder" kamper --include='*.kt'` returns 0 |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|--------------------|--------|
| IssuesWatcher accumulator | `accumulator: ArrayDeque<Issue>` | Detector `onIssue` lambdas via `lock.withLock { accumulator.addLast(issue) }` | Yes — real issues from detectors | FLOWING |
| DroppedIssueEvent callback | `droppedEvent: DroppedIssueEvent?` | `accumulator.removeFirst()` + `totalDropped += 1` under lock | Yes — actual dropped issue + monotonic counter | FLOWING |
| RecordingManager streaming export | `snapshot: List<RecordedSample>` | `synchronized(bufferLock) { recordingBuffer.toList() }` then `PerfettoExporter.exportToFile(snapshot, out)` | Yes — real recorded samples under lock, serialized outside | FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| SECURITY.md all required fields | `grep -c "sidali.mellouk\|Reporting a Vulnerability\|Response Timeline\|Supported Versions\|Security-Relevant" SECURITY.md` | 5/5 grepped 1 each | PASS |
| CAPACITY.md all required fields | `grep -c "IssuesModule\|RecordingManager\|maxStoredIssues\|4_200\|FIFO" CAPACITY.md` | 5/5 each ≥ 1 | PASS |
| KDoc on all 8 Config files | `grep -c "^/\*\*$" <each file>` | All 8 files return ≥ 1 | PASS |
| IssuesConfig.kt has 8 KDoc blocks | `grep -c "^/\*\*$" IssuesConfig.kt` | Returns 8 | PASS |
| README Security Considerations position | `awk '/^## /{print NR": "$0}' README.md \| grep -E "Lifecycle\|Security\|How-tos"` | Line 355: Lifecycle, Line 365: Security Considerations, Line 407: How-tos | PASS |
| No object Builder in codebase | `grep -rn "object Builder" kamper --include='*.kt' \| grep -v "//"` | 0 matches | PASS |
| Builder().apply count ≥ 44 | `grep -rE "\.Builder\(\)\.apply" kamper --include='*.kt' \| wc -l` | 47 matches | PASS |
| RecordingManager bufferLock count ≥ 5 | `grep -c "synchronized(bufferLock)" RecordingManager.kt` | 5 | PASS |
| README nonexistent identifiers removed | `grep -c "slowSpanEnabled\|droppedFramesEnabled\|crashEnabled..." README.md` | 0 | PASS |
| README `info.severity` removed | `grep -cE "info\.(severity\|type\|message)" README.md` | 0 | PASS |
| Perfetto gzip validity (SCALE-02) | Requires device — see human verification | Not run | SKIP (human needed) |

### Requirements Coverage

| Requirement | Source Plans | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| SEC-01 (ROADMAP Phase 8) | 08-04 | SECURITY.md with vulnerability reporting policy | SATISFIED | File exists at repo root with all required sections and contact email |
| DOC-02 (ROADMAP Phase 8) | 08-04 | All public API Config types have KDoc documentation | SATISFIED | 15/15 public Config types confirmed with class-level KDoc |
| Phase 8 SC3 (CAPACITY.md) | 08-04 | CAPACITY.md documents known scaling limits per module | SATISFIED | File exists at repo root with IssuesModule + RecordingManager + CPU ring buffer entries |
| SCALE-01 (plan-level) | 08-02 | IssuesWatcher FIFO cap + DroppedIssueEvent callback | SATISFIED | IssuesWatcher enforces cap; IssuesLock expect/actual for KMP thread safety; 6 tests pass |
| SCALE-02 (plan-level) | 08-03 | Streaming gzip Perfetto export | SATISFIED (pending human) | Code path wired; gzip validity requires device test |
| CR-01 (08-REVIEW.md) | 08-05 | README IssuesModule example corrected | SATISFIED | Nonexistent properties removed; real API used |
| CR-02 (08-REVIEW.md) | 08-05 | README IssueInfo listener corrected | SATISFIED | `val issue = info.issue` pattern; `info.severity` etc. gone |
| CR-03 (08-REVIEW.md) | 08-05 | All Builder singletons converted to class Builder | SATISFIED | 0 `object Builder` remain; 47 `Builder().apply` call sites |
| CR-04 (08-REVIEW.md) | 08-05 | RecordingManager buffer synchronized | SATISFIED | `bufferLock` + 5 `synchronized` blocks; snapshot pattern outside lock |

### Anti-Patterns Found

| File | Pattern | Severity | Impact |
|------|---------|----------|--------|
| None found | — | — | All four prior blockers (CR-01 through CR-04) closed. No new anti-patterns detected in key modified files. |

### Human Verification Required

#### 1. Perfetto Gzip Validity End-to-End (SCALE-02)

**Test:** Install debug APK on Android device; open Kamper panel; tap Start Recording; wait 60+ seconds; tap Stop; tap Export trace; share to Files or Drive; pull via `adb pull /sdcard/Download/kamper_*.perfetto-trace.gz`; run:
```
gunzip -t kamper_*.perfetto-trace.gz
file kamper_*.perfetto-trace.gz
```
**Expected:** `gunzip -t` exits 0; `file` reports "gzip compressed data"; share sheet shows `.perfetto-trace.gz` filename with `application/gzip` MIME type; extracted protobuf is non-empty. Optional: open extracted file in https://ui.perfetto.dev and confirm counter tracks render.

**Why human:** The streaming code path (`GZIPOutputStream(..).use` + `PerfettoExporter.exportToFile`) is correctly wired in code (VERIFIED), but the gzip footer validity (CRC32 + ISIZE written) and the FileProvider share flow can only be confirmed end-to-end on a real Android device. Plan 03 Task 3 was auto-approved (skip_checkpoints: true) by the orchestrator without a physical device run.

---

## Gaps Summary

No blocking gaps remain. All three ROADMAP Phase 8 success criteria are now satisfied in the codebase:
1. SECURITY.md created at repo root with full vulnerability disclosure policy
2. All 15 public Config types have class-level KDoc
3. CAPACITY.md created at repo root with per-module scaling limits

All four critical code-review findings from 08-REVIEW.md are closed:
- CR-01 + CR-02: README examples corrected to real public API
- CR-03: All 15 `object Builder` → `class Builder`; 47 call sites updated
- CR-04: RecordingManager buffer synchronized with `bufferLock` + 5 `synchronized` blocks

**One item requires human testing before the phase can be fully closed:** The Perfetto streaming gzip export end-to-end validity on a real Android device (SCALE-02 Task 3 checkpoint was auto-approved without a device run).

---

_Verified: 2026-04-26T15:30:00Z_
_Verifier: Claude (gsd-verifier)_
