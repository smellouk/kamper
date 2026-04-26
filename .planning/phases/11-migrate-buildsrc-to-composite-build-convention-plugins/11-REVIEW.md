---
phase: 11-migrate-buildsrc-to-composite-build-convention-plugins
reviewed: 2026-04-26T00:00:00Z
depth: standard
files_reviewed: 31
files_reviewed_list:
  - build-logic/build.gradle.kts
  - build-logic/settings.gradle.kts
  - build-logic/src/main/kotlin/AndroidConfigPlugin.kt
  - build-logic/src/main/kotlin/KamperPublishPlugin.kt
  - build-logic/src/main/kotlin/KmpLibraryPlugin.kt
  - build.gradle.kts
  - demos/android/build.gradle.kts
  - demos/compose/build.gradle.kts
  - demos/ios/build.gradle.kts
  - demos/ios/src/iosMain/kotlin/com/smellouk/kamper/ios/ui/ThermalViewController.kt
  - demos/jvm/build.gradle.kts
  - demos/macos/build.gradle.kts
  - demos/macos/src/macosMain/kotlin/com/smellouk/kamper/macos/ui/ThermalView.kt
  - demos/web/build.gradle.kts
  - demos/web/src/jsMain/kotlin/com/smellouk/kamper/web/ui/ThermalSection.kt
  - gradle/libs.versions.toml
  - kamper/api/build.gradle.kts
  - kamper/engine/build.gradle.kts
  - kamper/modules/cpu/build.gradle.kts
  - kamper/modules/fps/build.gradle.kts
  - kamper/modules/gc/build.gradle.kts
  - kamper/modules/issues/build.gradle.kts
  - kamper/modules/issues/src/commonTest/kotlin/com/smellouk/kamper/issues/IssueInfoSentinelTest.kt
  - kamper/modules/jank/build.gradle.kts
  - kamper/modules/memory/build.gradle.kts
  - kamper/modules/network/build.gradle.kts
  - kamper/modules/thermal/build.gradle.kts
  - kamper/ui/android/build.gradle.kts
  - kamper/xcframework/build.gradle.kts
  - settings.gradle.kts
findings:
  critical: 0
  warning: 5
  info: 4
  total: 9
status: issues_found
---

# Phase 11: Code Review Report

**Reviewed:** 2026-04-26
**Depth:** standard
**Files Reviewed:** 31
**Status:** issues_found

## Summary

This phase migrated build configuration from buildSrc to a composite build under `build-logic/` with three convention plugins: `KmpLibraryPlugin`, `AndroidConfigPlugin`, and `KamperPublishPlugin`. The structural migration is sound — plugin registration, includeBuild wiring, version catalog sharing, and the `afterEvaluate` timing workaround for `rootProject.extra` are all correctly handled. No security vulnerabilities or data-loss risks were found.

Five warnings require attention before shipping: a literal `"null"` string in snapshot version names, hardcoded dependency versions in `KmpLibraryPlugin` that are duplicated from the version catalog (creating silent drift risk), a malformed Maven SCM `connection` field that breaks tooling, coroutine scopes in the native demo views that are never cancelled on teardown, and a detekt configuration that silently excludes `kamper/ui/android` entirely. Four informational findings cover the insecure HTTP license URL in POM output, the `-Xskip-metadata-version-check` compiler workaround scope, a fragile `String.contains` path check for artifact ID derivation, and the absence of a `wasmJsTest` dependency block to match the declared `wasmJs` target.

## Warnings

### WR-01: Snapshot version contains literal string "null" when git hash fails

**File:** `build.gradle.kts:88-89`
**Issue:** `generateVersionName()` calls `"git rev-parse --short HEAD".execute()` which returns `String?`. The result is immediately interpolated into a string template without a null guard: `"$default-snapshot-$hash"`. When `execute()` returns `null` (e.g., in a CI sandbox without git history), Kotlin string interpolation calls `null.toString()` and produces a version like `1.2.3-snapshot-null`. This version string would be published as-is.
**Fix:**
```kotlin
fun generateVersionName(): String {
    val default = "git describe --tags --abbrev=0".execute() ?: "0.1.0"
    val isRelease = Env().containsKey("CI")
    return if (isRelease) {
        default
    } else {
        val hash = "git rev-parse --short HEAD".execute() ?: "unknown"
        "$default-snapshot-$hash"
    }
}
```

