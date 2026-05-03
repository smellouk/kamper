---
phase: 25-rename-the-library-from-kamper-to-konitor
reviewed: 2026-05-03T00:00:00Z
depth: standard
files_reviewed: 47
files_reviewed_list:
  - .github/workflows/publish-cocoapods.yml
  - .github/workflows/publish-kotlin.yml
  - .github/workflows/publish-npm.yml
  - build-logic/src/main/kotlin/KonitorPublishPlugin.kt
  - libs/api/src/commonMain/kotlin/com/smellouk/konitor/api/Cleanable.kt
  - libs/api/src/commonMain/kotlin/com/smellouk/konitor/api/Config.kt
  - libs/api/src/commonMain/kotlin/com/smellouk/konitor/api/CurrentPlatform.kt
  - libs/api/src/commonMain/kotlin/com/smellouk/konitor/api/Extensions.kt
  - libs/api/src/commonMain/kotlin/com/smellouk/konitor/api/Info.kt
  - libs/api/src/commonMain/kotlin/com/smellouk/konitor/api/InfoRepository.kt
  - libs/api/src/commonMain/kotlin/com/smellouk/konitor/api/IntegrationModule.kt
  - libs/api/src/commonMain/kotlin/com/smellouk/konitor/api/IWatcher.kt
  - libs/api/src/commonMain/kotlin/com/smellouk/konitor/api/KonitorDslMarker.kt
  - libs/api/src/commonMain/kotlin/com/smellouk/konitor/api/KonitorEvent.kt
  - libs/api/src/commonMain/kotlin/com/smellouk/konitor/api/Logger.kt
  - libs/api/src/commonMain/kotlin/com/smellouk/konitor/api/Performance.kt
  - libs/api/src/commonMain/kotlin/com/smellouk/konitor/api/PlatformTime.kt
  - libs/api/src/commonMain/kotlin/com/smellouk/konitor/api/UserEventInfo.kt
  - libs/api/src/commonMain/kotlin/com/smellouk/konitor/api/Watcher.kt
  - libs/engine/src/commonMain/kotlin/com.smellouk.konitor/Engine.kt
  - libs/engine/src/commonMain/kotlin/com.smellouk.konitor/EngineEventLock.kt
  - libs/engine/src/commonMain/kotlin/com.smellouk.konitor/EnginePlatformTime.kt
  - libs/engine/src/commonMain/kotlin/com.smellouk.konitor/EventRecord.kt
  - libs/engine/src/commonMain/kotlin/com.smellouk.konitor/EventToken.kt
  - libs/engine/src/commonMain/kotlin/com.smellouk.konitor/KonitorConfig.kt
  - libs/engine/src/commonMain/kotlin/com.smellouk.konitor/ValidationInfo.kt
  - libs/modules/cpu/src/commonMain/kotlin/com/smellouk/konitor/cpu/CpuConfig.kt
  - libs/modules/cpu/src/commonMain/kotlin/com/smellouk/konitor/cpu/CpuInfo.kt
  - libs/modules/cpu/src/commonMain/kotlin/com/smellouk/konitor/cpu/CpuPerformance.kt
  - libs/modules/cpu/src/commonMain/kotlin/com/smellouk/konitor/cpu/CpuWatcher.kt
  - libs/modules/cpu/src/commonMain/kotlin/com/smellouk/konitor/cpu/Module.kt
  - libs/modules/fps/src/commonMain/kotlin/com/smellouk/konitor/fps/FpsConfig.kt
  - libs/modules/fps/src/commonMain/kotlin/com/smellouk/konitor/fps/FpsInfo.kt
  - libs/modules/fps/src/commonMain/kotlin/com/smellouk/konitor/fps/FpsWatcher.kt
  - libs/modules/fps/src/commonMain/kotlin/com/smellouk/konitor/fps/Module.kt
  - libs/modules/memory/src/commonMain/kotlin/com/smellouk/konitor/memory/MemoryConfig.kt
  - libs/modules/memory/src/commonMain/kotlin/com/smellouk/konitor/memory/MemoryInfo.kt
  - libs/modules/memory/src/commonMain/kotlin/com/smellouk/konitor/memory/MemoryPerformance.kt
  - libs/modules/memory/src/commonMain/kotlin/com/smellouk/konitor/memory/MemoryWatcher.kt
  - libs/ui/kmm/src/commonMain/kotlin/com/smellouk/konitor/ui/KonitorUi.kt
  - libs/ui/kmm/src/commonMain/kotlin/com/smellouk/konitor/ui/KonitorUiConfig.kt
  - libs/ui/kmm/src/commonMain/kotlin/com/smellouk/konitor/ui/KonitorUiRepository.kt
  - libs/ui/kmm/src/commonMain/kotlin/com/smellouk/konitor/ui/KonitorUiSettings.kt
  - libs/ui/kmm/src/commonMain/kotlin/com/smellouk/konitor/ui/KonitorUiState.kt
  - libs/ui/kmm/src/commonMain/kotlin/com/smellouk/konitor/ui/compose/KonitorChip.kt
  - libs/ui/kmm/src/commonMain/kotlin/com/smellouk/konitor/ui/compose/KonitorPanel.kt
  - libs/ui/kmm/src/commonMain/kotlin/com/smellouk/konitor/ui/compose/KonitorTheme.kt
