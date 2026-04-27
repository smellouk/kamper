---
phase: 14-react-native-package-library-engine-ui
plan: "05"
subsystem: react-native
tags: [react-native, turbomodule, ios, objc++, cocoapods, xcframework, kotlin-native]

# Dependency graph
requires:
  - phase: 14-react-native-package-library-engine-ui
    plan: 01
    provides: NativeKamperModuleSpec Codegen spec + codegenConfig + package.json scaffold
  - phase: 14-react-native-package-library-engine-ui
    plan: 02
    provides: KamperUi.show() + KamperUi.hide() public facades on appleMain actual object

provides:
  - kamper/react-native/kamper-react-native.podspec — CocoaPods spec with install_modules_dependencies(s) for New Arch
  - kamper/react-native/ios/KamperTurboModule.h — TurboModule header: KamperTurboModule : NativeKamperModuleSpecBase <NativeKamperModuleSpec>
  - kamper/react-native/ios/KamperTurboModule.mm — ObjC++ TurboModule implementation: 4 iOS metric callbacks + overlay + JSI bridge

affects:
  - 14-06 (demo Podfile must replace KamperNative pod with react-native-kamper pod; pod install + xcodebuild validates full iOS compile)

# Tech tracking
tech-stack:
  added:
    - KamperTurboModule ObjC++ class hierarchy (NativeKamperModuleSpecBase + NativeKamperModuleSpec protocol)
    - CocoaPods podspec using install_modules_dependencies(s) New Arch macro
    - folly::Optional<bool> flagOrTrue helper for Codegen SpecStartConfig config parsing
  patterns:
    - Codegen-generated NativeKamperModuleSpecBase base class replaces RCTEventEmitter inheritance
    - install_modules_dependencies(s) replaces s.dependency 'React-Core' in podspec
    - dispatch_async(dispatch_get_main_queue()) required for all UIKit overlay calls from TurboModule
    - KamperKamperUi.shared show/hide assumed ObjC symbol for Kotlin/Native object member (verify at first build)
    - iOS XCFramework scope: only 4 of 8 events (cpu/fps/memory/network) emit native data

key-files:
  created:
    - kamper/react-native/kamper-react-native.podspec
    - kamper/react-native/ios/KamperTurboModule.h
    - kamper/react-native/ios/KamperTurboModule.mm
  modified: []

key-decisions:
  - "install_modules_dependencies(s) macro replaces all legacy React-Core dependency — single macro wires Codegen output, JSI flags, and module-map"
  - "vendored_frameworks path is ../xcframework/... (relative to kamper/react-native/ podspec location) not ../../../../kamper/xcframework/ (demo-relative path from old podspec)"
  - "iOS emits only 4 of 8 events (cpu/fps/memory/network) — XCFramework doesn't export GC/Issues/Jank/Thermal modules (Pitfall 2); JS hooks for those return null on iOS by design"
  - "KamperKamperUi.shared show/hide is assumed Kotlin/Native ObjC symbol — must verify at first iOS build with nm -gU on XCFramework binary; adjustable if symbol name differs"
  - "getTurboModule: returns std::make_shared<NativeKamperModuleSpecJSI>(params) — required for New Arch JSI bridge; without this the TurboModule fails to instantiate"
  - "flagOrTrue(folly::Optional<bool>) defaults missing config flags to YES (all modules enabled) — conservative safe default matching Plan 01 KamperConfig semantics"

patterns-established:
  - "New Arch iOS TurboModule: header inherits NativeKamperModuleSpecBase, implements NativeKamperModuleSpec; no legacy headers"
  - "podspec uses install_modules_dependencies(s) as single dependency declaration; no s.dependency 'React-Core'"
  - "overlay calls always dispatch_async main queue before touching UIKit/Kotlin-Native KamperUi"

requirements-completed: [D-04, D-05, D-06, D-08, D-10, D-11]

# Metrics
duration: 3min
completed: 2026-04-27
---

# Phase 14 Plan 05: iOS TurboModule — KamperTurboModule ObjC++ + CocoaPods Podspec Summary

**CocoaPods podspec with install_modules_dependencies New Arch macro + ObjC++ TurboModule wiring 4 iOS metric event callbacks (cpu/fps/memory/network) via KamperKamperBridge and dispatching overlay calls to main queue**

## Performance

- **Duration:** ~3 min
- **Started:** 2026-04-27T04:29:34Z
- **Completed:** 2026-04-27T04:33:19Z
- **Tasks:** 2
- **Files created:** 3

## Accomplishments

- Created `kamper-react-native.podspec` at `kamper/react-native/` with `install_modules_dependencies(s)` (New Arch TurboModule macro), correct `vendored_frameworks` path `../xcframework/...`, and `script_phases` to build the XCFramework before compile
- Created `ios/KamperTurboModule.h` declaring `KamperTurboModule : NativeKamperModuleSpecBase <NativeKamperModuleSpec>` — the correct TurboModule class hierarchy replacing legacy `RCTEventEmitter + RCTBridgeModule`
- Created `ios/KamperTurboModule.mm` implementing the full iOS TurboModule: 4 metric listener callbacks (cpu/fps/memory/network), per-module config gating via `flagOrTrue`, verbatim payload field names from existing KamperModule.mm, main-queue-dispatched overlay calls, JSI bridge method (`getTurboModule:` → `NativeKamperModuleSpecJSI`), and `moduleName` returning `@"KamperModule"`

## Task Commits

Each task was committed atomically:

1. **Task 1: Podspec + KamperTurboModule.h** - `161c3d3` (feat)
2. **Task 2: KamperTurboModule.mm implementation** - `17bd8b5` (feat)

## Files Created/Modified

