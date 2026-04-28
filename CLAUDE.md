# CLAUDE.md

Kamper is a Kotlin Multiplatform (KMP) performance monitoring library for Android, iOS, JVM, macOS, tvOS, JS, and WASM. This file is the authoritative reference for Claude Code agents and contributors working in this repository — all critical information is inlined here so you can act without exploring other files.

---

## Quick Start

- Clone: `git clone https://github.com/smellouk/kamper.git`
- Required: JDK 17. No Android device is needed for the default development workflow.
- Smoke test: `./gradlew :kamper:api:test :kamper:engine:test`
- Read the four sections below before opening a PR.

---

## Build & Test

Prefer the fast JVM path (`jvmTest`) for iterative development — it runs in seconds and requires no device.

| Command | Scope | Device needed? | When to use |
|---------|-------|----------------|-------------|
| `./gradlew :kamper:modules:<name>:jvmTest` | Single module, JVM only | No | Fast feedback — primary path for Claude sessions |
| `./gradlew :kamper:modules:<name>:test` | Single module, all unit tests | No | Includes androidUnitTest via Robolectric/MockK |
| `./gradlew :kamper:api:test` | API contracts | No | After modifying the `api` layer |
| `./gradlew :kamper:engine:test` | Engine | No | After modifying the `engine` layer |
| `./gradlew test` | All modules, all unit tests | No | Full pre-commit sweep |
| `./gradlew detekt` | Static analysis | No | Before every commit (zero-tolerance, `maxIssues: 0`) |
| `./gradlew connectedAndroidTest` | Instrumented on-device | **YES — emulator or physical device** | **DO NOT run in autonomous Claude sessions** |
| `./gradlew assembleXCFramework` | iOS/macOS XCFramework | No (compiles on host) | Only when publishing iOS artifacts |

> **WARNING:** `./gradlew connectedAndroidTest` requires a connected Android device or running emulator.
> It will fail in any environment without one. Never run it autonomously. If a user asks for
> instrumented tests, confirm a device is attached first.

**Note:** Common Gradle commands (`jvmTest`, `test`, `detekt`) are pre-approved in `.claude/settings.json` — Claude Code will not prompt for these. See `.claude/skills/kamper-check/SKILL.md` for the full approved command list. Device-requiring tasks like `connectedAndroidTest` are deliberately excluded.

**Detekt** runs with `autoCorrect: true` — it auto-fixes formatting violations on check. The config lives in `quality/code/detekt.yml`. Forbidden patterns include `TODO:`, `FIXME:`, `println(` in production code, and re-throwing caught exceptions.

---

## Module Patterns

Every Kamper performance module follows a strict 4-class structure. Deviation is a convention violation.

### The 4-Class Structure

| Class | Role | Visibility |
|-------|------|------------|
| `{Name}Info` | Data class implementing `Info`; the metric payload emitted to listeners | `public` |
| `{Name}Config` | Data class implementing `Config`; holds `isEnabled`, `intervalInMs`, `logger` | `public` |
| `{Name}Watcher` | Coroutine polling loop extending `Watcher<{Name}Info>` | `internal` |
| `{Name}Performance` | Lifecycle container extending `Performance<{Name}Config, IWatcher<{Name}Info>, {Name}Info>` | `internal` |

### INVALID Sentinel

Every `Info` subclass must have a companion `INVALID` constant. Callers check for `INVALID` before processing data; listeners are never invoked with invalid readings.

```kotlin
data class {Name}Info(val value: Double) : Info {
    companion object {
        val INVALID = {Name}Info(-1.0)
    }
}
```

### Builder / DEFAULT Pattern

Every `Config` must have a `@KamperDslMarker` Builder object and a `DEFAULT` companion val.

```kotlin
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

### expect/actual Module Declaration

Each module declares one `expect val` in `commonMain` and seven `actual val` implementations — one per platform:

```kotlin
// commonMain/kotlin/com/smellouk/kamper/{name}/Module.kt
expect val {Name}Module: PerformanceModule<{Name}Config, {Name}Info>
```

Actual implementations live in each of the 7 platform main source sets:
- `androidMain` — uses Android SDK APIs (e.g., `/proc/stat`, `ActivityManager`)
- `iosMain` — uses Darwin/iOS system APIs
- `jvmMain` — uses JVM MXBean APIs
- `macosMain` — uses macOS system APIs
- `tvosMain` — uses tvOS system APIs
- `jsMain` — JavaScript/browser APIs
- `wasmJsMain` — WebAssembly target

### Safety Rule (Hard — D-06)

All platform-specific calls inside `{Name}InfoSource` must be wrapped in `try/catch`. Exceptions are logged and absorbed. Kamper must never propagate an exception to the host application.

```kotlin
// Correct — exception is absorbed, INVALID returned
override fun getValue(): Double = try {
    // ... platform call
} catch (e: Exception) {
    logger.log(e.stackTraceToString())
    -1.0
}
```

Additional safety rules:
- Watcher loops use `while (isActive)`, never `while (true)`
- Use `logger.log(...)` — never `println(...)` in production code
- No `TODO:` or `FIXME:` comments (Detekt fails the build on these)

### Canonical Reference Module

`kamper/modules/cpu/` is the most complete and well-tested module. When in doubt, mirror what CPU does. Its `build.gradle.kts` is the reference for new modules:

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
                api(project(":kamper:api"))
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

New modules must also be registered in `settings.gradle.kts`:

```kotlin
include(":kamper:modules:{name}")
```

Use the `/kamper-new-module` project skill to scaffold a complete module skeleton automatically.

---

## Commit & PR Rules

**CLAUDE.md is the authoritative source for commit conventions until Phase 20 rewrites CONTRIBUTING.md.**

The content in `CONTRIBUTING.md` references an old git-flow workflow with a `develop` branch and emoji commits. Those conventions are **outdated** — ignore them. This section supersedes CONTRIBUTING.md.

### Conventional Commits Format

```
<type>(<scope>): <short description>
```

**Allowed types:**

| Type | When to use |
|------|-------------|
| `feat` | New feature or capability |
| `fix` | Bug fix |
| `chore` | Maintenance, dependency bumps, build changes |
| `docs` | Documentation only |
| `test` | Test additions or changes |
| `refactor` | Code restructure without behavior change |

**Allowed scopes:** `cpu`, `fps`, `memory`, `network`, `issues`, `jank`, `gc`, `thermal`, `engine`, `api`, `ui`, `build`, `deps`, `phase<N>`

**Examples:**

```
feat(cpu): add thermal throttle detection
fix(engine): guard against duplicate module install
chore(deps): bump coroutines to 1.10.1
docs(api): update Watcher KDoc
test(fps): add FpsWatcher interval test
refactor(memory): extract MemoryInfoMapper
```

**Rules:**
- No emojis anywhere
- No `resolves #N` or `fixes #N` footers
- No `develop` branch — `main` is the integration branch
- Description is imperative mood, lowercase, no trailing period
- Squash merge when merging executor worktree branches back

