---
phase: 25-rename-the-library-from-kamper-to-konitor
plan: "05"
subsystem: demos, rn-ui, ci
tags: [rename, konitor, demos, react-native, podspec, ci, release-please]
dependency_graph:
  requires: [25-01, 25-02, 25-03, 25-04]
  provides: [demos-konitor-packages, rn-konitor-exports, konitor-podspec, ci-konitor-secrets]
  affects: [demos, libs/ui/rn, .github/workflows, release-please-config.json]
tech_stack:
  added: []
  patterns: [git-mv for tracked file renames, BSD sed with word boundaries]
key_files:
  created:
    - Konitor.podspec
    - libs/ui/rn/react-native-konitor.podspec
    - libs/ui/rn/src/Konitor.ts
    - libs/ui/rn/src/NativeKonitorModule.ts
    - libs/ui/rn/src/hooks/useKonitor.ts
    - libs/ui/rn/src/__tests__/NativeKonitorModule.mock.ts
  modified:
    - demos/android/build.gradle.kts
    - demos/compose/build.gradle.kts
    - demos/compose/iosApp/project.yml
    - demos/ios/build.gradle.kts
    - demos/jvm/build.gradle.kts
    - demos/macos/build.gradle.kts
    - demos/tvos/build.gradle.kts
    - demos/react-native/android/app/build.gradle
    - demos/react-native/android/settings.gradle
    - demos/react-native/package.json
    - demos/react-native/app.json
    - libs/ui/rn/package.json
    - libs/ui/rn/src/index.ts
    - libs/ui/rn/src/types.ts
    - .github/workflows/publish-kotlin.yml
    - .github/workflows/publish-cocoapods.yml
    - .github/workflows/publish-npm.yml
    - release-please-config.json
decisions:
  - "Preserved github.com/smellouk/kamper repository URLs in podspecs and workflows per D-15 (repository URL is not renamed)"
  - "Fixed assembleKamperReleaseXCFramework → assembleKonitorReleaseXCFramework in publish-cocoapods.yml (BSD sed word-boundary sed missed it in camelCase; fixed as Rule 1)"
  - "Renamed KamperState → KonitorState in useKonitor.ts and exported KonitorState from index.ts"
metrics:
  duration: "~12 minutes"
  completed_date: "2026-05-03"
  tasks_completed: 2
  tasks_total: 2
  files_changed: 148 + 23
---

# Phase 25 Plan 05: Rename Demos, RN TypeScript, Podspecs, CI, and Release Config to Konitor

Renamed all demo source directories from `com/smellouk/kamper` to `com/smellouk/konitor`, updated all demo build configs, renamed React Native TypeScript files (Kamper.ts → Konitor.ts, NativeKamperModule.ts → NativeKonitorModule.ts, useKamper.ts → useKonitor.ts), renamed both podspecs, updated CI workflows to use KONITOR_GH_* secrets, and updated release-please-config.json with konitor component names.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Rename demo source directories and update build configs | 31ee835 | 148 files (12 demo source trees + 11 build configs) |
| 2 | Rename RN TypeScript files, update podspecs, CI workflows, release-please | 0236889 | 23 files (TS renames, podspecs, 3 CI YAMLs, release config) |

## What Was Done

### Task 1: Demo Source Directories and Build Configs

All 12 demo source trees renamed using `git mv`:
- `demos/android/src/main/java/com/smellouk/kamper/` → `konitor/`
- `demos/compose/src/{androidMain,commonMain,desktopMain,iosMain,wasmJsMain}/kotlin/com/smellouk/kamper/` → `konitor/`
- `demos/ios/src/iosMain/kotlin/com/smellouk/kamper/` → `konitor/`
- `demos/jvm/src/main/kotlin/com/smellouk/kamper/` → `konitor/`
- `demos/macos/src/macosMain/kotlin/com/smellouk/kamper/` → `konitor/`
- `demos/tvos/src/tvosMain/kotlin/com/smellouk/kamper/` → `konitor/`
- `demos/web/src/jsMain/kotlin/com/smellouk/kamper/` → `konitor/`
- `demos/react-native/android/app/src/main/java/com/smellouk/kamper/` → `konitor/`

All demo .kt files updated: `com.smellouk.kamper` → `com.smellouk.konitor` in package/import declarations, and class symbols (KamperUi → KonitorUi, KamperConfig → KonitorConfig, etc.).

Build configs updated:
- `demos/android/build.gradle.kts`: namespace + applicationId → `com.smellouk.konitor.android`
- `demos/compose/build.gradle.kts`: namespace + applicationId → `com.smellouk.konitor.compose`
- `demos/ios/build.gradle.kts`: KamperIOS → KonitorIOS, entryPoint → `com.smellouk.konitor.ios`
- `demos/jvm/build.gradle.kts`: mainClass → `com.smellouk.konitor.jvm.MainKt`
- `demos/macos/build.gradle.kts`: entryPoint → `com.smellouk.konitor.macos.main`
- `demos/tvos/build.gradle.kts`: KamperTVOS → KonitorTVOS, entryPoint → `com.smellouk.konitor.tvos`
- `demos/react-native/android/app/build.gradle`: namespace + applicationId → `com.smellouk.konitor.rn`
- `demos/react-native/android/settings.gradle`: all `com.smellouk.kamper:*` → `com.smellouk.konitor:*` in substitute module map
- `demos/react-native/package.json`: `react-native-kamper` → `react-native-konitor`
- `demos/react-native/app.json`: `KamperRn` → `KonitorRn`
- `demos/compose/iosApp/project.yml`: bundleIdPrefix + PRODUCT_BUNDLE_IDENTIFIER → `com.smellouk.konitor`

