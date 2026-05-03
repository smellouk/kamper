---
phase: 24
plan: 02
subsystem: api
tags: [tdd, user-event-info, event-logging, wave-1]
dependency_graph:
  requires: [24-01]
  provides: [UserEventInfo, UserEventInfoTest]
  affects: [libs/api]
tech_stack:
  added: []
  patterns: [tdd-red-green, data-class-invalid-sentinel, explicit-api]
key_files:
  created:
    - libs/api/src/commonMain/kotlin/com/smellouk/kamper/api/UserEventInfo.kt
    - libs/api/src/commonTest/kotlin/com/smellouk/kamper/api/UserEventInfoTest.kt
  modified: []
decisions:
  - "UserEventInfo placed in libs/api (not libs/engine) to avoid circular dependency for integration modules"
  - "INVALID sentinel is UserEventInfo(\"\", null) per D-14"
  - "public visibility modifiers explicit per api package convention (matches KamperEvent pattern)"
  - "durationMs: Long? — null for instant events, non-null for duration events (D-16/D-17)"
metrics:
  duration: 4m
  completed_date: "2026-05-02T17:56:00Z"
  tasks_completed: 2
  tasks_total: 2
  files_changed: 2
---

# Phase 24 Plan 02: UserEventInfo Data Class Summary

`UserEventInfo(name: String, durationMs: Long?) : Info` with INVALID sentinel in `libs/api`, plus two passing jvmTest assertions replacing Wave 0 @Ignore stubs.

## What Was Built

### Task 1: UserEventInfo data class (TDD GREEN)

**File:** `libs/api/src/commonMain/kotlin/com/smellouk/kamper/api/UserEventInfo.kt`

- `public data class UserEventInfo(val name: String, val durationMs: Long?) : Info`
- `public companion object` with `public val INVALID: UserEventInfo = UserEventInfo("", null)`
- Lives in `com.smellouk.kamper.api` package — same package as `Info`, no import needed
- Explicit `public` visibility modifiers matching `KamperEvent` convention
- KDoc documents D-13/D-14 design decisions and `durationMs` semantics (D-16/D-17)

**TDD flow:**
1. RED: Created `UserEventInfoTest.kt` with two `@Ignore`'d stubs that compile but skip
2. GREEN: Created `UserEventInfo.kt` — stubs can now reference the class
3. REFACTOR: Replaced stubs with real assertions (Task 2)

### Task 2: UserEventInfoTest real assertions

**File:** `libs/api/src/commonTest/kotlin/com/smellouk/kamper/api/UserEventInfoTest.kt`

- Removed `@Ignore` from both tests
- `userEventInfo_INVALID_equals_constructor_with_empty_name_and_null_duration`: verifies equality, name == "", durationMs == null
- `userEventInfo_implements_Info_marker_interface`: verifies instant + duration event instances are `Info` subtypes, durationMs round-trips correctly
- Both tests PASS: `./gradlew :libs:api:jvmTest` — 21 passing

## Decisions Made

1. **Explicit public modifiers:** Following `KamperEvent.kt` pattern — `public data class`, `public companion object`, `public val INVALID`
2. **No UNSUPPORTED variant:** Unlike `CpuInfo`, custom events have no platform restriction — all 7 platforms support them
3. **api package, no import needed:** `Info` is in the same package (`com.smellouk.kamper.api`), so no import is required for `UserEventInfo : Info`
4. **TDD RED via @Ignore stubs:** Test file created first with stubs that compile but are ignored, matching the Wave 0 pattern from Plan 01

## Commits

| Task | Description | Hash |
|------|-------------|------|
| Task 1 RED | UserEventInfoTest.kt @Ignore stubs | 0ae7d08 |
| Task 1 GREEN | UserEventInfo.kt production code | 6f7722f |
| Task 2 | Replace stubs with real assertions | dc7caad |

## Deviations from Plan

### Pre-existing Detekt Violations (Out of Scope)

The `./gradlew detekt` top-level task fails due to pre-existing violations in `libs/modules/thermal/` (7 issues: `NoMultipleSpaces` in `jvmMain/ThermalInfoRepositoryImpl.kt`, `MagicNumber` in `iosMain` and `tvosMain`). These are:
- Documented as deferred in Plan 01 SUMMARY
- Not caused by any files in this plan
- Not present in `UserEventInfo.kt` (zero detekt violations in new file)

The per-module `./gradlew :libs:api:detekt` task does not exist; the violations are scoped to the thermal module. Logged to deferred items.

**Note:** `demos/react-native/node_modules` symlink was created in the worktree (pointing to main repo's node_modules) as a Rule 3 fix to unblock Gradle task graph configuration. This is a worktree-environment-only change and is not tracked in git.

## Verification

- `./gradlew :libs:api:compileKotlinJvm` — BUILD SUCCESSFUL
- `./gradlew :libs:api:jvmTest --tests "com.smellouk.kamper.api.UserEventInfoTest"` — 2 passing
- `./gradlew :libs:api:jvmTest` — 21 passing (no regressions)
- UserEventInfo.kt: zero detekt violations (thermal module violations are pre-existing, out of scope)

## Known Stubs

None — both tests are real, passing assertions. No intentional stubs remain in files created or modified by this plan.

## Threat Flags

No new security-relevant surface introduced. `UserEventInfo` is a pure data class with no I/O, no network access, no file access, and no auth paths. T-24-A-01 (name string logging) and T-24-A-02 (API drift) are mitigated per the plan's threat model (documented in `data class` shape + ADR-004).

## Self-Check: PASSED

- [x] `libs/api/src/commonMain/kotlin/com/smellouk/kamper/api/UserEventInfo.kt` exists
- [x] `libs/api/src/commonTest/kotlin/com/smellouk/kamper/api/UserEventInfoTest.kt` exists
- [x] Commits 0ae7d08, 6f7722f, dc7caad exist in git log
- [x] `./gradlew :libs:api:jvmTest` — BUILD SUCCESSFUL, 21 passing
- [x] No @Ignore annotations remain in UserEventInfoTest.kt
- [x] INVALID sentinel: `UserEventInfo("", null) == UserEventInfo.INVALID` verified by test
