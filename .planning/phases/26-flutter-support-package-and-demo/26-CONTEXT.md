# Phase 26: flutter support package and demo - Context

**Gathered:** 2026-05-03
**Status:** Ready for planning

<domain>
## Phase Boundary

Deliver a `konitor_flutter` Flutter plugin package at `libs/ui/flutter/` and a Flutter demo app at `demos/flutter/` — mirroring the React Native package (`libs/ui/rn/`) and demo (`demos/react-native/`) in structure and feature scope. The plugin bridges Konitor's existing Kotlin and Swift/XCFramework native modules to Dart via platform channels (EventChannel per metric). Android and iOS are both in scope.

This phase does NOT publish the plugin to pub.dev — that is a release automation concern.

</domain>

<decisions>
## Implementation Decisions

### Event delivery to Dart
- **D-01:** Use `EventChannel` per metric — one named channel per module (cpu, fps, memory, network, issues, jank, gc, thermal, gpu). Dart API exposes typed streams: `Konitor.cpuStream`, `Konitor.fpsStream`, etc. Native side calls `eventSink.success(payload)` from inside `Konitor.addInfoListener<XxxInfo>`.
- **D-02:** Channel names follow pattern `com.smellouk.konitor/xxx` (e.g. `com.smellouk.konitor/cpu`).
- **D-03:** A separate `MethodChannel` (`com.smellouk.konitor/control`) handles imperative calls: `start(config)`, `stop()`, `showOverlay()`, `hideOverlay()`, `logEvent()`, `startEvent()`, `endEvent()`.

### Dart API shape
- **D-04:** Typed immutable Dart model classes for every metric: `CpuInfo`, `FpsInfo`, `MemoryInfo`, `NetworkInfo`, `IssueInfo`, `JankInfo`, `GcInfo`, `ThermalInfo`, `GpuInfo`, `UserEventInfo`. Each has a `factory XxxInfo.fromMap(Map<dynamic, dynamic> m)` constructor that parses the EventChannel payload.
- **D-05:** `KonitorConfig` Dart class with nullable `bool` fields (all default true when null), mirroring `KonitorConfig` TypeScript interface in `libs/ui/rn/src/types.ts`.
- **D-06:** Public barrel file `lib/konitor_flutter.dart` exports all model classes, the `Konitor` class, and stream accessors — same export surface as `libs/ui/rn/src/index.ts`.

### Platform scope
- **D-07:** Android + iOS, both in scope. Android: Kotlin `KonitorFlutterPlugin` implementing `FlutterPlugin` + `MethodCallHandler` + per-metric `EventChannel.StreamHandler`. iOS: Swift `KonitorPlugin` using the XCFramework (cpu/fps/memory/network available; jank/gc/thermal are Android-only no-ops on iOS).
- **D-08:** Platform-unavailable modules return `null` events (or emit nothing) — never crash. iOS parity with RN: cpu, fps, memory, network functional; jank/gc/thermal silently inactive.

### Overlay inclusion
- **D-09:** Include `showOverlay()` and `hideOverlay()` on both platforms for API parity with RN. Android implementation calls `KonitorUi.show(context)` / `KonitorUi.hide()` on the UI thread (same pattern as `KonitorTurboModule`). iOS implementation is a no-op stub (no native overlay exists). The Compose overlay runs in a separate `WindowManager` layer and does not conflict with Flutter's renderer.

### Monorepo dependency wiring
- **D-10:** Flutter plugin's `android/` side uses **local path dependencies** via Gradle composite build — `implementation project(':libs:engine')`, `implementation project(':libs:modules:cpu')`, etc. Mirrors `libs/ui/rn/android/build.gradle`. Root `settings.gradle.kts` adds `includeBuild("libs/ui/flutter/android")` (and `includeBuild("demos/flutter/android")` for the demo).
- **D-11:** **Root Gradle version** is authoritative — the Flutter plugin's `android/` does not declare its own Gradle wrapper. Apply `konitor.android.config` convention plugin (wrapped in try/catch as RN does) so compileSdk/minSdk/Java version stay in sync with the rest of the monorepo.
- **D-12:** iOS dependency: the Flutter plugin's `ios/` references Konitor via the CocoaPods podspec (`Konitor.podspec` / `KonitorFlutter.podspec`), pointing to the XCFramework — same strategy as `react-native-konitor.podspec`.

### Demo app
- **D-13:** Demo at `demos/flutter/` — a standard Flutter app (`flutter create`) showing all metrics. One screen with sections for CPU, FPS, Memory, Network, Issues, Jank, GC, Thermal, GPU (Android) and CPU, FPS, Memory, Network (iOS). Mirrors the metric tabs in the Android and RN demos.
- **D-14:** Demo uses the local path to `libs/ui/flutter/` via `pubspec.yaml` path dependency: `konitor_flutter: path: ../../libs/ui/flutter`.
- **D-15:** Demo `android/` is wired as a composite build in root `settings.gradle.kts` so it can resolve `:libs:*` projects directly.

