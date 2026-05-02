---
phase: 23
plan: "02"
subsystem: gpu
tags: [kmp, tdd, commonMain, gpu, sentinels]
dependency_graph:
  requires: ["23-01 (GPU module scaffold, block-commented test scaffolds)"]
  provides:
    - "GpuInfo data class with INVALID + UNSUPPORTED sentinels"
    - "GpuConfig with Builder and DEFAULT"
    - "GpuWatcher extending Watcher<GpuInfo>"
    - "GpuPerformance extending Performance<GpuConfig, IWatcher<GpuInfo>, GpuInfo>"
    - "expect val GpuModule: PerformanceModule<GpuConfig, GpuInfo>"
    - "GpuInfoRepository interface"
    - "GpuInfoSource interface"
    - "Temporary jvmMain Module.kt stub (Plan 03 overwrites)"
  affects:
    - "libs/modules/gpu/src/commonMain/"
    - "libs/modules/gpu/src/commonTest/"
    - "libs/modules/gpu/src/jvmMain/"
tech_stack:
  added: []
  patterns:
    - "TDD RED->GREEN cycle for sentinel/builder tests"
    - "expect/actual Module declaration (expect only; actuals in Plans 03/04/05)"
    - "INVALID (-1.0) + UNSUPPORTED (-2.0) dual-sentinel pattern (D-03/D-04/D-09/D-13)"
key_files:
  created:
    - libs/modules/gpu/src/commonMain/kotlin/com/smellouk/kamper/gpu/GpuInfo.kt
    - libs/modules/gpu/src/commonMain/kotlin/com/smellouk/kamper/gpu/GpuConfig.kt
    - libs/modules/gpu/src/commonMain/kotlin/com/smellouk/kamper/gpu/GpuWatcher.kt
    - libs/modules/gpu/src/commonMain/kotlin/com/smellouk/kamper/gpu/GpuPerformance.kt
    - libs/modules/gpu/src/commonMain/kotlin/com/smellouk/kamper/gpu/Module.kt
    - libs/modules/gpu/src/commonMain/kotlin/com/smellouk/kamper/gpu/repository/GpuInfoRepository.kt
    - libs/modules/gpu/src/commonMain/kotlin/com/smellouk/kamper/gpu/repository/source/GpuInfoSource.kt
    - libs/modules/gpu/src/jvmMain/kotlin/com/smellouk/kamper/gpu/Module.kt
  modified:
    - libs/modules/gpu/src/commonTest/kotlin/com/smellouk/kamper/gpu/GpuInfoSentinelTest.kt
    - libs/modules/gpu/src/commonTest/kotlin/com/smellouk/kamper/gpu/GpuInfoUnsupportedTest.kt
    - libs/modules/gpu/src/commonTest/kotlin/com/smellouk/kamper/gpu/GpuConfigBuilderTest.kt
decisions:
  - "GpuWatcher does not explicitly implement IWatcher<GpuInfo> — mirrors CpuWatcher which inherits it via Watcher<GpuInfo>"
  - "Temporary jvmMain Module.kt stub uses error() not null — fails fast with a clear message if accidentally invoked before Plan 03 lands"
  - "EMPTY import from com.smellouk.kamper.api.EMPTY extension property — mirrors CpuConfig exactly"
metrics:
  duration: "~2 minutes"
  completed: "2026-05-01T22:55:44Z"
  tasks_completed: 2
  files_created: 8
  files_modified: 3
---

# Phase 23 Plan 02: GPU commonMain Layer Summary

TDD RED->GREEN cycle for GpuInfo sentinels and GpuConfig builder; full commonMain interface layer (7 files) enabling parallel Wave 2 platform actuals.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 RED | Un-comment three commonTest scaffold files | 2be372b | GpuInfoSentinelTest.kt, GpuInfoUnsupportedTest.kt, GpuConfigBuilderTest.kt |
| 1 GREEN | Implement GpuInfo + GpuConfig | 7db4cbf | GpuInfo.kt, GpuConfig.kt |
| 2 | Add commonMain glue layer + jvmMain stub | b93539e | GpuWatcher.kt, GpuPerformance.kt, Module.kt (expect), GpuInfoRepository.kt, GpuInfoSource.kt, jvmMain/Module.kt |

## What Was Built

### Task 1 — TDD RED->GREEN: GpuInfo + GpuConfig

**RED phase (commit 2be372b):** Un-commented the three block-commented test scaffold files from Plan 01. Tests reference `GpuInfo` and `GpuConfig` which did not exist — RED gate confirmed via `compileTestKotlinJvm` failure with "Unresolved reference: GpuInfo".

**GREEN phase (commit 7db4cbf):**

`GpuInfo.kt` — public data class implementing `Info` with three Double fields per D-01:
- `utilization: Double` — GPU usage percentage 0..100
- `usedMemoryMb: Double` — VRAM used in MB
- `totalMemoryMb: Double` — total VRAM in MB
- `INVALID = GpuInfo(-1.0, -1.0, -1.0)` — transient runtime read failure sentinel (D-03/D-13)
- `UNSUPPORTED = GpuInfo(-2.0, -2.0, -2.0)` — permanent platform capability gap sentinel (D-04/D-09/D-13)

`GpuConfig.kt` — public data class implementing `Config` with `Builder` and `DEFAULT` companion. Builder pattern mirrors CpuConfig exactly.

