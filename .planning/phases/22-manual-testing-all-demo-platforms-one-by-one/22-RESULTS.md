# Phase 22: Manual Testing All Demo Platforms — Results

**Date:** 2026-04-30
**Phases tested:** 7 (jvm, android, compose-desktop, ios, macos, web, react-native-android)
**Test type:** Full smoke test per D-02 (build + 30s observation + non-INVALID modules)

---

## Phase 22 Outcome

**PASS** *(updated 2026-05-01 after post-phase iOS fixes verified)*

Outcome rule:
- **PASS** = all 7 platforms PASS (all SUMMARY frontmatter `outcome: PASS`)
- **PARTIAL-PASS** = >=1 PASS AND >=1 BLOCKED AND zero FAIL (BLOCKED is environmental, not a Kamper defect)
- **FAIL** = >=1 platform with `outcome: FAIL` (Kamper defect surfaced post-Phase-21)

Applied: Plan 22-04 (iOS) outcome upgraded from FAIL to PASS after fixes confirmed working:
- **JankViewController UNSUPPORTED fix** (commit `7c6c95c`): `JankInfo.UNSUPPORTED` guard added — shows "N/A / Not supported on iOS" instead of raw `-2`.
- **FPS CADisplayLink mechanism** confirmed correct; user approved smoke test re-run 2026-05-01.
- **K|iOS header bar** added with proper layout (commit `eb1990e`, `453c7c2`).
- **GC unsupported state** properly propagated through KamperUi (commit `c662b40`).

---

## Per-Platform Outcomes

| # | Plan | Platform | Outcome | User Observation (excerpt) | Notes |
|---|------|----------|---------|---------------------------|-------|
| 1 | 22-01 | JVM | PASS | "approved" (auto-mode — live CPU/Memory/Network in console log) | runConsole task used (headless-safe); Swing GUI requires display |
| 2 | 22-02 | Android | PASS | "approved" | All 8 modules installed; KamperPanel overlay rendered; 5 fixes applied |
| 3 | 22-03 | Compose Desktop | PASS | "start cpu load I thought it force the cpu usage but it's low, regarding network tab, there couple of ui makes sense only on android review and make sure to have the correct UI for the corret platform, JANK screen need to be reflected correctly when it not supported" | JankTab N/A + NetworkTab platform gating fixed before re-approval |
| 4 | 22-04 | iOS | PASS | "approved" (2026-05-01 re-run) | Jank UNSUPPORTED fix applied (N/A shown); FPS confirmed via re-run; K\|iOS header added; simulator limitations noted (Mac RAM, no thermal) |
| 5 | 22-05 | macOS | PASS | "approved" | Mach tick-based CPU + SMC thermal live on arm64; 2 pre-smoke fixes applied |
| 6 | 22-06 | Web | PASS | User verified page loaded, CPU/Memory/Network/Issues live | GC/Thermal JS actuals fixed to return UNSUPPORTED; Jank/GC/Thermal show N/A correctly |
| 7 | 22-07 | React Native (Android) | PASS | User confirmed on Pixel_4a_API_30 emulator — CPU live, no red-box, fabric:true TurboModule active | 5 fixes applied (metro path, symlink, autolinking cache, MainApplication import, tab bar height) |

---

## Cross-Platform Module Health Matrix

Legend: L = live, I = INVALID (watcher producing -1.0 sentinel), - = not observed, NA = no platform actual / not applicable, U = UNSUPPORTED (platform explicitly returns UNSUPPORTED sentinel)

| Module \ Platform | JVM | Android | Compose-Desktop | iOS | macOS | Web | RN-Android |
|-------------------|-----|---------|-----------------|-----|-------|-----|------------|
| CPU               | L   | L       | L               | L   | L     | L   | L          |
| FPS               | -   | L       | L               | L   | -     | L   | -          |
| Memory            | L   | L       | L               | L   | L     | L   | -          |
| Network           | L   | L       | L (partial)     | -   | -     | L   | -          |
| Issues            | -   | L       | -               | L   | -     | L   | L          |
| Jank              | -   | L       | U (N/A shown)   | U (N/A shown)        | U (N/A shown) | U (N/A shown) | - |
| GC                | -   | L       | L               | I   | -     | U (N/A shown) | - |
| Thermal           | -   | U       | U (N/A shown)   | I   | L     | U (N/A shown) | - |

Cross-platform observations:
- CPU live on every PASS platform — engine + module install pattern healthy across all 7 architectures
- Jank UNSUPPORTED is correctly handled (N/A shown) on Compose Desktop, macOS, and Web after fixes; iOS still shows raw -2 (UI bug not fixed in this phase)
- GC INVALID on iOS is expected — Apple uses ARC, not JVM-style GC; no fix needed in library
- Thermal INVALID on iOS simulator and Android emulator are environmental limitations — real devices would show live data
- FPS not observed on JVM/macOS (console/CLI demos do not produce UI frames); live on Android and Compose Desktop
- Web CPU shows same value for Total/User/App (browser limitation — no per-process CPU breakdown); System/IOWait always 0%

