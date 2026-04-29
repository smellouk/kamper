# kamper-new-module

## Purpose

Scaffolds a complete Kamper KMP performance module skeleton — all 8 platform source sets, `build.gradle.kts` using the `kamper.kmp.library` convention plugin, Info/Config/Watcher/Performance class stubs with INVALID sentinel and Builder/DEFAULT pattern, `expect`/`actual` Module.kt declarations, commonTest stubs, and the `settings.gradle.kts` registration line — leaving the new module ready to compile.

## When to Use

- User invokes `/kamper-new-module <name>` where `<name>` is the module name in lowercase letters and digits (e.g. `thermal2`, `battery`, `gpu`).
- The new module follows the standard Info/Config/Watcher/Performance 4-class structure.
- The module name does not already exist under `libs/modules/`.

## Steps

1. **Validate the name argument** — must be lowercase letters/digits only. Check that `libs/modules/{name}/` does not already exist. If it does, abort with a clear error.

2. **Compute names:**
   - `{name}` = the lowercase argument (e.g. `thermal2`)
   - `{Name}` = PascalCase form (e.g. `Thermal2`) — capitalize first letter and each letter after a digit

3. **Create the directory tree** under `libs/modules/{name}/src/`. Full structure:
   ```
   libs/modules/{name}/
   ├── build.gradle.kts
   └── src/
       ├── commonMain/kotlin/com/smellouk/kamper/{name}/
       │   ├── {Name}Info.kt
       │   ├── {Name}Config.kt
       │   ├── {Name}Watcher.kt
       │   ├── {Name}Performance.kt
       │   ├── Module.kt                     (expect val {Name}Module)
       │   └── repository/
       │       ├── {Name}InfoDto.kt
       │       ├── {Name}InfoMapper.kt
       │       └── {Name}InfoRepository.kt
       ├── commonTest/kotlin/com/smellouk/kamper/{name}/
       │   ├── {Name}ConfigBuilderTest.kt
       │   └── repository/
       │       └── {Name}InfoMapperTest.kt
       ├── androidMain/kotlin/com/smellouk/kamper/{name}/
       │   ├── Module.kt                     (actual val {Name}Module)
       │   └── repository/
       │       ├── {Name}InfoRepositoryImpl.kt
       │       └── source/
       │           └── Android{Name}InfoSource.kt
       ├── androidTest/kotlin/com/smellouk/kamper/{name}/
       │   └── repository/
       │       └── {Name}InfoRepositoryImplTest.kt
       ├── iosMain/kotlin/com/smellouk/kamper/{name}/Module.kt
       ├── jvmMain/kotlin/com/smellouk/kamper/{name}/Module.kt
       ├── macosMain/kotlin/com/smellouk/kamper/{name}/Module.kt
       ├── tvosMain/kotlin/com/smellouk/kamper/{name}/Module.kt
       ├── jsMain/kotlin/com/smellouk/kamper/{name}/Module.kt
       └── wasmJsMain/kotlin/com/smellouk/kamper/{name}/Module.kt
   ```

4. **Write `build.gradle.kts`** using the template in the Reference section below (substitute `{name}`).

5. **Write `commonMain` class stubs** using the verbatim stubs from the Reference section:
   - `{Name}Info.kt` — data class implementing `Info`, with companion `INVALID`
   - `{Name}Config.kt` — data class implementing `Config`, with `@KamperDslMarker Builder` and `DEFAULT`
   - `{Name}Watcher.kt` — internal class extending `Watcher<{Name}Info>`
   - `{Name}Performance.kt` — internal class extending `Performance<...>`
   - `Module.kt` — `expect val {Name}Module: PerformanceModule<{Name}Config, {Name}Info>`
   - `repository/{Name}InfoDto.kt`, `repository/{Name}InfoMapper.kt`, `repository/{Name}InfoRepository.kt` — stubs

6. **Write `{platform}Main/Module.kt` actual files** for all 7 platform actual source sets using the no-op actual stub from the Reference section. For `androidMain` and `jvmMain`, also write:
   - `repository/{Name}InfoRepositoryImpl.kt` (implements `{Name}InfoRepository`)
   - `repository/source/Android{Name}InfoSource.kt` / `Jvm{Name}InfoSource.kt` (platform data access stub)

7. **Write commonTest stubs:**
   - `{Name}ConfigBuilderTest.kt` — test that `{Name}Config.DEFAULT.isEnabled == false`
   - `repository/{Name}InfoMapperTest.kt` — test that `{Name}InfoMapper().map({Name}InfoDto.INVALID) == {Name}Info.INVALID`

8. **Write androidTest stub:**
   - `repository/{Name}InfoRepositoryImplTest.kt` — placeholder test referencing `{Name}InfoRepositoryImpl`

