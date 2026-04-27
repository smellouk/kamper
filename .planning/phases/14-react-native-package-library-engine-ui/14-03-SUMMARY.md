---
phase: 14
plan: "03"
subsystem: react-native
tags: [react-native, typescript, hooks, imperative-api, ref-counting, turbomodule, tdd]
dependency_graph:
  requires:
    - kamper/react-native/src/NativeKamperModule.ts (Plan 01 — Codegen TurboModule spec)
    - kamper/react-native/src/types.ts (Plan 01 — public TypeScript types)
    - kamper/react-native/src/__tests__/hooks.test.ts (Plan 00 — Wave 0 RED tests)
  provides:
    - kamper/react-native/src/Kamper.ts
    - kamper/react-native/src/hooks/useKamper.ts
    - kamper/react-native/src/hooks/useCpu.ts
    - kamper/react-native/src/hooks/useFps.ts
    - kamper/react-native/src/hooks/useMemory.ts
    - kamper/react-native/src/hooks/useNetwork.ts
    - kamper/react-native/src/hooks/useIssues.ts
    - kamper/react-native/src/hooks/useJank.ts
    - kamper/react-native/src/hooks/useGc.ts
    - kamper/react-native/src/hooks/useThermal.ts
    - kamper/react-native/src/index.ts
  affects:
    - 14-05 (RN demo wiring calls Kamper.start/stop/on via this layer)
    - 14-06 (tsc --noEmit type check validates this TS layer)
tech_stack:
  added:
    - "@testing-library/react-native ^12.0.0 devDependency (renderHook for hook tests)"
    - "react hooks (useEffect, useState) — engine lifecycle pattern"
  patterns:
    - "Module-level ref counting: _acquireEngine/_releaseEngine in useKamper.ts shared across all single-metric hooks"
    - "Codegen EventEmitter<T> callable pattern via unknown cast (not NativeEventEmitter)"
    - "useIssues accumulates list cap 100 (T-12-08 mitigate)"
key_files:
  created:
    - kamper/react-native/src/Kamper.ts
    - kamper/react-native/src/hooks/useKamper.ts
    - kamper/react-native/src/hooks/useCpu.ts
    - kamper/react-native/src/hooks/useFps.ts
    - kamper/react-native/src/hooks/useMemory.ts
    - kamper/react-native/src/hooks/useNetwork.ts
    - kamper/react-native/src/hooks/useIssues.ts
    - kamper/react-native/src/hooks/useJank.ts
    - kamper/react-native/src/hooks/useGc.ts
    - kamper/react-native/src/hooks/useThermal.ts
    - kamper/react-native/src/index.ts
  modified:
    - kamper/react-native/package.json (added test script + testing devDependencies)
decisions:
  - "Shared ref counter in useKamper.ts module scope (_acquireEngine/_releaseEngine) — single engine per JS process; single-metric hooks import these helpers rather than each having their own counter"
  - "EVENT_TO_PROP map in Kamper.ts: singular JS event names (cpu/fps/memory/network/issue/jank/gc/thermal) map to capitalized TurboModule property names (onCpu/onFps/...)"
  - "useIssues CONFIG_KEY is 'issues' (plural, KamperConfig field) but EVENT is 'issue' (singular, Codegen spec event name) — do not transpose"
  - "Codegen EventEmitter<T> cast through unknown: emitter properties are callable at runtime but the TS type system doesn't express it; cast is safe given mock verification"
  - "index.ts top-level showOverlay/hideOverlay re-export via lambda (not direct re-export) to avoid circular export reference"
  - "@testing-library/react-native installed in demos/react-native/node_modules (symlinked as kamper/react-native/node_modules) — Plan 06 to formalize install via workspace"
metrics:
  duration: "~8 minutes"
  completed: "2026-04-27"
  tasks_completed: 2
  tasks_total: 2
  files_created: 11
  files_modified: 1
---

# Phase 14 Plan 03: React Native JS/TS API Layer Summary

**TypeScript imperative API (Kamper.ts) + 9 React hooks with module-level ref-counted engine lifecycle, turning Wave 0 RED tests GREEN (28/28 passing)**

## What Was Built

### Imperative API — Kamper.ts

`Kamper` is a typed singleton wrapping the Codegen TurboModule:

| Method | Signature | Delegates To |
|--------|-----------|-------------|
| `start` | `(config?: KamperConfig): void` | `NativeKamperModule.start(config ?? {})` |
| `stop` | `(): void` | `NativeKamperModule.stop()` |
| `on` | `<K>(event: K, handler): KamperSubscription` | `NativeKamperModule.on{Capitalized}(handler)` |
| `off` | `(sub: KamperSubscription): void` | `sub.remove()` |
| `showOverlay` | `(): void` | `NativeKamperModule.showOverlay()` |
| `hideOverlay` | `(): void` | `NativeKamperModule.hideOverlay()` |

Event-name to TurboModule property map: `cpu→onCpu, fps→onFps, memory→onMemory, network→onNetwork, issue→onIssue, jank→onJank, gc→onGc, thermal→onThermal`

### Hooks — 9 Files

