---
phase: 23
plan: "01"
subsystem: gpu
tags: [kmp, gradle, cinterop, oshi, scaffold]
dependency_graph:
  requires: []
  provides: [":libs:modules:gpu Gradle project", "oshi-core 7.0.0 catalog entry", "gpuInfo.def IOKit binding"]
  affects: [settings.gradle.kts, gradle/libs.versions.toml]
tech_stack:
  added: ["com.github.oshi:oshi-core:7.0.0 (jvmMain only)"]
  patterns: ["expect/actual module structure", "IOKit cinterop (macOS arm64+x64 only)", "block-commented test scaffold"]
key_files:
  created:
    - libs/modules/gpu/build.gradle.kts
    - libs/modules/gpu/src/nativeInterop/cinterop/gpuInfo.def
    - libs/modules/gpu/src/commonTest/kotlin/com/smellouk/kamper/gpu/GpuInfoSentinelTest.kt
    - libs/modules/gpu/src/commonTest/kotlin/com/smellouk/kamper/gpu/GpuInfoUnsupportedTest.kt
    - libs/modules/gpu/src/commonTest/kotlin/com/smellouk/kamper/gpu/GpuConfigBuilderTest.kt
    - libs/modules/gpu/src/jvmTest/kotlin/com/smellouk/kamper/gpu/OshiGpuInfoSourceTest.kt
    - libs/modules/gpu/src/androidUnitTest/kotlin/com/smellouk/kamper/gpu/KgslAccessibilityProviderTest.kt
  modified:
    - settings.gradle.kts
    - gradle/libs.versions.toml
decisions:
  - "OSHI 7.0.0 pinned to exact version (no range) per T-23-01 supply-chain threat mitigation"
  - "IOKit cinterop restricted to macosArm64/macosX64 only — no iOS/tvOS (App Store rejection risk per D-07 / Pitfall 3)"
  - "Test files block-commented (not @Ignore) to achieve zero compile cost while maintaining git-tracked presence"
  - "100.0 upper-bound clamp in C body to mitigate T-23-04 Tampering threat (IOAccelerator reporting > 100%)"
metrics:
  duration: "~15 minutes"
  completed: "2026-05-01T22:49:31Z"
  tasks_completed: 3
  files_created: 7
  files_modified: 2
---

# Phase 23 Plan 01: GPU Module Bootstrap Summary

Bootstrap the `:libs:modules:gpu` Gradle project with OSHI 7.0.0 (JVM), IOKit cinterop (macOS only), and five block-commented test scaffold files — zero compile cost, ready for Waves 1–2 to unwrap.

## Tasks Completed

| Task | Name | Commit |
|------|------|--------|
| 1 | Register module + declare OSHI in version catalog | 10d913c |
| 2 | Write libs/modules/gpu/build.gradle.kts and gpuInfo.def | e1f520c |
| 3 | Scaffold five @Ignore-marked test files | eb1a12c |

## What Was Built

### Task 1 — settings.gradle.kts + gradle/libs.versions.toml

- Added `include(":libs:modules:gpu")` after `include(":libs:modules:thermal")` in `settings.gradle.kts`
- Added `oshi = "7.0.0"` to `[versions]` block in `gradle/libs.versions.toml`
- Added `oshi-core = { module = "com.github.oshi:oshi-core", version.ref = "oshi" }` to `[libraries]` block

### Task 2 — build.gradle.kts and gpuInfo.def

The `build.gradle.kts` mirrors `libs/modules/thermal/build.gradle.kts` with one addition: a `val jvmMain by getting` block that wires `libs.oshi.core` exclusively to JVM. iOS/tvOS targets are absent from the cinterop block — only `macosArm64()` and `macosX64()` register the `gpuInfo` cinterop.

The `gpuInfo.def` provides:
- Language: Objective-C
- Headers: Foundation/Foundation.h, IOKit/IOKitLib.h
- LinkerOpts: -framework IOKit
- C body: `kamper_gpu_utilization()` iterates IOAccelerator services, reads `"Device Utilization %"` (AGX/Apple Silicon) or `"GPU Activity(%)"` (Intel/AMD), clamps result to `[0.0, 100.0]`, returns `-2.0` if no IOAccelerator service found (UNSUPPORTED), returns `-1.0` on read failure (INVALID)

### Task 3 — Five test scaffold files