9. **Update `settings.gradle.kts`** by inserting `include(":libs:modules:{name}")` in alphabetical order within the kamper modules block. If the line already exists, abort with a clear error.

10. **Verification step:** Run `./gradlew :libs:modules:{name}:compileCommonMainKotlinMetadata` to confirm the scaffold compiles. If `./gradlew` commands are not yet in the `.claude/settings.json` allowlist (they are added by Plan 19-03), Claude Code will prompt for permission — accept the prompt for this task.

11. **Report back** to the user:
    - List of all files created (full paths)
    - The `settings.gradle.kts` diff (one new `include` line)
    - Next steps: implement the `{Name}Watcher` polling logic and the `Android{Name}InfoSource` platform read. Mirror `libs/modules/cpu/` for patterns.

## Reference

### build.gradle.kts template

```kotlin
plugins {
    id("kamper.kmp.library")
    id("kamper.publish")
}

android {
    namespace = "com.smellouk.kamper.{name}"
    buildFeatures { buildConfig = true }
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":libs:api"))
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.android)
                implementation(libs.androidx.annotation)
            }
        }
    }
}
```

### {Name}Info.kt stub

```kotlin
package com.smellouk.kamper.{name}

import com.smellouk.kamper.api.Info

data class {Name}Info(val value: Double) : Info {
    companion object {
        val INVALID = {Name}Info(-1.0)
    }
}
```

### {Name}Config.kt stub (with INVALID, Builder, DEFAULT)

```kotlin
package com.smellouk.kamper.{name}

import com.smellouk.kamper.api.Config
import com.smellouk.kamper.api.KamperDslMarker
import com.smellouk.kamper.api.Logger

data class {Name}Config(
    override val isEnabled: Boolean,
    override val intervalInMs: Long,
    val logger: Logger
) : Config {
    @KamperDslMarker
    object Builder {
        var isEnabled: Boolean = false
        var intervalInMs: Long = 1000L
        var logger: Logger = Logger.EMPTY
        fun build() = {Name}Config(isEnabled, intervalInMs, logger)
    }

    companion object {
        val DEFAULT = Builder.build()
    }
}
```

### commonMain/Module.kt (expect)

```kotlin
package com.smellouk.kamper.{name}

import com.smellouk.kamper.api.PerformanceModule

expect val {Name}Module: PerformanceModule<{Name}Config, {Name}Info>
```

### {platform}Main/Module.kt (actual — no-op platforms: iosMain, macosMain, tvosMain, jsMain, wasmJsMain)

```kotlin
package com.smellouk.kamper.{name}

import com.smellouk.kamper.api.PerformanceModule

actual val {Name}Module: PerformanceModule<{Name}Config, {Name}Info>
    get() = PerformanceModule(
        config = {Name}Config.DEFAULT,
        performance = {Name}Performance(
            watcher = {Name}Watcher(
                defaultDispatcher = kotlinx.coroutines.Dispatchers.Default,
                mainDispatcher = kotlinx.coroutines.Dispatchers.Main,
                repository = {Name}InfoRepositoryImpl({Name}InfoSource()),
                logger = {Name}Config.DEFAULT.logger
            ),
            logger = {Name}Config.DEFAULT.logger
        )
    )
```

### settings.gradle.kts registration

Insert this line in alphabetical order within the kamper modules include block:

```kotlin
include(":libs:modules:{name}")
```

### Canonical reference module

`libs/modules/cpu/` is the most complete and well-tested module. When in doubt about a file or pattern, mirror what CPU does. The CPU module has all 8 platform main source sets and complete test coverage.

### Convention reminders

- **Naming:** PascalCase classes (`{Name}Info`, `{Name}Config`, `{Name}Watcher`, `{Name}Performance`)
- **INVALID sentinel:** `companion object { val INVALID = {Name}Info(-1.0) }` on every Info class
- **Builder/DEFAULT:** every Config has `@KamperDslMarker object Builder { ... fun build() = ... }` and `companion object { val DEFAULT = Builder.build() }`
- **Safety:** every platform call in `{Platform}{Name}InfoSource` wrapped in `try/catch`; return `INVALID` on failure; never crash the host app
- **Coroutines:** Watcher loops use `while (isActive)`, never `while (true)`. Import `kotlinx.coroutines.isActive`.
- **Logging:** use `logger.log(...)`, never `println(...)` in production code
- **No forbidden comments:** `TODO:` and `FIXME:` cause Detekt build failures

### Allowlist note

`./gradlew` commands may not yet be in the `.claude/settings.json` allowlist when this skill is first used. After Plan 19-03 runs, `jvmTest`, `test`, and `detekt` are pre-approved. The compilation verification in Step 10 will trigger a permission prompt if the allowlist has not been extended yet — accept it for that task.