findings:
  critical: 2
  warning: 5
  info: 2
  total: 9
status: issues_found
---

# Phase 25: Code Review Report

**Reviewed:** 2026-05-03T00:00:00Z
**Depth:** standard
**Files Reviewed:** 47
**Status:** issues_found

## Summary

This phase renamed all Kotlin source packages from `com.smellouk.kamper.*` to `com.smellouk.konitor.*`. The Kotlin source package renames are complete and correct across all reviewed files. All package declarations, class names, and cross-module imports now consistently use `konitor`. No Kotlin source file was found to still declare a `kamper` package.

Two blockers were found in `KonitorPublishPlugin.kt`: the GitHub Packages repository URL still points to the old `kamper` repo path (breaking CI artifact publishing when the GitHub repo is renamed), and the Maven POM metadata baked into every published artifact still carries `kamper` URLs (misleading library consumers and violating the purpose of the rename). Five additional warnings cover unused imports, a `println` in production code without a detekt suppression, a missing `data` modifier on `MemoryConfig`, and a duplicated KDoc block.

---

## Critical Issues

### CR-01: GitHub Packages publish URL hardcodes old `kamper` repo path

**File:** `build-logic/src/main/kotlin/KonitorPublishPlugin.kt:63`
**Issue:** The Maven repository URL for GitHub Packages is hardcoded as `https://maven.pkg.github.com/smellouk/kamper`. GitHub Packages are scoped to the repository they are published from. When the GitHub repository is renamed from `kamper` to `konitor` (the explicit intent of this phase), every CI `publishAllPublicationsToGithubPackagesRepository` invocation will receive a 404 or 403 because the target URL no longer exists. Artifacts will not be published to GitHub Packages.
**Fix:**
```kotlin
// Replace line 63:
url = project.uri("https://maven.pkg.github.com/smellouk/konitor")
```

---

### CR-02: Maven POM metadata embeds `kamper` project/issue/SCM URLs in every published artifact

**File:** `build-logic/src/main/kotlin/KonitorPublishPlugin.kt:97,100,111,112,113`
**Issue:** Four URL fields that are baked into the Maven POM of every published library artifact still reference `smellouk/kamper`:
- `pom.url` (line 97) — the canonical project URL consumers see in their dependency metadata
- `issueManagement.url` (line 100) — where consumers are directed to file bugs
- `scm.connection` (line 111) — used by IDEs and build tools to locate source
- `scm.developerConnection` (line 112) — used for contributors to push SCM changes
- `scm.url` (line 113) — the browseable SCM URL

