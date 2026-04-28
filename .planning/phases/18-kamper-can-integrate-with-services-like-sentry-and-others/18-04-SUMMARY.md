---
phase: 18-kamper-can-integrate-with-services-like-sentry-and-others
plan: "04"
subsystem: kamper-firebase
tags: [integration, firebase, crashlytics, kmp, expect-actual, no-op, dsl, ios-nserror]

dependency_graph:
  requires:
    - phase: 18-01
      provides: KamperEvent data class + IntegrationModule interface + currentPlatform expect/actual
    - phase: 18-02
      provides: Engine.addIntegration() + dispatchToIntegrations() fan-out
    - phase: 18-03
      provides: settings.gradle.kts sentry include + KamperPublishPlugin integrations artifact naming
  provides:
    - FirebaseIntegrationModule implementing IntegrationModule — routes IssueInfo -> Firebase Crashlytics
    - FirebaseConfig DSL builder (forwardIssues: Boolean, default false per D-10)
    - FirebaseModule() public DSL factory
    - :kamper:integrations:firebase Gradle subproject registered in settings.gradle.kts
    - 6 platform actuals: Android (real Firebase), iOS (NSError wrap), JVM/macOS/JS/WasmJS (no-op)
  affects:
    - Plan 18-05 (OtelModule) follows same structural pattern

tech_stack:
  added:
    - com.google.firebase:firebase-crashlytics:20.0.5 (androidMain)
    - cocoapods pod("FirebaseCrashlytics") ~> 11.0 (iosMain via Kotlin/Native CocoaPods plugin)
    - org.jetbrains.kotlin.native.cocoapods plugin for iOS Kotlin/Native interop
  patterns:
    - expect/actual for platform-specific SDK calls — 6 actuals, 1 commonMain expect
    - Info.INVALID sentinel guard as first line of onEvent (T-16-04)
    - Double try/catch: outer in onEvent, inner in each actual (T-16-02 defense-in-depth)
    - object Builder with reset() for re-entrant DSL factory calls (FirebaseConfig pattern)
    - NSError wrapping for iOS Throwable->Crashlytics bridge (RESEARCH Pitfall 5)
    - JS/WasmJS included via no-op actuals — no SDK dependency blocks these targets

key-files:
  created:
    - kamper/integrations/firebase/build.gradle.kts
    - kamper/integrations/firebase/src/commonMain/kotlin/com/smellouk/kamper/firebase/FirebaseConfig.kt
    - kamper/integrations/firebase/src/commonMain/kotlin/com/smellouk/kamper/firebase/FirebaseIntegrationModule.kt
    - kamper/integrations/firebase/src/commonMain/kotlin/com/smellouk/kamper/firebase/Module.kt
    - kamper/integrations/firebase/src/commonMain/kotlin/com/smellouk/kamper/firebase/RecordNonFatal.kt
    - kamper/integrations/firebase/src/androidMain/kotlin/com/smellouk/kamper/firebase/RecordNonFatal.kt
    - kamper/integrations/firebase/src/iosMain/kotlin/com/smellouk/kamper/firebase/RecordNonFatal.kt
    - kamper/integrations/firebase/src/jvmMain/kotlin/com/smellouk/kamper/firebase/RecordNonFatal.kt
    - kamper/integrations/firebase/src/macosMain/kotlin/com/smellouk/kamper/firebase/RecordNonFatal.kt
    - kamper/integrations/firebase/src/jsMain/kotlin/com/smellouk/kamper/firebase/RecordNonFatal.kt
    - kamper/integrations/firebase/src/wasmJsMain/kotlin/com/smellouk/kamper/firebase/RecordNonFatal.kt
    - kamper/integrations/firebase/src/commonTest/kotlin/com/smellouk/kamper/firebase/FirebaseConfigBuilderTest.kt
    - kamper/integrations/firebase/src/commonTest/kotlin/com/smellouk/kamper/firebase/FirebaseIntegrationModuleTest.kt
  modified:
    - settings.gradle.kts — added include(":kamper:integrations:firebase")

