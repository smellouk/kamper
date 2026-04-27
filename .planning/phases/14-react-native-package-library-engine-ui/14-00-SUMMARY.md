---
phase: 14
plan: "00"
subsystem: react-native
tags: [jest, testing, wave-0, test-scaffold, turbomodule, mock]
dependency_graph:
  requires: []
  provides:
    - kamper/react-native/jest.config.js
    - kamper/react-native/src/__tests__/NativeKamperModule.mock.ts
    - kamper/react-native/src/__tests__/types.test.ts
    - kamper/react-native/src/__tests__/hooks.test.ts
  affects:
    - Plans 14-01 through 14-06 (test surface they must turn GREEN)
tech_stack:
  added:
    - Jest with @react-native/jest-preset
    - moduleNameMapper for TurboModule mock injection
  patterns:
    - Wave 0 test-first scaffold: failing tests declare contracts for downstream plans
    - TurboModule mock pattern: jest.fn() + makeEmitter() for EventEmitter<T> shape
key_files:
  created:
    - kamper/react-native/jest.config.js
    - kamper/react-native/src/__tests__/NativeKamperModule.mock.ts
    - kamper/react-native/src/__tests__/types.test.ts
    - kamper/react-native/src/__tests__/hooks.test.ts
  modified: []
decisions:
  - "Wave 0 files are intentionally RED until Plans 01 + 03 land — this is by design"
  - "moduleNameMapper uses two patterns (^\\./NativeKamperModule$ and ^\\.\\./NativeKamperModule$) to handle both src/ and __tests__/ relative import paths"
  - "No watch mode in jest.config.js — VALIDATION.md requires single-shot runs for automated sampling"
  - "transformIgnorePatterns comment contains keyword 'watchAll' — comment is explaining absence of watch mode, not enabling it"
metrics:
  duration: "2m"
  completed: "2026-04-27"
  tasks_completed: 1
  tasks_total: 1
  files_created: 4
  files_modified: 0
---

# Phase 14 Plan 00: Wave 0 Jest Test Scaffold Summary

Wave 0 Jest test infrastructure installed for react-native-kamper: jest.config.js with @react-native/jest-preset + moduleNameMapper, NativeKamperModule TurboModule mock, and two failing skeleton test suites (types + hooks lifecycle) that declare the D-09 type contracts and D-07 hook contracts for downstream Plans 01 and 03.

## What Was Built

### 4 Wave 0 Files Created

| File | Purpose | State |
|------|---------|-------|
| `kamper/react-native/jest.config.js` | Jest config: @react-native/jest-preset, moduleNameMapper, transformIgnorePatterns, no watch | Ready |
| `kamper/react-native/src/__tests__/NativeKamperModule.mock.ts` | TurboModule mock: 4 imperative jest.fn() + 8 EventEmitter jest.fn() + reset helper | Ready |
| `kamper/react-native/src/__tests__/types.test.ts` | Skeleton: 9 D-09 type shape assertions (RED until Plan 01 creates src/types.ts) | RED |
| `kamper/react-native/src/__tests__/hooks.test.ts` | Skeleton: useKamper + 8 single-metric hook mount/unmount + ref-count assertions (RED until Plan 03) | RED |

### Jest Configuration

The `jest.config.js` wires:
- **preset**: `@react-native/jest-preset` — leverages RN test environment setup from demo's `node_modules`
- **moduleNameMapper**: redirects `./NativeKamperModule` imports in test files to the mock, preventing real TurboModule registry access
- **transformIgnorePatterns**: ensures `kamper/react-native/src` TypeScript is compiled during tests
- **testMatch**: `src/__tests__/**/*.test.ts` — picks up both skeleton suites
- **No watch mode**: single-shot execution required by VALIDATION.md sampling spec

### NativeKamperModule Mock

The mock mirrors the Plan 01 Codegen spec exactly (T-12-W0-01 threat mitigation):
- 4 imperative methods: `start`, `stop`, `showOverlay`, `hideOverlay` → `jest.fn()`
- 8 EventEmitter functions: `onCpu`, `onFps`, `onMemory`, `onNetwork`, `onIssue`, `onJank`, `onGc`, `onThermal` → `makeEmitter()` (returns `{remove: jest.fn()}`)
- Default export: combined object matching `TurboModuleRegistry.getEnforcing<Spec>('KamperModule')`
- `_resetNativeKamperModuleMocks()`: clears all mock call records; call from `beforeEach`

### Skeleton Test Suites (RED State by Design)

**types.test.ts** — 1 `describe` + 9 `it()` blocks asserting:
- `CpuInfo`: 5 numeric fields (totalUseRatio, appRatio, userRatio, systemRatio, ioWaitRatio)
- `FpsInfo`: fps as number
- `MemoryInfo`: 4 numbers + isLowMemory boolean
- `NetworkInfo`: rxMb + txMb
- `IssueInfo`: required fields + optional durationMs/threadName
- `JankInfo`: 3 fields
- `GcInfo`: 3 fields
- `ThermalInfo`: state string + isThrottling boolean
- `KamperConfig`: 8 optional boolean module flags (D-08)

**hooks.test.ts** — 3 describe blocks:
- `useKamper umbrella hook`: mount calls `nativeStart` once; unmount calls `nativeStop` once
- `describe.each` for 8 single-metric hooks: each mount triggers start, each unmount triggers stop
- Shared ref counting: second hook mount does NOT call start again; last unmount calls stop

## Deviations from Plan

None — plan executed exactly as written.

## Notes for Downstream Plans

**Plan 01 (types.ts + NativeKamperModule.ts):**
- Must create `kamper/react-native/src/types.ts` exporting the 9 types asserted in `types.test.ts`
- Must include `@testing-library/react-native: ^12.0.0` in `package.json` devDependencies so `hooks.test.ts` can import `renderHook`
- Once types.ts ships, `types.test.ts` turns GREEN automatically

**Plan 03 (hooks/*.ts + Kamper.ts):**
- Must create `kamper/react-native/src/hooks/useKamper.ts` and 8 single-metric hooks
- Hooks must implement ref counting so multiple active hooks share one engine instance
- Once hooks ship, `hooks.test.ts` turns GREEN automatically

**Plan 06 / Phase wrap-up:**
- After both suites are GREEN, flip `14-VALIDATION.md` frontmatter to `wave_0_complete: true` (it is already `true`) and `nyquist_compliant: true` (already `true`)

## Threat Mitigation

| Threat ID | Disposition | Status |
|-----------|-------------|--------|
| T-12-W0-01 | Mitigate | Mock exports exactly match Plan 01 Codegen spec (4 imperative + 8 emitters); acceptance criteria grep enforces count |
| T-12-W0-02 | Accept | moduleNameMapper redirect is test-time only; bundled production code never resolves mock path |

## Self-Check: PASSED

Files verified:
- `kamper/react-native/jest.config.js` — FOUND
- `kamper/react-native/src/__tests__/NativeKamperModule.mock.ts` — FOUND
- `kamper/react-native/src/__tests__/types.test.ts` — FOUND
- `kamper/react-native/src/__tests__/hooks.test.ts` — FOUND

Commit 0287d05 — FOUND
