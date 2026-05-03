---
phase: 24-add-the-option-log-events-which-will-allow-to-see-them-in-pe
plan: 05
subsystem: integrations
tags: [sentry, kotlin-multiplatform, breadcrumbs, user-events, integration-adapters]

# Dependency graph
requires:
  - phase: 24-02
    provides: UserEventInfo data class in libs/api
  - phase: 24-04
    provides: SentryIntegrationModule baseline with existing onEvent dispatch pattern

provides:
  - SentryConfig.forwardEvents: Boolean = true (D-25)
  - SentryIntegrationModule "event" branch dispatching UserEventInfo to Sentry breadcrumbs (D-26)
  - 4 new test functions verifying event branch routing

affects:
  - 24-06 (FirebaseConfig/FirebaseIntegrationModule — same forwardEvents pattern)
  - 24-07 (OtelConfig/OtelIntegrationModule — same forwardEvents pattern)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "forwardEvents: Boolean = true as opt-in-by-default config field pattern for integration adapters"
    - "handleUserEvent() private helper with as? UserEventInfo ?: return defensive cast"
    - "Inline breadcrumb construction with category/message/level for custom events"

key-files:
  created: []
  modified:
    - libs/integrations/sentry/src/commonMain/kotlin/com/smellouk/kamper/sentry/SentryConfig.kt
    - libs/integrations/sentry/src/commonMain/kotlin/com/smellouk/kamper/sentry/SentryIntegrationModule.kt
    - libs/integrations/sentry/src/commonTest/kotlin/com/smellouk/kamper/sentry/SentryConfigBuilderTest.kt
    - libs/integrations/sentry/src/commonTest/kotlin/com/smellouk/kamper/sentry/SentryIntegrationModuleTest.kt

key-decisions:
  - "DEFAULT_FORWARD_EVENTS = true (const val) — events opt-in by default, matching D-25"
  - "handleUserEvent() as a private helper (not reusing handleBreadcrumb) — message format with duration suffix differs from the metric toString() format"
  - "Inline breadcrumb construction in handleUserEvent avoids overloading handleBreadcrumb with incompatible message logic"

patterns-established:
  - "forwardEvents pattern: const val DEFAULT_FORWARD_EVENTS + Builder var + toString() entry (mirrors forwardFps)"
  - "Defensive as? UserEventInfo ?: return cast before accessing typed fields (T-24-D-02)"

requirements-completed: []

# Metrics
duration: 7min
completed: 2026-05-02
---

# Phase 24 Plan 05: Sentry Event Integration Summary

**SentryConfig gains `forwardEvents=true` opt-in flag (D-25) and SentryIntegrationModule gains "event" branch forwarding UserEventInfo as Sentry breadcrumbs with category "kamper.event" (D-26)**

## Performance

- **Duration:** ~7 min
- **Started:** 2026-05-02T18:13:18Z
- **Completed:** 2026-05-02T18:20:30Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments

- Added `forwardEvents: Boolean` to `SentryConfig` data class with `DEFAULT_FORWARD_EVENTS = true` (const val), Builder var, and toString() entry
- Implemented `"event"` branch in `SentryIntegrationModule.onEvent` that casts `event.info as? UserEventInfo`, guards on `config.forwardEvents`, and calls `Sentry.addBreadcrumb` with `category = "kamper.event"`, `level = SentryLevel.INFO`, and duration-aware message format
- Added 3 new `SentryConfigBuilderTest` cases (default=true, opt-out=false, toString contains forwardEvents)
- Added 4 new `SentryIntegrationModuleTest` cases (instant event, duration suffix, skip when disabled, INVALID guard)
- Total test count in sentry integration: 16 (was 9)

## Task Commits

Each task was committed atomically:

1. **Task 1: Add forwardEvents to SentryConfig** - `be9ee2a` (feat)
2. **Task 2: Add event branch to SentryIntegrationModule + extend tests** - `6e568a3` (feat)

**Plan metadata:** committed with SUMMARY.md

## Files Created/Modified

