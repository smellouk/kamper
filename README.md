# Konitor

[![License](https://img.shields.io/github/license/smellouk/konitor)](LICENSE)
[![Release](https://img.shields.io/github/v/tag/smellouk/konitor?label=release&color=blue)](https://github.com/smellouk/konitor/releases)
[![Issues](https://img.shields.io/github/issues/smellouk/konitor)](https://github.com/smellouk/konitor/issues)
[![Stars](https://img.shields.io/github/stars/smellouk/konitor?style=social)](https://github.com/smellouk/konitor/stargazers)
[![CI](https://img.shields.io/github/actions/workflow/status/smellouk/konitor/pull-request.yml?branch=main&label=CI)](https://github.com/smellouk/konitor/actions/workflows/pull-request.yml)

**Kotlin Multiplatform performance monitoring.** A plugin-based library that gives you live CPU, FPS, memory, network, jank, GC, thermal, and issue detection across Android, iOS, JVM, macOS, and Web — through a single unified API.

<img src="screenshots/1.gif" width="320" align="right"/>

Konitor uses a zero-boilerplate `install()` model — each of the eight modules is independently installable, so you only pay for what you use. The flagship feature is the Konitor UI debug overlay: a floating chip that appears automatically in Android debug builds with zero app code, and attaches with one line on iOS. Konitor runs across Android, iOS, JVM, macOS, JS, and Wasm, delivering consistent metric callbacks through a single listener API on every platform.

<br clear="right"/>

---

## Modules

Konitor ships eight independently-installable performance modules. Install only what you need.

| Name | Description | Platforms |
|------|-------------|-----------|
| CPU | Total, user, system, and per-app CPU usage ratios | Android · iOS · JVM · macOS · Web |
| FPS | Frames per second via platform frame-timing APIs | Android · iOS · JVM · macOS · Web |
| Memory | Heap usage, PSS (Android), and available RAM | Android · iOS · JVM · macOS · Web¹ |
| Network | Bytes received / transmitted per interval | Android² · iOS · JVM · macOS · Web³ |
| Jank | Dropped frames and slow renders | Android · JVM |
| GC | Garbage collection runs and pause time | Android · JVM |
| Thermal | Device thermal state and throttling | Android |
| Issues | ANR, crash, dropped frames, memory pressure, slow start | Android · JVM |

> ¹ Heap metrics via `performance.memory` (Chromium-based browsers only).
> ² Full support requires API 23+. API 16–22 reports system-level traffic only.
> ³ Bandwidth estimate via the [Network Information API](https://developer.mozilla.org/en-US/docs/Web/API/NetworkInformation) (Chrome / Edge).

---

## Quick start

Three steps to live performance metrics.

**Step 1 — Add Maven Central dependencies**

```kotlin
// build.gradle.kts (Module: app)
dependencies {
    implementation("com.smellouk.konitor:engine:$konitorVersion")
    implementation("com.smellouk.konitor:cpu-module:$konitorVersion")
}
```

**Step 2 — Set up Konitor and install a module**

```kotlin
Konitor.setup {
    logger = Logger.DEFAULT
}.apply {
    install(CpuModule)
    start()
}
```

**Step 3 — Register a listener**

```kotlin
Konitor.addInfoListener<CpuInfo> { cpu ->
    if (cpu == CpuInfo.INVALID) return@addInfoListener
    println("CPU: ${cpu.totalUseRatio}")
}
```

### Lifecycle

```kotlin
Konitor.start()   // begin polling
Konitor.stop()    // pause polling
Konitor.clear()   // uninstall + remove listeners
```

On Android, `lifecycle.addObserver(Konitor)` wires `start`/`stop`/`clear` to the Activity lifecycle automatically.

---

## Konitor UI

A floating chip overlay that sits over any screen and shows live performance metrics. Tap to expand, tap again to open the full panel. Drag to either edge — it snaps flush with no extra rounded corner on the anchored side. Shake the device to restore a collapsed chip.

**Android: zero app code required.** A `ContentProvider` auto-initialises the overlay in debug builds and disables it automatically in release. Add the dependency and you're done:

```kotlin
debugImplementation("com.smellouk.konitor:ui-android:$konitorVersion")
```

**iOS: one line in `AppDelegate`.**

```swift
// AppDelegate.swift
import KonitorUi

func application(_ application: UIApplication,
                 didFinishLaunchingWithOptions ...) -> Bool {
    KonitorUi.shared.attach()
    return true
}
```

<p align="center">
  <img src="screenshots/2.gif" width="300"/>
</p>

### Panel tabs

<table>
  <tr>
    <td align="center"><b>Activity</b></td>
    <td align="center"><b>Perfetto</b></td>
    <td align="center"><b>Issues</b></td>
    <td align="center"><b>Settings</b></td>
  </tr>
  <tr>
    <td><img src="screenshots/7.png" width="160"/></td>
    <td><img src="screenshots/8.png" width="160"/></td>
    <td><img src="screenshots/9.png" width="160"/></td>
    <td><img src="screenshots/10.png" width="160"/></td>
  </tr>
</table>

### Chip states

<table>
  <tr>
    <td align="center">Peek (default)</td>
    <td align="center">Expanded — core metrics</td>
    <td align="center">Expanded — all metrics</td>
  </tr>
  <tr>
    <td><img src="screenshots/3.png" width="160"/></td>
    <td><img src="screenshots/5.png" width="160"/></td>
    <td><img src="screenshots/4.png" width="160"/></td>
  </tr>
</table>

### Optional configuration

```kotlin
// In Application.onCreate() or anywhere before first activity launch
KonitorUi.configure {
    isEnabled  = true
    position   = ChipPosition.TOP_END  // TOP_START | TOP_END | CENTER_START | CENTER_END | BOTTOM_START | BOTTOM_END
}
```

### Perfetto tracing

The **Perfetto** tab lets you record a session in-app and export a `.perfetto-trace` file directly from the share sheet — no ADB or Android Studio required. Open the file at [ui.perfetto.dev](https://ui.perfetto.dev) to analyse counter tracks for CPU, FPS, Memory, Network, Jank, GC, and Thermal.

---

## Service Integrations

Konitor can forward metrics and crash events to third-party observability services. Each
integration is a separate artifact — add only the ones you need. Nothing is forwarded unless
you explicitly enable it in the DSL config (all forwarding flags default to `false`).

Use `addIntegration()` on the `Konitor` engine instance to attach an integration module:

```kotlin
Konitor
    .install(CpuModule)
    .install(MemoryModule)
    .addIntegration(SentryModule(dsn = "https://abc123@sentry.io/123456") {
        forwardIssues      = true   // IssueInfo -> Sentry.captureException
        forwardCpuAbove    = 80f    // CPU > 80 % -> Sentry breadcrumb
        forwardMemoryAbove = 85f    // Memory > 85 % -> Sentry breadcrumb
        forwardFps         = false
    })
```

### Sentry

Routes `IssueInfo` events as `Sentry.captureException` and CPU / Memory / FPS metrics as Sentry
breadcrumbs (only when the configured threshold is exceeded).

**Dependency:**

```kotlin
dependencies {
    implementation("com.smellouk.konitor:sentry-integration:$konitorVersion")
}
```

**Supported platforms:** Android, iOS, JVM, macOS (JS and WasmJS excluded — `sentry-kotlin-multiplatform:0.13.0` does not publish JS/WasmJS artifacts).

**DSL options:**

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `dsn` | `String` | required | Your Sentry project DSN |
| `forwardIssues` | `Boolean` | `false` | Send `IssueInfo` as `captureException` |
| `forwardCpuAbove` | `Float?` | `null` | Send CPU breadcrumb when ratio exceeds this value (0–100) |
| `forwardMemoryAbove` | `Float?` | `null` | Send memory breadcrumb when ratio exceeds this value (0–100) |
| `forwardFps` | `Boolean` | `false` | Send FPS breadcrumb on every poll |

---

### Firebase Crashlytics

Routes `IssueInfo` events as Firebase Crashlytics non-fatal exceptions. CPU, memory, and FPS
are not forwarded — Crashlytics is for error tracking, not performance metrics.

**Dependency:**

```kotlin
dependencies {
    implementation("com.smellouk.konitor:firebase-integration:$konitorVersion")
}
```

**Supported platforms:** Android (real Crashlytics SDK), iOS (NSError wrapping via CocoaPods).
On JVM, macOS, JS, and WasmJS the integration is a no-op — no platform guard is needed in
your code.

> **Note:** Firebase must already be initialised by the host app before Konitor starts.
> On Android this means a valid `google-services.json` and the `com.google.gms.google-services`
> plugin. On iOS this means a valid `GoogleService-Info.plist` loaded at app launch.

**DSL usage:**

```kotlin
Konitor
    .install(IssuesModule())
    .addIntegration(
        FirebaseModule {
            forwardIssues = true  // IssueInfo -> Crashlytics.recordException / recordError
        }
    )
```

---

### OpenTelemetry (Grafana, Datadog, New Relic, Honeycomb, …)

Exports CPU, memory, and FPS metrics as OpenTelemetry gauge measurements over OTLP HTTP.
One OTLP endpoint covers all compatible backends — no need for separate `konitor-grafana`
or `konitor-datadog` artifacts.

**Dependency:**

```kotlin
dependencies {
    implementation("com.smellouk.konitor:opentelemetry-integration:$konitorVersion")
}
```

**Supported platforms:** Android and JVM (real OTLP gauge export via opentelemetry-java 1.51.0).
On iOS, macOS, JS, and WasmJS the integration is a no-op — the opentelemetry-kotlin SDK
has no Metrics API for those targets.

**DSL usage:**

```kotlin
Konitor
    .install(CpuModule)
    .install(MemoryModule)
    .addIntegration(
        OpenTelemetryModule(
            otlpEndpointUrl = "https://otlp-gateway-prod-us-central-0.grafana.net/otlp/v1/metrics"
        ) {
            otlpAuthToken         = "Bearer glc_eyJ..."
            forwardCpu            = true
            forwardMemory         = true
            forwardFps            = false
            exportIntervalSeconds = 30L
        }
    )
```

**DSL options:**

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `otlpEndpointUrl` | `String` | required | OTLP HTTP metrics endpoint (`http://` or `https://` prefix required) |
| `otlpAuthToken` | `String?` | `null` | Bearer token or API key for the endpoint |
| `forwardCpu` | `Boolean` | `false` | Export CPU ratio as `konitor.cpu.usage` gauge |
| `forwardMemory` | `Boolean` | `false` | Export heap usage as `konitor.memory.usage` gauge |
| `forwardFps` | `Boolean` | `false` | Export FPS as `konitor.fps` gauge |
| `exportIntervalSeconds` | `Long` | `30` | How often the OTLP reader flushes gauges to the backend |

---

## Platform support

| Module   | Android | iOS | JVM | macOS | Web |
|----------|:-------:|:---:|:---:|:-----:|:---:|
| CPU      | ✅ | ✅ | ✅ | ✅ | ✅ |
| FPS      | ✅ | ✅ | ✅ | ✅ | ✅ |
| Memory   | ✅ | ✅ | ✅ | ✅ | ✅¹ |
| Network  | ✅² | ✅ | ✅ | ✅ | ✅³ |
| Jank     | ✅ | ❌ | ✅ | ❌ | ❌ |
| GC       | ✅ | ❌ | ✅ | ❌ | ❌ |
| Thermal  | ✅ | ❌ | ❌ | ❌ | ❌ |
| Issues   | ✅ | ❌ | ✅ | ❌ | ❌ |
| Konitor UI| ✅ | ✅⁴ | ❌ | ❌ | ❌ |

> ¹ Heap metrics via `performance.memory` (Chromium-based browsers only).
> ² Full support requires API 23+. API 16–22 reports system-level traffic only.
> ³ Bandwidth estimate via the [Network Information API](https://developer.mozilla.org/en-US/docs/Web/API/NetworkInformation) (Chrome / Edge).
> ⁴ Requires `KonitorUi.attach()` in `AppDelegate` — no auto-init on iOS.

---

## Versioning

Konitor follows [semantic versioning](https://semver.org/):

- **Patch** (`1.0.x`) — bug fixes; no API changes
- **Minor** (`1.x.0`) — new modules or features; backward compatible
- **Major** (`x.0.0`) — breaking API changes; frozen for all v1.x releases

The latest release is always available on [GitHub Releases](https://github.com/smellouk/konitor/releases).
Changes are listed in [CHANGELOG.md](CHANGELOG.md).

---

## Contributing

Contributions are welcome. Please read the guides below before opening a pull request:

- [`CONTRIBUTING.md`](CONTRIBUTING.md) — contribution guide (PR process, commit conventions)
- [`CLAUDE.md`](CLAUDE.md) — operational reference for build commands, module patterns, and commit conventions used by both Claude agents and human contributors
- [GitHub Releases](https://github.com/smellouk/konitor/releases) — published release notes

New module idea? Browse [`libs/modules/`](libs/modules/) for the canonical 4-class structure (Info, Config, Watcher, Performance), or run the `/konitor-new-module` Claude skill if you have Claude Code.

---

## Acknowledgements

Inspired by [AndroidGodEye](https://github.com/Kyson/AndroidGodEye) by Kyson.

---

## License

```
Copyright 2021 S. Mellouk

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
