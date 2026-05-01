---
phase: 22-manual-testing-all-demo-platforms-one-by-one
verified: 2026-05-01T15:00:00Z
status: human_needed
score: 8/9 truths verified
overrides_applied: 0
re_verification:
  previous_status: gaps_found
  previous_score: 7/9
  gaps_closed:
    - "JankViewController UNSUPPORTED sentinel (-2 → N/A): fix confirmed at HEAD (lines 85-90)"
    - "IosFpsTimer CADisplayLink: coroutine-based timer replaced with CADisplayLink in 7c6c95c — code substantive and wired"
  gaps_remaining:
    - "No human-approved iOS re-run after CADisplayLink FPS fix; FPS correctness on simulator/device unconfirmed"
  regressions: []
human_verification:
  - test: "Re-run iOS smoke test after the CADisplayLink FPS fix (commit 7c6c95c). Build with ./gradlew :demos:ios:linkDebugExecutableIosSimulatorArm64, install in simulator via assembleIosSimulatorApp task, observe FPS tab for 30+ seconds."
    expected: "FPS label shows a numeric value (e.g. 58, 60) updating every ~1 second — not stuck at '--'. CPU and Memory showing live values (same as original run). Jank tab shows 'N/A / Not supported on iOS' instead of raw '-2'. Overall: user can reply 'approved'."
    why_human: "FPS delivery via CADisplayLink requires a running app on simulator or real device. Static analysis cannot confirm CADisplayLink fires correctly in the simulator environment. The original failure was specifically that '--' never updated; the fix switches the mechanism but has not been observed running."
  - test: "After confirming FPS works, update 22-04-SUMMARY.md outcome to PASS (or document as 'PASS after fix'), update 22-RESULTS.md platform matrix iOS row and aggregate outcome to PASS."
    expected: "22-04-SUMMARY.md frontmatter shows outcome: PASS; 22-RESULTS.md shows Phase 22 aggregate outcome PASS (7/7)."
    why_human: "Planning document updates require human decision on whether to retroactively update the SUMMARY or create a new re-run SUMMARY."
---

# Phase 22: Manual Testing All Demo Platforms — Re-Verification Report

**Phase Goal:** Validate that all 7 Kamper demo apps (jvm, android, compose-desktop, ios, macos, web, react-native) build and pass a 30-second smoke test on the post-Phase-21 monorepo (`:libs:*` paths). A passing smoke test means: BUILD SUCCESSFUL + app launches without crashing + `addInfoListener` callbacks deliver non-INVALID values for ≥CPU and Memory during 30 seconds of observation.

**Verified:** 2026-05-01T15:00:00Z
**Status:** human_needed
**Re-verification:** Yes — after gap closure session (2026-05-01)

---

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | All 7 demo Gradle/source files contain zero stale `:kamper:*` path references | VERIFIED | Greps confirmed in initial verification; no build files touched since then. |
| 2 | JVM demo builds and passes smoke test (CPU/Memory/Network live) | VERIFIED | 22-01-SUMMARY `outcome: PASS`. `runConsole` task confirmed live metrics. |
| 3 | Android demo builds and passes smoke test (≥3 modules live, KamperPanel rendered) | VERIFIED | 22-02-SUMMARY `outcome: PASS`. User `approved`. All 8 modules live. |
| 4 | Compose Desktop demo builds and passes smoke test | VERIFIED | 22-03-SUMMARY `outcome: PASS`. JankTab UNSUPPORTED fix + NetworkTab platform gating applied. Additional 6b32bde fix passes JankInfo.UNSUPPORTED through desktop listener. |
| 5 | iOS demo builds and passes smoke test (CPU and Memory non-INVALID) | HUMAN_NEEDED | CPU was confirmed live (L) and Memory live (L) in original 22-04 run. Two fixes applied post-run: (a) JankViewController UNSUPPORTED guard — VERIFIED in code (lines 85-90); (b) IosFpsTimer switched to CADisplayLink — VERIFIED in code. Neither fix has been confirmed by a human re-run. FPS correctness unverifiable by static analysis. |
| 6 | macOS demo builds and passes smoke test (CPU and Memory live) | VERIFIED | 22-05-SUMMARY `outcome: PASS`. User `approved`. |
| 7 | Web JS demo builds and passes smoke test (CPU/FPS/Memory live, unsupported modules show N/A) | VERIFIED | 22-06-SUMMARY `outcome: PASS`. GC/Thermal JS actuals return UNSUPPORTED. |
| 8 | React Native Android demo builds and passes smoke test (CPU live, TurboModule active) | VERIFIED | 22-07-SUMMARY `outcome: PASS`. CPU live on Pixel_4a_API_30; fabric:true TurboModule. |
| 9 | All 7 platforms complete and aggregate outcome is PASS | HUMAN_NEEDED | 22-RESULTS.md still records FAIL (not updated). Becomes PASS after iOS re-run succeeds and documents are updated. |
| 10 | 22-RESULTS.md exists with cross-platform module health matrix and fixes inventory | VERIFIED | File exists; 56-cell matrix and 32-entry fixes inventory complete. |

