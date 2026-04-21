# Kamper

[![License](https://img.shields.io/github/license/smellouk/kamper)](LICENSE)
[![Release](https://img.shields.io/github/v/tag/smellouk/kamper?label=release&color=blue)](https://github.com/smellouk/kamper/releases)
[![Issues](https://img.shields.io/github/issues/smellouk/kamper)](https://github.com/smellouk/kamper/issues)
[![Stars](https://img.shields.io/github/stars/smellouk/kamper?style=social)](https://github.com/smellouk/kamper/stargazers)

**Kotlin Multiplatform performance monitoring.** A plugin-based library that gives you live CPU, FPS, memory, and network metrics across Android, JVM, macOS, and Web — through a single unified API.

---

## Platform support

| Module  | Android | JVM | macOS | Web (JS) |
|---------|:-------:|:---:|:-----:|:--------:|
| CPU     | ✅ | ✅ | ✅ | ✅ |
| FPS     | ✅ | ✅ | ✅ | ✅ |
| Memory  | ✅ | ✅ | ✅ | ✅¹ |
| Network | ✅² | ✅ | ✅ | ✅³ |

> ¹ Heap metrics via `performance.memory` (Chromium-based browsers only).  
> ² Full support requires API 23+. API 16–22 reports system-level traffic only.  
> ³ Bandwidth estimate via the [Network Information API](https://developer.mozilla.org/en-US/docs/Web/API/NetworkInformation) (Chrome / Edge).

---

## Screenshots

| CPU | FPS | Memory | Network |
|-----|-----|--------|---------|
| <img src="https://user-images.githubusercontent.com/13059906/147838954-6888f2e0-8552-47e3-9fc0-c4e035b2f117.png" width="180"/> | <img src="https://user-images.githubusercontent.com/13059906/147838957-04b57afc-f9e0-4cdf-b3d9-e465853043c4.png" width="180"/> | <img src="https://user-images.githubusercontent.com/13059906/147838962-991b3dba-b743-434f-ba1e-1357ccc59e12.png" width="180"/> | <img src="https://user-images.githubusercontent.com/13059906/147838968-051ff7ab-6419-417e-b5d7-b9f22c403fc0.png" width="180"/> |

---

## Download

Add the GitHub Packages repository, then pull the engine and whichever modules you need:

```kotlin
repositories {
    maven("https://maven.pkg.github.com/smellouk/kamper")
}

dependencies {
    val kamperVersion = "<latest-version>"

    implementation("com.smellouk.kamper:engine:$kamperVersion")

    // Add only what you need
    implementation("com.smellouk.kamper:cpu-module:$kamperVersion")
    implementation("com.smellouk.kamper:fps-module:$kamperVersion")
    implementation("com.smellouk.kamper:memory-module:$kamperVersion")
    implementation("com.smellouk.kamper:network-module:$kamperVersion")
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

    addInfoListener<CpuInfo>     { info -> /* update UI / analytics */ }
    addInfoListener<FpsInfo>     { info -> /* update UI / analytics */ }
    addInfoListener<MemoryInfo>  { info -> /* update UI / analytics */ }
    addInfoListener<NetworkInfo> { info -> /* update UI / analytics */ }

    start()
}
```

On **Android**, attach Kamper to the lifecycle and it handles `start`, `stop`, and `clear` automatically:

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
        logger       = Logger.EMPTY
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
install(
    FpsModule {
        isEnabled = true
        logger    = Logger.EMPTY
    }
)

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
<summary><strong>Network</strong> — bytes received/transmitted per interval</summary>

```kotlin
install(
    NetworkModule {
        isEnabled    = true
        intervalInMs = 1_000
        logger       = Logger.EMPTY
    }
)

addInfoListener<NetworkInfo> { info ->
    if (info == NetworkInfo.INVALID || info == NetworkInfo.NOT_SUPPORTED) return@addInfoListener
    println("↓ ${info.rxSystemTotalInMb} MB  ↑ ${info.txSystemTotalInMb} MB")
}
```
</details>

---

## Lifecycle

```kotlin
Kamper.start()   // begin polling all installed modules
Kamper.stop()    // pause polling (modules stay installed)
Kamper.clear()   // uninstall all modules and remove all listeners
```

---

## Samples

| Sample | Stack | Description |
|--------|-------|-------------|
| [`samples/android`](samples/android) | Android Views | Full demo with animated metric screens |
| [`samples/compose`](samples/compose) | Compose Multiplatform | Shared UI on Android, Desktop, and WasmJS |
| [`samples/jvm`](samples/jvm) | Swing | Desktop monitor with live charts |
| [`samples/macos`](samples/macos) | AppKit (Kotlin/Native) | Native macOS performance monitor |
| [`samples/web`](samples/web) | Kotlin/JS + DOM | Browser demo with Catppuccin-themed UI |

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