Once the library is published under the `konitor` branding, these URLs either break (if the repo is renamed) or misidentify the source repository. Maven Central validates SCM metadata and mismatched URLs cause confusion in dependency analysis tools (e.g., Renovate, Dependabot, OSS Review Toolkit).
**Fix:**
```kotlin
// Replace the pom { ... } block URLs (lines 97, 100, 111–113):
url.set("https://github.com/smellouk/konitor")
// ...
url.set("https://github.com/smellouk/konitor/issues")
// ...
connection.set("scm:git:git://github.com/smellouk/konitor.git")
developerConnection.set("scm:git:ssh://git@github.com/smellouk/konitor.git")
url.set("https://github.com/smellouk/konitor")
```

---

## Warnings

### WR-01: Unused import `com.smellouk.konitor.api.EMPTY` in five files

**File:** `libs/engine/src/commonMain/kotlin/com.smellouk.konitor/Engine.kt:5`, `libs/engine/src/commonMain/kotlin/com.smellouk.konitor/KonitorConfig.kt:3`, `libs/modules/cpu/src/commonMain/kotlin/com/smellouk/konitor/cpu/CpuConfig.kt:4`, `libs/modules/fps/src/commonMain/kotlin/com/smellouk/konitor/fps/FpsConfig.kt:4`, `libs/modules/memory/src/commonMain/kotlin/com/smellouk/konitor/memory/MemoryConfig.kt:4`
**Issue:** All five files import `com.smellouk.konitor.api.EMPTY` (the `val Logger.Companion.EMPTY` companion extension), but every call site uses the fully-qualified `Logger.EMPTY` form, which does not require a separate import. The `import com.smellouk.konitor.api.EMPTY` line is therefore unused. While `ForbiddenImport` is set to `active: false` in detekt and this does not break the build, it is noise and may confuse reviewers about which `EMPTY` symbol is intended.
**Fix:** Remove the `import com.smellouk.konitor.api.EMPTY` line from each of the five files. The existing `import com.smellouk.konitor.api.Logger` import already makes `Logger.EMPTY` resolvable.

---

### WR-02: `Logger.SIMPLE` uses `println` with no detekt suppression

**File:** `libs/api/src/commonMain/kotlin/com/smellouk/konitor/api/Logger.kt:15`
**Issue:** CLAUDE.md lists `println(` as a forbidden pattern in production code, and the detekt config registers `kotlin.io.println` under `ForbiddenMethodCall`. Although `ForbiddenMethodCall` is currently `active: false`, this is an intentional production logger implementation that future detekt tightening could break without warning. The use of `println` in `Logger.SIMPLE` is architecturally intentional (it is the explicit "write to console" logger) but there is no suppression annotation to document this intent or protect against accidental activation of the detekt rule.
**Fix:**
```kotlin
val Logger.Companion.SIMPLE: Logger
    get() = object : Logger {
        @Suppress("ForbiddenMethodCall") // intentional: SIMPLE logger wraps println by design
        override fun log(message: String) {
            println("Konitor: $message")
        }
    }
```

---

### WR-03: `MemoryConfig` is a plain `class`, not a `data class`, deviating from module convention

**File:** `libs/modules/memory/src/commonMain/kotlin/com/smellouk/konitor/memory/MemoryConfig.kt:7`
**Issue:** CLAUDE.md specifies: "Data class implementing `Config`". `CpuConfig` and `FpsConfig` are `data class`. `MemoryConfig` is a plain `class`. This means `MemoryConfig` lacks `copy()`, `equals()`, `hashCode()`, and `toString()` — all of which are expected by callers that destructure or compare config instances. Engine code that does equality checks on configs (e.g., to detect reconfiguration) will silently use reference equality on `MemoryConfig` while using structural equality on `CpuConfig` and `FpsConfig`. This is a correctness inconsistency.
**Fix:**
```kotlin
// Change line 7:
data class MemoryConfig(
    override val isEnabled: Boolean,
    override val intervalInMs: Long,
    val logger: Logger
) : Config {
```

---

### WR-04: `KonitorPublishPlugin.kt` KDoc still describes plugin ID as `id("kamper.publish")`