- `kamper/react-native/kamper-react-native.podspec` — CocoaPods spec: `s.name = 'react-native-kamper'`, platform `:ios, '14.0'`, `source_files = 'ios/*.{h,m,mm}'`, `vendored_frameworks = '../xcframework/...'`, `install_modules_dependencies(s)`, `script_phases` assembleKamperReleaseXCFramework
- `kamper/react-native/ios/KamperTurboModule.h` — TurboModule header with `#import <NativeKamperModuleSpec/NativeKamperModuleSpec.h>` and `@interface KamperTurboModule : NativeKamperModuleSpecBase <NativeKamperModuleSpec>`
- `kamper/react-native/ios/KamperTurboModule.mm` — Full ObjC++ implementation: `start:` with 4 listener blocks, `stop`, `showOverlay`, `hideOverlay`, `getTurboModule:`, `+ moduleName`

## Decisions Made

- `vendored_frameworks` path corrected to `../xcframework/build/XCFrameworks/release/Kamper.xcframework` — the existing `KamperNative.podspec` was at `demos/react-native/ios/KamperNative/` (4 levels deep), our podspec is at `kamper/react-native/` (2 levels from repo root), so the path is shorter
- `script_phases` still uses `$PODS_ROOT/../../../..` to reach repo root because PODS_ROOT is computed at consumer app build time (inside `demos/react-native/ios/Pods`), not relative to the podspec
- `[KamperKamperUi.shared show]` / `[KamperKamperUi.shared hide]` used as primary assumption for Kotlin/Native ObjC binding — Plan 02 added `fun show()` and `actual fun hide()` as non-actual/actual members of `actual object KamperUi` in appleMain; Kotlin/Native generates `.shared` accessor for objects and the method name matches the Kotlin function name

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Removed Codegen-conflicting strings from comments**
- **Found during:** Task 1 and Task 2 acceptance criteria verification
- **Issue:** Acceptance criteria use `grep -c` checks that count strings in comments. The plan-provided comment templates contained the exact strings being checked: `s.dependency 'React-Core'` (in podspec comment), `RCTBridgeModule`/`RCTEventEmitter` (in header comment), `[KamperKamperUi.shared show]` (in .mm comment), `JS::NativeKamperModule::SpecStartConfig` (in .mm comment). Each caused a false-positive extra count that violated the 0 or 1 count requirements.
- **Fix:** Rewrote comment text to describe the concepts without using the exact strings that the acceptance criteria grep for. Semantic intent fully preserved.
- **Files modified:** `kamper-react-native.podspec`, `ios/KamperTurboModule.h`, `ios/KamperTurboModule.mm`
- **Verification:** All grep counts now match exactly (0 for anti-patterns, 1 for each positive check)
- **Committed in:** `161c3d3` (Task 1) and `17bd8b5` (Task 2)

---

**Total deviations:** 1 category auto-fixed (Rule 1 - grep/comment conflicts in plan template text; same pattern as Plan 01 deviation)
**Impact on plan:** Comment text adjusted only. No functional or behavioral changes. All file contents semantically match the plan specification.

## Issues Encountered

None — both tasks completed cleanly. Comment text adjustments were minor.

## Known Stubs

One conditional assumption requiring build-time verification:
- `[KamperKamperUi.shared show]` / `[KamperKamperUi.shared hide]` — Kotlin/Native ObjC symbol names for `KamperUi.show()` and `KamperUi.hide()`. These are the primary assumption; if wrong, run `nm -gU kamper/xcframework/build/XCFrameworks/release/Kamper.xcframework/ios-arm64/Kamper.framework/Kamper | grep -i kamperui` and update `KamperTurboModule.mm`. This is NOT a data stub (no empty data flows to UI) — it is a build-time symbol name that can only be verified by building the XCFramework on macOS. Plan 14-06 includes this verification step.

## Threat Flags

None — no new network endpoints, auth paths, or file access patterns beyond what the plan's threat model (`T-12-16` through `T-12-21`) already covers. All mitigations implemented as specified:
- T-12-16 (Tampering/config): `flagOrTrue` with typed `SpecStartConfig` — type confusion impossible at JSI boundary
- T-12-17 (InfoDisclosure/overlay): `dispatch_async` (not `dispatch_sync`) — JS thread freed immediately after dispatch
- T-12-18 (DoS/main-queue): `dispatch_async` (non-blocking) confirmed in both `showOverlay` and `hideOverlay`
- T-12-19 (Spoofing/moduleName): Single `@"KamperModule"` literal, matches Android + Codegen
- T-12-20 (Tampering/_bridge double-init): `if (_bridge) return` guard in `start:` method

## Next Phase Readiness

- iOS TurboModule structure complete — ready for Plan 14-06 (demo wiring)
- Plan 14-06 must update `demos/react-native/ios/Podfile` to replace `pod 'KamperNative', :path => 'KamperNative'` with `pod 'react-native-kamper', :path => '../../../../kamper/react-native'`
- First `pod install` will run `install_modules_dependencies(s)` and generate `NativeKamperModuleSpec.h` — this is the Codegen-generated header that `KamperTurboModule.h` imports
- First `xcodebuild` will verify Kotlin/Native ObjC symbol names for `KamperKamperUi.shared show/hide`
- XCFramework must be built before `pod install` (or `script_phases` handles it automatically)

## Self-Check: PASSED

Files exist:
- kamper/react-native/kamper-react-native.podspec: FOUND
- kamper/react-native/ios/KamperTurboModule.h: FOUND
- kamper/react-native/ios/KamperTurboModule.mm: FOUND

Commits exist:
- 161c3d3: FOUND (Task 1: podspec + header)
- 17bd8b5: FOUND (Task 2: .mm implementation)

---
*Phase: 14-react-native-package-library-engine-ui*
*Completed: 2026-04-27*
