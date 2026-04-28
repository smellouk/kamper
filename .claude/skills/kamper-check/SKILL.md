# kamper-check

## Purpose

Documents the right Gradle command for any check scope (single module fast, single module full, api, engine, all modules, detekt) and explicitly flags `connectedAndroidTest` as device-required so Claude never invokes it autonomously.

## When to Use

- After modifying any production code, before committing.
- When the user asks "run the tests", "run check", or "verify this builds" — pick the narrowest scope first.
- When the agent is unsure which Gradle task path is correct for a given change.
- Select scope based on what was modified:
  - Single module file then `./gradlew :kamper:modules:<name>:jvmTest`
  - `api` or `engine` layer then `./gradlew :kamper:api:test` or `./gradlew :kamper:engine:test`
  - Multiple modules or pre-commit then `./gradlew test`
  - Always run `./gradlew detekt` before committing

## Steps

1. **Identify the scope** — determine whether the change touches a single module, the api/engine layer, or multiple modules.

2. **Pick the fast path first** — run `:kamper:modules:<name>:jvmTest` for a single module. This runs ONLY the JVM unit tests and typically completes in 1-3 seconds per module. Prefer this for fast feedback during iterative development.

3. **Run the chosen command** from the project root: `./gradlew <task-path>`.

4. **If green and the change is non-trivial**, run the full unit test sweep: `./gradlew test`.

5. **Always run detekt before committing**: `./gradlew detekt`. Zero-tolerance policy — the build fails on any issue. Detekt runs with `autoCorrect: true` so it auto-fixes formatting violations.

6. **DO NOT run `./gradlew connectedAndroidTest`** — this task requires a connected Android device or running emulator. It will fail in autonomous Claude sessions where no device is attached. If the user explicitly requests instrumented tests, ask them to confirm a device is attached before proceeding.

7. **Report results** — include the test count, duration, and any failures. For Detekt, include the issue count and which files were flagged.

## Reference

### Command table (verified against .planning/codebase/TESTING.md and .claude/settings.json)

| Command | Scope | Device needed? | When to use |
|---------|-------|----------------|-------------|
| `./gradlew :kamper:modules:<name>:jvmTest` | Single module, JVM only | No | Fast feedback — primary path for Claude sessions |
| `./gradlew :kamper:modules:<name>:test` | Single module, all unit tests | No | Includes androidUnitTest via Robolectric/MockK |
| `./gradlew :kamper:api:test` | API contracts | No | After modifying the `api` layer |
| `./gradlew :kamper:engine:test` | Engine | No | After modifying the `engine` layer |
| `./gradlew test` | All modules, all unit tests | No | Full pre-commit sweep |
| `./gradlew detekt` | Static analysis | No | Before every commit (zero-tolerance) |
| `./gradlew connectedAndroidTest` | Instrumented on-device | **YES — emulator or physical device** | **NEVER in autonomous Claude sessions** |
| `./gradlew assembleXCFramework` | iOS/macOS XCFramework | No (compiles on host) | Only when publishing iOS artifacts |

### Why jvmTest is preferred

- No Android SDK or emulator required
- Runs in 1-3 seconds per module
- Covers all `commonTest` sources via the JVM target
- The jvmTest target is the fast, deterministic feedback loop for iterative work

### connectedAndroidTest — explicitly off-limits for Claude sessions

This task requires a connected Android device or running emulator. In a Claude Code autonomous session, no such device is attached, so this task will **FAIL**. Do not run it unless the user has explicitly confirmed a device is attached and you have been granted explicit permission.

### Settings allowlist

The following commands are pre-approved in `.claude/settings.json` (no permission prompt required):

- `./gradlew *:jvmTest:*`
- `./gradlew *:test:*`
- `./gradlew :kamper:api:test`
- `./gradlew :kamper:engine:test`
- `./gradlew test`
- `./gradlew detekt`

Anything else (e.g. `connectedAndroidTest`, `assembleXCFramework`, `publish`) will trigger a permission prompt — consider it a signal that you may be reaching for the wrong tool.

### Module names

The 8 existing modules (for task path substitution):
`cpu`, `fps`, `memory`, `network`, `issues`, `jank`, `gc`, `thermal`

Example: `./gradlew :kamper:modules:cpu:jvmTest`

### Detekt zero-tolerance policy

Detekt is configured with `maxIssues: 0` in `quality/code/detekt.yml`. The build fails on any single issue. Detekt runs with `autoCorrect: true` so it automatically fixes formatting violations. The following are always build errors:

- `TODO:` or `FIXME:` comment markers (`ForbiddenComment` rule)
- Re-throwing caught exceptions (`RethrowCaughtException` rule)
- Printing stack traces (`PrintStackTrace` rule)
- `var` where `val` suffices (`VarCouldBeVal` rule)
- Wildcard imports (`NoWildcardImports` rule)
- Unused imports (`NoUnusedImports` rule)
