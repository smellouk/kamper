# Codebase Structure

**Analysis Date:** 2026-04-26

## Directory Layout

```
kamper/                                 # Repository root
├── libs/                               # Library source (Gradle project)
│   ├── api/                            # Layer 1: shared contracts (KMP)
│   ├── engine/                         # Layer 2: orchestration (KMP)
│   ├── modules/                        # Layer 3: metric implementations
│   │   ├── cpu/                        # CPU usage (expect/actual, 8 platforms)
│   │   ├── fps/                        # Frame rate (expect/actual, Android primary)
│   │   ├── memory/                     # Heap memory (per-platform, Context required on Android)
│   │   ├── network/                    # Network I/O (expect/actual)
│   │   ├── issues/                     # ANR, crash, slow start, dropped frames, mem pressure
│   │   ├── jank/                       # JankStats-based jank detection (Android)
│   │   ├── gc/                         # GC run counter (JVM)
│   │   └── thermal/                    # Thermal status (Android API 29+)
│   ├── ui/
│   │   └── android/                    # Layer 4: Compose overlay + chip (KMP source sets)
│   └── xcframework/                    # iOS/macOS XCFramework packaging
├── build-logic/                        # Gradle convention plugins (composite build)
├── demos/                              # Runnable sample apps
│   ├── android/                        # Android demo app
│   ├── compose/                        # Compose multiplatform demo
│   ├── ios/                            # iOS demo app
│   ├── jvm/                            # JVM CLI demo
│   ├── macos/                          # macOS demo
│   ├── web/                            # JS/WASM web demo
│   └── react-native/                   # React Native demo (Gradle includeBuild composite)
│       └── android/                    # Android Gradle root for the RN composite
├── quality/                            # Detekt / lint configuration
├── screenshots/                        # Documentation screenshots
├── .planning/                          # GSD planning artefacts
│   ├── codebase/                       # Codebase analysis documents (this directory)
│   │   └── adr/                        # Architecture Decision Records
│   └── phases/                         # Phase plans
├── .github/workflows/                  # CI configuration
├── settings.gradle.kts                 # Module registration
├── build.gradle.kts                    # Root build config
├── gradle.properties                   # Kotlin/AGP versions, signing flags
└── CHANGELOG.md / README.md / CONTRIBUTING.md
```

---

## Source Set Layout (KMP)

Every KMP Gradle module follows this source set convention. The exact set of platforms varies per module.

```
libs/modules/<name>/src/
├── commonMain/kotlin/com/smellouk/kamper/<name>/
│   ├── <Name>Info.kt               # data class : Info
│   ├── <Name>Config.kt             # data class : Config  (+ Builder object)
│   ├── <Name>Performance.kt        # : Performance<Config, IWatcher<Info>, Info>
│   ├── <Name>Watcher.kt            # : Watcher<Info>
│   ├── Module.kt                   # expect val <Name>Module: PerformanceModule<...>
│   └── repository/
│       ├── <Name>InfoDto.kt        # raw platform data transfer object
│       ├── <Name>InfoMapper.kt     # DTO → Info conversion
│       └── <Name>InfoRepository.kt # internal interface : InfoRepository<Info>
├── commonTest/kotlin/...           # unit tests (mappers, config builders)
├── androidMain/kotlin/...
│   ├── Module.kt                   # actual val <Name>Module
│   └── repository/
│       ├── <Name>InfoRepositoryImpl.kt
│       └── source/
│           └── <Android>InfoSource.kt
├── androidTest/kotlin/...          # instrumented tests (Robolectric or device)
├── jvmMain/kotlin/...
│   └── (same sub-structure as androidMain)
├── iosMain/kotlin/...
│   └── (same sub-structure)
├── tvosMain/kotlin/...
├── macosMain/kotlin/...
├── jsMain/kotlin/...
└── wasmJsMain/kotlin/...
```