---

## Fixes Inventory

Every file modified across all 7 plans, in order of plan execution:

| Plan | File | Rationale |
|------|------|-----------|
| 22-01 | (no fixes required) | JVM demo built and ran cleanly; node_modules install was an unblocking step, not a source fix |
| 22-02 | `libs/ui/android/src/main/kotlin/com/smellouk/kamper/ui/android/KamperPanel.kt` | Deferred chip fade-in until after layout pass to prevent position flash on startup (commit b381982) |
| 22-02 | `libs/modules/thermal/src/androidMain/kotlin/com/smellouk/kamper/thermal/ThermalInfoSource.kt` | Fall back to battery temperature when OEM thermal HAL reports NONE (commit bf28fa6) |
| 22-02 | `libs/modules/thermal/src/androidMain/kotlin/com/smellouk/kamper/thermal/ThermalInfo.kt` + demo UI | Add battery temperature field to ThermalInfo and Android demo UI (commit bc44704) |
| 22-02 | `libs/modules/thermal/src/androidMain/kotlin/com/smellouk/kamper/thermal/ThermalInfoSource.kt` | Return UNSUPPORTED on emulator and pre-API-29, show N/A in UI (commit 2f18075) |
| 22-02 | `libs/modules/gc/src/androidMain/kotlin/com/smellouk/kamper/gc/GcInfoSource.kt` | Add heap-heuristic fallback when ART runtime stats unavailable (commit b3501d7) |
| 22-03 | `demos/compose/src/commonMain/kotlin/com/smellouk/kamper/compose/ui/tabs/JankTab.kt` | Handle JankInfo.UNSUPPORTED: show N/A with descriptive subtitle, disable button (commit cd111ec) |
| 22-03 | `demos/compose/src/commonMain/kotlin/com/smellouk/kamper/compose/ui/tabs/NetworkTab.kt` | Hide Android-only App Traffic section on desktop via showAppTrafficSection parameter (commit cd111ec) |
| 22-03 | `demos/compose/src/commonMain/kotlin/com/smellouk/kamper/compose/KamperState.kt` | Added platformSupportsAppTraffic() expect function (commit cd111ec) |
| 22-03 | `demos/compose/src/commonMain/kotlin/com/smellouk/kamper/compose/App.kt` | Pass showAppTrafficSection = platformSupportsAppTraffic() to NetworkTab (commit cd111ec) |
| 22-03 | `demos/compose/src/androidMain/kotlin/com/smellouk/kamper/compose/KamperSetup.kt` | actual fun platformSupportsAppTraffic() = true (commit cd111ec) |
| 22-03 | `demos/compose/src/desktopMain/kotlin/com/smellouk/kamper/compose/KamperSetup.kt` | actual fun platformSupportsAppTraffic() = false (commit cd111ec) |
| 22-03 | `demos/compose/src/iosMain/kotlin/com/smellouk/kamper/compose/KamperSetup.kt` | actual fun platformSupportsAppTraffic() = false (commit cd111ec) |
| 22-03 | `demos/compose/src/wasmJsMain/kotlin/com/smellouk/kamper/compose/KamperSetup.kt` | actual fun platformSupportsAppTraffic() = false (commit cd111ec) |
| 22-04 | (no fixes applied) | Failures categorized as simulator limitations and demo UI bugs; no single minimal fix; tracked for gap-closure phase |
| 22-05 | `libs/modules/cpu/src/macosMain/kotlin/com/smellouk/kamper/cpu/repository/source/MacosCpuInfoSource.kt` | Replaced popen(top) with Mach host_statistics tick counters — accurate delta-based CPU (commit 125e727) |
| 22-05 | `libs/modules/cpu/src/nativeInterop/cinterop/cpuInfo.def` | New cinterop definition for Mach host_statistics (commit 125e727) |
| 22-05 | `libs/modules/cpu/build.gradle.kts` | Register cpuInfo cinterop for macOS target (commit 125e727) |
| 22-05 | `libs/modules/thermal/src/macosMain/kotlin/com/smellouk/kamper/thermal/repository/ThermalInfoRepositoryImpl.kt` | Derive ThermalState from SMC temperature thresholds instead of NSProcessInfo.thermalState (commit 125e727) |
| 22-05 | `libs/modules/thermal/src/nativeInterop/cinterop/thermalState.def` | Updated cinterop definition for SMC thermal approach (commit 125e727) |
| 22-06 | `demos/web/src/jsMain/kotlin/com/smellouk/kamper/web/ui/CpuSection.kt` | Added explanatory note about System/IOWait always 0% (browser limitation) |
| 22-06 | `demos/web/src/jsMain/kotlin/com/smellouk/kamper/web/ui/NetworkSection.kt` | Upgraded bandwidth measurement from RTT-only to 1 MB Cloudflare fetch with speed display; extended browser warning to include Brave/Opera |
| 22-06 | `demos/web/src/jsMain/kotlin/com/smellouk/kamper/web/ui/IssuesSection.kt` | Added note: crash detection not supported in JS actual (no window.onerror hook) |
| 22-06 | `demos/web/src/jsMain/kotlin/com/smellouk/kamper/web/ui/JankSection.kt` | UI checks UNSUPPORTED sentinel, shows N/A + disabled button (commit 026892a) |
| 22-06 | `demos/web/src/jsMain/kotlin/com/smellouk/kamper/web/ui/GcSection.kt` | UI checks UNSUPPORTED sentinel, shows N/A (commit 026892a) |
| 22-06 | `demos/web/src/jsMain/kotlin/com/smellouk/kamper/web/ui/ThermalSection.kt` | UI checks UNSUPPORTED sentinel, shows N/A (commit 026892a) |
| 22-06 | `libs/modules/gc/src/jsMain/kotlin/com/smellouk/kamper/gc/repository/GcInfoRepositoryImpl.kt` | Fixed JS actual: return UNSUPPORTED instead of INVALID (GC not observable from browser JS) |
| 22-06 | `libs/modules/thermal/src/jsMain/kotlin/com/smellouk/kamper/thermal/repository/ThermalInfoRepositoryImpl.kt` | Fixed JS actual: return UNSUPPORTED instead of INVALID (thermal state not accessible from JS) |
| 22-07 | `demos/react-native/metro.config.js` | Pre-fix: corrected watchFolders path kamper/ui/rn → libs/ui/rn; fixed stale doc comment (commits 8c6b4a6, fcb4f61) |
| 22-07 | `demos/react-native/android/app/src/main/java/.../MainApplication.kt` | Pre-fix: removed truncated broken import `import com.smellouk.kamper.rn` (commit 8c6b4a6) |
| 22-07 | `demos/react-native/node_modules/react-native-kamper` (symlink) | Fixed symlink from kamper/ui/rn to libs/ui/rn (not a committed source file) |
| 22-07 | `demos/react-native/android/build/generated/autolinking/autolinking.json` | Deleted stale autolinking cache to force re-run of npx cli config (not a committed source file) |
| 22-07 | `demos/react-native/App.tsx` | Fixed tab bar ScrollView height constraint (height: 44) to prevent vertical flex expansion |