- `libs/integrations/sentry/src/commonMain/kotlin/com/smellouk/kamper/sentry/SentryConfig.kt` — Added `forwardEvents: Boolean` (6th field), `DEFAULT_FORWARD_EVENTS = true` const val, `Builder.forwardEvents`, `build()` pass-through, `toString()` entry
- `libs/integrations/sentry/src/commonMain/kotlin/com/smellouk/kamper/sentry/SentryIntegrationModule.kt` — Added `import UserEventInfo`, `"event"` branch in `when`, new `handleUserEvent()` private method
- `libs/integrations/sentry/src/commonTest/kotlin/com/smellouk/kamper/sentry/SentryConfigBuilderTest.kt` — Added 3 tests for `forwardEvents` default, opt-out, and `toString` inclusion
- `libs/integrations/sentry/src/commonTest/kotlin/com/smellouk/kamper/sentry/SentryIntegrationModuleTest.kt` — Added 4 `event_branch_*` tests matching plan verbatim function names

## Decisions Made

- Used `const val DEFAULT_FORWARD_EVENTS: Boolean = true` (not `val`) — Detekt `MayBeConst` rule flagged it; fixed inline
- Implemented `handleUserEvent()` as a standalone private helper rather than reusing `handleBreadcrumb()`. The existing helper uses `event.info.toString()` as the message body. Custom events need a different format (`info.name` or `"info.name (durationMs ms)"`), so a separate helper keeps the logic clear and avoids parameter explosion on `handleBreadcrumb`
- Breadcrumb message for duration events: `"${info.name} (${info.durationMs} ms)"` — matches D-26 spec exactly

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed MayBeConst detekt violation on DEFAULT_FORWARD_EVENTS**
- **Found during:** Task 1 (SentryConfig implementation)
- **Issue:** Added `public val DEFAULT_FORWARD_EVENTS: Boolean = true` — Detekt flagged it as `MayBeConst` (same rule that would apply to `DEFAULT_FORWARD_ISSUES`/`DEFAULT_FORWARD_FPS`, but those pre-existed before this session's detekt run)
- **Fix:** Changed to `public const val DEFAULT_FORWARD_EVENTS: Boolean = true`
- **Files modified:** `SentryConfig.kt`
- **Verification:** `./gradlew detekt` shows no sentry-related errors after fix
- **Committed in:** `be9ee2a` (Task 1 commit)

---

**Total deviations:** 1 auto-fixed (Rule 1 - Detekt violation)
**Impact on plan:** Trivial const qualifier fix; no behavior change.

## Issues Encountered

- iOS/macOS native test link tasks (`linkDebugTestIosSimulatorArm64`, `linkDebugTestMacosArm64`, `linkDebugTestMacosX64`) fail due to pre-existing Sentry Cocoa pod/linker issue in the test environment — confirmed pre-existing by running `build` task before our changes. JVM tests and compilation all pass. Out of scope per deviation scope boundary rule.
- `demos/react-native/node_modules` missing in worktree — symlinked from main repo (`/Users/smellouk/Developer/git/kamper/demos/react-native/node_modules`) to unblock Gradle configuration.

## Known Stubs

None — all production code is fully wired.

## Threat Flags

None — no new network endpoints or trust boundaries introduced. The `handleUserEvent()` method accesses the existing Sentry SDK call path already present in the module (T-24-D-02 mitigated by `as? UserEventInfo ?: return`).

## Next Phase Readiness

- Sentry integration is complete for custom events. Breadcrumbs with `category="kamper.event"` will appear in Sentry issue context alongside CPU/memory/FPS breadcrumbs.
- Plans 24-06 and 24-07 can apply the same `forwardEvents` field pattern to Firebase and OpenTelemetry configs/modules respectively.

## Self-Check: PASSED

- FOUND: `.planning/phases/24-.../24-05-SUMMARY.md`
- FOUND: commit `be9ee2a` (Task 1 — forwardEvents in SentryConfig)
- FOUND: commit `6e568a3` (Task 2 — event branch in SentryIntegrationModule)

---
*Phase: 24-add-the-option-log-events-which-will-allow-to-see-them-in-pe*
*Completed: 2026-05-02*
