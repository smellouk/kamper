# Phase 22: Manual Testing All Demo Platforms - Context

**Gathered:** 2026-04-29
**Status:** Ready for planning

<domain>
## Phase Boundary

Validate that all 7 Kamper demo apps build and pass a full smoke test after the Phase 21 monorepo rename (`kamper/` → `libs/`, `:kamper:*` → `:libs:*` Gradle project paths). Each demo is tested on its own platform; failures block plan completion until fixed inline. This is a post-rename validation phase, not a feature development phase.

**Platforms in scope (7 total):**
- `demos/android/` — Android demo app
- `demos/compose/` — Compose Multiplatform desktop demo
- `demos/jvm/` — JVM CLI demo
- `demos/ios/` — iOS demo
- `demos/macos/` — macOS demo
- `demos/web/` — JS/WASM web demo
- `demos/react-native/` — React Native demo (composite build, own android/ + ios/ subdirs)

</domain>

<decisions>
## Implementation Decisions

### Platform Scope
- **D-01:** All 7 demo platforms are in scope. No platform is skipped or deferred, including those requiring special tooling (Xcode for iOS/macOS, browser for web, RN toolchain for react-native).

### Pass/Fail Criteria
- **D-02:** A demo passes when it completes a **full smoke test**: app starts without crashing, all installed modules report live values (not INVALID), the UI overlay renders (where applicable), and no ANRs or crashes occur during a 30-second run.
- **D-03:** Build compiling is necessary but not sufficient — a demo that builds but immediately crashes or shows no metrics fails the smoke test.

### Issue Handling
- **D-04:** If a demo fails the smoke test, **plan execution for that platform is blocked** until the issue is diagnosed and fixed. No plan advances to "done" while its demo fails. Fixes are applied inline within the same plan.
- **D-05:** The most likely failure class is stale `:kamper:*` path references surviving from before Phase 21 (in build.gradle.kts files, settings includes, or source code imports). Executor should grep for these before running builds.

### Plan Structure
- **D-06:** One plan per platform = 7 plans. Order: jvm → android → compose → ios → macos → web → react-native (easiest/fastest toolchain first, most complex last).
- **D-07:** Each plan follows the same structure: (1) grep for stale path references, (2) Claude runs the build command, (3) user launches the app and reports observations, (4) Claude interprets results and diagnoses any failures, (5) apply fixes if needed, (6) re-run build + smoke test to confirm pass.

### Execution Assistance Model
- **D-08:** Claude executes all Gradle/npm/xcodebuild commands and reports their output. The user manually launches the app or emulator and reports back what they observe (log lines, UI state, errors). Claude interprets the feedback and issues the next command or diagnoses the failure.
- **D-09:** Each plan must include explicit "what to observe" instructions so the user knows what a passing smoke test looks like for that platform.

### Claude's Discretion
- Exact grep patterns to detect stale `:kamper:*` references — any systematic approach is fine.
- Whether to add a final summary plan (plan 08) documenting all test results — include if useful.
- Order of module testing within each demo (CPU first as canonical reference module is fine).
- Whether to use `./gradlew run` or platform-specific launch commands per demo — choose whatever produces the fastest feedback loop.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Demo Apps (primary targets)
- `demos/android/build.gradle.kts` — Android demo Gradle config; check for stale `:kamper:*` references
- `demos/compose/build.gradle.kts` — Compose Multiplatform demo Gradle config
- `demos/jvm/build.gradle.kts` — JVM CLI demo Gradle config
- `demos/ios/build.gradle.kts` — iOS demo Gradle config (if present; may use Xcode project)
- `demos/macos/build.gradle.kts` — macOS demo Gradle config
- `demos/web/build.gradle.kts` — JS/WASM web demo Gradle config
- `demos/react-native/android/` — RN composite build Android root; own settings/build files

### Phase 21 Rename Context (what changed)
- `.planning/phases/21-monorepo-structure-and-clean-up-e-g-renaming-kamper-to-libs-/21-CONTEXT.md` — full list of what was renamed (D-01 through D-09); use to understand what stale references look like
- `settings.gradle.kts` — root module registration; should now use `:libs:*` paths; demos may depend on these includes

### Build System Reference
- `CLAUDE.md` §"Build & Test" — Gradle commands, module path prefix (`:libs:`), pre-approved commands
- `.planning/codebase/STRUCTURE.md` — current directory layout after Phase 21

### Testing Constraints
- `CLAUDE.md` §WARNING — `connectedAndroidTest` requires a connected device; do NOT run autonomously for Android instrumented tests. Use `jvmTest` for fast path. The Android demo smoke test requires a running emulator or device (human-in-the-loop per D-08).

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `libs/modules/cpu/` — canonical reference module; if CPU metrics show live values in a demo, the engine and module install pattern is working correctly
- `./gradlew :demos:jvm:run` (or equivalent) — JVM demo is the fastest smoke test; confirm this task exists in `demos/jvm/build.gradle.kts`

### Established Patterns
- **Post-rename path pattern:** All library Gradle projects now live under `:libs:` (e.g., `:libs:modules:cpu`, `:libs:api`, `:libs:engine`). Any `project(":kamper:*")` in a demo build file is a stale reference that will cause a build failure.
- **Demo apps are NOT published** — they depend on library modules via local `project()` references, not Maven coordinates. Path correctness is critical.
- **React Native composite build:** `demos/react-native/android/` is an `includeBuild` composite; its own `settings.gradle.kts` may have separate path declarations to check.

### Integration Points
- `settings.gradle.kts` (root) — includes `includeBuild("demos/react-native/android")` per Phase 21; verify this is still correct post-rename
- `demos/android/build.gradle.kts` — depends on `:libs:engine` and specific module projects; these must match the root settings include list

</code_context>

<specifics>
## Specific Ideas

- The most common failure mode is a `project(":kamper:something")` reference that was missed in the Phase 21 sweep. Run `grep -r ":kamper:" demos/` as the first action in every plan.
- JVM demo is the fastest to validate (no emulator, no Xcode). Start with `demos/jvm/` to confirm the basic build infrastructure is healthy before tackling platform-specific demos.
- For iOS/macOS: if the demos use KMP `./gradlew linkDebugFrameworkIosSimulatorArm64` or similar, Claude can run those. The user then opens Xcode or the built binary.
- React Native: check both `demos/react-native/metro.config.js` and `demos/react-native/android/` settings for any Kamper path references.

</specifics>

<deferred>
## Deferred Ideas

- None — discussion stayed within phase scope.

</deferred>

---

*Phase: 22-manual-testing-all-demo-platforms-one-by-one*
*Context gathered: 2026-04-29*
