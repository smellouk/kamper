---
phase: 23
plan: "03"
subsystem: gpu
tags: [kmp, android, jvm, oshi, sysfs, tdd, probe-before-read]
dependency_graph:
  requires: ["23-02 (GPU commonMain: GpuInfo, GpuConfig, GpuWatcher, GpuPerformance, expect Module, interfaces)"]
  provides:
    - "Android actual GpuModule with kgsl Adreno probe-before-read + Mali devfreq fallback"
    - "JVM actual GpuModule with OSHI 7.0.0 partial data (utilization=-1.0, totalMb=vramMb)"
    - "KgslAccessibilityProviderTest GREEN"
    - "OshiGpuInfoSourceTest GREEN"
  affects:
    - "libs/modules/gpu/src/androidMain/"
    - "libs/modules/gpu/src/jvmMain/"
    - "libs/modules/gpu/src/androidUnitTest/"
    - "libs/modules/gpu/src/jvmTest/"
tech_stack:
  added: ["oshi-core 7.0.0 consumed in jvmMain (declared in Wave 1)"]
  patterns:
    - "probe-before-read with Boolean? cache (null=unprobed, false=permanently INVALID, true=read)"
    - "TDD RED->GREEN per task"
    - "D-06 safety rule: try/catch on every platform call absorbing to GpuInfo.INVALID"
    - "D-02 partial data: JVM returns utilization=-1.0 even when VRAM found"
    - "D-05 dual-path Android: kgsl Adreno first, devfreq Mali fallback"
    - "frequency-proxy utilization: (cur_freq/max_freq)*100 for Mali devfreq"
key_files:
  created:
    - libs/modules/gpu/src/androidMain/kotlin/com/smellouk/kamper/gpu/Module.kt
    - libs/modules/gpu/src/androidMain/kotlin/com/smellouk/kamper/gpu/repository/GpuInfoRepositoryImpl.kt
    - libs/modules/gpu/src/androidMain/kotlin/com/smellouk/kamper/gpu/repository/KgslAccessibilityProvider.kt
    - libs/modules/gpu/src/androidMain/kotlin/com/smellouk/kamper/gpu/repository/DevfreqAccessibilityProvider.kt
    - libs/modules/gpu/src/androidMain/kotlin/com/smellouk/kamper/gpu/repository/source/KgslGpuInfoSource.kt
    - libs/modules/gpu/src/androidMain/kotlin/com/smellouk/kamper/gpu/repository/source/DevfreqGpuInfoSource.kt
    - libs/modules/gpu/src/jvmMain/kotlin/com/smellouk/kamper/gpu/repository/GpuInfoRepositoryImpl.kt
    - libs/modules/gpu/src/jvmMain/kotlin/com/smellouk/kamper/gpu/repository/source/OshiGpuInfoSource.kt
  modified:
    - libs/modules/gpu/src/jvmMain/kotlin/com/smellouk/kamper/gpu/Module.kt (replaced Plan 02 error() stub)
    - libs/modules/gpu/src/androidUnitTest/kotlin/com/smellouk/kamper/gpu/KgslAccessibilityProviderTest.kt (un-commented)
    - libs/modules/gpu/src/jvmTest/kotlin/com/smellouk/kamper/gpu/OshiGpuInfoSourceTest.kt (un-commented)
decisions:
  - "D-02 partial data confirmed: JVM OshiGpuInfoSource returns GpuInfo(utilization=-1.0, usedMemoryMb=-1.0, totalMemoryMb=vramMb) when VRAM found — OSHI has no utilization API"
  - "D-05 probe-before-read: KgslAccessibilityProvider.isAccessible() returns false on Robolectric/JVM host (no /sys/class/kgsl/) confirming the catch-and-return-false path works"
  - "devfreq Mali utilization is frequency-proxy (cur_freq/max_freq*100) — not true busy; documented as best-effort per 23-RESEARCH Open Question #2"
  - "Magic number constants (MIN_PCT=0.0, MAX_PCT=100.0, PCT_MULTIPLIER=100.0) extracted to companion objects to satisfy detekt MagicNumber rule"
  - "Android Module.kt follows CpuModule.kt pattern exactly: @Suppress(FunctionNaming) on DSL function, no @KamperDslMarker on function"
metrics:
  duration: "~7 minutes"
  completed: "2026-05-01T23:05:00Z"
  tasks_completed: 2
  files_created: 8
  files_modified: 3
---

# Phase 23 Plan 03: GPU Android + JVM Platform Actuals Summary