### Task 2: RN TypeScript, Podspecs, CI, Release Config

TypeScript renames:
- `Kamper.ts` → `Konitor.ts` (export `const Konitor`, types `KonitorApi`, `KonitorEventMap`, `KonitorSubscription`)
- `NativeKamperModule.ts` → `NativeKonitorModule.ts` (TurboModuleRegistry name `'KonitorModule'`)
- `hooks/useKamper.ts` → `hooks/useKonitor.ts` (`KamperState` → `KonitorState`)
- `__tests__/NativeKamperModule.mock.ts` → `__tests__/NativeKonitorModule.mock.ts`

`libs/ui/rn/package.json`:
- `"name": "react-native-konitor"`
- `codegenConfig.name`: `NativeKonitorModuleSpec`
- `codegenConfig.android.javaPackageName`: `com.smellouk.konitor.rn`
- `codegenConfig.ios.modulesProvider`: `KonitorModule: KonitorTurboModule`
- `"ios"`: `ios/KonitorTurboModule.h`

Podspecs:
- `libs/ui/rn/react-native-kamper.podspec` → `react-native-konitor.podspec` (content fully updated)
- `Kamper.podspec` → `Konitor.podspec` (s.name = 'Konitor', Konitor.xcframework URL)

CI Workflows:
- `publish-kotlin.yml`: `KAMPER_GH_USER/PAT` → `KONITOR_GH_USER/PAT`
- `publish-cocoapods.yml`: `assembleKonitorReleaseXCFramework`, `Konitor.xcframework`, `pod trunk push Konitor.podspec`
- `publish-npm.yml`: `react-native-konitor` references

`release-please-config.json`:
- Root component: `"package-name": "konitor"`, `"component": "konitor"`, extra-file `Konitor.podspec`
- Release notes body: `com.smellouk.konitor` coordinates, `pod 'Konitor'`, `Konitor.xcframework.zip`
- RN component: `"package-name": "react-native-konitor"`, npm install examples updated

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] BSD sed word-boundary missed assembleKamperReleaseXCFramework in publish-cocoapods.yml**
- **Found during:** Task 2 verification
- **Issue:** The `[[:<:]]Kamper[[:>:]]` BSD word-boundary pattern doesn't match `Kamper` embedded in camelCase identifiers (`assembleKamperReleaseXCFramework`). The generic sed substitution missed this.
- **Fix:** Applied direct Edit to replace `assembleKamperReleaseXCFramework` → `assembleKonitorReleaseXCFramework` in `publish-cocoapods.yml`
- **Files modified:** `.github/workflows/publish-cocoapods.yml`
- **Commit:** 0236889

**2. [Rule 2 - Missing Critical Functionality] KamperState type not renamed in useKonitor.ts**
- **Found during:** Task 2 execution
- **Issue:** The `KamperState` interface in `useKonitor.ts` was exported and used as the hook return type but was missed by the sed substitutions.
- **Fix:** Renamed `KamperState` → `KonitorState` throughout `useKonitor.ts` and exported `KonitorState` from `index.ts`
- **Files modified:** `libs/ui/rn/src/hooks/useKonitor.ts`, `libs/ui/rn/src/index.ts`
- **Commit:** 0236889

**3. [Rule 1 - Bug] Podspec comment stale references**
- **Found during:** Task 2
- **Issue:** `react-native-konitor.podspec` header still said `# kamper-react-native.podspec` and referenced `NativeKamperModuleSpec.h` and `KamperNative.podspec` in comments
- **Fix:** Updated all comment text to use konitor naming
- **Files modified:** `libs/ui/rn/react-native-konitor.podspec`
- **Commit:** 0236889

## Threat Surface Scan

No new network endpoints, auth paths, file access patterns, or schema changes introduced. All changes are renaming of existing identifiers. The TurboModuleRegistry module name string `'KonitorModule'` in `NativeKonitorModule.ts` correctly matches the renamed Android `KonitorTurboModule.NAME` (renamed in plan 03) per T-25-05-01.

## Known Stubs

None — this plan contains no data stubs or placeholder values.

## Self-Check: PASSED

- `find demos -type d -name "kamper" | grep -v "/build/" ...`: 0 (FOUND)
- `test -f Konitor.podspec`: PASS
- `test ! -f Kamper.podspec`: PASS
- `grep "KONITOR_GH_USER" .github/workflows/publish-kotlin.yml`: PASS
- `test -f libs/ui/rn/src/Konitor.ts`: PASS
- `grep '"react-native-konitor"' libs/ui/rn/package.json`: PASS
- Commit 31ee835 exists: VERIFIED
- Commit 0236889 exists: VERIFIED
