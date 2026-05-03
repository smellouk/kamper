# Contributing to Konitor

Thank you for your interest in contributing to Konitor! This guide covers everything you need
to set up, build, follow the module patterns, and submit a conformant pull request.

---

## Prerequisites

- **JDK 17** is required. No other JDK version is supported.
- No Android device is required for the default development workflow.
  Device-attached tests are a separate step covered below.
- Smoke test on a fresh clone:
  ```
  ./gradlew :libs:api:test :libs:engine:test
  ```
  Both tasks must pass before you begin work.

---

## Clone and build

```bash
git clone https://github.com/smellouk/konitor.git
cd konitor
```

Run the smoke test to confirm the build is healthy:

```bash
./gradlew :libs:api:test :libs:engine:test
```

Both tasks compile and test without a connected device.

---

## Development workflow

Prefer the fast JVM path for iterative development — it runs in seconds and requires no device.

| Command | Scope | When to use |
|---------|-------|-------------|
| `./gradlew :libs:modules:<name>:jvmTest` | Single module, JVM only | Fast feedback (primary path) |
| `./gradlew :libs:modules:<name>:test` | Single module, all unit tests | Includes androidUnitTest |
| `./gradlew :libs:api:test` | API contracts | After modifying the api layer |
| `./gradlew :libs:engine:test` | Engine | After modifying the engine layer |
| `./gradlew test` | All modules, all unit tests | Full pre-commit sweep |
| `./gradlew detekt` | Static analysis (zero-tolerance) | Before every commit |

**Static analysis:** `detekt` runs with `maxIssues: 0` — any new issue fails the build.
It also runs with `autoCorrect: true`, so formatting violations are fixed in-place on check.
The configuration lives in `quality/code/detekt.yml`.

Forbidden patterns that Detekt enforces:

- `TODO:` or `FIXME:` comments in production code
- `println(` calls in production code
- Re-throwing a caught exception without wrapping

---

## Device-requiring tests

> **`./gradlew connectedAndroidTest` requires a connected Android device or running emulator.**
> It will fail in any environment without one. Confirm a device is attached before running.

Do not run `connectedAndroidTest` in a CI job or automation context that lacks a device.
All standard unit tests (including Android unit tests via Robolectric) run without a device
using the commands in the Development workflow table above.

---

## Module pattern

Every Konitor performance module follows a strict 4-class structure. Deviating from this
structure is a convention violation. The canonical reference module is `libs/modules/cpu/` —
when in doubt, mirror what CPU does.

### 4-class structure

| Class | Role | Visibility |
|-------|------|------------|
| `{Name}Info` | Data class implementing `Info`; the metric payload emitted to listeners | `public` |
| `{Name}Config` | Data class implementing `Config`; holds `isEnabled`, `intervalInMs`, `logger` | `public` |
| `{Name}Watcher` | Coroutine polling loop extending `Watcher<{Name}Info>` | `internal` |
| `{Name}Performance` | Lifecycle container extending `Performance<{Name}Config, IWatcher<{Name}Info>, {Name}Info>` | `internal` |

### INVALID sentinel

Every `Info` subclass must declare a companion `INVALID` constant. Listeners are never invoked
with an invalid reading — callers check for `INVALID` before processing data.

```kotlin
data class {Name}Info(val value: Double) : Info {
    companion object {
        val INVALID = {Name}Info(-1.0)
    }
}
```

### Builder / DEFAULT pattern

Every `Config` must expose a `@KonitorDslMarker` Builder object and a `DEFAULT` companion val.

```kotlin
data class {Name}Config(
    override val isEnabled: Boolean,
    override val intervalInMs: Long,
    val logger: Logger
) : Config {
    @KonitorDslMarker
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

### Safety rule

All platform-specific calls inside `{Name}InfoSource` must be wrapped in `try/catch`.
Exceptions are logged and absorbed — Konitor must never propagate an exception to the host
application.

```kotlin
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

---

## Commit format

Konitor uses [Conventional Commits](https://www.conventionalcommits.org/).

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

**Allowed scopes:** `cpu`, `fps`, `memory`, `network`, `issues`, `jank`, `gc`, `thermal`,
`engine`, `api`, `ui`, `build`, `deps`, `phase<N>`

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

- Imperative mood, lowercase after the colon, no trailing period
- No emojis anywhere in commit messages
- No issue-tracker footers (e.g. `resolves` or `closes` followed by a hash and issue number)
- No issue-ID prefix on the subject line (no `#123` at the start)

---

## Branch strategy

- `main` is the integration branch. All pull requests target `main` directly.
- There is no `develop` branch.
- Pull requests are squash-merged. The squashed commit message must conform to the
  Conventional Commits format above.

---

## Pull request process

Before opening a pull request, verify all items in this checklist:

- [ ] `./gradlew detekt` passes (zero issues)
- [ ] `./gradlew :libs:modules:<name>:jvmTest` passes for every touched module
- [ ] No `TODO:` or `FIXME:` comments in changed files
- [ ] Public API changes are additive only (no removals or signature breaks)

The same checklist appears in the PR template — fill it out honestly. Reviewers will verify
each item.

---

## Code review

Konitor is maintained by a single maintainer. Please be patient — reviews typically happen
within a few days.

Reviewers check:

- All items in the PR checklist above
- Module-pattern adherence (4-class structure, INVALID sentinel, Builder/DEFAULT, safety rule)
- Commit message format (Conventional Commits, no emojis, imperative mood)

To self-review your module changes before submitting, compare your implementation against
`libs/modules/cpu/` — it is the most complete and well-tested module in the repository.