**File:** `build-logic/src/main/kotlin/KonitorPublishPlugin.kt:13`
**Issue:** The KDoc comment states "Applied as: `id("kamper.publish")` in publishing modules' plugins{} block." The plugin IS currently registered with ID `kamper.publish` (in `build-logic/build.gradle.kts` line 32) and all consuming modules use `id("kamper.publish")`, so the comment is technically accurate today. However, this is a rename phase and the KDoc inside `KonitorPublishPlugin.kt` actively perpetuates the old name. If the plugin ID is later migrated to `konitor.publish` the comment will be updated, but if it is not, the mismatch between the class name (`KonitorPublishPlugin`) and the plugin ID (`kamper.publish`) will persist indefinitely without a clear action item.

The comment on line 15 also refers to "kamper/publish.gradle.kts" as the migration source, which is a now-deleted pre-rename artifact reference.
**Fix:** Update the KDoc to reflect the rename intent and track the plugin-ID migration:
```kotlin
 * Applied as: id("kamper.publish") in publishing modules' plugins{} block.
 * TODO(phase25): rename plugin ID to "konitor.publish" and update all consuming build.gradle.kts files.
```
Or alternatively rename the plugin ID now and update all `build.gradle.kts` files that apply it.

---

### WR-05: Duplicated KDoc block on `Engine.drainEvents()`

**File:** `libs/engine/src/commonMain/kotlin/com.smellouk.konitor/Engine.kt:262-271`
**Issue:** The `drainEvents()` function has two consecutive KDoc comments — one from lines 262–264 and another from lines 266–270 — both asserting "Phase 24 D-10". Only the second comment (lines 266–270) is attached to the function; the first is a detached floating comment that will not appear in generated documentation. The first comment incorrectly states "does NOT clear the buffer" while the attached second comment is silent on that behaviour (which is a distinguishing characteristic callers need).
**Fix:** Remove the detached first comment block (lines 262–264). The attached KDoc (lines 266–270) should be expanded to include the "does NOT clear the buffer" note:
```kotlin
/**
 * Phase 24 D-10. Snapshot of the current event buffer; does NOT clear the buffer. Called by
 * [com.smellouk.konitor.ui.RecordingManager] to fold custom events into the
 * Perfetto trace at export time.
 */
fun drainEvents(): List<EventRecord> = eventBufferLock.withLock { eventBuffer.toList() }
```

---

## Info

### IN-01: `KonitorPublishPlugin.kt` KDoc references deleted artifact `kamper/publish.gradle.kts`

**File:** `build-logic/src/main/kotlin/KonitorPublishPlugin.kt:15`
**Issue:** Line 15 reads: "This plugin is the verbatim migration of kamper/publish.gradle.kts (58 lines) into a Plugin<Project> Kotlin class." The file `kamper/publish.gradle.kts` no longer exists — it was the pre-rename script that this plugin replaced. The comment is dead historical context that could confuse new contributors trying to locate the canonical reference.
**Fix:** Replace the reference with the current authoritative plugin file path or remove the historical provenance note:
```kotlin
 * This plugin is the canonical Maven publishing configuration for Konitor library modules,
 * migrated from the legacy publish.gradle.kts script during the kamper→konitor rename.
```

---

### IN-02: `CpuConfig`, `FpsConfig`, and `MemoryConfig` Builder is a `class`, not `object`, deviating from CLAUDE.md pattern

**File:** `libs/modules/cpu/src/commonMain/kotlin/com/smellouk/konitor/cpu/CpuConfig.kt:22`, `libs/modules/fps/src/commonMain/kotlin/com/smellouk/konitor/fps/FpsConfig.kt:26`, `libs/modules/memory/src/commonMain/kotlin/com/smellouk/konitor/memory/MemoryConfig.kt:16`
**Issue:** CLAUDE.md documents the canonical Builder pattern as `object Builder` (singleton). All three reviewed modules use `class Builder` (must be instantiated). Using a class means callers must write `CpuConfig.Builder().apply { ... }.build()` rather than the DSL-style `CpuConfig.Builder.apply { ... }.build()`. This is a pre-existing inconsistency relative to the documented pattern, not introduced by Phase 25, but it is worth flagging for the next module-pattern cleanup pass. It is not a regression from this phase.
**Fix:** Per CLAUDE.md, change each `class Builder` to `object Builder` and ensure state resets between builds if needed.

---

_Reviewed: 2026-05-03T00:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