key-decisions:
  - "buildSrc/Modules.kt does not exist (Phase 11 migrated to build-logic); used string literals directly per sentry pattern"
  - "kamper/publish.gradle.kts does not exist; used id(\"kamper.publish\") convention plugin and id(\"kamper.android.config\") instead of apply(from=...)"
  - "JS/WasmJS targets INCLUDED: unlike sentry-kotlin-multiplatform:0.13.0, firebase has no KMP transitive dep that blocks these targets — no-op actuals compile fine"
  - "org.jetbrains.kotlin.native.cocoapods plugin applied for iOS CocoaPods interop — bundled with kotlin-gradle-plugin 2.3.21, no separate declaration needed"
  - "Full assemble fails on iOS due to Xcode iOS 26.2 SDK not installed — expected per plan note; JVM/JS/WasmJS assemble succeeds"
  - "moduleName route handles both 'issue' and 'issues' for forward-compat, though Engine produces 'issue' per 18-02 summary"

requirements-completed:
  - D-04
  - D-05
  - D-07
  - D-10

duration: ~8min
completed: "2026-04-28"
---

# Phase 18 Plan 04: kamper-firebase Integration Module Summary

**New `:kamper:integrations:firebase` KMP subproject implementing IntegrationModule to forward Kamper IssueInfo events to Firebase Crashlytics: Android uses `FirebaseCrashlytics.getInstance().recordException`, iOS wraps Throwable in NSError per RESEARCH Pitfall 5, all other targets are no-ops. 10 tests pass.**

## Performance

- **Duration:** ~8 min
- **Started:** 2026-04-28
- **Completed:** 2026-04-28
- **Tasks:** 5 (all executed)
- **Files modified/created:** 14

## Accomplishments

- New Gradle subproject `:kamper:integrations:firebase` registered in `settings.gradle.kts` and building
- `build.gradle.kts` with KMP targets: Android, JVM, macosX64/Arm64, iosArm64/SimulatorArm64, JS(IR), WasmJs
- `org.jetbrains.kotlin.native.cocoapods` plugin + `pod("FirebaseCrashlytics") { version = "~> 11.0" }` for iOS interop
- `android.firebase-crashlytics:20.0.5` for Android (consolidated artifact, no -ktx suffix needed)
- `FirebaseConfig` data class with `forwardIssues: Boolean` (default: `false` per D-10), `object Builder` with `reset()`
- `FirebaseIntegrationModule` routing table: `"issue"/"issues"` -> `recordNonFatal` (when forwardIssues=true), all other moduleNames dropped (per D-07, CPU/memory/FPS are not Crashlytics use cases)
- Info.INVALID guard as first line of `onEvent` (T-16-04), outer try/catch in `onEvent` (T-16-02)
- `FirebaseModule() { ... }` public DSL factory with `@KamperDslMarker` + `@Suppress("FunctionNaming")`
- `internal expect fun recordNonFatal` in commonMain + 6 platform actuals
- 10 tests (4 Builder + 6 Module) all passing on `:kamper:integrations:firebase:jvmTest`
- T-16-04b verified: no Firebase imports in jvmMain/macosMain/jsMain/wasmJsMain

## Subproject Target Set

| Target | Included | Reason |
|--------|----------|--------|
| androidTarget | YES | Real Firebase Crashlytics SDK |
| jvm | YES | no-op actual |
| macosX64 | YES | no-op actual |
| macosArm64 | YES | no-op actual |
| iosArm64 | YES | NSError wrapping via CocoaPods interop |
| iosSimulatorArm64 | YES | NSError wrapping via CocoaPods interop |
| js(IR) | YES | no-op actual (no blocking transitive dep) |
| wasmJs | YES | no-op actual (no blocking transitive dep) |

## DSL Usage Example

```kotlin
Kamper
    .install(IssuesModule)
    .addIntegration(
        FirebaseModule {
            forwardIssues = true  // IssueInfo -> Firebase Crashlytics non-fatal
        }
    )
```

NOTE: Firebase MUST already be initialized by the host app (google-services.json on Android,
GoogleService-Info.plist on iOS). On JVM, macOS, JS, and WasmJS, the `recordNonFatal` actual is
a no-op — consumers do NOT need platform guards.

## Why CPU/Memory/FPS Are NOT Forwarded

Per D-07 (Firebase Crashlytics is for non-fatal exceptions, NOT for performance metrics):
- Crashlytics is designed for crash reporting and error tracking — sending every CPU/FPS/memory
  poll would produce thousands of events per session, overwhelming dashboards.
- The `when (event.moduleName)` routing only handles `"issue"/"issues"` → `recordNonFatal`.
- All other moduleNames (cpu, memory, fps, network, jank, gc, thermal) fall to `else -> Unit`.
- If users need performance metrics in Firebase, they should use Firebase Performance Monitoring
  (a separate SDK that the host app configures directly — not a Kamper concern).

