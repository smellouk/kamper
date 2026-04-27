---
phase: 14-react-native-package-library-engine-ui
plan: 01
subsystem: react-native
tags: [react-native, turbomodule, codegen, typescript, npm, javascript]

# Dependency graph
requires:
  - phase: 13-stack-alignment-dependency-unification
    provides: AGP 8.13.0 + KGP 2.3.21 aligned build; namespace com.smellouk.kamper.*
provides:
  - kamper/react-native/package.json — npm package metadata with codegenConfig block
  - kamper/react-native/tsconfig.json — TypeScript compiler config for library
  - kamper/react-native/babel.config.js — Babel preset for RN library
  - kamper/react-native/.gitignore — Ignores for build outputs, Pods, and node_modules
  - kamper/react-native/src/NativeKamperModule.ts — Codegen TurboModule spec (single source of truth for Android/iOS interfaces)
  - kamper/react-native/src/types.ts — Public TypeScript types: 9 interfaces (8 metric payloads + KamperConfig)
affects:
  - 14-02 (TS API layer consumes types.ts + NativeKamperModule)
  - 14-03 (Android TurboModule implements NativeKamperModuleSpec generated from this spec)
  - 14-04 (iOS TurboModule implements ObjC++ protocol generated from this spec)
  - 14-05 (demo wiring validates the full chain)

# Tech tracking
tech-stack:
  added:
    - react-native-kamper npm package scaffold (v0.1.0)
    - TypeScript TurboModule Codegen spec pattern
    - react-native/Libraries/Types/CodegenTypes EventEmitter<T>
  patterns:
    - Codegen spec as single source of truth — NativeKamperModule.ts generates both Android Java abstract class and iOS ObjC++ protocol
    - Inline payload types in spec file — Codegen processes spec standalone; cannot import from types.ts
    - Object param for start(config: Object) — Codegen requirement; strict typing lives in types.ts KamperConfig
    - Module name literal KamperModule — must match Android KamperTurboModule.NAME, iOS moduleName, and TurboModuleRegistry key

key-files:
  created:
    - kamper/react-native/package.json
    - kamper/react-native/tsconfig.json
    - kamper/react-native/babel.config.js
    - kamper/react-native/.gitignore
    - kamper/react-native/src/NativeKamperModule.ts
    - kamper/react-native/src/types.ts
  modified: []

key-decisions:
  - "Module name literal 'KamperModule' used in TurboModuleRegistry.getEnforcing — must be reused verbatim in Plans 04/05 Android NAME and iOS moduleName"
  - "Codegen EventEmitter<T> used (not NativeEventEmitter) — generates emitOnXxx methods on spec base class; aligns with RN New Architecture"
  - "start(config: Object) typed as Object (not KamperConfig) — Codegen requirement for JSI boundary; native side parses with ReadableMap.hasKey()"
  - "Payload types duplicated inline in spec — Codegen constraint; types.ts provides richer interfaces for consumer API (Plan 03)"
  - "iOS scope: only 4 events have native emitters in Plan 05 (cpu/fps/memory/network); spec lists all 8 for symmetry; iOS silently omits issues/jank/gc/thermal"

patterns-established:
  - "Codegen spec as single source of truth: NativeKamperModule.ts generates Android Java abstract class and iOS ObjC++ protocol"
  - "Dual type files: spec file has inline Codegen payload types; types.ts has rich public interfaces for consumer apps"
  - "peerDependency RN >=0.73.0 enforces New Architecture only (TurboModule requirement)"

requirements-completed: [D-01, D-03, D-04, D-05, D-08, D-09]

# Metrics
duration: 15min
completed: 2026-04-27
---

# Phase 14 Plan 01: react-native-kamper npm Library Scaffold Summary

**TurboModule Codegen spec for react-native-kamper with 4 imperative methods + 8 EventEmitter<T> properties generating Android/iOS native interfaces**

## Performance

- **Duration:** ~15 min
- **Started:** 2026-04-27
- **Completed:** 2026-04-27
- **Tasks:** 2
- **Files created:** 6

## Accomplishments