---

### WR-02: Hardcoded dependency versions in KmpLibraryPlugin diverge from version catalog

**File:** `build-logic/src/main/kotlin/KmpLibraryPlugin.kt:67,78`
**Issue:** `kotlinx-coroutines-test` is hardcoded as `"org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1"` and `mockk` is hardcoded as `"io.mockk:mockk:1.14.3"`. These versions happen to match `gradle/libs.versions.toml` today, but the catalog is the single source of truth. When either version is bumped in the catalog, `KmpLibraryPlugin.kt` will silently use the stale version for all KMP library modules' test dependencies, causing version skew across the build. Because `Plugin<Project>` implementations cannot access the type-safe version catalog accessor, the fix requires reading from the `VersionCatalog` service:
**Fix:**
```kotlin
// Inside configure<KotlinMultiplatformExtension>:
val catalog = project.extensions
    .getByType(org.gradle.api.artifacts.VersionCatalogsExtension::class.java)
    .named("libs")

val coroutinesTestVersion = catalog.findVersion("coroutines").get().requiredVersion
val mockkVersion = catalog.findVersion("mockk").get().requiredVersion

sourceSets.getByName("commonTest").dependencies {
    implementation(project.dependencies.kotlin("test-common"))
    implementation(project.dependencies.kotlin("test-annotations-common"))
    implementation(
        project.dependencies.create("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesTestVersion")
    )
}

sourceSets.findByName("androidInstrumentedTest")?.dependencies {
    implementation(project.dependencies.create("io.mockk:mockk:$mockkVersion"))
}
```

---

### WR-03: Maven POM `scm.connection` uses wrong format; breaks SCM tooling

**File:** `build-logic/src/main/kotlin/KamperPublishPlugin.kt:86`
**Issue:** The Maven SCM `connection` field is set to `"https://github.com/smellouk/kamper.git"`. The Maven SCM specification requires the format `scm:git:https://...` — without the `scm:git:` prefix, tools that parse `<scm><connection>` (e.g., maven-release-plugin, Nexus IQ, dependency-track) will fail to resolve the SCM URL or silently treat it as an unsupported protocol.
**Fix:**
```kotlin
scm {
    connection.set("scm:git:https://github.com/smellouk/kamper.git")
    url.set("https://github.com/smellouk/kamper")
}
```

---

### WR-04: Stress coroutine jobs not cancelled on native view teardown (iOS and macOS)

**File:** `demos/ios/src/iosMain/kotlin/com/smellouk/kamper/ios/ui/ThermalViewController.kt:103-106`
**File:** `demos/macos/src/macosMain/kotlin/com/smellouk/kamper/macos/ui/ThermalView.kt:99-103`
**Issue:** When the user activates CPU stress and then dismisses the view (or the app backgrounds), `stressJobs` contains live `CoroutineScope(Dispatchers.Default)` coroutines. Neither `ThermalViewController` nor `ThermalView` override any lifecycle teardown hook (`viewDidDisappear`/`viewWillDisappear` for UIKit; `removeFromSuperview`/a Kotlin Native `deinit` equivalent for AppKit) to cancel those coroutines. The jobs continue burning CPU until the process exits. Because the scope is `Dispatchers.Default` (not tied to any lifecycle), garbage collection of the view does not stop them.
**Fix for ThermalViewController:**
```kotlin
override fun viewDidDisappear(animated: Boolean) {
    super.viewDidDisappear(animated)
    if (stressActive) {
        stressJobs.forEach { it.cancel() }
        stressJobs = emptyList()
        stressActive = false
        stressBtn.setTitle("Start CPU Stress", forState = UIControlStateNormal)
    }
}
```
Apply equivalent cleanup (`removeFromSuperview` override or a `cleanup()` method called by the parent) in `ThermalView`.

