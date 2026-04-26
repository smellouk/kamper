---
phase: 11-migrate-buildsrc-to-composite-build-convention-plugins
verified: 2026-04-26T20:00:00Z
status: human_needed
score: 8/9 must-haves verified
overrides_applied: 0
human_verification:
  - test: "Run ./gradlew assemble from project root and confirm BUILD SUCCESSFUL"
    expected: "All 18 modules compile with the new composite build infrastructure; no Unresolved reference errors; assemble exits 0"
    why_human: "build-logic/build/ does not exist in the working tree (Gradle hasn't been run post-clone); plugin descriptors cannot be verified without a Gradle execution; the SUMMARY documents ./gradlew assemble passed (1081 tasks) but that cannot be confirmed programmatically without running Gradle"
  - test: "Confirm build cache hit rate is maintained or improved versus the old buildSrc setup"
    expected: "Build cache hit rate for incremental builds is equal to or better than pre-migration"
    why_human: "Build cache performance is a runtime metric that requires comparative Gradle runs and cannot be verified by static analysis"
---

# Phase 11: Migrate buildSrc to Composite Build Verification Report

**Phase Goal:** Replace buildSrc with composite build convention plugins (build-logic)
**Verified:** 2026-04-26T20:00:00Z
**Status:** human_needed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| #  | Truth | Status | Evidence |
|----|-------|--------|----------|
| 1  | buildSrc source files (Libs.kt, Config.kt, Modules.kt, build.gradle.kts) are deleted | ✓ VERIFIED | `test -f buildSrc/src/main/kotlin/Libs.kt` returns false; no tracked files in buildSrc via `git ls-files buildSrc/`; only Gradle cache dirs (.gradle, .kotlin, build) remain |
| 2  | Convention plugins in build-logic/ (3 classes: KmpLibraryPlugin, AndroidConfigPlugin, KamperPublishPlugin) exist and are substantive | ✓ VERIFIED | All 3 .kt files present; each declares a real `Plugin<Project>` implementation; KmpLibraryPlugin applies 3 plugins + 10 targets + 5 test dep blocks; AndroidConfigPlugin configures compileSdk=35/minSdk=21/Java17; KamperPublishPlugin has full maven-publish wiring in afterEvaluate |
| 3  | gradle/libs.versions.toml exists with 17 versions, 14 libraries, 13 plugin aliases | ✓ VERIFIED | File present; Python count: versions=17, libraries=14, plugins=13; all required entries confirmed (kotlin=2.3.20, agp=8.12.0, mokkery=3.3.0, kotlin-gradle-plugin, android-gradle-plugin, mokkery-gradle-plugin, kamper-kmp-library, kamper-android-config, kamper-publish) |
| 4  | All modules use convention plugins for shared build config — zero Libs.*/Modules.*/Config.*/Versions.* references | ✓ VERIFIED | Wide regression grep across all *.gradle.kts in kamper/ and demos/ returns zero matches; apply(from) refs absent; kotlin("multiplatform") shorthand absent |
| 5  | 9 standard KMP modules apply id("kamper.kmp.library"); 9 publishing modules apply id("kamper.publish"); api correctly omits publish | ✓ VERIFIED | grep count: kamper.kmp.library=10 (api+engine+8 modules), kamper.publish=9 (engine+8 modules); api has no kamper.publish and no publishLibraryVariants; all 9 publishing modules have both kamper.publish and publishLibraryVariants("release") |
| 6  | Root settings.gradle.kts wires build-logic via pluginManagement.includeBuild AND has 18 include() statements | ✓ VERIFIED | includeBuild("build-logic") confirmed inside pluginManagement{} block (Python regex check); include() count=18 |
| 7  | Root build.gradle.kts has no buildscript{} block, no subprojects{} block, retains extra.apply{} and detekt config | ✓ VERIFIED | grep confirms buildscript ABSENT, subprojects ABSENT, extra.apply PRESENT, alias(libs.plugins.detekt) PRESENT, detektPlugins(libs.detekt.formatting) PRESENT, set("LIB_VERSION_NAME") PRESENT |
| 8  | kamper/publish.gradle.kts is deleted | ✓ VERIFIED | test -f kamper/publish.gradle.kts returns false |
| 9  | Build cache hit rate maintained or improved (ROADMAP SC3) | ? UNCERTAIN | Requires human: runtime metric needing comparative Gradle executions; ./gradlew assemble also needs a human run to confirm plugin descriptors and full compilation work in the current state |

**Score:** 8/9 truths verified (9th requires human)

### Deferred Items

