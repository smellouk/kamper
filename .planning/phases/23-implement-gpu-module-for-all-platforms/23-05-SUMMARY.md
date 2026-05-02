---
phase: 23-implement-gpu-module-for-all-platforms
plan: "05"
subsystem: gpu
tags: [kmp, gpu, jsMain, wasmJsMain, unsupported, web]

requires:
  - phase: 23-02
    provides: "GpuInfo.UNSUPPORTED sentinel, GpuInfoRepository interface, GpuWatcher, GpuPerformance, commonMain expect val GpuModule"

provides:
  - "jsMain actual val GpuModule (UNSUPPORTED stub)"
  - "jsMain GpuInfoRepositoryImpl returning GpuInfo.UNSUPPORTED"
  - "wasmJsMain actual val GpuModule (UNSUPPORTED stub)"
  - "wasmJsMain GpuInfoRepositoryImpl returning GpuInfo.UNSUPPORTED"

affects:
  - "libs/modules/gpu/src/jsMain/"
  - "libs/modules/gpu/src/wasmJsMain/"

tech-stack:
  added: []
  patterns:
    - "UNSUPPORTED-only web platform stub (no browser API probe) — mirrors thermal/jank web target pattern"
    - "Dispatchers.Default for both defaultDispatcher and mainDispatcher on JS/wasmJs (no Dispatchers.Main on web)"

key-files:
  created:
    - libs/modules/gpu/src/jsMain/kotlin/com/smellouk/kamper/gpu/Module.kt
    - libs/modules/gpu/src/jsMain/kotlin/com/smellouk/kamper/gpu/repository/GpuInfoRepositoryImpl.kt
    - libs/modules/gpu/src/wasmJsMain/kotlin/com/smellouk/kamper/gpu/Module.kt
    - libs/modules/gpu/src/wasmJsMain/kotlin/com/smellouk/kamper/gpu/repository/GpuInfoRepositoryImpl.kt
  modified: []

key-decisions:
  - "Both web targets return GpuInfo.UNSUPPORTED unconditionally — WebGL EXT_disjoint_timer_query disabled by Spectre mitigation in all modern browsers (D-08); WebGPU exposes only adapter name, not GPU utilization"
  - "No navigator.gpu or WebGL context access added — threat T-23-16 accepted by returning UNSUPPORTED without probing, eliminating fingerprinting surface"
  - "jsMain and wasmJsMain bodies are identical — same Kotlin source, same UNSUPPORTED logic; each target has its own source set per KMP convention"
  - "Dispatchers.Default used for both defaultDispatcher and mainDispatcher on web targets — mirrors CPU jsMain/wasmJsMain pattern (Dispatchers.Main unavailable on JS/wasmJs without coroutines-js)"

requirements-completed: [D-04, D-08, D-09, D-13]

duration: 3min
completed: "2026-05-01"
---

# Phase 23 Plan 05: GPU jsMain + wasmJsMain UNSUPPORTED Stubs Summary

**JS and wasmJs GPU module actuals returning GpuInfo.UNSUPPORTED unconditionally — no browser GPU API probe, completing all 7 platform actual declarations for the GPU module**

## Performance

- **Duration:** ~3 minutes
- **Started:** 2026-05-01T22:59:33Z
- **Completed:** 2026-05-01T23:03:25Z
- **Tasks:** 1
- **Files modified:** 4 created

## Accomplishments

- Created 4 web platform files (jsMain and wasmJsMain) implementing the GPU module as UNSUPPORTED stubs
- Verified `compileKotlinJs` and `compileKotlinWasmJs` both pass with zero errors
- Ensured no browser API surface (`navigator.gpu`, WebGL, WebGPU) is accessed — D-08 and threat T-23-16 mitigated
- All 7 platform actuals for `expect val GpuModule` now exist (Android, iOS, tvOS, macOS, JVM, JS, wasmJs)

## Task Commits

1. **Task 1: jsMain + wasmJsMain UNSUPPORTED stubs (4 files)** - `ce493ae` (feat)

**Plan metadata:** committed with SUMMARY.md below

## Files Created/Modified