---

### WR-05: Detekt task silently excludes kamper/ui/android entirely

**File:** `build.gradle.kts:66`
**Issue:** The detekt task excludes `"**/android/"`. This ant-glob matches any path segment named exactly `android` — which includes the `kamper/ui/android/` module directory. All Kotlin sources under `kamper/ui/android/src/` are therefore never analysed by detekt. This is the largest non-demo production module in the UI layer, containing `KamperPanelActivity`, `KamperUiRepository`, `PerfettoExporter`, and `ModuleLifecycleManager`. The exclusion appears to be a leftover from an earlier state where Android sources caused detekt failures; it is now broader than intended.
**Fix:** Replace the blanket directory exclusion with a more targeted one, or remove it entirely if Android sources are now clean:
```kotlin
// Replace:
exclude("**/android/")
// With the actual path to demo Android sources if that was the intent:
exclude("**/demos/android/")
// kamper/ui/android will then be included in analysis.
```

---

## Info

### IN-01: Apache License URL uses HTTP instead of HTTPS in POM output

**File:** `build-logic/src/main/kotlin/KamperPublishPlugin.kt:73`
**Issue:** The license URL is `"http://www.apache.org/licenses/LICENSE-2.0.txt"`. Apache redirects this to HTTPS. Maven Central validators and dependency scanners increasingly flag HTTP URLs in POM metadata.
**Fix:** `url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")`

---

### IN-02: `-Xskip-metadata-version-check` suppresses a Kotlin compiler safety check globally

**File:** `build-logic/build.gradle.kts:16`
**Issue:** The flag `-Xskip-metadata-version-check` is added to every `KotlinCompile` task in the `build-logic` project. While the comment correctly explains the mismatch (Gradle's embedded Kotlin 2.0.21 vs. plugin classpath compiled with 2.3.x), this flag disables a check that exists to prevent binary incompatibility. If a future Kotlin release changes metadata in a breaking way, the flag will silently allow compilation to proceed against incompatible bytecode. This is an accepted pragmatic workaround, but should be tracked and removed once the Gradle embedded Kotlin version catches up.
**Fix:** Add a `TODO` comment with a Kotlin/Gradle version condition to ensure it gets revisited:
```kotlin
// TODO: Remove when Gradle ships with embedded Kotlin >= 2.3.x (currently 2.0.21)
freeCompilerArgs.addAll("-Xskip-metadata-version-check")
```

---

### IN-03: `projectDir.parent.contains("modules")` is a fragile String substring check

**File:** `build-logic/src/main/kotlin/KamperPublishPlugin.kt:62`
**Issue:** The artifactId suffix `-module` is appended based on `project.projectDir.parent.contains("modules")`. This is a path substring check: any future module whose parent directory path happens to contain the word "modules" will silently receive the `-module` suffix even if unintended. The check also relies on `File.parent` returning a non-null `String`, which is guaranteed for Gradle subprojects but is not enforced by the type system. A project-path check is more precise and idiomatic in Gradle.
**Fix:**
```kotlin
val projectName = if (project.path.startsWith(":kamper:modules:")) {
    "${project.name}-module"
} else {
    project.name
}
```

---

### IN-04: wasmJs target declared in KmpLibraryPlugin but wasmJsTest deps not injected

**File:** `build-logic/src/main/kotlin/KmpLibraryPlugin.kt:60,86-89`
**Issue:** `KmpLibraryPlugin` declares `wasmJs { browser() }` as a target alongside `js(IR)`, but only injects test dependencies for `commonTest`, `androidUnitTest`, `androidInstrumentedTest`, `jvmTest`, and `jsTest`. There is no `wasmJsTest` block. Tests written under `wasmJsTest` will not have `kotlin("test")` on their compile classpath, causing compilation failures if wasmJs-specific tests are added.
**Fix:** Add after the `jsTest` block:
```kotlin
// wasmJsTest deps
sourceSets.findByName("wasmJsTest")?.dependencies {
    implementation(project.dependencies.kotlin("test"))
}
```

---

_Reviewed: 2026-04-26_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