### File layout
- **D-16:** Library at `libs/ui/flutter/` — `lib/` (Dart), `android/` (Kotlin plugin), `ios/` (Swift plugin), `pubspec.yaml`, `CHANGELOG.md`.
- **D-17:** Demo at `demos/flutter/` — standard Flutter project with `lib/main.dart` as entry point.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### React Native reference (structural mirror)
- `libs/ui/rn/src/types.ts` — TypeScript type definitions; Dart model classes MUST have the same fields
- `libs/ui/rn/src/NativeKonitorModule.ts` — full method surface: start/stop/showOverlay/hideOverlay/reportJsMemory/logEvent/startEvent/endEvent; Flutter control channel must expose the same imperative methods (except JS-specific: reportJsMemory/reportJsGc/reportCrash/beginSpan/endSpan)
- `libs/ui/rn/android/src/main/java/com/smellouk/konitor/rn/KonitorTurboModule.kt` — Android native bridge reference; Kotlin plugin implementation mirrors this
- `libs/ui/rn/android/build.gradle` — reference build.gradle for Flutter plugin's android/ (composite build, try/catch plugin application, minSdk, Java version)
- `libs/ui/rn/react-native-konitor.podspec` — reference podspec for Flutter plugin's ios/ CocoaPods integration
- `libs/ui/rn/ios/KonitorTurboModule.mm` — iOS ObjC++ bridge reference; Swift FlutterPlugin mirrors the same module installation logic

### Demo reference
- `demos/react-native/App.tsx` — RN demo showing all metric subscriptions; Flutter demo UI mirrors this metric coverage

### Monorepo build
- `settings.gradle.kts` — root; Flutter composite includes must be added here
- `build-logic/src/main/kotlin/AndroidConfigPlugin.kt` — convention plugin applied by `konitor.android.config`; governs compileSdk/minSdk/Java

### Existing native modules
- `libs/modules/cpu/`, `libs/modules/fps/`, `libs/modules/memory/`, `libs/modules/network/` — core modules (Android + iOS)
- `libs/modules/issues/`, `libs/modules/jank/`, `libs/modules/gc/`, `libs/modules/thermal/`, `libs/modules/gpu/` — Android-only modules
- `libs/engine/src/commonMain/kotlin/com.smellouk.konitor/Engine.kt` — install/start/stop/addInfoListener API
- `libs/engine/src/androidMain/kotlin/com/smellouk/konitor/Konitor.kt` — Android LifecycleObserver singleton

### UI overlay
- `libs/ui/android/src/androidMain/kotlin/com/smellouk/konitor/ui/KonitorUi.kt` — `show(context)` / `hide()` facade used by Flutter plugin overlay

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `libs/ui/rn/android/src/main/java/com/smellouk/konitor/rn/KonitorTurboModule.kt` — complete Android bridge with all 10 modules wired; Flutter `KonitorFlutterPlugin` copies the same `addInfoListener` + payload mapping logic, replacing `emitOnXxx()` calls with `EventChannel.EventSink.success(map)`
- `libs/ui/rn/ios/KonitorTurboModule.mm` — iOS bridge with 4 modules (cpu/fps/memory/network); Swift plugin mirrors the same install/listener pattern
- `libs/ui/rn/src/types.ts` — field-for-field reference for Dart model class definitions

### Established Patterns
- **INVALID sentinel guard**: every `addInfoListener` checks `if (info == XxxInfo.INVALID) return` before emitting — this pattern MUST be preserved in the Flutter plugin
- **try/catch plugin application**: `android/build.gradle` applies `konitor.android.config` inside `try { } catch (UnknownPluginException)` — required for the plugin to build both standalone and in the monorepo composite
- **UI-thread overlay dispatch**: `KonitorUi.show()` must be called on the main thread; use Flutter's `activity.runOnUiThread { }` equivalent
- **Stop-then-clear on restart**: `Konitor.stop(); Konitor.clear()` before re-installing — prevents duplicate listeners on repeated start() calls (same as RN TurboModule)

### Integration Points
- `settings.gradle.kts` — add `includeBuild("libs/ui/flutter/android")` and `includeBuild("demos/flutter/android")`
- Flutter plugin `android/build.gradle` → `implementation project(':libs:engine')` + per-module dependencies
- Flutter plugin `ios/KonitorFlutter.podspec` → `s.dependency 'Konitor'` pointing to the XCFramework

</code_context>

<specifics>
## Specific Ideas

- User confirmed: "what RN offers (lib + demo) flutter should also offer it" — full feature parity with the RN package is the acceptance bar
- User confirmed: "one gradle version (root used for flutter)" — no separate Gradle wrapper in the Flutter plugin; root version governs
- Overlay: user explicitly chose Android + iOS (no-op on iOS), not Android only — expose the API on both platforms

</specifics>

<deferred>
## Deferred Ideas

- pub.dev publishing / version tagging — release automation concern, separate phase
- Flutter-side JS memory / GC / crash reporting analogs (Dart VM stats) — no RN equivalent concept; out of scope

</deferred>

---

*Phase: 26-flutter-support-package-and-demo*
*Context gathered: 2026-05-03*
