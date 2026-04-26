---
phase: 11-migrate-buildsrc-to-composite-build-convention-plugins
plan: 02
subsystem: infra
tags: [gradle, build-logic, convention-plugin, kmp, android, maven-publish]

# Dependency graph
requires:
  - phase: 11-migrate-buildsrc-to-composite-build-convention-plugins
    plan: 01
    provides: build-logic/ skeleton + plugin registrations + version catalog
provides:
  - build-logic/src/main/kotlin/AndroidConfigPlugin.kt — compileSdk=35 + minSdk=21 + Java 17 + jvmTarget=17 convention plugin
  - build-logic/src/main/kotlin/KmpLibraryPlugin.kt — 10 KMP targets + 3 plugins + AndroidConfigPlugin + 5 test-dep sourcesets
  - build-logic/src/main/kotlin/KamperPublishPlugin.kt — full maven-publish migration from publish.gradle.kts
  - Plugin IDs kamper.kmp.library, kamper.android.config, kamper.publish are now backed by real classes and fully resolvable
affects: [11-03, 11-04, all KMP library modules]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - kotlin-dsl configure<T>{} extension (reified) for PublishingExtension configuration
    - withId("com.android.library") guard for safe AndroidConfigPlugin application on non-Android projects
    - project.configure<T>{} + project.extensions.configure(T::class.java){} for extension access in Plugin<Project>
    - -Xskip-metadata-version-check for Gradle 8.13 embedded Kotlin 2.0.21 vs compileOnly classpath Kotlin 2.3.20 mismatch
    - org.gradle.kotlin.dsl.kotlin import for DependencyHandler.kotlin() in plain .kt plugin files

key-files:
  created:
    - build-logic/src/main/kotlin/AndroidConfigPlugin.kt
    - build-logic/src/main/kotlin/KmpLibraryPlugin.kt
    - build-logic/src/main/kotlin/KamperPublishPlugin.kt
  modified:
    - build-logic/build.gradle.kts
  deleted:
    - build-logic/src/main/kotlin/.gitkeep

key-decisions:
  - "project.configure<T>{} (kotlin-dsl reified extension) used for KamperPublishPlugin instead of configure(T::class.java){} — the Java Action<in T> contravariant SAM conversion fails type inference with named lambda parameters"
  - "AndroidConfigPlugin uses project.extensions.configure(LibraryExtension::class.java){} with this-receiver lambda (no named parameter) — matches kotlin-dsl extension function contract"
  - "-Xskip-metadata-version-check added to build-logic/build.gradle.kts — Gradle 8.13 embeds Kotlin 2.0.21 which rejects compileOnly classpath jars compiled with Kotlin 2.3.x"
  - "org.gradle.kotlin.dls.kotlin import added to KmpLibraryPlugin — DependencyHandler.kotlin() extension not available in plain .kt without explicit import"
  - "buildSrc/ and kamper/publish.gradle.kts still present — untouched in this plan; deletion is Plan 04 only"

requirements-completed: [BUILD-CONVENTION-01, BUILD-CONVENTION-02, BUILD-CONVENTION-03]

# Metrics
duration: 8min
completed: 2026-04-26
---

# Phase 11 Plan 02: Convention Plugin Classes Summary

**Three Kotlin Plugin<Project> convention plugin classes backed by real implementations; plugin IDs kamper.kmp.library, kamper.android.config, kamper.publish are resolvable and compile cleanly under Gradle 8.13**

## Performance

- **Duration:** ~8 min
- **Started:** 2026-04-26T18:38:39Z
- **Completed:** 2026-04-26T18:46:00Z
- **Tasks:** 4 (3 file-creation + 1 verification with 4 deviation auto-fixes)
- **Files modified:** 5 (3 created, 1 modified, 1 deleted)

## Accomplishments

