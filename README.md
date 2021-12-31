# 🎯 Kamper
![GitHub](https://img.shields.io/github/license/smellouk/kamper)
![GitHub issues](https://img.shields.io/github/issues/smellouk/kamper)
![GitHub tag (latest by date)](https://img.shields.io/github/v/tag/smellouk/kamper)
![GitHub Repo stars](https://img.shields.io/github/stars/smellouk/kamper?style=social)
![GitHub package.json version](https://img.shields.io/github/package-json/v/smellouk/kamper)

Kamper is a KMP/KMM library that implements a unified way to track application performances.
The solution is based on plugin design patterns.

## 💻 Currently Supported platforms
| Platform    | Status      |
| ----------- | ----------- |
| Android     | ✅           |
| JVM         | 🚧          |
| iOS         | ❓           |
| Windows     | ❓           |
| MacOs       | ❓           |
| Linux       | ❓           |

### Android 
Min required api for kamper is 16. 

| Module      | Api level      |
| ----------- | ----------- |
| CPU         | Work as expected on all versions bellow 26 <. For 26+ the system usage is not reported (Under investigation)        |
| FPS         | +16         |
| Memory      | +16         |
| Network     | +23         |

## ⬇️ Install
To Install the library in your project you need:
```kotlin
repositories {
    maven("https://maven.pkg.github.com/smellouk/kamper")
}
```
then
```kotlin
dependencies {
    implementation("com.smellouk.kamper:engine:$KAMPER_VERSION")
    // Implement the need modules
    implementation("com.smellouk.kamper:cpu-module:$KAMPER_VERSION")
    implementation("com.smellouk.kamper:fps-module:$KAMPER_VERSION")
    implementation("com.smellouk.kamper:memory-module:$KAMPER_VERSION")
    implementation("com.smellouk.kamper:network-module:$KAMPER_VERSION")
}
```
## 🏗️️ Usage

### Lifecycle
By default the library is lifecycle aware, you can add it to your activity without the need of caring of kampers' lifecycle.
```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        lifecycle.addObserver(Kamper) // Added to lifecycle manager
    }
}
```

if you wish to handle the library lifecycle then you need:
```kotlin
    fun start() {
    // Will start the kamper engine
    Kamper.start()
}

fun stop() {
    // Will stop kamper engine
    Kamper.stop()
}

fun clean() {
    // Will clean kamper engine from installed module and clean all added listeners
    // (This is critical to not miss it due to the added listeners, otherwise if you forget it, it will introduce memory leaks)
    Kamper.clean()
}
```

### For production
Kamper is production-ready. it also offers a way to install only the needed modules.

#### CPU Monitoring
```kotlin
dependencies {
    // Don't forget to implement the needed module
    implementation("com.smellouk.kamper:cpu-module:$KAMPER_VERSION")
    implementation("com.smellouk.kamper:fps-module:$KAMPER_VERSION")
    implementation("com.smellouk.kamper:memory-module:$KAMPER_VERSION")
    implementation("com.smellouk.kamper:network-module:$KAMPER_VERSION")
}
```
```kotlin
fun initKamper() {
    Kamper.setup {
        logger = if (isDebug) Logger.DEFAULT else Logger.EMPTY
    }.apply {
        // CPU
        // Quick install default(isEnabled=true, intervalInMs=1000, logger=Logger.EMPTY)
        install(CpuModule)
        // Or
        install(
            CpuModule {
                isEnabled = cpuMonitoringIsEnabled
                intervalInMs = 1000
                logger = if (isDebug) Logger.DEFAULT else Logger.EMPTY
            }
        )
        addInfoListener<CpuInfo> { cpuInfo ->
            if (cpuInfo != CpuInfo.INVALID) {
                // Update UI or send it to your default tracking tool
            } else {
                // Do something else
            }
        }
        
        // FPS
        // Quick install default(isEnabled=true, logger=Logger.EMPTY)
        install(FpsModule)
        // Or
        install(
            FpsModule {
                isEnabled = isFpsModuleEnabled
                logger = if (isDebug) Logger.DEFAULT else Logger.EMPTY
            }
        )
        addInfoListener<FpsInfo> { fpsInfo ->
            if (fpsInfo != FpsInfo.INVALID) {
                // Update UI or send it to your default tracking tool
            } else {
                // Do something else
            }
        }
        
        // Memory
        install(
            MemoryModule(applicationContext) {
                isEnabled = isMemoryModuleEnabled
                intervalInMs = 1000
                logger = if (isDebug) Logger.DEFAULT else Logger.EMPTY
            }
        )
        addInfoListener<MemoryInfo> { memoryInfo ->
            if (memoryInfo != MemoryInfo.INVALID) {
                // Update UI or send it to your default tracking tool
            } else {
                // Do something else
            }
        }
        
        // Network
        // Quick install default(isEnabled=true, intervalInMs=1000,logger=Logger.EMPTY)
        install(NetworkModule)
        // Or
        install(
            NetworkModule {
                isEnabled = isNetworkModuleEnabled
                intervalInMs = 1000
                logger = if (isDebug) Logger.DEFAULT else Logger.EMPTY
            }
        )
        addInfoListener<NetworkInfo> { memoryInfo ->
            if (memoryInfo != NetworkInfo.INVALID) {
                // Update UI or send it to your default tracking tool
            } else {
                // Do something else
            }
        }
    }
}
```

### For Debug/Qa
Kamper also offers UI debug tools that allow you to monitor app performance visually. This tool could be used for debugging purposes or for Qa testing.

Feature still under development[coming soon.]

## ✨ Samples/Demos
Our [samples](samples/) dir showcase how to use the library. (Currently, the samples are only for android)

Use cases:
* High usage of CPU [CPU performance]
* FPS usage of the main thread [FPS performance]
* High usage of the memory [Memory performance]
* High usage of the bandwidth [Network performance]

## 🤔 How-tos
* How to test maven publishing locally?

To publish a version locally for testing purposes you just need to call
```shell
./gradlew publishAllPublicationsToLocalMavenRepository
```
The created artifacts can be found in the root build/maven directory.

* How I can create a new performance module?

check our available modules[kamper/modules] and follow the same design or contact us for any support by creating a github issue.

## 🤝 Contribution
We're looking for contributors, help us to improve Kamper.

Please check our [contribution guide](CONTRIBUTING.md).

## 🙏 Acknowledgments
Big thanks to [Kyson/AndroidGodEye](https://github.com/Kyson/AndroidGodEye) for inspiration.

## ⚖ License
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