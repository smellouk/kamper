# Kamper

[![License](https://img.shields.io/github/license/smellouk/kamper)](LICENSE)
[![Release](https://img.shields.io/github/v/tag/smellouk/kamper?label=release&color=blue)](https://github.com/smellouk/kamper/releases)
[![Issues](https://img.shields.io/github/issues/smellouk/kamper)](https://github.com/smellouk/kamper/issues)
[![Stars](https://img.shields.io/github/stars/smellouk/kamper?style=social)](https://github.com/smellouk/kamper/stargazers)

**Kotlin Multiplatform performance monitoring.** A plugin-based library that gives you live CPU, FPS, memory, network, jank, GC, thermal, and issue detection across Android, iOS, JVM, macOS, and Web — through a single unified API.

<img src="screenshots/1.gif" width="320" align="right"/>

### Highlights

- **Zero-boilerplate** — one `install()` call per module
- **Kamper UI** — floating debug overlay for Android, auto-enabled in debug builds, zero app code required
- **8 modules** — CPU, FPS, Memory, Network, Jank, GC, Thermal, Issues
- **Multiplatform** — Android, iOS, JVM, macOS, Web (JS + Wasm)
- **Perfetto export** — record a session and open it at [ui.perfetto.dev](https://ui.perfetto.dev) with one tap

<br clear="right"/>

---

## Kamper UI

A floating chip overlay that sits over any screen and shows live performance metrics. Tap to expand, tap again to open the full panel. Drag to either edge — it snaps flush with no extra rounded corner on the anchored side. Shake the device to restore a collapsed chip.

**Android: zero app code required.** A `ContentProvider` auto-initialises the overlay in debug builds and disables it automatically in release.

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
| Kamper UI| ✅ | ❌ | ❌ | ❌ | ❌ |

> ¹ Heap metrics via `performance.memory` (Chromium-based browsers only).  
> ² Full support requires API 23+. API 16–22 reports system-level traffic only.  
> ³ Bandwidth estimate via the [Network Information API](https://developer.mozilla.org/en-US/docs/Web/API/NetworkInformation) (Chrome / Edge).

---

## Installation

Add the GitHub Packages repository, then pull the engine and whichever modules you need:

```kotlin
repositories {
    maven("https://maven.pkg.github.com/smellouk/kamper")
}

dependencies {
    val kamperVersion = "<latest-version>"

    implementation("com.smellouk.kamper:engine:$kamperVersion")

    // Core metrics
    implementation("com.smellouk.kamper:cpu-module:$kamperVersion")
    implementation("com.smellouk.kamper:fps-module:$kamperVersion")
    implementation("com.smellouk.kamper:memory-module:$kamperVersion")
    implementation("com.smellouk.kamper:network-module:$kamperVersion")

    // Advanced metrics
    implementation("com.smellouk.kamper:jank-module:$kamperVersion")
    implementation("com.smellouk.kamper:gc-module:$kamperVersion")
    implementation("com.smellouk.kamper:thermal-module:$kamperVersion")
    implementation("com.smellouk.kamper:issues-module:$kamperVersion")

    // Android debug overlay (auto-init, no code required)
    debugImplementation("com.smellouk.kamper:ui-android:$kamperVersion")
}
```

---

## Quick start

```kotlin
Kamper.setup {
    logger = Logger.DEFAULT   // swap for Logger.EMPTY in production
}.apply {
    install(CpuModule)
    install(FpsModule)
    install(MemoryModule())
    install(NetworkModule)
    install(JankModule)
    install(GcModule)
    install(ThermalModule)
    install(IssuesModule())

    addInfoListener<CpuInfo>     { info -> /* update UI / analytics */ }
    addInfoListener<FpsInfo>     { info -> /* update UI / analytics */ }
    addInfoListener<MemoryInfo>  { info -> /* update UI / analytics */ }
    addInfoListener<NetworkInfo> { info -> /* update UI / analytics */ }
    addInfoListener<JankInfo>    { info -> /* dropped frame alerts   */ }
    addInfoListener<GcInfo>      { info -> /* GC pressure tracking   */ }
    addInfoListener<ThermalInfo> { info -> /* thermal throttling     */ }
    addInfoListener<IssueInfo>   { info -> /* ANR / crash / slow UI  */ }

    start()
}
```

On **Android**, attach Kamper to the lifecycle for automatic `start` / `stop` / `clear`:

```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(Kamper)
    }
}
```

---

## Modules

<details>
<summary><strong>CPU</strong> — total, user, system, and app CPU ratios</summary>

```kotlin
install(
    CpuModule {
        isEnabled    = true
        intervalInMs = 1_000
    }
)

addInfoListener<CpuInfo> { info ->
    if (info == CpuInfo.INVALID) return@addInfoListener
    println("Total: ${"%.1f".format(info.totalUseRatio * 100)}%")
    println("App:   ${"%.1f".format(info.appRatio * 100)}%")
}
```
</details>

<details>
<summary><strong>FPS</strong> — frames per second via platform frame-timing APIs</summary>

```kotlin
install(FpsModule)

addInfoListener<FpsInfo> { info ->
    if (info != FpsInfo.INVALID) println("FPS: ${info.fps}")
}
```
</details>

<details>
<summary><strong>Memory</strong> — heap usage, PSS (Android), and available RAM</summary>

```kotlin
install(MemoryModule())

addInfoListener<MemoryInfo> { info ->
    if (info == MemoryInfo.INVALID) return@addInfoListener
    println("Heap used: ${info.heapMemoryInfo.allocatedInMb} MB")
    println("RAM free:  ${info.ramInfo.availableRamInMb} MB")
}
```
</details>

<details>
<summary><strong>Network</strong> — bytes received / transmitted per interval</summary>

```kotlin
install(NetworkModule {
    intervalInMs = 1_000
})

addInfoListener<NetworkInfo> { info ->
    if (info == NetworkInfo.INVALID || info == NetworkInfo.NOT_SUPPORTED) return@addInfoListener
    println("↓ ${info.rxSystemTotalInMb} MB  ↑ ${info.txSystemTotalInMb} MB")
}
```
</details>

<details>
<summary><strong>Jank</strong> — dropped frames and slow renders (Android + JVM)</summary>

```kotlin
install(JankModule {
    frameThresholdMs          = 32   // flag frames slower than this
    consecutiveFrameThreshold = 3    // after N consecutive slow frames
})

addInfoListener<JankInfo> { info ->
    if (info == JankInfo.INVALID) return@addInfoListener
    println("Dropped frames: ${info.droppedFrames}")
}
```
</details>

<details>
<summary><strong>GC</strong> — garbage collection runs and pause time (Android + JVM)</summary>

```kotlin
install(GcModule)

addInfoListener<GcInfo> { info ->
    if (info == GcInfo.INVALID) return@addInfoListener
    println("GC runs: +${info.countDelta}  Pause: +${info.pauseMsDelta} ms")
}
```
</details>

<details>
<summary><strong>Thermal</strong> — device thermal state and throttling (Android)</summary>

```kotlin
install(ThermalModule)

addInfoListener<ThermalInfo> { info ->
    if (info == ThermalInfo.INVALID) return@addInfoListener
    println("Thermal: ${info.state}  Throttling: ${info.isThrottling}")
}
```
</details>

<details>
<summary><strong>Issues</strong> — ANR, crash, dropped frames, memory pressure, slow start (Android + JVM)</summary>

```kotlin
install(
    IssuesModule {
        slowSpanEnabled    = true
        slowSpanThresholdMs = 1_000

        droppedFramesEnabled           = true
        droppedFrameThresholdMs        = 32
        droppedFrameConsecutiveThreshold = 3

        crashEnabled          = true
        memoryPressureEnabled = true
        anrEnabled            = true
        slowStartEnabled      = true
    }
)

addInfoListener<IssueInfo> { info ->
    println("[${info.severity}] ${info.type}: ${info.message}")
}
```
</details>

---

## Kamper UI — Android debug overlay

Add the dependency (use `debugImplementation` so it never ships to production):

```kotlin
debugImplementation("com.smellouk.kamper:ui-android:$kamperVersion")
```

That's it. The overlay appears automatically in every debug build via a `ContentProvider`. No `Application` or `Activity` code needed. It is completely stripped from release builds.

### Optional configuration

```kotlin
// In Application.onCreate() or anywhere before first activity launch
KamperUi.configure {
    isEnabled  = true
    position   = ChipPosition.TOP_END  // TOP_START | TOP_END | CENTER_START | CENTER_END | BOTTOM_START | BOTTOM_END
}
```

### Perfetto tracing

The **Perfetto** tab lets you record a session in-app and export a `.perfetto-trace` file directly from the share sheet — no ADB or Android Studio required. Open the file at [ui.perfetto.dev](https://ui.perfetto.dev) to analyse counter tracks for CPU, FPS, Memory, Network, Jank, GC, and Thermal.

---

## Demos

| Demo | Stack | Platform | Screenshot |
|------|-------|----------|------------|
| [`demos/android`](demos/android) | Android Views | Android | <img src="screenshots/5.png" width="120"/> |
| [`demos/compose`](demos/compose) | Compose Multiplatform | Android · Desktop · Web | — |
| [`demos/jvm`](demos/jvm) | Swing | JVM / Desktop | <img src="screenshots/11.png" width="200"/> |
| [`demos/macos`](demos/macos) | AppKit (Kotlin/Native) | macOS | — |
| [`demos/ios`](demos/ios) | UIKit (Kotlin/Native) | iOS | — |
| [`demos/web`](demos/web) | Kotlin/JS + DOM | Browser | <img src="screenshots/12.png" width="200"/> |
| [`demos/react-native`](demos/react-native) | React Native | Android · iOS | — |

---

## Lifecycle

```kotlin
Kamper.start()   // begin polling all installed modules
Kamper.stop()    // pause polling (modules stay installed)
Kamper.clear()   // uninstall all modules and remove all listeners
```

---

## How-tos

<details>
<summary>Publish to local Maven for testing</summary>

```shell
./gradlew publishAllPublicationsToLocalMavenRepository
```

Artifacts are written to `build/maven/`.
</details>

<details>
<summary>Create a custom performance module</summary>

Browse [`kamper/modules/`](kamper/modules/) and follow the same `expect`/`actual` structure. Open a [GitHub issue](https://github.com/smellouk/kamper/issues) if you need guidance.
</details>

---

## Contributing

Contributions are welcome. Please read the [contribution guide](CONTRIBUTING.md) before opening a pull request.

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