- Created `build-logic/src/main/kotlin/AndroidConfigPlugin.kt` — configures LibraryExtension (compileSdk=35, minSdk=21, Java 17) behind withId("com.android.library") guard; configures KotlinCompile jvmTarget=17 on all tasks
- Created `build-logic/src/main/kotlin/KmpLibraryPlugin.kt` — applies 3 plugins (kotlin.multiplatform, com.android.library, dev.mokkery) + AndroidConfigPlugin; configures all 10 KMP targets; injects 5 test sourceSet dep blocks
- Created `build-logic/src/main/kotlin/KamperPublishPlugin.kt` — verbatim Plugin<Project> migration of kamper/publish.gradle.kts; full afterEvaluate wrap; GithubPackages + LocalMaven repos; full POM with artifactId rewrite
- Deleted `build-logic/src/main/kotlin/.gitkeep` (directory now holds real .kt files)
- `./gradlew -p build-logic classes` exits 0 (BUILD SUCCESSFUL with only deprecation warnings)
- `./gradlew help` exits 0 (root build unaffected)
- Three plugin descriptors auto-generated: kamper.kmp.library.properties, kamper.android.config.properties, kamper.publish.properties

## Task Commits

1. **Task 1: Author AndroidConfigPlugin.kt** — `6793057` (chore)
2. **Task 2: Author KmpLibraryPlugin.kt** — `a963f31` (chore)
3. **Task 3: Author KamperPublishPlugin.kt** — `d9ec7f7` (chore)
4. **Task 4 deviation fixes (compilation errors)** — `e84ca82` (fix)

## Files Created/Modified

- `build-logic/src/main/kotlin/AndroidConfigPlugin.kt` — Plugin<Project> that applies Kamper Android SDK + Java 17 defaults to any project with com.android.library applied (guarded by withId). Sets compileSdk=35, minSdk=21, sourceCompatibility/targetCompatibility=VERSION_17, jvmTarget=17 on all KotlinCompile tasks.
- `build-logic/src/main/kotlin/KmpLibraryPlugin.kt` — Plugin<Project> applying org.jetbrains.kotlin.multiplatform + com.android.library + dev.mokkery by full plugin ID strings. Applies AndroidConfigPlugin. Configures all 10 KMP targets. Injects commonTest (3 deps) + androidUnitTest + androidInstrumentedTest + jvmTest + jsTest dep blocks. Does NOT include publishLibraryVariants (Pitfall 8).
- `build-logic/src/main/kotlin/KamperPublishPlugin.kt` — Plugin<Project> migration of kamper/publish.gradle.kts. Applies maven-publish, sets group="com.smellouk.kamper". All publishing config inside afterEvaluate{}. GithubPackages repo gated by CI env var. Reads KAMPER_GH_USER/KAMPER_GH_PAT from rootProject.extra. Full POM with -module suffix for modules/ path.
- `build-logic/build.gradle.kts` — Added KotlinCompile configureEach block with -Xskip-metadata-version-check to handle Gradle 8.13 embedded Kotlin 2.0.21 vs compileOnly classpath Kotlin 2.3.x metadata version mismatch.
- `build-logic/src/main/kotlin/.gitkeep` — DELETED (directory now holds 3 .kt files)

## Decisions Made

- `project.configure<PublishingExtension>{}` (kotlin-dsl reified extension) instead of `configure(PublishingExtension::class.java){}` in KamperPublishPlugin — the Java API uses `Action<in T>` (contravariant) which causes SAM conversion type inference failures with named lambda parameters in Kotlin 2.0.21
- `project.extensions.configure(LibraryExtension::class.java){}` with implicit `this` receiver (no named parameter) in AndroidConfigPlugin — the kotlin-dsl configureEach and withType lambdas are extension functions (this-receiver), not regular lambdas with explicit parameters
- `org.gradle.kotlin.dsl.kotlin` import added in KmpLibraryPlugin — `DependencyHandler.kotlin()` is a kotlin-dsl extension only available in .kts scripts unless explicitly imported
- `-Xskip-metadata-version-check` added to build-logic compilation — Gradle 8.13 bundles Kotlin 2.0.21 but compileOnly classpath (kotlin-gradle-plugin 2.3.20, mokkery 3.3.0) has Kotlin 2.3.x metadata; without the flag, embedded compiler rejects those jars as errors
- `buildSrc/` and `kamper/publish.gradle.kts` untouched — deletion deferred to Plan 04 as documented in Plan 01

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Lambda parameter type inference failure in configure() calls**
- **Found during:** Task 4 (./gradlew -p build-logic classes)
- **Issue:** Multiple `configure(T::class.java) { param -> ... }` and `configureEach { task -> ... }` calls failed with "None of the following functions can be called" and "Expression cannot be invoked as a function". The kotlin-dsl extension for these methods uses extension function lambdas (this-receiver), not regular lambda parameters.
- **Fix:** Changed all lambdas to use implicit `this` (no explicit parameter name) for `configure{}` and `configureEach{}` calls. For `KamperPublishPlugin`, switched to `project.configure<PublishingExtension>{}` (kotlin-dsl reified overload) which handles the contravariant SAM conversion correctly.
- **Files modified:** AndroidConfigPlugin.kt, KmpLibraryPlugin.kt, KamperPublishPlugin.kt
- **Commit:** e84ca82