**Score:** 8/9 truths verified (Truth 5 and 9 pending one human checkpoint)

### Fixes Applied Since Initial Verification (2026-04-30)

These commits were applied after the 22-04-SUMMARY was recorded (2026-04-29 22:52) and after the initial VERIFICATION.md was written (2026-04-30):

| Commit | Date | Fix | Status |
|--------|------|-----|--------|
| `7c6c95c` | 2026-04-29 23:23 | IosFpsTimer: CADisplayLink replacing coroutine timer; JankViewController UNSUPPORTED guard added | VERIFIED in code — no re-run |
| `a3c72a3` | 2026-04-29 23:54 | IosFpsTimer: frameListeners list (multi-consumer); GcInfoRepositoryImpl.kt (iOS) returns UNSUPPORTED; GcViewController handles UNSUPPORTED; IssuesViewController crash button injects fake issue | VERIFIED in code |
| `c662b40` | 2026-05-01 14:28 | ModuleLifecycleManager listener ordering fix (appleMain + androidMain); GcInfo.UNSUPPORTED handling in Compose overlay | VERIFIED — iOS demo Main.kt uses direct Kamper API (not ModuleLifecycleManager); this fix is for the KamperUi chip overlay, not the iOS demo app itself |
| `eb1990e` | 2026-05-01 14:40 | K\|iOS header bar added; RootViewController refactored (UITabBarController embedded as child, prevents safeArea double-offset) | VERIFIED in code |
| `453c7c2` | 2026-05-01 14:43 | K\|iOS header cosmetic polish (title alignment, dot position, size) | VERIFIED in code |
| `6b32bde` | 2026-04-30 21:01 | Compose desktop JankInfo.UNSUPPORTED passthrough | VERIFIED — affects Compose, not iOS UIKit demo |

### Artifact Detail: JankViewController.kt

`demos/ios/src/iosMain/kotlin/com/smellouk/kamper/ios/ui/JankViewController.kt` lines 83-90:

```kotlin
fun update(info: JankInfo) {
    if (info == JankInfo.INVALID) return
    if (info == JankInfo.UNSUPPORTED) {
        bigLabel.text   = "N/A"
        ratioLabel.text = "Not supported on iOS"
        worstLabel.text = ""
        return
    }
    ...
}
```

Status: VERIFIED. The `-2` sentinel display bug is fixed. The guard was committed in `7c6c95c`.

### Artifact Detail: IosFpsTimer.kt

`libs/modules/fps/src/iosMain/kotlin/com/smellouk/kamper/fps/repository/source/IosFpsTimer.kt` lines 39-46:

```kotlin
fun start() {
    if (displayLink != null) return
    val t = DisplayLinkTarget { val now = currentTimeNanos(); frameListeners.forEach { it(now) } }
    linkTarget = t
    val link = CADisplayLink.displayLinkWithTarget(t, NSSelectorFromString("tick"))
    link.addToRunLoop(NSRunLoop.mainRunLoop(), forMode = NSDefaultRunLoopMode)
    displayLink = link
}
```