## iOS NSError Wrapping Shape

```kotlin
val nsError = NSError.errorWithDomain(
    domain = throwable::class.simpleName ?: "KamperFirebase",
    code = 0L,
    userInfo = mapOf(
        NSLocalizedDescriptionKey to (throwable.message ?: "Kamper issue"),
        "kamper.module" to event.moduleName,
        "kamper.platform" to event.platform,
        "kamper.timestampMs" to event.timestampMs.toString()
    )
)
Crashlytics.crashlytics().recordError(nsError)
```

Per RESEARCH Pitfall 5: `code = 0L` matches the `NSInteger` typealias on Kotlin/Native.
`domain` uses the Kotlin throwable class name as the Crashlytics error type identifier.

## Task Commits

| Task | Hash | Description |
|------|------|-------------|
| Task 1 | `5670abf` | feat(18-04): register :kamper:integrations:firebase in settings.gradle.kts |
| Task 2 | `097e323` | feat(18-04): create kamper/integrations/firebase/build.gradle.kts |
| Task 3 | `0e71f82` | feat(18-04): add commonMain sources (FirebaseConfig, FirebaseIntegrationModule, Module, RecordNonFatal expect) |
| Task 4 | `ca5240d` | feat(18-04): add per-platform recordNonFatal actuals |
| Task 5 | `5174754` | test(18-04): add FirebaseConfigBuilderTest and FirebaseIntegrationModuleTest (10 tests) |

## Files Created/Modified

- `settings.gradle.kts` — Added `include(":kamper:integrations:firebase")`
- `kamper/integrations/firebase/build.gradle.kts` — KMP targets + Firebase Crashlytics dep + CocoaPods pod
- `kamper/integrations/firebase/src/commonMain/.../FirebaseConfig.kt` — DSL config builder
- `kamper/integrations/firebase/src/commonMain/.../FirebaseIntegrationModule.kt` — IntegrationModule implementation
- `kamper/integrations/firebase/src/commonMain/.../Module.kt` — Public DSL factory
- `kamper/integrations/firebase/src/commonMain/.../RecordNonFatal.kt` — expect fun declaration
- `kamper/integrations/firebase/src/androidMain/.../RecordNonFatal.kt` — FirebaseCrashlytics.getInstance()
- `kamper/integrations/firebase/src/iosMain/.../RecordNonFatal.kt` — NSError wrapping + recordError
- `kamper/integrations/firebase/src/jvmMain/.../RecordNonFatal.kt` — no-op
- `kamper/integrations/firebase/src/macosMain/.../RecordNonFatal.kt` — no-op
- `kamper/integrations/firebase/src/jsMain/.../RecordNonFatal.kt` — no-op
- `kamper/integrations/firebase/src/wasmJsMain/.../RecordNonFatal.kt` — no-op
- `kamper/integrations/firebase/src/commonTest/.../FirebaseConfigBuilderTest.kt` — 4 Builder tests
- `kamper/integrations/firebase/src/commonTest/.../FirebaseIntegrationModuleTest.kt` — 6 module routing tests

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] buildSrc/Modules.kt does not exist**
- **Found during:** Task 1
- **Issue:** Plan referenced `buildSrc/src/main/kotlin/Modules.kt` for adding `object Integrations { const val FIREBASE = "..." }` — this file does not exist. The project migrated from buildSrc to build-logic convention plugins in Phase 11.
- **Fix:** Skipped Modules.kt step. Used string literal `":kamper:api"` directly in build.gradle.kts, same pattern as all other modules post-Phase-11.
- **Files modified:** None (deviation is a skip, not a code change)

**2. [Rule 3 - Blocking] kamper/publish.gradle.kts does not exist**
- **Found during:** Task 2
- **Issue:** Plan specified `apply(from = projectDir.resolve("../../../publish.gradle.kts"))` — this file was replaced by `KamperPublishPlugin.kt` in Phase 11 composite build migration.
- **Fix:** Applied `id("kamper.publish")` and `id("kamper.android.config")` convention plugins per sentry pattern. The `KamperPublishPlugin.kt` integrations artifact naming fix is already in place from Plan 18-03.
- **Files modified:** None beyond task scope (KamperPublishPlugin already handles integrations branch)