**2. [Rule 1 - Bug] DependencyHandler.kotlin() unresolved in KmpLibraryPlugin**
- **Found during:** Task 4 compilation
- **Issue:** `project.dependencies.kotlin("test-common")` failed with "Expression 'kotlin' cannot be invoked as a function" — the `kotlin()` shorthand on `DependencyHandler` is a kotlin-dsl extension only available in .kts scripts without explicit import.
- **Fix:** Added `import org.gradle.kotlin.dsl.kotlin` to KmpLibraryPlugin.kt
- **Files modified:** KmpLibraryPlugin.kt
- **Commit:** e84ca82

**3. [Rule 1 - Bug] Unclosed block comment in KmpLibraryPlugin KDoc**
- **Found during:** Task 4 compilation
- **Issue:** KDoc comment contained `demos/*` which the compiler parsed as an unclosed block comment opener (`/*`).
- **Fix:** Changed to `demos modules` (no asterisk glob notation in KDoc)
- **Files modified:** KmpLibraryPlugin.kt
- **Commit:** e84ca82

**4. [Rule 1 - Bug] Gradle embedded Kotlin 2.0.21 rejects compileOnly classpath Kotlin 2.3.x metadata**
- **Found during:** Task 4 compilation (all three fix attempts above)
- **Issue:** After fixing source-level errors, remaining build failure was Kotlin compiler (2.0.21) treating Kotlin 2.3.x metadata in compileOnly classpath jars as hard errors: "Module was compiled with an incompatible version of Kotlin. The binary version of its metadata is 2.3.0, expected version is 2.0.0."
- **Fix:** Added `-Xskip-metadata-version-check` compiler argument in build-logic/build.gradle.kts KotlinCompile configureEach block. This suppresses the metadata version check while still providing full compilation/type-checking capability.
- **Files modified:** build-logic/build.gradle.kts
- **Commit:** e84ca82

---

**Total deviations:** 4 auto-fixed (all Rule 1 - compile-time bugs triggered by the Plugin<Project> vs .kts-script context differences and Gradle version mismatch)
**Impact on plan:** All deviations resolved; plan goal achieved. The -Xskip-metadata-version-check workaround is a pragmatic fix for the Gradle 8.13 / Kotlin 2.3.x classpath version gap. When the project upgrades to Gradle 8.14+ (which ships with Kotlin 2.1+), this flag can be removed.

## Known Stubs

None — all three plugin classes are fully implemented with real behavior. No placeholder code.

## Threat Flags

None — no new network endpoints, auth paths, file access patterns, or schema changes introduced.

The `KamperPublishPlugin` credential handling mirrors the existing `kamper/publish.gradle.kts` exactly. Credentials are only read inside `afterEvaluate` and only used for `repo.credentials{}` configuration — never logged, echoed, or stored to disk.

---
*Phase: 11-migrate-buildsrc-to-composite-build-convention-plugins*
*Completed: 2026-04-26*

## Self-Check: PASSED

- build-logic/src/main/kotlin/AndroidConfigPlugin.kt: FOUND
- build-logic/src/main/kotlin/KmpLibraryPlugin.kt: FOUND
- build-logic/src/main/kotlin/KamperPublishPlugin.kt: FOUND
- build-logic/src/main/kotlin/.gitkeep: ABSENT (correct — deleted in Task 1)
- Commit 6793057: FOUND (Task 1 - AndroidConfigPlugin.kt)
- Commit a963f31: FOUND (Task 2 - KmpLibraryPlugin.kt)
- Commit d9ec7f7: FOUND (Task 3 - KamperPublishPlugin.kt)
- Commit e84ca82: FOUND (Task 4 deviation fixes)