### PR Checklist

Before opening a pull request:

- [ ] `./gradlew detekt` passes (zero issues)
- [ ] `./gradlew :kamper:modules:<name>:jvmTest` passes for every touched module
- [ ] No `TODO:` or `FIXME:` comments in changed files

---

## Architecture & Key Files

### 4-Layer Model

```
┌──────────────────────────────────────────────────────────────────┐
│  Layer 1 — API Contracts                                         │
│  kamper/api/src/commonMain/                                      │
│  Info, Config, InfoRepository, IWatcher, Performance,            │
│  PerformanceModule, Logger, Cleanable                            │
└────────────────────────┬─────────────────────────────────────────┘
                         │ depends on
┌────────────────────────▼─────────────────────────────────────────┐
│  Layer 2 — Engine (Orchestration)                                │
│  kamper/engine/src/                                              │
│  Engine (install/uninstall/start/stop/clear)                     │
│  Kamper object (platform actuals with lifecycle integration)     │
└────────────────────────┬─────────────────────────────────────────┘
                         │ depends on
┌────────────────────────▼─────────────────────────────────────────┐
│  Layer 3 — Modules (Implementations)                             │
│  kamper/modules/{cpu,fps,memory,network,issues,jank,gc,thermal}/ │
│  {Name}Info, {Name}Config, {Name}Performance, {Name}Watcher,     │
│  {Name}InfoRepository, {Name}InfoRepositoryImpl, {Name}InfoSource │
└────────────────────────┬─────────────────────────────────────────┘
                         │ depends on
┌────────────────────────▼─────────────────────────────────────────┐
│  Layer 4 — UI (Presentation)                                     │
│  kamper/ui/android/src/                                          │
│  KamperUi (expect/actual attach/detach)                          │
│  KamperPanel / KamperChip (Compose overlay)                      │
└──────────────────────────────────────────────────────────────────┘
```

**Dependency rule:** Lower-numbered layers never import from higher-numbered layers.

### Key Files

| File | Purpose |
|------|---------|
| `kamper/api/src/commonMain/kotlin/com/smellouk/kamper/api/Watcher.kt` | Core coroutine polling loop — the heart of all modules |
| `kamper/engine/src/commonMain/kotlin/com.smellouk.kamper/Engine.kt` | Module registry; install/uninstall/start/stop |
| `kamper/modules/cpu/` | Canonical reference module — most complete implementation |
| `build-logic/src/main/kotlin/KmpLibraryPlugin.kt` | Convention plugin applied by every KMP module |
| `settings.gradle.kts` | New modules must be registered here |
| `.planning/` | GSD planning artifacts (planning docs, phase plans, ADRs) — not shipped in the published library |
| `.claude/settings.json` | Claude Code allowlist for Gradle and shell commands |
| `.claude/skills/` | Project-specific Claude skills (`/kamper-new-module`, `/kamper-check`, `/kamper-module-review`, `/kamper-migrate-agp`, `/kamper-migrate-gradle`, `/kamper-migrate-kotlin`) |

### Relevant ADRs

The ADRs live in `.planning/codebase/adr/`:

- **ADR-001 — Plugin architecture (manual install pattern):** Modules are installed explicitly via `Kamper.install(XxxModule)` rather than auto-discovered. This gives consuming apps precise control over which metrics are active.
- **ADR-002 — KMP expect/actual pattern:** Platform-specific behavior is isolated behind `expect`/`actual` declarations. Common business logic lives in `commonMain`; platform adapters live in platform source sets.
- **ADR-003 — Listener pattern:** Metric data is delivered via push (`InfoListener<I>` callbacks) rather than pull. Listeners are registered on the `Engine` and receive data on `Dispatchers.Main`.
- **ADR-004 — No breaking changes (API freeze):** The public API (`Info`, `Config`, `Watcher`, `Performance`, `Engine`) is frozen for the v1.0 milestone. Additive changes only.