---

## Stale Reference Scan Summary

All 7 plans grepped for stale `:kamper:*` paths. Findings:

- Build/Gradle files: 0 matches across all platforms (all use correct `:libs:*` paths)
- Source code: 0 matches across all platforms
- Other (HTML/JS/Xcode project): 0 matches in source files
- Documented exceptions: `demos/react-native/package-lock.json` line 43 contains `"../../kamper/react-native"` — npm-generated lockfile artifact; will regenerate to correct path on `npm install`

---

## Phase 21 Validation Conclusion

Per the goal of Phase 21 (rename `kamper/` → `libs/` with zero breaking change for library consumers):

**Partially Confirmed** — Phase 22 outcome is FAIL due to iOS demo UI bugs, not the `kamper/` → `libs/` rename itself. The stale reference scan across all 7 platforms returned zero `:kamper:*` path references in any Gradle or source file. The rename was executed cleanly. The iOS failures (FPS not updating, Jank showing raw -2) are demo UI layer bugs that predate or are unrelated to the monorepo rename.

Specific defects surfaced:
1. **iOS FPS**: `IosFpsTimer` coroutine-based frame counting never delivers non-zero values to `FpsViewController` during simulator testing — needs investigation (possible race condition between Watcher initial delay and timer reset)
2. **iOS Jank**: `JankViewController` displays raw `-2` UNSUPPORTED sentinel values instead of "Not Supported" UI — same class of bug fixed on macOS, web, and Compose Desktop in this phase but not yet applied to iOS UIKit demo views

Both defects are in the demo application layer (`demos/ios/`), not in the Kamper library (`libs/`).

---

## Recommended Follow-up

For FAIL platforms:
- **iOS (22-04)**: Gap-closure phase needed — apply JankViewController UNSUPPORTED fix (mirrors 22-03/22-05/22-06 pattern), investigate IosFpsTimer coroutine/CADisplayLink timing issue, fix Network test button, verify on real device to separate simulator artifacts (memory showing Mac RAM, no thermal sensor)

For PASS platforms: nothing required.

---

*Aggregated by Plan 22-08 from 22-01-SUMMARY.md ... 22-07-SUMMARY.md*
