---
phase: 22-manual-testing-all-demo-platforms-one-by-one
plan: 07
subsystem: testing
tags: [react-native, android, metro, autolinking, turbomodule]

requires:
  - phase: 22-06
    provides: web demo smoke test complete

provides:
  - React Native Android demo validated on Pixel_4a_API_30 (API 30) emulator
  - metro.config.js fully corrected (path + doc comment)
  - Broken node_modules symlink (kamper→libs) fixed
  - Stale autolinking cache cleared — react-native-kamper now autolinks correctly
  - Tab indicator UI regression fixed (height: 44 on tabBar ScrollView)

affects: [22-08]

tech-stack:
  added: []
  patterns: [RN autolinking cache invalidation, horizontal ScrollView height constraint]

key-files:
  created:
    - .planning/phases/22-manual-testing-all-demo-platforms-one-by-one/22-07-SUMMARY.md
  modified:
    - demos/react-native/metro.config.js
    - demos/react-native/App.tsx

key-decisions:
  - "Broken node_modules/react-native-kamper symlink (kamper/ui/rn → libs/ui/rn) fixed manually; npm install would also regenerate it"
  - "Autolinking cache at android/build/generated/autolinking/autolinking.json deleted to force re-run of npx cli config"
  - "Tab bar ScrollView fixed with height: 44 — RN horizontal ScrollViews expand vertically without explicit height"

patterns-established:
  - "RN autolinking cache (autolinking.json + *.sha) must be cleared when node_modules symlinks change"
  - "Horizontal ScrollView tab bars need explicit height to prevent vertical flex expansion"

requirements-completed: []

outcome: PASS
platform: react-native-android
date: 2026-04-30

duration: 45min
completed: 2026-04-30
---

# Plan 22-07: React Native Demo Smoke Test Summary

**React Native Android demo builds and runs on Pixel_4a emulator — KamperModule TurboModule active (fabric:true), CPU live at ~8-11%, no crashes, 3 additional fixes applied beyond the 2 planned pre-fixes**

## Performance

- **Duration:** ~45 min
- **Completed:** 2026-04-30
- **Tasks:** 8 (Tasks 6 & 7 merged: diagnosis + fix in one pass)
- **Files modified:** 3

## Pre-Fixes Applied (Planned)

1. **`demos/react-native/metro.config.js` — kamper → libs path** (already done in commit `8c6b4a6`; doc comment on line 8 still stale → fixed in `fcb4f61`)
2. **`demos/react-native/android/app/.../MainApplication.kt` — removed truncated `import com.smellouk.kamper.rn`** (done in `8c6b4a6`; `KamperTurboPackage()` registration preserved)

## Stale Reference Scan (Task 3)

All four grep checks returned `NO_MATCHES`:
1. `:kamper:` in Gradle files — NO_MATCHES
2. `":kamper` in Kotlin/Java source — NO_MATCHES
3. `kamper/engine|kamper/modules|kamper/api|kamper/ui` in demos/react-native/ — NO_MATCHES
4. `'kamper'` in JS/TS/JSON (excluding node_modules) — NO_MATCHES

(Note: `package-lock.json` line 43 contains `"../../kamper/react-native"` — documented npm lockfile artifact, will regenerate on `npm install`.)

## Build Outcome

- **Method:** `cd demos/react-native/android && ./gradlew :app:assembleDebug`
- **Result:** `BUILD SUCCESSFUL` (after 3 additional fixes — see below)
- **APK:** `demos/react-native/android/app/build/outputs/apk/debug/app-debug.apk`
- **react-native-kamper** correctly autolinked: `:react-native-kamper:compileDebugKotlin` appeared in build task graph

## Smoke Test Outcome (Task 5)

**PASS** — User result: emulator-5554 (Pixel_4a_API_30)

- Metro started, bundle compiled (627/628 modules)
- APK installed and launched via `./gradlew :app:installDebug` + `adb shell am start`
- `ReactNativeJS: Running "KamperRn" with {"rootTag":1,"initialProps":{},"fabric":true}` — TurboModule bridge active
- No red-box, no FATAL EXCEPTION
- CPU live values: Total ~8-11%, App ~8-11%, User ~5-8%, System ~2-3%