**Engine source sets:**
```
libs/engine/src/
├── commonMain/kotlin/com.smellouk.kamper/
│   ├── Engine.kt
│   └── KamperConfig.kt
├── commonTest/kotlin/com/smellouk/kamper/
│   ├── EngineTest.kt
│   ├── EngineValidateTest.kt
│   └── KamperConfigBuilderTest.kt
├── androidMain/kotlin/com/smellouk/kamper/Kamper.kt   # LifecycleObserver
├── androidTest/kotlin/com/smellouk/kamper/KamperTest.kt
├── jvmMain/kotlin/com/smellouk/kamper/Kamper.kt       # setup() + plain Engine
├── iosMain/kotlin/com/smellouk/kamper/Kamper.kt       # bare object
├── jsMain/kotlin/com/smellouk/kamper/Kamper.kt
├── macosMain/kotlin/com/smellouk/kamper/Kamper.kt
├── tvosMain/kotlin/com/smellouk/kamper/Kamper.kt
└── wasmJsMain/kotlin/com/smellouk/kamper/Kamper.kt
```

**API source sets:**
```
libs/api/src/
├── commonMain/kotlin/com/smellouk/kamper/api/
│   ├── Cleanable.kt
│   ├── Config.kt
│   ├── Extensions.kt           # kBytesToMb, bytesToMb, nanosToSeconds
│   ├── Info.kt
│   ├── InfoRepository.kt
│   ├── IWatcher.kt
│   ├── KamperDslMarker.kt
│   ├── Logger.kt               # interface + SIMPLE/EMPTY; expect DEFAULT
│   ├── Performance.kt          # open class Performance + PerformanceModule
│   └── Watcher.kt              # coroutine polling implementation
├── commonTest/kotlin/.../
│   ├── ExtensionsTest.kt
│   ├── PerformanceTest.kt
│   ├── TestListeners.kt
│   └── WatcherTest.kt
└── {platform}Main/.../DEFAULT.kt   # actual Logger.DEFAULT per platform
```

**UI source sets:**
```
libs/ui/android/src/
├── commonMain/kotlin/com/smellouk/kamper/ui/
│   ├── KamperUi.kt             # expect object KamperUi
│   ├── KamperUiConfig.kt
│   ├── KamperUiRepository.kt   # internal expect class
│   ├── KamperUiSettings.kt
│   ├── KamperUiState.kt
│   ├── IssueSerializer.kt
│   ├── PerfettoExporter.kt
│   ├── RecordedSample.kt
│   └── compose/
│       ├── KamperPanel.kt      # full bottom-sheet panel (4 tabs)
│       ├── KamperChip.kt       # compact HUD overlay
│       ├── IssuesTab.kt
│       ├── PerfettoTab.kt
│       ├── Sparkline.kt
│       ├── KamperTheme.kt
│       ├── FormatUtils.kt
│       └── PlatformPerfetto.kt # expect
├── androidMain/kotlin/com/smellouk/kamper/ui/
│   ├── KamperUi.kt             # actual object KamperUi (ContentProvider auto-attach)
│   ├── KamperUiRepository.kt   # actual class — owns Engine, modules, StateFlows
│   ├── KamperUiInitProvider.kt # ContentProvider for zero-code init
│   ├── KamperPanelActivity.kt
│   ├── AndroidOverlayManager.kt
│   └── compose/PlatformPerfetto.kt  # actual
├── appleMain/kotlin/.../       # iOS/tvOS actual stubs
└── iosMain/kotlin/.../
```

---

## Directory Purposes

**`libs/api/`:**
- Purpose: Shared type contracts used by every other module. Zero platform dependencies.
- Key files: `Performance.kt`, `Watcher.kt`, `InfoRepository.kt`, `Config.kt`, `Info.kt`

**`libs/engine/`:**
- Purpose: `Engine` class (module registry) and `Kamper` singleton per platform. Consumers import this to call `install`, `start`, `stop`, `clear`, `addInfoListener`.
- Key files: `Engine.kt`, `KamperConfig.kt`, `{platform}Main/Kamper.kt`

**`libs/modules/`:**
- Purpose: One Gradle subproject per metric domain. Each is independently publishable and independently installable.
- Key pattern: `commonMain/Module.kt` declares `expect val XxxModule`; each `{platform}Main/Module.kt` provides `actual val XxxModule`.

**`libs/ui/android/`:**
- Purpose: Opt-in visual overlay. Has its own private `Engine` instance. Consumers only call `KamperUi.configure { }` — no manual module installation needed.
- Key files: `KamperUiRepository.kt` (state management), `KamperPanel.kt` (Compose UI), `AndroidOverlayManager.kt` (window management)