Status: WIRED. CADisplayLink fires `tick()` on every vsync → timestamp sent to `frameListeners` → `FpsInfoSource` counts frames per second → `FpsWatcher` delivers `FpsInfo` → `FpsViewController.update()` sets label text. Data flow is complete in code. Human verification needed to confirm fires in simulator.

### Artifact Detail: Main.kt — Listener Ordering

`demos/ios/src/iosMain/kotlin/com/smellouk/kamper/ios/Main.kt` `setupKamper()` function: all 8 `Kamper.install()` calls happen first, then all 8 `addInfoListener` calls, then `Kamper.start()`. This is the correct ordering per the Kamper API contract (Engine creates listener slots during `install()`). No ordering regression.

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `22-01-SUMMARY.md` | JVM smoke test outcome | VERIFIED | `outcome: PASS` |
| `22-02-SUMMARY.md` | Android smoke test outcome | VERIFIED | `outcome: PASS` |
| `22-03-SUMMARY.md` | Compose Desktop outcome | VERIFIED | `outcome: PASS` |
| `22-04-SUMMARY.md` | iOS smoke test outcome | VERIFIED (records honest FAIL) | `outcome: FAIL` — pre-fix run. Fix commits exist but no PASS re-run recorded. |
| `22-05-SUMMARY.md` | macOS smoke test outcome | VERIFIED | `outcome: PASS` |
| `22-06-SUMMARY.md` | Web smoke test outcome | VERIFIED | `outcome: PASS` |
| `22-07-SUMMARY.md` | React Native outcome | VERIFIED | `outcome: PASS` |
| `22-08-SUMMARY.md` | Aggregation summary | VERIFIED | Documents FAIL (6/7) — not updated post-fix |
| `22-RESULTS.md` | Cross-platform health matrix | VERIFIED | Exists, 56-cell matrix; still shows iOS FAIL |
| `demos/ios/src/iosMain/.../JankViewController.kt` | UNSUPPORTED guard | VERIFIED | Lines 85-90 present at HEAD |
| `libs/modules/fps/src/iosMain/.../IosFpsTimer.kt` | CADisplayLink-based | VERIFIED | CADisplayLink present at HEAD (not coroutine) |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `demos/jvm/build.gradle.kts` | `:libs:engine, :libs:modules:*` | project dependencies | VERIFIED | No stale `:kamper:` refs |
| `demos/android/build.gradle.kts` | `:libs:engine, :libs:modules:*, :libs:ui:kmm` | project dependencies | VERIFIED | BUILD SUCCESSFUL |
| `demos/compose/build.gradle.kts` | `:libs:*` | project dependencies | VERIFIED | BUILD SUCCESSFUL |
| `demos/ios/build.gradle.kts` | `:libs:*` | project dependencies | VERIFIED | BUILD SUCCESSFUL |
| `demos/macos/build.gradle.kts` | `:libs:*` | project dependencies | VERIFIED | BUILD SUCCESSFUL |
| `demos/web/build.gradle.kts` | `:libs:*` | project dependencies | VERIFIED | BUILD SUCCESSFUL |
| `demos/react-native/metro.config.js` | `libs/ui/rn` | watchFolders | VERIFIED | Correct path |
| `IosFpsTimer.start()` | `FpsInfoSource.addFrameListener` | CADisplayLink tick | VERIFIED | Substantive wiring in code |
| `JankViewController.update()` | `JankInfo.UNSUPPORTED` check | guard at line 85 | VERIFIED | Returns "N/A" + "Not supported on iOS" |

### Behavioral Spot-Checks

Step 7b: SKIPPED for device/app-dependent targets. Phase itself is a behavioral smoke test — relevant checks are human verification checkpoints.

| Behavior | Result | Status |
|----------|--------|--------|
| Zero stale `:kamper:*` refs across all 7 demo Gradle files | Confirmed in initial verification; no Gradle files modified since | PASS |
| JankViewController guard for UNSUPPORTED | Lines 85-90 confirmed at HEAD | PASS |
| IosFpsTimer uses CADisplayLink (not coroutine) | `CADisplayLink.displayLinkWithTarget(...)` confirmed at HEAD | PASS |
| FPS delivers live values in simulator | Cannot verify without running the app | SKIP (human needed) |
| 22-04-SUMMARY records updated PASS outcome | Still records `outcome: FAIL` — no re-run documented | PENDING |

