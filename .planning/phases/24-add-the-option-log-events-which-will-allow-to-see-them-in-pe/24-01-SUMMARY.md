---
phase: 24
plan: 01
subsystem: engine, api, ui
tags: [tdd, test-scaffolds, event-logging, wave-0]
dependency_graph:
  requires: []
  provides: [EngineEventTest, UserEventInfoTest, PerfettoExporterEventTest]
  affects: [libs/engine, libs/api, libs/ui/kmm]
tech_stack:
  added: []
  patterns: [wave-0-ignore-stubs, tdd-red-phase]
key_files:
  created:
    - libs/engine/src/commonTest/kotlin/com/smellouk/kamper/EngineEventTest.kt
    - libs/api/src/commonTest/kotlin/com/smellouk/kamper/api/UserEventInfoTest.kt
    - libs/ui/kmm/src/androidUnitTest/kotlin/com/smellouk/kamper/ui/PerfettoExporterEventTest.kt
  modified: []
decisions:
  - "@Ignore stubs used instead of failing bodies to allow compilation before production code lands"
  - "KDoc comment wording avoids literal @Ignore text to keep grep-count acceptance criteria accurate"
metrics:
  duration: 4m
  completed_date: "2026-05-02T17:48:50Z"
  tasks_completed: 3
  tasks_total: 3
  files_changed: 3
---

# Phase 24 Plan 01: Wave 0 Test Scaffolds Summary

Wave 0 TDD scaffolds — 19 @Ignore'd @Test stubs across three files covering D-01..D-21 of the event logging API.

## What Was Built

Three new Kotlin test source files were created as Wave 0 scaffolds for the Phase 24 event logging API. Each file contains comment-only `@Test @Ignore` stubs that compile successfully but are skipped at runtime until the corresponding production code lands in Plans 02–08.

### Files Created

**1. libs/engine/src/commonTest/kotlin/com/smellouk/kamper/EngineEventTest.kt**
- 12 `@Test @Ignore` stubs covering Engine event API decisions D-01..D-12
- Tests: logEvent buffering, startEvent/endEvent token lifecycle, measureEvent block execution and exception safety, eventsEnabled guard (noop behavior), dumpEvents formatting, event buffer 1000-entry cap, drainEvents snapshot semantics, clear behavior
- Target: `./gradlew :libs:engine:compileTestKotlinJvm`

**2. libs/api/src/commonTest/kotlin/com/smellouk/kamper/api/UserEventInfoTest.kt**
- 2 `@Test @Ignore` stubs covering D-13 (Info interface) and D-14 (INVALID sentinel)
- Tests: INVALID equals empty-name/null-duration constructor, implements Info marker interface
- Target: `./gradlew :libs:api:compileTestKotlinJvm`

**3. libs/ui/kmm/src/androidUnitTest/kotlin/com/smellouk/kamper/ui/PerfettoExporterEventTest.kt**
- 5 `@Test @Ignore` stubs covering D-16..D-21 (Perfetto event encoding)
- Tests: events track descriptor without counter, TYPE_INSTANT packet, TYPE_SLICE_BEGIN/END pair, TrackEvent name on BEGIN/INSTANT only, empty events list backward compatibility
- Target: `./gradlew :libs:ui:kmm:compileDebugUnitTestKotlinAndroid`

## Decisions Made

1. **@Ignore stubs over failing bodies:** Each test body contains only a decision-reference comment (`// covers D-XX`) rather than `TODO()` calls or production code references. This ensures the files compile before Plans 02–08 deliver the production implementations.

2. **KDoc avoids literal @Ignore text:** The file header comment references "the Ignore annotation" rather than the literal `@Ignore` token to prevent inflating the `grep -c '@Ignore'` acceptance criteria count.

## Commits

| Task | Description | Hash |
|------|-------------|------|
| Task 1 | EngineEventTest.kt — 12 stubs (D-01..D-12) | 70e0442 |
| Task 2 | UserEventInfoTest.kt — 2 stubs (D-13, D-14) | 8df69f2 |
| Task 3 | PerfettoExporterEventTest.kt — 5 stubs (D-19, D-21) | 5a717eb |

## Deviations from Plan

### Deferred Items (Out-of-Scope Pre-Existing Issues)

**Pre-existing detekt violations in libs/modules/thermal/ (not caused by this plan)**
- `NoMultipleSpaces` in `thermal/src/jvmMain/ThermalInfoRepositoryImpl.kt` (5 occurrences)
- `MagicNumber` in `thermal/src/iosMain/ThermalInfoRepositoryImpl.kt` and `thermal/src/tvosMain/ThermalInfoRepositoryImpl.kt` (2 occurrences)
- These violations exist in files not modified by this plan and are pre-existing from Phase 23 work
- None of the three new test files introduced any detekt violations
- Logged to deferred items for Phase 24 cleanup

## Verification

- `./gradlew :libs:engine:compileTestKotlinJvm` — BUILD SUCCESSFUL
- `./gradlew :libs:api:compileTestKotlinJvm` — BUILD SUCCESSFUL
- `./gradlew :libs:ui:kmm:compileDebugUnitTestKotlinAndroid` — BUILD SUCCESSFUL
- New test files: zero detekt violations (verified via full detekt run, violations found are pre-existing in thermal module)

## Known Stubs

All 19 tests are intentional stubs. They will be un-ignored as their corresponding production implementations land:

| File | Tests | Unlocked by |
|------|-------|-------------|
| EngineEventTest.kt | 12 stubs (D-01..D-12) | Plan 04 (Engine event API) |
| UserEventInfoTest.kt | 2 stubs (D-13, D-14) | Plan 02 (UserEventInfo data class) |
| PerfettoExporterEventTest.kt | 5 stubs (D-16..D-21) | Plan 08 (Perfetto event encoding) |

## Self-Check: PASSED

- [x] EngineEventTest.kt exists at correct path
- [x] UserEventInfoTest.kt exists at correct path
- [x] PerfettoExporterEventTest.kt exists at correct path
- [x] Commits 70e0442, 8df69f2, 5a717eb exist in git log
- [x] All three compile tasks pass with BUILD SUCCESSFUL