Android actual (kgsl Adreno probe-before-read with Mali devfreq fallback) and JVM actual (OSHI 7.0.0 partial data — utilization unavailable) for GpuModule; 2 tests activated and GREEN; Plan 02 jvmMain stub replaced.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 RED | Activate KgslAccessibilityProviderTest | 33a8759 | KgslAccessibilityProviderTest.kt |
| 1 GREEN | Android actual: Module + RepositoryImpl + 2 providers + 2 sources | 3d59200 | Module.kt, GpuInfoRepositoryImpl.kt, KgslAccessibilityProvider.kt, DevfreqAccessibilityProvider.kt, KgslGpuInfoSource.kt, DevfreqGpuInfoSource.kt |
| 2 RED | Activate OshiGpuInfoSourceTest | fa4065f | OshiGpuInfoSourceTest.kt |
| 2 GREEN | JVM actual: Module (replaces stub) + RepositoryImpl + OshiGpuInfoSource | 3446425 | Module.kt (overwrite), GpuInfoRepositoryImpl.kt, OshiGpuInfoSource.kt |

## What Was Built

### Task 1 — Android Actual (kgsl + devfreq probe-before-read)

**RED phase (commit 33a8759):** Un-commented `KgslAccessibilityProviderTest.kt` referencing `KgslAccessibilityProvider` which did not exist. Compile failure confirmed RED gate.

**GREEN phase (commit 3d59200):** Six androidMain source files:

- `KgslAccessibilityProvider.kt` — stateless object probing `/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage`. Returns `false` on any exception (no sysfs on non-Android host). Probe result cached in repository, not in this object.
- `DevfreqAccessibilityProvider.kt` — stateless object probing `/sys/class/devfreq/` for a Mali/GPU directory with readable `cur_freq`. Also provides `findMaliDir()` helper consumed by `DevfreqGpuInfoSource`.
- `KgslGpuInfoSource.kt` — reads Adreno GPU busy percentage; `toLongOrNull()` parse with `MIN_PCT..MAX_PCT` range clamp; `usedMemoryMb=-1.0, totalMemoryMb=-1.0` per D-02 (no public memory path without root). All exceptions caught with `logger.log(e.stackTraceToString())` per D-06.
- `DevfreqGpuInfoSource.kt` — reads Mali devfreq `cur_freq`/`max_freq` and computes frequency-proxy utilization `(cur/max*100)`, clamped via `coerceIn(MIN_PCT, MAX_PCT)`. Returns `GpuInfo.INVALID` if `DevfreqAccessibilityProvider.findMaliDir()` returns null.
- `GpuInfoRepositoryImpl.kt` — probe-before-read with `platformSupported: Boolean?` cache (null=unprobed, false=permanently INVALID). On first call probes both providers; returns kgsl result, falls back to devfreq if kgsl returns INVALID.
- `Module.kt` — `actual val GpuModule` + `fun GpuModule(builder)` DSL function, following CpuModule.kt Android pattern exactly.

**Test result:** `KgslAccessibilityProviderTest.isAccessible returns false on hosts without sysfs kgsl path` — GREEN. On a Robolectric/JVM host, `/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage` doesn't exist so `FileInputStream` throws `FileNotFoundException`, which the `catch (_: Exception)` block absorbs, returning `false`. This validates D-05 probe semantics.

### Task 2 — JVM Actual (OSHI 7.0.0 partial data)

**RED phase (commit fa4065f):** Un-commented `OshiGpuInfoSourceTest.kt` referencing `OshiGpuInfoSource` which did not exist. Compile failure confirmed RED gate. (Plan 02 jvmMain stub also throws `error(...)` at runtime — double RED.)

**GREEN phase (commit 3446425):** Three jvmMain source files:

- `OshiGpuInfoSource.kt` — OSHI-backed source. `SystemInfo().hardware` constructed lazily via `runCatching` (mitigates T-23-10: headless server with no JNA). `getInfo()` strategy:
  - `hardware == null` or `graphicsCards.isEmpty()` → `GpuInfo.UNSUPPORTED`
  - `cards[0].vRam <= 0` → `GpuInfo.UNSUPPORTED`
  - `vRam > 0` → `GpuInfo(utilization=-1.0, usedMemoryMb=-1.0, totalMemoryMb=vramMb)` per D-02
  - Any caught exception → `GpuInfo.INVALID`
- `GpuInfoRepositoryImpl.kt` — one-line delegate: `override fun getInfo(): GpuInfo = source.getInfo()`
- `Module.kt` — overwrites Plan 02 `error(...)` stub. `mainDispatcher = Dispatchers.Default` (JVM has no Main dispatcher, mirrors CpuJvmModule).

**Test result:** `OshiGpuInfoSource never throws and never returns null` — GREEN. Returns either `UNSUPPORTED` (macOS CI host may or may not have a GPU visible to OSHI) or partial data. `assertNotNull(info)` passes in all cases.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Detekt MagicNumber violations in KgslGpuInfoSource and DevfreqGpuInfoSource**
- **Found during:** Post-GREEN detekt run
- **Issue:** `0.0` and `100.0` in `pct in 0.0..100.0` (KgslGpuInfoSource) and `coerceIn(0.0, 100.0)` with `* 100.0` (DevfreqGpuInfoSource) triggered detekt `MagicNumber` rule (`maxIssues: 0`)
- **Fix:** Extracted named constants `MIN_PCT = 0.0`, `MAX_PCT = 100.0`, and `PCT_MULTIPLIER = 100.0` into `private companion object` blocks
- **Files modified:** `KgslGpuInfoSource.kt`, `DevfreqGpuInfoSource.kt`
- **Commit:** fe4223f