### Requirements Coverage

No requirement IDs assigned to Phase 22 (ROADMAP states "Requirements: TBD"). No cross-reference needed.

### Anti-Patterns Found

| File | Pattern | Severity | Impact |
|------|---------|----------|--------|
| `demos/react-native/package-lock.json:44` | `"../../kamper/react-native"` stale path | Info | npm-generated lockfile artifact — no impact on build |
| `22-RESULTS.md` | Still documents Phase 22 aggregate outcome as FAIL | Warning | Planning doc inconsistency — will become accurate after iOS re-run confirms PASS |

---

## Human Verification Required

### 1. iOS Smoke Test Re-Run (FPS confirmation)

**Test:** Build the iOS demo for simulator with `./gradlew :demos:ios:assembleIosSimulatorApp` (task added in `c662b40`), install in iPhone simulator via `xcrun simctl install booted build/ios/KamperIOS.app`, launch, observe all 8 tabs for 30 seconds:
- CPU tab: shows live percentage values (non-zero)
- FPS tab: shows a numeric fps value (e.g. 58–60) updating every ~1 second — NOT stuck at "--"
- Memory tab: shows values (will show Mac RAM on simulator — acceptable)
- Jank tab: shows "N/A" and "Not supported on iOS" — NOT "-2"
- GC tab: shows "N/A" or ARC note
- Network/Issues/Thermal: as designed

**Expected:** User can reply `approved`. CPU non-INVALID (confirmed in original run). Memory non-INVALID (confirmed in original run). FPS non-stuck (fix applied; unconfirmed). Jank shows N/A (fix verified in code).

**Why human:** CADisplayLink firing in iOS simulator cannot be verified by static analysis. The original failure was FPS stuck at "--"; the CADisplayLink fix addresses the root cause but requires a live run to confirm.

### 2. Post-Approval Document Update

**Test:** After iOS re-run succeeds and user approves, update: (a) `22-04-SUMMARY.md` — note the fixes applied and outcome upgrade; (b) `22-RESULTS.md` — update iOS row FPS column from `I` to `L`, update Jank row iOS column from `U (raw -2 displayed)` to `U (N/A shown)`, update aggregate outcome to PASS; (c) optionally append a fix entry to the fixes inventory for the post-hoc iOS fixes.

**Expected:** 22-RESULTS.md Phase 22 aggregate outcome becomes PASS (7/7).

**Why human:** Planning document retroactive updates require human decision on how to represent post-hoc fixes in the historical record.

---

## Gap Closure Summary (Re-Verification)

The two gaps from the initial verification (2026-04-30) have been partially resolved:

**Gap 1 — iOS JankViewController raw `-2`:** CLOSED. Code fix confirmed at HEAD (lines 85-90). No re-run needed to validate this — it's a simple sentinel guard.

**Gap 2 — iOS FPS never updating:** PARTIALLY CLOSED. The root cause (coroutine-based timer that raced with the Watcher's initial delay) was fixed by switching to CADisplayLink (`7c6c95c`). The fix is substantive and correctly wired in code. A human-approved re-run is needed to confirm the fix works in practice.

**Gap 3 — Phase 22 aggregate FAIL status:** DEFERRED to after iOS re-run. Once FPS is confirmed working and user approves the iOS smoke test, both `22-04-SUMMARY.md` and `22-RESULTS.md` should be updated to reflect the fixed state.

**Critical distinction for the phase goal:** The phase ROADMAP goal minimum criterion is "non-INVALID values for ≥CPU and Memory." The original 22-04 run confirmed both CPU (`L`) and Memory (`L`) were live. The FPS issue (separate from the goal minimum) is now fixed in code. A re-run would likely result in user `approved` and phase PASS — but the PASS is contingent on the re-run, not on static analysis alone.

---

_Verified: 2026-05-01T15:00:00Z_
_Verifier: Claude (gsd-verifier)_
_Re-verification: Yes — initial status was gaps_found; closed 1.5 of 2 gaps; FPS fix needs human confirmation_
