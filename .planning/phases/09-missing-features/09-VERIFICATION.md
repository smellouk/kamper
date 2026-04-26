---
phase: 09-missing-features
verified: 2026-04-26T15:03:00Z
status: human_needed
score: 11/13 must-haves verified
overrides_applied: 0
human_verification:
  - test: "Run connected Android instrumented tests for KamperConfigReceiverTest"
    expected: "All 3 tests (enabled=true, enabled=false, missing-extra) pass on a real device"
    why_human: "No device in executor environment; only compileDebugAndroidTestKotlin was verified. Plan acceptance criteria explicitly require device execution."
  - test: "Run connected Android instrumented tests for CpuInfoRepositoryImplUnsupportedTest"
    expected: "All 3 tests (UNSUPPORTED branch, cache no-retry, Pitfall-6 guard) pass on a real device"
    why_human: "No device in executor environment; only compileDebugAndroidTestKotlin was verified."
  - test: "Visual verification of UNSUPPORTED tile rendering (CPU and Thermal) in Kamper UI overlay"
    expected: "CPU tile shows 'Unsupported' in SUBTEXT gray (no blue accent), progress bar shows only the SURFACE track, no sparkline. Tile returns to active state after next valid CPU sample. Same check for Thermal."
    why_human: "Task 3 of 09-04 was auto-approved by the executor — physical device visual check explicitly acknowledged as pending. Plan 09-04 has a blocking checkpoint:human-verify gate that was bypassed."
---

# Phase 09: Missing Features Verification Report

