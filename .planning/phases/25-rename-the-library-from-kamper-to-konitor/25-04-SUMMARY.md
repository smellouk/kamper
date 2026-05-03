---
phase: 25-rename-the-library-from-kamper-to-konitor
plan: 04
subsystem: build
tags: [gradle, android, cmake, jni, cinterop, xcframework, konitor, rename]

# Dependency graph
requires:
  - phase: 25-03
    provides: Kotlin source files and class names already renamed to Konitor* in libs/
provides:
  - All libs/ build.gradle.kts and build.gradle files using com.smellouk.konitor namespaces
  - BOM artifact coordinates updated from kamper to konitor groupId
  - GPU native library consistently named konitor_gpu_fdinfo (CMakeLists.txt + JNI load)
  - JNI function prefix updated to Java_com_smellouk_konitor in konitor_fdinfo.c
  - gpuInfo.def struct/function renamed to KonitorGpuStats/konitor_gpu_stats
  - MacosGpuInfoSource.kt cinterop import and call updated to konitor_gpu_stats
  - konitor_file_paths.xml replaces kamper_file_paths.xml
  - Theme.Konitor.Transparent defined in libs/ui/kmm themes.xml
  - apple-sdk XCFramework baseName = "Konitor"
affects: [25-05, 25-06, 25-07, build-config, android-demo, gpu-module]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Consistent JNI library name across CMakeLists.txt, System.loadLibrary, and C function prefix"
    - "cinterop packageName matches Kotlin package of generated bindings"

key-files:
  created:
    - libs/modules/gpu/src/androidMain/cpp/konitor_fdinfo.c
    - libs/ui/kmm/src/androidMain/res/xml/konitor_file_paths.xml
  modified:
    - libs/api/build.gradle.kts
    - libs/engine/build.gradle.kts
    - libs/bom/build.gradle.kts
    - libs/modules/cpu/build.gradle.kts
    - libs/modules/fps/build.gradle.kts
    - libs/modules/gc/build.gradle.kts
    - libs/modules/gpu/build.gradle.kts
    - libs/modules/issues/build.gradle.kts
    - libs/modules/jank/build.gradle.kts
    - libs/modules/memory/build.gradle.kts
    - libs/modules/network/build.gradle.kts
    - libs/modules/thermal/build.gradle.kts
    - libs/ui/kmm/build.gradle.kts
    - libs/ui/rn/android/build.gradle
    - libs/integrations/sentry/build.gradle.kts
    - libs/integrations/firebase/build.gradle.kts
    - libs/integrations/opentelemetry/build.gradle.kts
    - libs/rn/build.gradle.kts
    - libs/apple-sdk/build.gradle.kts
    - libs/modules/gpu/src/androidMain/cpp/CMakeLists.txt
    - libs/modules/gpu/src/androidMain/kotlin/com/smellouk/konitor/gpu/repository/source/FdinfoJni.kt
    - libs/modules/gpu/src/nativeInterop/cinterop/gpuInfo.def
    - libs/modules/gpu/src/macosMain/kotlin/com/smellouk/konitor/gpu/repository/source/MacosGpuInfoSource.kt
    - libs/ui/kmm/src/androidMain/res/values/themes.xml
    - demos/android/src/main/res/values/themes.xml
    - demos/android/src/main/res/layout/fragment_cpu.xml
    - demos/android/src/main/res/layout/fragment_fps.xml
    - demos/android/src/main/res/layout/fragment_gpu.xml
    - demos/android/src/main/res/layout/fragment_memory.xml
    - demos/android/src/main/res/layout/fragment_network.xml
    - demos/android/src/main/AndroidManifest.xml

key-decisions:
  - "GitHub repository URL (https://github.com/smellouk/kamper) left unchanged — it is the actual repo URL, not a namespace"
  - "Convention plugin IDs (kamper.kmp.library, kamper.publish) not renamed here — that is scope for a separate plan targeting build-logic/"
  - "kamper_fdinfo.c renamed via git mv to preserve history; JNI function prefix updated atomically"

patterns-established:
  - "JNI consistency: CMakeLists.txt library name, System.loadLibrary() call, and C function JNI prefix must all agree"

requirements-completed: []

# Metrics
duration: 12min
completed: 2026-05-03
---

# Phase 25 Plan 04: Build Config and Native Rename Summary