**`demos/`:**
- Purpose: Runnable reference apps. Not published. Show integration patterns per platform.
- Key files: `demos/android/`, `demos/jvm/`, `demos/compose/`

**`build-logic/`:**
- Purpose: Shared Gradle convention plugins as a standalone composite build. Contains `KmpLibraryPlugin`, `AndroidConfigPlugin`, and `KamperPublishPlugin`. Has its own `settings.gradle.kts` with independent `repositories{}` using `FAIL_ON_PROJECT_REPOS`.

**`quality/`:**
- Purpose: Detekt configuration files for static analysis.

**`.planning/`:**
- Purpose: GSD planning artefacts. Not shipped in the library. Contains phase plans, codebase analysis docs, and ADRs.

---

## Key File Locations

**Entry Points:**
- `libs/engine/src/androidMain/kotlin/com/smellouk/kamper/Kamper.kt` — Android `Kamper` singleton with lifecycle hooks
- `libs/engine/src/jvmMain/kotlin/com/smellouk/kamper/Kamper.kt` — JVM `Kamper` singleton
- `libs/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/KamperUiInitProvider.kt` — ContentProvider that auto-initialises UI overlay

**Configuration:**
- `settings.gradle.kts` — module include list
- `gradle.properties` — Kotlin, AGP, Compose versions
- `build-logic/src/main/kotlin/KamperPublishPlugin.kt` — Maven coordinate and publication settings (Kotlin convention plugin, applied via `id("kamper.publish")`)

**Core Logic:**
- `libs/api/src/commonMain/kotlin/com/smellouk/kamper/api/Watcher.kt` — the polling engine shared by all modules
- `libs/api/src/commonMain/kotlin/com/smellouk/kamper/api/Performance.kt` — lifecycle manager for a single module
- `libs/engine/src/commonMain/kotlin/com.smellouk.kamper/Engine.kt` — module registry

**Testing:**
- `libs/api/src/commonTest/` — pure KMP unit tests
- `libs/modules/{name}/src/commonTest/` — mapper + config builder tests (no platform code)
- `libs/modules/{name}/src/androidTest/` — instrumented tests for platform sources

---

## Naming Conventions

**Files:**
- One top-level declaration per file; file name matches the class/object/interface name.
- Module factory files always named `Module.kt` in both `commonMain` and `{platform}Main`.
- Platform source files prefixed with platform name: `ProcCpuInfoSource.kt`, `JvmCpuInfoSource.kt`, `IosCpuInfoSource.kt`.

**Packages:**
- `com.smellouk.kamper.api` — contracts
- `com.smellouk.kamper` — engine and `Kamper` singleton
- `com.smellouk.kamper.{module}` — module public API (`XxxInfo`, `XxxConfig`, `XxxModule`)
- `com.smellouk.kamper.{module}.repository` — internal data access
- `com.smellouk.kamper.{module}.repository.source` — raw platform source
- `com.smellouk.kamper.ui` — UI layer

**Classes:**
- Info types: `XxxInfo` (data class implementing `Info`)
- Config types: `XxxConfig` (data class implementing `Config`, with `Builder` inner object)
- Repository port: `XxxInfoRepository : InfoRepository<XxxInfo>` (internal interface)
- Repository adapter: `XxxInfoRepositoryImpl` (internal class)
- Raw source: `XxxInfoSource` or `{Platform}XxxInfoSource` (internal class)
- Watcher: `XxxWatcher : Watcher<XxxInfo>` (internal class)
- Performance: `XxxPerformance : Performance<XxxConfig, IWatcher<XxxInfo>, XxxInfo>` (internal class)

---

## Where to Add New Code

### Adding a new performance module

1. Create `libs/modules/xxx/` as a new Gradle subproject with a `build.gradle.kts` targeting the same KMP platform set as existing modules.
2. Add `include(":libs:modules:xxx")` to `settings.gradle.kts`.
3. Add `commonMain` source set under `libs/modules/xxx/src/commonMain/kotlin/com/smellouk/kamper/xxx/` with: `XxxInfo.kt`, `XxxConfig.kt`, `XxxWatcher.kt`, `XxxPerformance.kt`, `Module.kt` (expect), `repository/XxxInfoRepository.kt`, `repository/XxxInfoDto.kt`, `repository/XxxInfoMapper.kt`.
4. Add `{platform}Main` source sets for each target platform with: `Module.kt` (actual), `repository/XxxInfoRepositoryImpl.kt`, `repository/source/XxxInfoSource.kt`.
5. Add `commonTest` with mapper and config builder tests.
6. To expose in the UI overlay, add the module to `libs/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/KamperUiRepository.kt` following the existing install/uninstall helper pattern.