| Hook | Return Type | Config Key | Event Name |
|------|------------|------------|------------|
| `useKamper(config?)` | `KamperState` | all | all 8 |
| `useCpu()` | `CpuInfo \| null` | `cpu` | `cpu` |
| `useFps()` | `FpsInfo \| null` | `fps` | `fps` |
| `useMemory()` | `MemoryInfo \| null` | `memory` | `memory` |
| `useNetwork()` | `NetworkInfo \| null` | `network` | `network` |
| `useIssues()` | `IssueInfo[]` | `issues` | `issue` |
| `useJank()` | `JankInfo \| null` | `jank` | `jank` |
| `useGc()` | `GcInfo \| null` | `gc` | `gc` |
| `useThermal()` | `ThermalInfo \| null` | `thermal` | `thermal` |

### Ref-Counting Architecture

`useKamper.ts` exports two `@internal` helpers:

```typescript
export function _acquireEngine(config: KamperConfig): void  // start on first ref
export function _releaseEngine(): void                      // stop on last ref
```

All single-metric hooks call `_acquireEngine({MODULE: true})` on mount and `_releaseEngine()` on unmount. The module-level `activeRefs` counter ensures:
- First hook mount: `Kamper.start()` called once
- Subsequent hook mounts: no restart (ref count increments only)
- Non-last unmount: no stop (ref count decrements only)
- Last hook unmount: `Kamper.stop()` called once

### Barrel Export — index.ts

```typescript
export { Kamper } from './Kamper';
export type { KamperApi, KamperEventMap, KamperSubscription } from './Kamper';
export { useKamper, useCpu, useFps, useMemory, useNetwork, useIssues, useJank, useGc, useThermal } from './hooks/*';
export type { CpuInfo, FpsInfo, MemoryInfo, NetworkInfo, IssueInfo, JankInfo, GcInfo, ThermalInfo, KamperConfig } from './types';
export const showOverlay = (): void => _Kamper.showOverlay();  // D-12 top-level
export const hideOverlay = (): void => _Kamper.hideOverlay();  // D-12 top-level
```

### Test Results (TDD GREEN)

All 28 tests pass:
- `types.test.ts`: 9 tests (all GREEN — types.ts from Plan 01 already satisfies them)
- `hooks.test.ts`: 19 tests (GREEN after this plan's implementation)
  - useKamper umbrella: mount/unmount lifecycle
  - 8 single-metric hooks: mount/unmount lifecycle (describe.each)
  - Ref-counting: second mount does NOT call start again; last unmount stops engine

## Task Commits

| Task | Description | Commit | Files |
|------|-------------|--------|-------|
| Task 1 | Kamper.ts + index.ts | `6225b32` | Kamper.ts, index.ts |
| Task 2 | 9 hook files (GREEN) | `d88a7f9` | hooks/useKamper.ts + 8 hooks |
| chore | package.json test deps | `a91ec12` | package.json |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Added test script + testing devDependencies to package.json**
- **Found during:** Task 2 test execution setup
- **Issue:** `kamper/react-native/package.json` had no `test` script and no `@testing-library/react-native`, `@types/jest`, or `jest` devDependencies, making it impossible to run tests via `npm test`
- **Fix:** Added `"test": "jest --no-coverage"` script; added `@testing-library/react-native ^12.0.0`, `@types/jest ^29.6.3`, `jest ^29.6.3` to devDependencies
- **Files modified:** `kamper/react-native/package.json`
- **Commit:** `a91ec12`

**2. [Rule 1 - Bug] Fixed JSDoc comment text to pass grep-count acceptance criterion**
- **Found during:** Task 2 acceptance criteria verification
- **Issue:** `grep -c "Kamper.stop()" useKamper.ts` returned 2 (one code line, one JSDoc comment line); plan expects 1
- **Fix:** Changed JSDoc text from "Calls Kamper.stop() when refs hit zero" to "Calls the engine stop when refs hit zero"
- **Files modified:** `kamper/react-native/src/hooks/useKamper.ts` (comment only — no functional change)
- **Commit:** Part of `d88a7f9`

**3. [Rule 3 - Blocking] Created node_modules symlink + installed @testing-library/react-native**
- **Found during:** Task 2 test execution
- **Issue:** `kamper/react-native/` has no `node_modules`; `@testing-library/react-native` not in `demos/react-native/node_modules`; tests could not resolve imports
- **Fix:** Ran `npm install --save-dev @testing-library/react-native@^12.0.0` in `demos/react-native/`; created symlink `kamper/react-native/node_modules -> demos/react-native/node_modules` (gitignored)
- **Impact:** Worktree-local only; not committed (gitignored by `kamper/react-native/.gitignore`)
- **Note for Plan 06:** Formalize via workspace or local install script

## Known Stubs

None — all hooks return `null` (or `[]` for useIssues) as their initial state. This is the intended behavior: no data flows until the native engine emits events. The initial `null` state is a correct documented empty state, not a stub.

## Threat Flags

None. The JS/TS layer adds no new network endpoints, auth paths, or file access patterns. Per the plan's threat model:
- T-12-07: Accepted — handler exceptions don't propagate to native
- T-12-08: Mitigated — `useIssues` caps list at 100 entries
- T-12-09: Mitigated — JSDoc notes `showOverlay` is for `__DEV__` use; native guard in Plans 04/05
- T-12-10: Accepted — `_acquireEngine`/`_releaseEngine` are `@internal`, not exported from index.ts

## Self-Check: PASSED

All 11 created files exist. All 3 task commits confirmed in git log:
- `6225b32` — FOUND (feat: Kamper.ts + index.ts)
- `d88a7f9` — FOUND (feat: 9 hook files GREEN)
- `a91ec12` — FOUND (chore: package.json test deps)

All 28 Jest tests pass (types.test.ts: 9 + hooks.test.ts: 19).