## Known Stubs

None. The Plan 02 jvmMain `error(...)` stub has been replaced with the real OSHI-backed implementation. No placeholder data sources exist.

## Threat Surface Scan

All mitigations in the plan's threat register confirmed implemented:

| Threat | Mitigation | Location |
|--------|-----------|----------|
| T-23-08 (Tampering - sysfs content) | `toLongOrNull()` + `MIN_PCT..MAX_PCT` range check | KgslGpuInfoSource.kt |
| T-23-09 (Tampering - devfreq content) | `max > 0L` guard + `coerceIn(MIN_PCT, MAX_PCT)` | DevfreqGpuInfoSource.kt |
| T-23-10 (DoS - OSHI constructor throws) | `runCatching { SystemInfo().hardware }.getOrNull()` | OshiGpuInfoSource.kt |
| T-23-11 (EoP - SELinux SecurityException) | `catch (e: Exception)` absorbs all including SecurityException | KgslGpuInfoSource.kt, DevfreqGpuInfoSource.kt |

## TDD Gate Compliance

| Task | Gate | Commit | Status |
|------|------|--------|--------|
| Task 1 | RED (test) | 33a8759 | PASS — compile failure confirmed: KgslAccessibilityProvider unresolved |
| Task 1 | GREEN (feat) | 3d59200 | PASS — KgslAccessibilityProviderTest GREEN |
| Task 2 | RED (test) | fa4065f | PASS — compile failure confirmed: OshiGpuInfoSource unresolved |
| Task 2 | GREEN (feat) | 3446425 | PASS — OshiGpuInfoSourceTest GREEN |

## Verification Results

```
./gradlew :libs:modules:gpu:testDebugUnitTest -q    # PASS — KgslAccessibilityProviderTest GREEN
./gradlew :libs:modules:gpu:jvmTest -q              # PASS — 4 GREEN: 3 commonTest + 1 jvmTest
./gradlew detekt                                    # PASS — 0 issues after magic number fix
ls .../androidMain/kotlin/.../gpu/repository/ | wc -l          # 3 (Impl + 2 providers)
ls .../androidMain/kotlin/.../gpu/repository/source/ | wc -l   # 2 (kgsl + devfreq)
ls .../jvmMain/kotlin/.../gpu/repository/source/ | wc -l       # 1 (oshi)
grep -L '@Ignore' .../OshiGpuInfoSourceTest.kt     # file listed (no @Ignore)
grep -L '@Ignore' .../KgslAccessibilityProviderTest.kt          # file listed (no @Ignore)
grep -c 'error("GpuModule jvm actual not yet' .../jvmMain/.../Module.kt  # 0 (stub gone)
```

## Self-Check: PASSED

Files created/modified:

- libs/modules/gpu/src/androidMain/kotlin/com/smellouk/kamper/gpu/Module.kt: FOUND
- libs/modules/gpu/src/androidMain/kotlin/com/smellouk/kamper/gpu/repository/GpuInfoRepositoryImpl.kt: FOUND
- libs/modules/gpu/src/androidMain/kotlin/com/smellouk/kamper/gpu/repository/KgslAccessibilityProvider.kt: FOUND
- libs/modules/gpu/src/androidMain/kotlin/com/smellouk/kamper/gpu/repository/DevfreqAccessibilityProvider.kt: FOUND
- libs/modules/gpu/src/androidMain/kotlin/com/smellouk/kamper/gpu/repository/source/KgslGpuInfoSource.kt: FOUND
- libs/modules/gpu/src/androidMain/kotlin/com/smellouk/kamper/gpu/repository/source/DevfreqGpuInfoSource.kt: FOUND
- libs/modules/gpu/src/jvmMain/kotlin/com/smellouk/kamper/gpu/Module.kt: FOUND (stub overwritten)
- libs/modules/gpu/src/jvmMain/kotlin/com/smellouk/kamper/gpu/repository/GpuInfoRepositoryImpl.kt: FOUND
- libs/modules/gpu/src/jvmMain/kotlin/com/smellouk/kamper/gpu/repository/source/OshiGpuInfoSource.kt: FOUND
- libs/modules/gpu/src/androidUnitTest/kotlin/com/smellouk/kamper/gpu/KgslAccessibilityProviderTest.kt: FOUND (un-commented)
- libs/modules/gpu/src/jvmTest/kotlin/com/smellouk/kamper/gpu/OshiGpuInfoSourceTest.kt: FOUND (un-commented)

Commits verified:
- 33a8759: test(gpu): activate Android KgslAccessibilityProviderTest
- 3d59200: feat(gpu): implement Android actual with kgsl + devfreq probe-before-read
- fa4065f: test(gpu): activate JVM OshiGpuInfoSourceTest
- 3446425: feat(gpu): implement JVM actual with OSHI partial data (no utilization)
- fe4223f: fix(gpu): extract magic number constants to satisfy detekt MagicNumber rule