**3. [Rule 3 - Infrastructure] React Native node_modules symlink missing in worktree**
- **Found during:** Task 1 Gradle configuration
- **Issue:** Gradle configuration failed because `demos/react-native/android/settings.gradle` references `@react-native/gradle-plugin` from node_modules, which doesn't exist in the worktree.
- **Fix:** Created symlink `demos/react-native/node_modules -> /Users/smellouk/.../demos/react-native/node_modules`
- **Files modified:** None (symlink only, not tracked by git)

---

**Total deviations:** 3 auto-fixed (all Rule 3 blocking fixes)
**Impact on plan:** Build artifacts are equivalent. The convention plugin approach is the correct post-Phase-11 pattern. JS/WasmJS targets are included (expanded from sentry) because firebase has no blocking transitive dep.

## Note for Plan 18-05

- `settings.gradle.kts` pattern: add `include(":kamper:integrations:opentelemetry")` after the firebase include
- `KamperPublishPlugin.kt` artifact-name fix already in place — just use `id("kamper.publish")`
- No `buildSrc/Modules.kt` to modify
- `moduleName` for IssueInfo is `"issue"` (singular) per Plan 18-02 summary

## Known Stubs

None — FirebaseIntegrationModule fully wired. Android calls real Firebase Crashlytics SDK. iOS uses NSError wrapping via CocoaPods interop. JVM/macOS/JS/WasmJS are intentional no-ops per D-07.

## Threat Surface Scan

No new network endpoints or auth paths beyond existing Firebase SDK calls.

| Flag | File | Description |
|------|------|-------------|
| trust_boundary: firebase-android | androidMain/RecordNonFatal.kt | FirebaseCrashlytics.getInstance() called inside try/catch; host app must initialize Firebase |
| trust_boundary: firebase-ios | iosMain/RecordNonFatal.kt | Crashlytics.crashlytics().recordError(NSError) called inside try/catch |

Mitigations:
- T-16-02: double try/catch (onEvent outer + actual inner)
- T-16-03: forwardIssues defaults to false; test locks it
- T-16-04: Info.INVALID guard first line of onEvent; test locks it
- T-16-04b: no Firebase imports in jvmMain/macosMain/jsMain/wasmJsMain (verified by grep)
- T-16-11: custom keys bounded to kamper.module, kamper.platform, kamper.timestampMs (non-PII)

## Self-Check: PASSED

Files verified:
- `kamper/integrations/firebase/build.gradle.kts` — FOUND
- `kamper/integrations/firebase/src/commonMain/.../FirebaseConfig.kt` — FOUND
- `kamper/integrations/firebase/src/commonMain/.../FirebaseIntegrationModule.kt` — FOUND
- `kamper/integrations/firebase/src/commonMain/.../Module.kt` — FOUND
- `kamper/integrations/firebase/src/commonMain/.../RecordNonFatal.kt` — FOUND
- `kamper/integrations/firebase/src/androidMain/.../RecordNonFatal.kt` — FOUND
- `kamper/integrations/firebase/src/iosMain/.../RecordNonFatal.kt` — FOUND
- `kamper/integrations/firebase/src/jvmMain/.../RecordNonFatal.kt` — FOUND
- `kamper/integrations/firebase/src/macosMain/.../RecordNonFatal.kt` — FOUND
- `kamper/integrations/firebase/src/jsMain/.../RecordNonFatal.kt` — FOUND
- `kamper/integrations/firebase/src/wasmJsMain/.../RecordNonFatal.kt` — FOUND
- `kamper/integrations/firebase/src/commonTest/.../FirebaseConfigBuilderTest.kt` — FOUND
- `kamper/integrations/firebase/src/commonTest/.../FirebaseIntegrationModuleTest.kt` — FOUND
- `settings.gradle.kts` — include(":kamper:integrations:firebase") verified

Commits verified:
- `5670abf` — Task 1 feat commit
- `097e323` — Task 2 feat commit
- `0e71f82` — Task 3 feat commit
- `ca5240d` — Task 4 feat commit
- `5174754` — Task 5 test commit

Tests: 10/10 passing on `:kamper:integrations:firebase:jvmTest`
Compilation: compileKotlinMetadata, compileKotlinJvm, compileKotlinJs, compileKotlinWasmJs all BUILD SUCCESSFUL
T-16-04b: No Firebase imports in no-op target actuals (grep returns 0 lines)

---
*Phase: 18-kamper-can-integrate-with-services-like-sentry-and-others*
*Completed: 2026-04-28*