None — no later phases address items unresolved in Phase 11.

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `gradle/libs.versions.toml` | Version catalog: 17 versions, 14 libraries, 13 plugins | ✓ VERIFIED | All counts confirmed; all required entries present |
| `build-logic/settings.gradle.kts` | Standalone build identity + catalog wiring | ✓ VERIFIED | rootProject.name="build-logic"; from(files("../gradle/libs.versions.toml")) present |
| `build-logic/build.gradle.kts` | kotlin-dsl + 3 compileOnly deps + gradlePlugin{} registrations | ✓ VERIFIED | kotlin-dsl applied; compileOnly(libs.kotlin.gradle.plugin/android.gradle.plugin/mokkery.gradle.plugin); 3 plugin IDs registered with implementationClass mappings |
| `build-logic/src/main/kotlin/KmpLibraryPlugin.kt` | KMP boilerplate plugin: 10 targets + 3 plugins + test deps | ✓ VERIFIED | class KmpLibraryPlugin : Plugin<Project>; all 10 targets; 3 pluginManager.apply() calls; AndroidConfigPlugin applied; 5 sourceSet dep blocks; publishLibraryVariants NOT in implementation (only in KDoc comment) |
| `build-logic/src/main/kotlin/AndroidConfigPlugin.kt` | Android SDK 35 + Java 17 defaults | ✓ VERIFIED | class AndroidConfigPlugin : Plugin<Project>; compileSdk=35; minSdk=21; withId("com.android.library") guard present |
| `build-logic/src/main/kotlin/KamperPublishPlugin.kt` | maven-publish migration with afterEvaluate | ✓ VERIFIED | class KamperPublishPlugin : Plugin<Project>; maven-publish apply; afterEvaluate wraps all publishing config; rootProject.extra["LIB_VERSION_NAME/KAMPER_GH_USER/KAMPER_GH_PAT"] reads present |
| `settings.gradle.kts` | pluginManagement.includeBuild("build-logic") + 18 includes | ✓ VERIFIED | includeBuild inside pluginManagement block confirmed; 18 include() lines |
| `build.gradle.kts` (root, cleaned) | No buildscript/subprojects; catalog aliases; extra.apply{} preserved | ✓ VERIFIED | All negative checks pass; all positive preservation checks pass |
| `buildSrc/` | DELETED (source files) | ✓ VERIFIED | Libs.kt, Config.kt, Modules.kt, build.gradle.kts absent; no git-tracked files remain; only Gradle cache dirs present |
| `kamper/publish.gradle.kts` | DELETED | ✓ VERIFIED | File absent |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `build-logic/build.gradle.kts` gradlePlugin{} | `gradle/libs.versions.toml` | compileOnly(libs.kotlin.gradle.plugin) | ✓ WIRED | compileOnly deps use catalog aliases; catalog has kotlin-gradle-plugin, android-gradle-plugin, mokkery-gradle-plugin entries |
| `settings.gradle.kts` | `build-logic/` | pluginManagement.includeBuild("build-logic") | ✓ WIRED | includeBuild() confirmed inside pluginManagement{} block |
| `kamper/engine/build.gradle.kts` plugins{} | `build-logic/.../KmpLibraryPlugin.kt` | id("kamper.kmp.library") | ✓ WIRED | Convention plugin ID declared; gradlePlugin{} registration maps to KmpLibraryPlugin class |
| `kamper/engine/build.gradle.kts` plugins{} | `build-logic/.../KamperPublishPlugin.kt` | id("kamper.publish") | ✓ WIRED | Convention plugin ID declared; gradlePlugin{} registration maps to KamperPublishPlugin class |
| `module build.gradle.kts` dependencies | `gradle/libs.versions.toml` | libs.kotlinx.coroutines.core, libs.androidx.annotation, etc. | ✓ WIRED | All Libs.* references replaced with libs.* catalog aliases; regression grep returns zero buildSrc refs |
| `build.gradle.kts` extra.apply{} | `build-logic/.../KamperPublishPlugin.kt` | rootProject.extra["LIB_VERSION_NAME/KAMPER_GH_USER/KAMPER_GH_PAT"] | ✓ WIRED | KamperPublishPlugin reads rootProject.extra inside afterEvaluate; root build.gradle.kts preserves extra.apply{} block with all three set() calls |

### Data-Flow Trace (Level 4)

Not applicable — this phase produces build infrastructure (Gradle plugins, catalog, settings), not components that render dynamic data. No data-flow trace required.

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| build-logic compiles standalone | `./gradlew -p build-logic classes` | Not run (no Gradle cache; would require network + Gradle daemon) | ? SKIP (human needed) |
| Full build succeeds after migration | `./gradlew assemble` | Not run in verification — SUMMARY documents BUILD SUCCESSFUL (1081 tasks, commit 04ab67d) | ? SKIP (human needed — confirm on clean run) |
| No buildSrc references remain | `grep -rn 'Libs\.\|Modules\.\|Config\.\|Versions\.' kamper/ demos/ build.gradle.kts settings.gradle.kts --include='*.gradle.kts'` | Zero matches | ✓ PASS |
| Plugin descriptor registrations | `find build-logic/build -name '*.properties' -path '*gradle-plugins*'` | build-logic/build/ does not exist (not yet built in current checkout) | ? SKIP (human needed) |

### Requirements Coverage