All five files contain only a `package` declaration and a block-commented test body (`/* ... */`). The comment contains the full Wave 1/2 test code for future implementers to un-comment. Zero compilation units are emitted, so `./gradlew :libs:modules:gpu:jvmTest -q` exits 0.

| File | Wave | Tests inside comment |
|------|------|---------------------|
| GpuInfoSentinelTest.kt | Wave 1 | INVALID sentinel field validation |
| GpuInfoUnsupportedTest.kt | Wave 1 | UNSUPPORTED sentinel + inequality check |
| GpuConfigBuilderTest.kt | Wave 1 | GpuConfig.Builder DSL validation |
| OshiGpuInfoSourceTest.kt | Wave 2 | OSHI source never-throws/null guarantee |
| KgslAccessibilityProviderTest.kt | Wave 2 | kgsl sysfs probe returns false on non-Android host |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] React Native node_modules missing in worktree**
- **Found during:** Task 1 (running `./gradlew help -q`)
- **Issue:** The worktree did not carry the `demos/react-native/node_modules` symlink that the main repo has. The composite `includeBuild("demos/react-native/android")` in `settings.gradle.kts` requires `@react-native/gradle-plugin` which lives in `node_modules`. This caused ALL Gradle operations to fail.
- **Fix:** Created a symlink `demos/react-native/node_modules -> /Users/smellouk/Developer/git/kamper/demos/react-native/node_modules` in the worktree. This is a worktree-local runtime fix, not committed.
- **Files modified:** None (symlink only, covered by `demos/react-native/.gitignore`)

**2. [Plan Refinement] Test bodies block-commented instead of @Ignore annotated**
- **Reason:** As documented in the plan's `<refinement_note>`, test files reference types (`GpuInfo`, `GpuConfig`, `OshiGpuInfoSource`, `KgslAccessibilityProvider`) that don't exist yet. `@Ignore` skips execution but NOT compilation — the files would fail to compile. Per the refinement note, bodies were wrapped in `/* ... */` block comments instead.
- **Impact:** None. Wave 1 and Wave 2 plans are documented to unwrap these comments as their RED step.

## Known Stubs

None. This plan creates scaffolding/configuration only — no production code with stub data sources.

## Threat Flags

No new network endpoints, auth paths, or schema changes at trust boundaries introduced. The `gpuInfo.def` C body calls IOKit (macOS-only, local kernel interface) — covered by the plan's threat model (T-23-02, T-23-03, T-23-04 all mitigated in the C body).

## Verification Results

```
./gradlew :libs:modules:gpu:tasks -q      # PASS
./gradlew :libs:modules:gpu:jvmTest -q    # PASS (zero tests compiled — all bodies block-commented)
grep -c '^include(":libs:modules:gpu")$' settings.gradle.kts          # 1
grep -c '^oshi-core' gradle/libs.versions.toml                         # 1
grep -c 'cinterops.create("gpuInfo")' libs/modules/gpu/build.gradle.kts  # 1
grep -c 'kamper_gpu_utilization' libs/modules/gpu/src/nativeInterop/cinterop/gpuInfo.def  # 1
ls libs/modules/gpu/src/commonTest/kotlin/com/smellouk/kamper/gpu/ | wc -l  # 3
```

## Self-Check: PASSED

Files created:
- libs/modules/gpu/build.gradle.kts: FOUND
- libs/modules/gpu/src/nativeInterop/cinterop/gpuInfo.def: FOUND
- libs/modules/gpu/src/commonTest/kotlin/com/smellouk/kamper/gpu/GpuInfoSentinelTest.kt: FOUND
- libs/modules/gpu/src/commonTest/kotlin/com/smellouk/kamper/gpu/GpuInfoUnsupportedTest.kt: FOUND
- libs/modules/gpu/src/commonTest/kotlin/com/smellouk/kamper/gpu/GpuConfigBuilderTest.kt: FOUND
- libs/modules/gpu/src/jvmTest/kotlin/com/smellouk/kamper/gpu/OshiGpuInfoSourceTest.kt: FOUND
- libs/modules/gpu/src/androidUnitTest/kotlin/com/smellouk/kamper/gpu/KgslAccessibilityProviderTest.kt: FOUND

Commits verified:
- 10d913c: chore(phase23): register :libs:modules:gpu and declare oshi 7.0.0 in version catalog
- e1f520c: chore(phase23): add gpu module build.gradle.kts and gpuInfo.def IOKit cinterop
- eb1a12c: test(phase23): scaffold five block-commented gpu test placeholders for Waves 1 and 2