- Created `kamper/react-native/` npm library scaffold with codegenConfig declaring `NativeKamperModuleSpec` — the Codegen contract that generates Android Java abstract class and iOS ObjC++ protocol
- Created `NativeKamperModule.ts` Codegen TurboModule spec with 4 imperative methods (start/stop/showOverlay/hideOverlay) + 8 EventEmitter<T> properties (onCpu/onFps/onMemory/onNetwork/onIssue/onJank/onGc/onThermal)
- Created `types.ts` with 9 public TypeScript interfaces (CpuInfo, FpsInfo, MemoryInfo, NetworkInfo, IssueInfo, JankInfo, GcInfo, ThermalInfo, KamperConfig) with field names matching Kotlin emit payloads exactly

## Task Commits

Each task was committed atomically:

1. **Task 1: Create package.json + tsconfig + babel + .gitignore scaffold** - `0417eb2` (feat)
2. **Task 2: Create Codegen TurboModule spec NativeKamperModule.ts and types.ts** - `4b1704f` (feat)

## Files Created/Modified

- `kamper/react-native/package.json` — npm package metadata; codegenConfig with name=NativeKamperModuleSpec, jsSrcsDir=./src, javaPackageName=com.smellouk.kamper.rn, iOS modulesProvider KamperModule->KamperTurboModule
- `kamper/react-native/tsconfig.json` — TypeScript strict config; jsx=react-native; excludes android/ios
- `kamper/react-native/babel.config.js` — Babel with @react-native/babel-preset
- `kamper/react-native/.gitignore` — Ignores build/, Pods/, Codegen-generated/, node_modules/, .idea/
- `kamper/react-native/src/NativeKamperModule.ts` — Codegen TurboModule spec: Spec extends TurboModule with 12 members (4 imperative + 8 EventEmitter); default export TurboModuleRegistry.getEnforcing<Spec>('KamperModule')
- `kamper/react-native/src/types.ts` — 9 exported interfaces; field names verbatim from KamperModule.kt emit payloads

## Decisions Made

- Module name literal `KamperModule` must be reused verbatim in Android `KamperTurboModule.NAME`, iOS `+ moduleName`, and `TurboModuleRegistry.getEnforcing` — any mismatch causes runtime "module not registered" error
- `start(config: Object)` uses `Object` (not `KamperConfig`) at the Codegen boundary — native side parses with `ReadableMap.hasKey()`; richer typed API lives in Plan 03 wrapper
- Payload types inline in spec file (not imported from types.ts) — Codegen processes this file standalone and rejects external type references in EventEmitter<T>
- iOS XCFramework only emits CPU/FPS/Memory/Network — spec lists all 8 events; iOS implementation in Plan 05 simply never emits the 4 Android-only ones

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Removed NativeEventEmitter string from code comments**
- **Found during:** Task 2 (acceptance criteria verification)
- **Issue:** Plan acceptance criteria requires `grep -c "NativeEventEmitter"` returns 0, but the plan's own template comment text contained the string "NativeEventEmitter". Grep counts strings in comments too.
- **Fix:** Replaced comment phrases mentioning "NativeEventEmitter" with equivalent descriptions ("legacy event emitter pattern deprecated for TurboModules")
- **Files modified:** kamper/react-native/src/NativeKamperModule.ts
- **Verification:** `grep -c "NativeEventEmitter" kamper/react-native/src/NativeKamperModule.ts` returns 0
- **Committed in:** `4b1704f` (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (Rule 1 - comment/grep conflict in plan template)
**Impact on plan:** Minor comment text adjustment only. Semantic intent preserved. No functional changes.

## Issues Encountered

None — both tasks completed cleanly.

## Known Stubs

None — this plan creates contract files only (types + spec). No runtime data flows exist yet.

## Threat Flags

None — no new network endpoints, auth paths, or file access patterns introduced. Files are TypeScript/JSON definitions only.

## Next Phase Readiness

- Codegen contract established — `NativeKamperModule.ts` is the single source of truth for Plans 14-03 (Android) and 14-04 (iOS) TurboModule implementations
- Module name literal `KamperModule` locked — Plans 14-03/14-04 must match exactly
- 8 event names locked: `onCpu`, `onFps`, `onMemory`, `onNetwork`, `onIssue`, `onJank`, `onGc`, `onThermal`
- iOS scope: only first 4 events (onCpu/onFps/onMemory/onNetwork) will have native emitters in Plan 14-04
- `node_modules` not yet installed — `tsc --noEmit` will run in Wave 3 after demo wiring

## Self-Check: PASSED

All created files exist. Both task commits confirmed in git log.

---
*Phase: 14-react-native-package-library-engine-ui*
*Completed: 2026-04-27*