**Phase Goal:** Implement missing features — UNSUPPORTED sentinel constants across all Info types, ADB-controllable BroadcastReceiver, CPU unsupported detection, UI surface for unsupported metrics, Watcher timestamp plumbing, and Engine.validate() health-check API.
**Verified:** 2026-04-26T15:03:00Z
**Status:** human_needed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Every Info subclass (CpuInfo, FpsInfo, MemoryInfo, NetworkInfo, GcInfo, JankInfo, ThermalInfo, IssueInfo) exposes a public companion val UNSUPPORTED with -2 sentinel values (or -2L, -2F, -2f as appropriate), distinct from INVALID | ✓ VERIFIED | All 8 subclasses confirmed in codebase: CpuInfo(-2.0×5), FpsInfo(-2), MemoryInfo+HeapMemoryInfo+PssInfo+RamInfo, NetworkInfo(-2F×4), GcInfo(-2L×4), JankInfo(-2,-2f,-2L), ThermalInfo(ThermalState.UNSUPPORTED,false), IssueInfo(Issue.UNSUPPORTED) |
| 2 | Info base interface gains a companion val UNSUPPORTED (anonymous-object pattern, mirrors INVALID) | ✓ VERIFIED | `val UNSUPPORTED = object : Info {}` confirmed in Info.kt line 6 |
| 3 | ThermalState enum gains UNSUPPORTED entry distinct from UNKNOWN | ✓ VERIFIED | ThermalState.kt line 12 contains `UNSUPPORTED` entry |
| 4 | Issue.UNSUPPORTED has timestampMs=-2L (distinct from INVALID's -1L) and id="unsupported" | ✓ VERIFIED | Issue.kt confirmed: id="unsupported", timestampMs=-2L |
| 5 | NetworkInfo.NOT_SUPPORTED is retained unchanged alongside new UNSUPPORTED | ✓ VERIFIED | NetworkInfo.kt line 15 retains `val NOT_SUPPORTED = NetworkInfo(-100F, ...)` |
| 6 | Performance base class gains @Volatile open var lastValidSampleAt: Long = 0L AND @Volatile open var installedAt: Long = 0L | ✓ VERIFIED | Performance.kt lines 18 and 24 confirmed; fields changed to `open` (non-internal) for cross-module access from Engine.validate() |
| 7 | KamperConfigReceiver class exists with BroadcastReceiver delegation, safe default (getBooleanExtra("enabled", true)), and is declared in AndroidManifest.xml with android:exported="false" | ✓ VERIFIED | KamperConfigReceiver.kt exists with exact body; AndroidManifest.xml line 26-29 contains receiver with CONFIGURE action and exported=false |
| 8 | KamperConfigReceiverTest has 3 tests covering enabled=true, enabled=false, and missing-extra branches; tests compile | ✓ VERIFIED (compile only) | Test file confirmed with 3 @Test methods using mockkObject(Kamper) and verify(exactly=1) assertions; compile exits 0. Device execution not verified — human needed |
| 9 | CpuInfoRepositoryImpl returns CpuInfo.UNSUPPORTED when both /proc/stat and shell fallback are unavailable, with one-time platformSupported cache | ✓ VERIFIED | CpuInfoRepositoryImpl.kt has: platformSupported field (line 17), early-return guard (line 21), both-sources-INVALID branch (lines 43-45), and platformSupported=true terminal state (line 49-50). CpuInfoRepositoryImplUnsupportedTest exists with 3 tests. Device execution not verified — human needed |
| 10 | KamperUiState gains cpuUnsupported and thermalUnsupported boolean fields; ModuleLifecycleManager listeners detect UNSUPPORTED and self-correct on valid samples; MetricCard gains unsupported parameter with gray-tile rendering; CPU/Thermal call sites in ActivityTab updated | ✓ VERIFIED | All 4 files confirmed: KamperUiState.kt lines 26-27, ModuleLifecycleManager.kt lines 77-88 and 139-148, PanelComponents.kt line 767, ActivityTab.kt lines 48-57 and 123-144. VISUAL rendering on device — human needed |
| 11 | currentApiTimeMs() internal expect/actual exists in api module with 7 platform actuals; IWatcher.startWatching gains optional onSampleDelivered parameter; Watcher invokes callback after listener dispatch; Performance.start() sets installedAt on first call and wires callback to update lastValidSampleAt | ✓ VERIFIED | 8 PlatformTime.kt files confirmed; IWatcher.kt has onSampleDelivered param; Watcher.kt invokes it inside withContext(mainDispatcher) after forEach; Performance.start() has installedAt guard and callback wiring |
| 12 | ValidationInfo data class exists in engine commonMain with EMPTY/INVALID companions; Engine.init{} seeds mapListeners[ValidationInfo::class]; Engine.clear() re-seeds; Engine.validate() computes elapsed using lastValidSampleAt/installedAt anchor, returns problem strings, and emits ValidationInfo to listeners | ✓ VERIFIED | ValidationInfo.kt confirmed; Engine.kt lines 35-40 (init), 64 (clear re-seed), 133-155 (validate() with installedAt anchor, VALIDATION_THRESHOLD_MS=10_000L, listener emission, return) |
| 13 | engineCurrentTimeMs() internal expect/actual exists in engine module with 7 platform actuals | ✓ VERIFIED | EnginePlatformTime.kt confirmed in commonMain and all 7 platform actuals (android, jvm, ios, macos, tvos, js, wasmJs) |

**Score:** 11/13 truths verified (2 are VERIFIED at compile level, pending device execution; 1 visual rendering pending human)

Note: Truths 8, 9, and 10 are classified as VERIFIED for their code-level assertions; the human_needed items are specifically the device-execution and visual-rendering aspects that cannot be verified programmatically.

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `kamper/api/src/commonMain/kotlin/com/smellouk/kamper/api/Info.kt` | Info.UNSUPPORTED companion | ✓ VERIFIED | Line 6: `val UNSUPPORTED = object : Info {}` |
| `kamper/api/src/commonMain/kotlin/com/smellouk/kamper/api/Performance.kt` | lastValidSampleAt + installedAt fields | ✓ VERIFIED | Lines 18, 24 — @Volatile open var, both fields present and wired in start() |
| `kamper/modules/cpu/src/commonMain/kotlin/com/smellouk/kamper/cpu/CpuInfo.kt` | CpuInfo.UNSUPPORTED = CpuInfo(-2.0 x5) | ✓ VERIFIED | Line 14 confirmed |
| `kamper/modules/thermal/src/commonMain/kotlin/com/smellouk/kamper/thermal/ThermalState.kt` | ThermalState.UNSUPPORTED enum entry | ✓ VERIFIED | Line 12 confirmed |
| `kamper/modules/issues/src/commonMain/kotlin/com/smellouk/kamper/issues/Issue.kt` | Issue.UNSUPPORTED with timestampMs=-2L | ✓ VERIFIED | id="unsupported", timestampMs=-2L confirmed |
| `kamper/modules/cpu/src/commonTest/kotlin/com/smellouk/kamper/cpu/CpuInfoUnsupportedTest.kt` | 3 @Test methods for CpuInfo.UNSUPPORTED | ✓ VERIFIED | 3 tests confirmed; assertNotEquals, -2.0 and -1.0 assertions present |
| `kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/KamperConfigReceiver.kt` | BroadcastReceiver with safe default | ✓ VERIFIED | Class exists, getBooleanExtra("enabled", true), Kamper.start()/stop() delegation |
| `kamper/ui/android/src/androidMain/AndroidManifest.xml` | `<receiver>` with CONFIGURE action, exported=false | ✓ VERIFIED | Lines 26-29 confirmed |
| `kamper/ui/android/src/androidTest/kotlin/com/smellouk/kamper/ui/KamperConfigReceiverTest.kt` | 3 tests with mockkObject | ✓ VERIFIED (compile) | 3 @Test methods, mockkObject(Kamper), verify(exactly=1) — device not available |
| `kamper/modules/cpu/src/androidMain/kotlin/com/smellouk/kamper/cpu/repository/CpuInfoRepositoryImpl.kt` | platformSupported cache + UNSUPPORTED return | ✓ VERIFIED | platformSupported field, early-return guard, both-sources-INVALID branch, terminal true state |
| `kamper/modules/cpu/src/androidTest/kotlin/com/smellouk/kamper/cpu/repository/CpuInfoRepositoryImplUnsupportedTest.kt` | 3 tests covering UNSUPPORTED + cache + Pitfall 6 | ✓ VERIFIED (compile) | 3 @Test methods, verify(exactly=0)/{1}/{2} assertions confirmed |
| `kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/KamperUiState.kt` | cpuUnsupported + thermalUnsupported fields | ✓ VERIFIED | Lines 26-27 confirmed |
| `kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/ModuleLifecycleManager.kt` | UNSUPPORTED detection + self-correct in listeners | ✓ VERIFIED | cpuListener and thermalListener both have UNSUPPORTED guards and false-reset on valid samples |
| `kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/PanelComponents.kt` | MetricCard unsupported param + gray tint | ✓ VERIFIED | Line 767 param, lines ~770-775 three-state when for tint/textColor |
| `kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/ActivityTab.kt` | isCpuUnsupported + isThermalUnsupported call sites + ThermalState.UNSUPPORTED arm | ✓ VERIFIED | Lines 48-57, 123-144, 133 confirmed |
| `kamper/api/src/commonMain/kotlin/com/smellouk/kamper/api/PlatformTime.kt` | internal expect fun currentApiTimeMs(): Long | ✓ VERIFIED | Confirmed present |
| `kamper/api/src/{android,jvm,ios,macos,tvos,js,wasmJs}Main/.../PlatformTime.kt` | 7 platform actuals | ✓ VERIFIED | All 7 files confirmed present |
| `kamper/api/src/commonMain/kotlin/com/smellouk/kamper/api/IWatcher.kt` | onSampleDelivered parameter | ✓ VERIFIED | Line 7 confirmed |
| `kamper/api/src/commonMain/kotlin/com/smellouk/kamper/api/Watcher.kt` | onSampleDelivered?.invoke() after listener dispatch | ✓ VERIFIED | Line 43 confirmed inside withContext(mainDispatcher) |
| `kamper/api/src/commonTest/kotlin/com/smellouk/kamper/api/WatcherTest.kt` | 5 @Test methods (3 original + 2 new) | ✓ VERIFIED | Confirmed: 5 tests with callbackInvocations counter, assertions at 2 and 0 |
| `kamper/engine/src/commonMain/kotlin/com.smellouk.kamper/ValidationInfo.kt` | data class ValidationInfo with EMPTY/INVALID | ✓ VERIFIED | Confirmed with both companions = ValidationInfo(emptyList()) |
| `kamper/engine/src/commonMain/kotlin/com.smellouk.kamper/Engine.kt` | init{} seed + clear() re-seed + validate() method | ✓ VERIFIED | Lines 35-40, 64, 133-155 confirmed; installedAt anchor, listener emission, 10s threshold |
| `kamper/engine/src/commonMain/kotlin/com.smellouk.kamper/EnginePlatformTime.kt` | internal expect fun engineCurrentTimeMs(): Long | ✓ VERIFIED | Confirmed present |
| `kamper/engine/src/{android,jvm,ios,macos,tvos,js,wasmJs}Main/.../EnginePlatformTime.kt` | 7 platform actuals | ✓ VERIFIED | All 7 files confirmed present |
| `kamper/engine/src/commonTest/kotlin/com/smellouk/kamper/EngineValidateTest.kt` | 6 @Test methods replacing stub | ✓ VERIFIED | 6 tests confirmed: empty base, problem, regression guard (installedAt-recent), Pitfall-1, D-12, A5 |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| Info.UNSUPPORTED (commonMain api) | All 8 Info subclasses' UNSUPPORTED constants | INVALID anonymous-object pattern extended | ✓ WIRED | All 8 subclasses confirmed with distinct -2 sentinels |
| Performance.lastValidSampleAt | Watcher onSampleDelivered → Performance.start() callback | @Volatile open var; callback = { lastValidSampleAt = currentApiTimeMs() } | ✓ WIRED | Performance.kt line 46: `onSampleDelivered = { lastValidSampleAt = currentApiTimeMs() }` |
| Performance.installedAt | Engine.validate() elapsed anchor | first-start guard in start(); read in Engine.validate() | ✓ WIRED | Engine.kt lines 137-140: `val installed = performance.installedAt` used in elapsed `when` |
| CpuInfoRepositoryImpl platformSupported=false | getInfo() returns CpuInfo.UNSUPPORTED (cached) | early-return guard at top of getInfo() | ✓ WIRED | `if (platformSupported == false) return CpuInfo.UNSUPPORTED` line 21 |
| ADB shell intent → KamperConfigReceiver.onReceive() | Kamper.start() / Kamper.stop() | AndroidManifest <receiver> with CONFIGURE action + getBooleanExtra routing | ✓ WIRED | Manifest entry confirmed; onReceive body routes correctly |
| KamperUiState.cpuUnsupported=true | MetricCard renders "Unsupported" gray tile | ActivityTab derivedStateOf → MetricCard(unsupported=isCpuUnsupported, current="Unsupported", fraction=0f, history=emptyList()) | ✓ WIRED | ActivityTab lines 48-57 confirmed |
| ThermalState.UNSUPPORTED (new enum entry) | ActivityTab thermal when() compiles | ThermalState.UNSUPPORTED -> 0f arm in ActivityTab | ✓ WIRED | ActivityTab line 133 confirmed |
| Engine.validate() | ValidationInfo listeners + caller return | mapListeners[ValidationInfo::class] iteration + return problems | ✓ WIRED | Engine.kt lines 151-154 confirmed |
| Engine.init {} | mapListeners[ValidationInfo::class] seeded before any module install | init block with mutableListOf() assignment | ✓ WIRED | Engine.kt lines 35-40 confirmed |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|--------------------|--------|
| ActivityTab CPU MetricCard | cpuUnsupported from KamperUiState | ModuleLifecycleManager.cpuListener detects CpuInfo.UNSUPPORTED from CpuInfoRepositoryImpl | Yes — real sentinel detection from actual repository probe result | ✓ FLOWING |
| Engine.validate() | problems list | performanceList.mapNotNull { } using lastValidSampleAt/installedAt timestamps from actual Performance.start() | Yes — real system time millis from currentApiTimeMs() actuals | ✓ FLOWING |
| WatcherTest callback counter | callbackInvocations | real lambda invoked by Watcher after listener dispatch | Yes — real counter | ✓ FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| CpuInfo.UNSUPPORTED != CpuInfo.INVALID | grep confirms -2.0 vs -1.0 sentinels | Structurally distinct confirmed | ✓ PASS |
| Info.UNSUPPORTED companion exists | grep on Info.kt | val UNSUPPORTED = object : Info {} at line 6 | ✓ PASS |
| Engine.validate() validate method exists | grep on Engine.kt | fun validate(): List<String> at line 133 | ✓ PASS |
| KamperConfigReceiver delegates to Kamper.start/stop | grep on receiver file | if (enabled) Kamper.start() else Kamper.stop() confirmed | ✓ PASS |
| KamperConfigReceiverTest instrumented execution | connectedDebugAndroidTest | SKIP — no device available | ? SKIP |
| CpuInfoRepositoryImplUnsupportedTest instrumented execution | connectedDebugAndroidTest | SKIP — no device available | ? SKIP |
| UNSUPPORTED UI tile visual rendering | Physical device inspection | SKIP — device required | ? SKIP |

### Requirements Coverage

The ROADMAP declares `Requirements: FEAT-01` for Phase 9, with two Success Criteria:

| Success Criterion | Status | Evidence |
|-------------------|--------|---------|
| SC1: All previously deferred platform features are implemented or explicitly out-of-scoped | ✓ SATISFIED | The 09-CONTEXT.md defines the 3 deferred features (FEAT-01 UNSUPPORTED sentinels + CPU probe, FEAT-02 BroadcastReceiver, FEAT-03 Engine.validate()). All three are implemented in code. The ROADMAP plan names differ from actual plan content (naming mismatch in ROADMAP, not a gap in implementation). |
| SC2: Feature parity between Android and JVM where applicable | ✓ SATISFIED | Engine.validate() is in commonMain (all platforms). currentApiTimeMs() and engineCurrentTimeMs() have JVM actuals. UNSUPPORTED sentinels are in commonMain. The BroadcastReceiver is Android-only by design (platform-specific). |

Note: The ROADMAP's plan names (09-01: "Audit deferred features list", 09-02: "JVM network feature completion", etc.) do not match the actual executed plan names, but the plans' actual content fully addresses the phase goal and success criteria.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `kamper/modules/cpu/src/androidMain/kotlin/com/smellouk/kamper/cpu/repository/CpuInfoRepositoryImpl.kt` | 71 | Operator-precedence bug: `it.toLongOrNull() ?: 0L > 0L` should be `(it.toLongOrNull() ?: 0L) > 0L` (CR-01 from 09-REVIEW.md) | ⚠️ Warning | Makes /proc/stat accessibility check unreliable — may silently misclassify zero-data /proc/stat as accessible, preventing fallback to shell source. Functional bug but does not block UNSUPPORTED probe for the total-unavailability case (both sources returning INVALID). |
| `kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/ModuleLifecycleManager.kt` | 177-219 | FPS fields `fpsActive`, `fpsFrameCount`, `fpsWindowStartNanos` are plain `var` written from multiple threads (CR-02 from 09-REVIEW.md) | ⚠️ Warning | Thread-safety gap — stopFps() may not be visible to Choreographer doFrame callback, causing infinite FPS registration after monitoring is stopped. Not introduced by Phase 9 (pre-existing architectural issue) but present in Phase 9 scope files. |

Both anti-patterns are documented in the phase's own 09-REVIEW.md as critical findings. They were identified post-execution but not fixed within this phase.

### Human Verification Required

#### 1. KamperConfigReceiverTest Device Execution

**Test:** Install a debug build with Kamper UI on a connected Android device. Run `./gradlew :kamper:ui:android:connectedDebugAndroidTest --tests "com.smellouk.kamper.ui.KamperConfigReceiverTest"`.
**Expected:** All 3 tests pass — enabled=true calls Kamper.start() once, enabled=false calls Kamper.stop() once, missing extra defaults to Kamper.start() once.
**Why human:** No connected device in executor environment. The instrumented test compiles cleanly (compileDebugAndroidTestKotlin exits 0) but cannot be executed without a device.

#### 2. CpuInfoRepositoryImplUnsupportedTest Device Execution

**Test:** On a connected Android device, run `./gradlew :kamper:modules:cpu:connectedDebugAndroidTest --tests "com.smellouk.kamper.cpu.repository.CpuInfoRepositoryImplUnsupportedTest"`.
**Expected:** All 3 tests pass — UNSUPPORTED returned when both sources return CpuInfoDto.INVALID, cache prevents retrying sources, shell success does not trigger UNSUPPORTED.
**Why human:** No connected device in executor environment. Tests compile cleanly.

#### 3. Visual UNSUPPORTED Tile Rendering

**Test:** Install the sample app on a physical Android device. Force cpuUnsupported=true (via debug scaffold, Robolectric, or by running on a device where /proc/stat and shell top are both blocked). Open the Kamper overlay. Inspect the CPU MetricCard.
**Expected:**
- Tile shows "Unsupported" label in KamperTheme.SUBTEXT color (grayed, no blue accent)
- Progress bar shows only the SURFACE track (fraction=0f, no fill)
- No sparkline below the bar
- Light/dark theme both render correctly
- After un-mocking (or restart), tile returns to normal CPU blue on next valid sample (Pitfall-6 self-correct)
**Why human:** Task 3 of 09-04 was a blocking `checkpoint:human-verify` gate that was auto-approved by the executor based on assembleDebug+lintDebug passing — not physical device confirmation. The plan explicitly required human visual confirmation.

### Gaps Summary

No blocking code-level gaps found. The implementation is substantive and wired:
- All UNSUPPORTED sentinel constants exist with correct -2 sentinel values
- BroadcastReceiver exists, is registered, compiles, and routes correctly
- CPU capability probe is implemented with caching
- UI state and rendering pipeline is fully wired
- Watcher timestamp plumbing is complete
- Engine.validate() exists, uses installedAt anchor, emits ValidationInfo

The human_needed items are device-dependent test executions and a visual UI verification — programmatic verification cannot substitute for them. The anti-patterns (CR-01 operator precedence, CR-02 FPS thread-safety) are documented in the 09-REVIEW.md as issues to address; they do not block phase goal achievement but should be addressed before v1.0 shipment.

---

_Verified: 2026-04-26T15:03:00Z_
_Verifier: Claude (gsd-verifier)_