| Requirement | Source | Description | Status | Evidence |
|-------------|--------|-------------|--------|----------|
| BUILD-01 (ROADMAP) | Phase 11 requirement | Replace buildSrc with composite build convention plugins | ✓ SATISFIED | All 3 convention plugin classes implemented; buildSrc source deleted; all modules use new plugins; catalog in place |
| BR-01 (v1.0-REQUIREMENTS.md) | Build & Release Requirements | Build Modernization — Replace buildSrc with composite build convention plugins | ✓ SATISFIED | Same evidence as BUILD-01 |
| BUILD-FOUNDATION-01/02/03 | Plan 01 | libs.versions.toml + build-logic skeleton + settings wiring | ✓ SATISFIED | All artifacts verified present and substantive |
| BUILD-CONVENTION-01/02/03 | Plan 02 | KmpLibraryPlugin + AndroidConfigPlugin + KamperPublishPlugin classes | ✓ SATISFIED | All 3 classes verified present and substantive |
| BUILD-MIGRATE-LIB-01/02/03 | Plan 03 | 12 kamper/* modules migrated to convention plugins | ✓ SATISFIED | 10 modules use kamper.kmp.library; api and xcframework use appropriate alternatives; zero Libs.*/Modules.* refs |
| BUILD-MIGRATE-DEMOS-01 | Plan 04 | 6 demo modules migrated to catalog aliases | ✓ SATISFIED | All 6 demos verified; zero Libs.*/Modules.*/Config.*/Versions.* refs |
| BUILD-MIGRATE-ROOT-01 | Plan 04 | Root build.gradle.kts cleaned | ✓ SATISFIED | buildscript{} and subprojects{} blocks removed; extra.apply{} preserved; catalog aliases in place |
| BUILD-CLEANUP-01 | Plan 04 | buildSrc/ and kamper/publish.gradle.kts deleted | ✓ SATISFIED | Both confirmed absent (no git-tracked files remain) |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `build-logic/build.gradle.kts` | 16 | `-Xskip-metadata-version-check` compiler flag | ℹ️ Info | Workaround for Gradle 8.13 embedded Kotlin 2.0.21 vs compileOnly classpath Kotlin 2.3.x metadata mismatch. Documented in SUMMARY 02 as intentional; removable when project upgrades to Gradle 8.14+ |
| `settings.gradle.kts` | 18 | `PREFER_SETTINGS` instead of `FAIL_ON_PROJECT_REPOS` | ⚠️ Warning | Documented deviation: KGP 2.3.20 AbstractSetupTask unconditionally adds nodejs.org/yarn ivy repos at project configuration time; FAIL_ON_PROJECT_REPOS blocks this unconditionally. PREFER_SETTINGS + explicit ivy repos in settings achieves equivalent security. Documented in SUMMARY 04 as intentional. |
| `kamper/ui/android/build.gradle.kts` | 4 | `id("org.jetbrains.compose")` instead of `alias(libs.plugins.compose.multiplatform)` | ⚠️ Warning | Documented deviation (SUMMARY 03): using alias() with a plugin already on the buildscript classpath triggered "already on classpath with unknown version" hard error during transitional wave. Functionally equivalent. Plan 03 Plan had expected alias() but id() was required. |
| `kamper/xcframework/build.gradle.kts` | 4 | `id("org.jetbrains.kotlin.multiplatform")` instead of `alias(libs.plugins.kotlin.multiplatform)` | ⚠️ Warning | Same documented deviation as above. id() is functionally equivalent; SUMMARY 03 documents the reason. |

### Human Verification Required

#### 1. Full Gradle Build Confirmation

**Test:** From the repo root, run `./gradlew assemble` (or at minimum `./gradlew help` and `./gradlew -p build-logic classes`)
**Expected:** BUILD SUCCESSFUL; no "Plugin with id ... not found" errors; no "Unresolved reference: Libs/Modules/Config/Versions" errors; plugin descriptors (kamper.kmp.library.properties, kamper.android.config.properties, kamper.publish.properties) generated under build-logic/build/
**Why human:** build-logic/build/ does not exist in the working tree — the Gradle build has not been run in the current checkout session. All source files are correct and commits are verified, but plugin descriptors and compiled classes can only be confirmed via an actual Gradle execution.

#### 2. Build Cache Hit Rate

**Test:** Run `./gradlew assemble --build-cache` twice and compare the task execution count between runs; optionally compare against a pre-migration branch
**Expected:** Build cache hit rate maintained or improved versus old buildSrc setup
**Why human:** Runtime performance metric; requires comparative Gradle runs across configurations; cannot be measured by static analysis.

### Gaps Summary

No structural gaps were found. All source files are in place, all build configuration is correct, and all documented deviations are explained with rationale.

The two warnings (PREFER_SETTINGS and id() vs alias() for composite-classpath plugins) are intentional documented deviations, not mistakes. They reflect real constraints of the migration environment:
- PREFER_SETTINGS: Kotlin/JS toolchain architectural constraint (KGP AbstractSetupTask)
- id() for Compose/multiplatform: transitional-wave classpath conflict now resolved by buildSrc deletion (alias() would work in Phase 12 once classpath loading is fully clean)

The only remaining item is a Gradle execution to confirm the build actually runs successfully, which requires human action.

---

_Verified: 2026-04-26T20:00:00Z_
_Verifier: Claude (gsd-verifier)_