## Module Health

| Module | Status | Notes |
|--------|--------|-------|
| CPU | live | ~8-11% total, /proc/stat EACCES (API 30 expected), shell/top fallback used |
| FPS | not observed in logcat | FPS tab visible in app |
| Memory | not observed in logcat | Memory tab visible in app |
| Network | not observed in logcat | Network tab visible in app |
| Issues | live | CrashDetector registered (UncaughtExceptionHandler replaced — normal) |
| Jank | not observed in logcat | Jank tab visible in app |
| GC | not observed in logcat | GC tab visible in app |
| Thermal | not observed in logcat | Thermal tab visible in app |

## Bridge State

`KamperModule registered` — confirmed via `fabric:true` in ReactNativeJS log and `CrashDetector` Issues module hook firing. TurboModule New Architecture path active.

## KamperPanel Native Overlay (RN)

**Rendered** — visible in screenshot as floating chip in bottom-right corner showing CPU, FPS, MEM, NET, and Issues count (10).

## Additional Fixes Applied (Beyond 2 Pre-Fixes)

### Fix 3: Broken node_modules/react-native-kamper symlink
- **File:** `demos/react-native/node_modules/react-native-kamper` (symlink)
- **Issue:** Symlink pointed to `../../../kamper/ui/rn` (old monorepo path pre-Phase-21 rename)
- **Fix:** `rm node_modules/react-native-kamper && ln -s ../../../libs/ui/rn react-native-kamper`
- **Why build still failed after:** Autolinking cache (`autolinking.json`) was stale

### Fix 4: Stale autolinking cache
- **File:** `demos/react-native/android/build/generated/autolinking/autolinking.json`
- **Issue:** Cache generated with broken symlink — `react-native-kamper` absent from cached config; cache only invalidated by lockfile SHA changes (not symlink changes)
- **Fix:** Deleted `autolinking.json` + `*.sha` files; next Gradle run re-ran `npx @react-native-community/cli config`, correctly found `react-native-kamper`
- **Result:** `react-native-kamper:compileDebugKotlin` appeared in build; `Unresolved reference 'KamperTurboPackage'` resolved

### Fix 5: Tab indicator UI regression
- **File:** `demos/react-native/App.tsx`
- **Issue:** Horizontal `ScrollView` tab bar had no height constraint; in RN flex layout it expanded vertically to fill all available space, pushing the blue `borderBottomColor` indicator to the very bottom of a large empty area
- **Fix:** `tabBar: {backgroundColor: C.mantle, height: 44}` — caps the tab bar at 44px, tab indicator appears directly under tab labels
- **Screenshot:** User confirmed in emulator

## Issues Encountered

1. **Unresolved reference 'KamperTurboPackage'** — root cause was broken `node_modules` symlink (not the `import` line; the import was already cleaned up). Required two separate fixes: symlink + cache invalidation.
2. **Autolinking cache architecture** — RN Gradle plugin caches `npx cli config` output keyed on lockfile SHA hashes. Symlink changes don't invalidate it. Must manually delete `autolinking.json` when symlinks change.
3. **Tab bar height** — horizontal ScrollViews in RN need explicit height in flex column layouts.

## Self-Check: PASSED

- metro.config.js: `grep -c "'libs', 'ui', 'rn'"` → 1; `grep -c "kamper"` → 0 ✓
- MainApplication.kt: no `^import com\.smellouk\.kamper\.rn$`; `KamperTurboPackage` present ✓
- Stale reference scan: all 4 greps NO_MATCHES ✓
- BUILD SUCCESSFUL, APK produced ✓
- App launched on emulator, CPU live, no crashes ✓
- `outcome: PASS` in frontmatter ✓

## Next Phase Readiness

Plan 22-08 (results aggregation) can now run. All 7 platform plans complete.