### Adding a new platform to an existing module

1. Add a new source set directory: `libs/modules/{name}/src/{newPlatform}Main/kotlin/com/smellouk/kamper/{name}/`.
2. Create `Module.kt` with `actual val XxxModule`.
3. Create `repository/XxxInfoRepositoryImpl.kt` and `repository/source/{Platform}XxxInfoSource.kt`.
4. Register the new target in the module's `build.gradle.kts` KMP target block.

### Adding a new UI composable

- Place in `libs/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/`.
- If it requires platform-specific implementation, create an `expect fun` in `commonMain` and `actual fun` in each platform source set (following `PlatformPerfetto.kt`).

### Adding a utility to the API layer

- Pure computation utilities go in `libs/api/src/commonMain/kotlin/com/smellouk/kamper/api/Extensions.kt`.
- New interfaces/types go in their own file in the same package.
- Never introduce Android/JVM imports in `commonMain`.

### Adding tests

- Pure logic (mapper, config, Extensions): `libs/modules/{name}/src/commonTest/` or `libs/api/src/commonTest/`
- Platform-specific behaviour: `libs/modules/{name}/src/androidTest/` (instrumented) or `libs/modules/{name}/src/jvmTest/`

---

## Special Directories

**`libs/xcframework/`:**
- Purpose: Packages the compiled iOS/macOS targets into an XCFramework for Swift/Objective-C consumption.
- Generated: Yes (output of Gradle `assembleXCFramework` task)
- Committed: No

**`build-logic/`:**
- Purpose: Gradle convention plugin composite build. Introduced in Phase 11 as the replacement for the legacy Gradle plugin directory. Contains hand-authored convention plugins applied by all Kamper library modules.
- Generated: No (hand-authored)
- Committed: Yes

**`.planning/`:**
- Purpose: GSD planning system artefacts. Phase documents, codebase analysis, ADRs.
- Generated: Partially (by GSD commands)
- Committed: Yes

**`kotlin-js-store/`:**
- Purpose: Yarn lockfile for the JS/WASM targets.
- Generated: Yes (by Gradle Kotlin/JS plugin)
- Committed: Yes (for reproducible JS builds)

**`demos/`:**
- Purpose: Reference integration apps. Not part of the published library.
- Generated: No
- Committed: Yes

---

---

## Gradle Build Infrastructure

Established in Phases 11–12. Applies to the entire monorepo.

### Composite Builds

| Composite Root | Purpose | wired via |
|----------------|---------|-----------|
| `build-logic/` | Convention plugins (KmpLibraryPlugin, AndroidConfigPlugin, KamperPublishPlugin) | `includeBuild("build-logic")` in root `settings.gradle.kts` |
| `demos/react-native/android/` | React Native Android demo (framework-owned Groovy DSL preserved) | `includeBuild("demos/react-native/android")` in root `settings.gradle.kts` |

### Gradle Performance Flags (`gradle.properties`)

| Flag | Value | Effect |
|------|-------|--------|
| `org.gradle.configuration-cache` | `true` | Task graph serialized; reruns skip configuration phase |
| `org.gradle.parallel` | `true` | Modules compiled in parallel across daemon threads |
| `org.gradle.jvmargs` | `-Xmx4096m` | Daemon heap sized for 18 parallel KMP module compilations |

### Repository Management (`settings.gradle.kts`)

- `dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS) }` — root settings declares `google()` + `mavenCentral()`; module-level `repositories {}` blocks are discouraged but not hard-blocked (PREFER_SETTINGS chosen over FAIL_ON_PROJECT_REPOS because KotlinJS toolchain requires adding ivy repositories at configuration time).
- `build-logic/settings.gradle.kts` uses `FAIL_ON_PROJECT_REPOS` independently — it is a separate Gradle build and does not inherit from the root.

*Structure analysis: 2026-04-26*