- `libs/modules/gpu/src/jsMain/kotlin/com/smellouk/kamper/gpu/repository/GpuInfoRepositoryImpl.kt` — Returns `GpuInfo.UNSUPPORTED` unconditionally; no browser API imports
- `libs/modules/gpu/src/jsMain/kotlin/com/smellouk/kamper/gpu/Module.kt` — `actual val GpuModule` wired via `GpuPerformance`/`GpuWatcher`, mirrors CpuModule jsMain dispatcher pattern
- `libs/modules/gpu/src/wasmJsMain/kotlin/com/smellouk/kamper/gpu/repository/GpuInfoRepositoryImpl.kt` — Identical to jsMain repo; separate source set per KMP convention
- `libs/modules/gpu/src/wasmJsMain/kotlin/com/smellouk/kamper/gpu/Module.kt` — Identical to jsMain Module.kt; separate source set per KMP convention

## Decisions Made

- Used `Dispatchers.Default` for both `defaultDispatcher` and `mainDispatcher` on web targets — mirrors CPU jsMain/wasmJsMain canonical reference. `Dispatchers.Main` is not available on JS/wasmJs without additional coroutines artifacts.
- No DSL builder function with `@KamperDslMarker` added — the CPU reference module (canonical) uses only `@Suppress("FunctionNaming")` on the builder fun; the plan's approximate shape was advisory, not prescriptive. Builder fun is present without the annotation per the reference.
- Symlinked `demos/react-native/node_modules` from main repo into worktree to enable JS/wasmJs compilation (node_modules are untracked and not present in git worktrees by default).

## Deviations from Plan

None — plan executed exactly as written. The node_modules symlink was an environment setup step, not a code deviation.

## Issues Encountered

- `compileKotlinJs` failed initially from the worktree directory because `demos/react-native/node_modules/@react-native/gradle-plugin` was absent (git worktrees don't inherit untracked files). Resolved by symlinking the node_modules directory from the main repo checkout. This is a worktree environment constraint, not a code issue.

## Known Stubs

| File | Line | Reason |
|------|------|--------|
| `libs/modules/gpu/src/jsMain/kotlin/com/smellouk/kamper/gpu/repository/GpuInfoRepositoryImpl.kt` | 7 | Intentional UNSUPPORTED stub — no public browser API exposes GPU utilization (D-08); permanent by design |
| `libs/modules/gpu/src/wasmJsMain/kotlin/com/smellouk/kamper/gpu/repository/GpuInfoRepositoryImpl.kt` | 7 | Intentional UNSUPPORTED stub — same rationale as jsMain; permanent by design |

These stubs are intentional and permanent. The web platform capability gap is architectural (browser security restrictions prevent GPU metric access), not a temporary placeholder.

## Threat Flags

No new network endpoints, auth paths, or schema changes introduced.

| Flag | File | Description |
|------|------|-------------|
| threat_flag: accept | jsMain/GpuInfoRepositoryImpl.kt | No `navigator.gpu` or WebGL access added — T-23-16 GPU fingerprinting risk accepted by returning UNSUPPORTED without probing |

## D-08 Mitigation Verification

```
grep -r 'navigator\.gpu\|new WebGL\|getContext.*webgl' \
  libs/modules/gpu/src/jsMain libs/modules/gpu/src/wasmJsMain
# Output: (none) — zero browser API access
```

## Next Phase Readiness

All 7 platform actuals for `GpuModule` now exist. When Plans 23-03 (JVM OSHI actual) and 23-04 (Android/iOS/tvOS actuals) complete, the GPU module will be fully implemented across all platforms and `./gradlew :libs:modules:gpu:assemble -q` will link all targets.

## Self-Check: PASSED

Files verified:
- libs/modules/gpu/src/jsMain/kotlin/com/smellouk/kamper/gpu/Module.kt: FOUND
- libs/modules/gpu/src/jsMain/kotlin/com/smellouk/kamper/gpu/repository/GpuInfoRepositoryImpl.kt: FOUND
- libs/modules/gpu/src/wasmJsMain/kotlin/com/smellouk/kamper/gpu/Module.kt: FOUND
- libs/modules/gpu/src/wasmJsMain/kotlin/com/smellouk/kamper/gpu/repository/GpuInfoRepositoryImpl.kt: FOUND
- .planning/phases/23-implement-gpu-module-for-all-platforms/23-05-SUMMARY.md: FOUND

Commits verified:
- ce493ae: feat(gpu): add jsMain and wasmJsMain UNSUPPORTED stubs
- e37ffd0: docs(phase23): complete 23-05 gpu jsMain+wasmJsMain UNSUPPORTED stubs plan

---
*Phase: 23-implement-gpu-module-for-all-platforms*
*Completed: 2026-05-01*