**All libs/ Gradle build files updated to com.smellouk.konitor namespaces; GPU native lib renamed from kamper_gpu_fdinfo to konitor_gpu_fdinfo with consistent JNI bridging; Android XML resources and themes updated**

## Performance

- **Duration:** ~12 min
- **Started:** 2026-05-03T00:00:00Z
- **Completed:** 2026-05-03T00:12:00Z
- **Tasks:** 2
- **Files modified:** 33 (19 build files + 14 source/resource files)

## Accomplishments

- Replaced all `com.smellouk.kamper` namespace/coordinate occurrences in 19 libs/ build.gradle.kts and build.gradle files (0 remaining)
- Updated BOM artifact coordinates from `com.smellouk.kamper:*` to `com.smellouk.konitor:*`
- Renamed GPU native C library from `kamper_gpu_fdinfo` to `konitor_gpu_fdinfo` across CMakeLists.txt, JNI load call, and C function prefix
- Renamed `kamper_fdinfo.c` to `konitor_fdinfo.c`; updated `gpuInfo.def` struct/function to KonitorGpuStats/konitor_gpu_stats
- Renamed `kamper_file_paths.xml` to `konitor_file_paths.xml`; updated Theme.Kamper.Transparent to Theme.Konitor.Transparent
- apple-sdk XCFramework baseName set to "Konitor"

## Task Commits

Each task was committed atomically:

1. **Task 1: Update namespace/applicationId in all libs/ build.gradle.kts files and BOM artifact coordinates** - `40cd81e` (refactor)
2. **Task 2: Rename GPU native lib, update cinterop def, update JNI C function prefix, rename Android XML resources, update themes** - `54ce1f1` (refactor)

**Plan metadata:** committed in SUMMARY commit

## Files Created/Modified

- `libs/modules/gpu/src/androidMain/cpp/konitor_fdinfo.c` - Renamed from kamper_fdinfo.c; JNI function prefix updated to Java_com_smellouk_konitor
- `libs/modules/gpu/src/androidMain/cpp/CMakeLists.txt` - library name updated to konitor_gpu_fdinfo; source file reference updated
- `libs/modules/gpu/src/androidMain/kotlin/.../FdinfoJni.kt` - System.loadLibrary("konitor_gpu_fdinfo")
- `libs/modules/gpu/src/nativeInterop/cinterop/gpuInfo.def` - KonitorGpuStats struct and konitor_gpu_stats function
- `libs/modules/gpu/src/macosMain/kotlin/.../MacosGpuInfoSource.kt` - import and call updated to konitor_gpu_stats
- `libs/ui/kmm/src/androidMain/res/xml/konitor_file_paths.xml` - Renamed from kamper_file_paths.xml; cache-path name updated
- `libs/ui/kmm/src/androidMain/res/values/themes.xml` - Theme.Konitor.Transparent
- `libs/bom/build.gradle.kts` - com.smellouk.konitor:* artifact coordinates
- `libs/apple-sdk/build.gradle.kts` - XCFramework("Konitor"), baseName = "Konitor"
- All 19 libs/ build.gradle.kts and build.gradle files - com.smellouk.konitor namespaces

## Decisions Made

- GitHub repository URL (`https://github.com/smellouk/kamper`) was left unchanged — this is the actual repository URL on GitHub, not a package namespace. The sed pattern targeted `com.smellouk.kamper` (dot notation) so the URL was never matched.
- Convention plugin IDs (`kamper.kmp.library`, `kamper.publish`, `kamper.android.config`) were not renamed — these are defined in `build-logic/` which is out of scope for this plan; the acceptance criteria in the plan does not cover them.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- All libs/ build config surfaces now consistently use `com.smellouk.konitor`
- GPU JNI bridge is internally consistent: CMakeLists.txt, System.loadLibrary, and C function prefix all reference `konitor_gpu_fdinfo` / `Java_com_smellouk_konitor`
- Ready for Plan 05 which handles demos/ and root-level build config rename

## Self-Check

- `libs/modules/gpu/src/androidMain/cpp/konitor_fdinfo.c` — FOUND
- `libs/ui/kmm/src/androidMain/res/xml/konitor_file_paths.xml` — FOUND
- `40cd81e` — FOUND (git log)
- `54ce1f1` — FOUND (git log)

## Self-Check: PASSED

---
*Phase: 25-rename-the-library-from-kamper-to-konitor*
*Completed: 2026-05-03*