All 4 test methods GREEN:
- `GpuInfoSentinelTest.INVALID should have all Double fields set to -1_0`
- `GpuInfoUnsupportedTest.UNSUPPORTED should have all fields set to -2_0`
- `GpuInfoUnsupportedTest.INVALID and UNSUPPORTED must not be equal`
- `GpuConfigBuilderTest.build should build correct config`

### Task 2 — commonMain Glue Layer + Temporary JVM Stub

Five additional commonMain files created to complete the module contract:

- `GpuWatcher.kt` — `internal class GpuWatcher` extends `Watcher<GpuInfo>` with standard four-param constructor (defaultDispatcher, mainDispatcher, repository, logger). Mirrors CpuWatcher pattern.
- `GpuPerformance.kt` — `internal class GpuPerformance` extends `Performance<GpuConfig, IWatcher<GpuInfo>, GpuInfo>`. Mirrors CpuPerformance.
- `Module.kt` (commonMain) — `expect val GpuModule: PerformanceModule<GpuConfig, GpuInfo>` declaration. Actuals to be provided by Plans 03 (JVM), 04 (Android/iOS/tvOS), 05 (macOS/JS/WASM).
- `GpuInfoRepository.kt` — `internal interface GpuInfoRepository : InfoRepository<GpuInfo>` at package `com.smellouk.kamper.gpu.repository`.
- `GpuInfoSource.kt` — `internal interface GpuInfoSource { fun getInfo(): GpuInfo }` at package `com.smellouk.kamper.gpu.repository.source`. Single-call point-in-time API (no DTO/delta layer needed unlike CPU).

**Temporary stub** `jvmMain/Module.kt` — `actual val GpuModule` that calls `error(...)` so the JVM target compiles and jvmTest passes. Plan 03 overwrites this with the real OSHI-backed implementation.

## Deviations from Plan

None — plan executed exactly as written.

## Known Stubs

| File | Line | Reason |
|------|------|--------|
| `libs/modules/gpu/src/jvmMain/kotlin/com/smellouk/kamper/gpu/Module.kt` | 6 | Temporary `error(...)` stub — Plan 23-03 (JVM OSHI actual) overwrites with real implementation |

This stub is intentional and documented in the plan. It does NOT block the plan's goal (commonMain layer + GREEN tests). Plan 03 resolves it.

## Threat Flags

No new network endpoints, auth paths, or schema changes introduced. Public API additions are additive-only per ADR-004 (T-23-06 mitigated). Kotlin `data class` structural equality confirms INVALID and UNSUPPORTED are distinct instances (T-23-05 mitigated by test).

## TDD Gate Compliance

| Gate | Commit | Status |
|------|--------|--------|
| RED (test commit) | 2be372b | PASS — `test(gpu): add failing GpuInfo + GpuConfig tests` |
| GREEN (feat commit) | 7db4cbf | PASS — `feat(gpu): implement GpuInfo + GpuConfig with INVALID/UNSUPPORTED sentinels` |

## Verification Results

```
./gradlew :libs:modules:gpu:jvmTest --rerun-tasks    # PASS — 4 tests GREEN
ls libs/modules/gpu/src/commonMain/kotlin/com/smellouk/kamper/gpu/ | wc -l  # 6 (5 files + 1 dir)
ls libs/modules/gpu/src/commonMain/kotlin/com/smellouk/kamper/gpu/repository/ | wc -l  # 2 (file + source dir)
ls libs/modules/gpu/src/commonMain/kotlin/com/smellouk/kamper/gpu/repository/source/ | wc -l  # 1
grep -L '@Ignore' libs/modules/gpu/src/commonTest/kotlin/com/smellouk/kamper/gpu/*.kt | wc -l  # 3
```

## Self-Check: PASSED

Files verified:
- libs/modules/gpu/src/commonMain/kotlin/com/smellouk/kamper/gpu/GpuInfo.kt: FOUND
- libs/modules/gpu/src/commonMain/kotlin/com/smellouk/kamper/gpu/GpuConfig.kt: FOUND
- libs/modules/gpu/src/commonMain/kotlin/com/smellouk/kamper/gpu/GpuWatcher.kt: FOUND
- libs/modules/gpu/src/commonMain/kotlin/com/smellouk/kamper/gpu/GpuPerformance.kt: FOUND
- libs/modules/gpu/src/commonMain/kotlin/com/smellouk/kamper/gpu/Module.kt: FOUND
- libs/modules/gpu/src/commonMain/kotlin/com/smellouk/kamper/gpu/repository/GpuInfoRepository.kt: FOUND
- libs/modules/gpu/src/commonMain/kotlin/com/smellouk/kamper/gpu/repository/source/GpuInfoSource.kt: FOUND
- libs/modules/gpu/src/jvmMain/kotlin/com/smellouk/kamper/gpu/Module.kt: FOUND (temporary stub)
- libs/modules/gpu/src/commonTest/kotlin/com/smellouk/kamper/gpu/GpuInfoSentinelTest.kt: FOUND (un-commented)
- libs/modules/gpu/src/commonTest/kotlin/com/smellouk/kamper/gpu/GpuInfoUnsupportedTest.kt: FOUND (un-commented)
- libs/modules/gpu/src/commonTest/kotlin/com/smellouk/kamper/gpu/GpuConfigBuilderTest.kt: FOUND (un-commented)

Commits verified:
- 2be372b: test(gpu): add failing GpuInfo + GpuConfig tests
- 7db4cbf: feat(gpu): implement GpuInfo + GpuConfig with INVALID/UNSUPPORTED sentinels
- b93539e: feat(gpu): add commonMain glue layer and temporary jvmMain stub
