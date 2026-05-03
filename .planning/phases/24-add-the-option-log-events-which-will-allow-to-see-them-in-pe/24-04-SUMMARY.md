---
phase: 24
plan: 04
subsystem: engine
tags: [engine, kmp, events, buffer, perfetto, thread-safety]
dependency_graph:
  requires:
    - 24-02 (UserEventInfo API data class)
    - 24-03 (EngineEventLock + engineCurrentTimeNs)
  provides:
    - Engine.logEvent / startEvent / endEvent / measureEvent (public event API)
    - Engine.dumpEvents / drainEvents (buffer inspection)
    - Engine.eventBuffer / eventBufferLock (internal buffer state)
    - EventRecord (internal data class)
    - EventToken (public class with internal constructor)
    - KamperConfig.eventsEnabled (D-06 guard)
  affects:
    - libs/engine (core event API)
    - plan 05+ (Wave 3 integrations consume KamperEvent(moduleName="event"))
    - plan 06+ (Perfetto export consumes drainEvents())
tech_stack:
  added:
    - ArrayDeque<EventRecord> (circular event buffer, capped at 1000)
    - EventToken (opaque start/end pairing handle with internal constructor)
    - EventRecord (internal timestampNs/name/durationNs triple)
  patterns:
    - FIFO eviction on buffer overflow (D-08)
    - Inline measureEvent for zero-allocation lambda wrapping (D-04)
    - Sentinel token (startNs=0) for disabled-events no-op path (D-06)
    - StringBuilder-based dumpEvents format (D-07)
key_files:
  created:
    - libs/engine/src/commonMain/kotlin/com.smellouk.kamper/EventRecord.kt
    - libs/engine/src/commonMain/kotlin/com.smellouk.kamper/EventToken.kt
  modified:
    - libs/engine/src/commonMain/kotlin/com.smellouk.kamper/KamperConfig.kt
    - libs/engine/src/commonMain/kotlin/com.smellouk.kamper/Engine.kt
    - libs/engine/src/commonTest/kotlin/com/smellouk/kamper/EngineEventTest.kt
decisions:
  - D-01: logEvent buffers EventRecord(timestampNs, name, null) and dispatches KamperEvent(moduleName="event")
  - D-02: startEvent returns EventToken(name, startNs) with internal constructor to prevent forgery
  - D-03: endEvent computes durationNs = engineCurrentTimeNs() - token.startNs
  - D-04: measureEvent is inline to avoid lambda allocation; try/finally guarantees endEvent runs
  - D-06: eventsEnabled=false returns immediately from all four event APIs; startEvent returns sentinel token (startNs=0)
  - D-07: dumpEvents formats via StringBuilder to logger with Journey-style begin/end markers and worst duration
  - D-08: eventBuffer capped at EVENT_BUFFER_CAPACITY=1000; oldest evicted on overflow
  - D-10: drainEvents() is a snapshot (toList()) that does NOT clear the buffer
  - D-11: all eventBuffer reads/writes guarded by eventBufferLock.withLock {}
  - D-12: Engine.clear() extended to call eventBufferLock.withLock { eventBuffer.clear() }
  - drainEvents visibility: made @PublishedApi internal (EventRecord is internal; public fun would expose internal type)
metrics:
  duration: ~4 minutes
  completed: 2026-05-02
  tasks_completed: 3
  tasks_total: 3
  files_created: 2
  files_modified: 3
---

# Phase 24 Plan 04: Engine Event API (logEvent/startEvent/endEvent/measureEvent) Summary

Engine custom-event API (D-01..D-12): logEvent/startEvent/endEvent/measureEvent with 1000-record circular buffer, eventsEnabled guard, dumpEvents formatting, drainEvents snapshot, and thread-safety via EngineEventLock — all 12 EngineEventTest assertions passing.

---

## Tasks Completed

| Task | Description | Commit |
|------|-------------|--------|
| 1 | KamperConfig.eventsEnabled + EventRecord + EventToken | 94a779f |
| 2 | Engine event API + buffer + clear() update | f9d212a |
| 3 | 12 EngineEventTest stubs replaced with real assertions | e84288f |

---

## What Was Built

### Task 1: Data Classes + KamperConfig.eventsEnabled

**KamperConfig.kt** — added `val eventsEnabled: Boolean = true` as second constructor param with matching Builder var. `DEFAULT` updated to `KamperConfig(logger = Logger.EMPTY, eventsEnabled = true)`. Source-compatible with existing callers using named params.

**EventRecord.kt** — new internal data class `EventRecord(timestampNs: Long, name: String, durationNs: Long?)`. Instant events have `durationNs = null`; duration slices have non-null durationNs. Aligns with Perfetto TYPE_INSTANT / TYPE_SLICE_BEGIN patterns.

**EventToken.kt** — new public class with `internal constructor(name: String, startNs: Long)`. Public so `Engine.startEvent` can return it to callers; internal constructor prevents token forgery (T-24-C-06).

### Task 2: Engine Event API

Six new public functions added to `open class Engine`:

| Function | Behavior |
|----------|---------|
| `logEvent(name)` | Buffer instant EventRecord + dispatch KamperEvent(moduleName="event") |
| `startEvent(name)` | Return EventToken with startNs; sentinel (startNs=0) when disabled |
| `endEvent(token)` | Compute durationNs, buffer duration EventRecord, dispatch |
| `measureEvent(name, block)` | Inline try/finally wrapper around startEvent/endEvent |
| `dumpEvents()` | Format buffer to logger: markers + per-event lines + worst duration |
| `drainEvents()` | Snapshot list without clearing (internal, @PublishedApi) |

Also added:
- `eventBufferLock: EngineEventLock` and `eventBuffer: ArrayDeque<EventRecord>` fields
- `bufferEvent()` and `dispatchEvent()` private helpers
- `EVENT_BUFFER_CAPACITY = 1000` and `NS_PER_MS = 1_000_000L` companion constants
- `eventBufferLock.withLock { eventBuffer.clear() }` appended to `clear()`

### Task 3: 12 EngineEventTest Tests

All 12 stub functions filled with real assertions. Two helper classes inlined:
- `RecordingLogger` — captures `log()` calls to a mutable list
- `RecordingIntegration` — captures `onEvent()` calls to a mutable list

Spin-wait technique used in endEvent/dumpEvents tests to ensure `engineCurrentTimeNs()` advances without thread sleep dependencies.

---

## Decisions Made

**drainEvents() visibility (deviation from plan template):** The plan template showed `fun drainEvents(): List<EventRecord>` as a public function, but `EventRecord` is `internal`. Kotlin rejects a `public fun` returning an `internal` type. Resolution: `@PublishedApi internal fun drainEvents()` — accessible from inline functions and from `commonTest` (same module), matching all downstream consumers (RecordingManager, tests).

---

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking Issue] react-native node_modules missing in worktree**

- **Found during:** Task 1 verification (first Gradle invocation)
- **Issue:** The worktree was freshly created and does not have `demos/react-native/node_modules/` — the `includeBuild("demos/react-native/android")` in `settings.gradle.kts` fails immediately since the RN Gradle plugin is not present
- **Fix:** Created a symlink `demos/react-native/node_modules -> /Users/smellouk/Developer/git/kamper/demos/react-native/node_modules` to reuse the main repo's installed modules. The symlink resolves the plugin resolution without npm install
- **Files modified:** `demos/react-native/node_modules` (untracked symlink — not committed)

**2. [Rule 1 - Bug] drainEvents() public visibility exposes internal EventRecord type**

- **Found during:** Task 2 compilation
- **Issue:** `public fun drainEvents(): List<EventRecord>` fails with "public function exposes its internal return type argument EventRecord"
- **Fix:** Changed to `@PublishedApi internal fun drainEvents()` — semantically equivalent for all consumers (same module, inline functions), aligns with Kotlin's `@PublishedApi` pattern used elsewhere in Engine
- **Files modified:** `libs/engine/src/commonMain/kotlin/com.smellouk.kamper/Engine.kt`

**3. [Rule 1 - Bug] Logger.EMPTY unresolved in test file**

- **Found during:** Task 3 test compilation
- **Issue:** `Logger.EMPTY` in the `engine()` factory function default parameter was unresolved — the extension property `val Logger.Companion.EMPTY` requires explicit import of `com.smellouk.kamper.api.EMPTY`
- **Fix:** Added `import com.smellouk.kamper.api.EMPTY` to `EngineEventTest.kt`
- **Files modified:** `libs/engine/src/commonTest/kotlin/com/smellouk/kamper/EngineEventTest.kt`

---

## Verification

- `./gradlew :libs:engine:compileKotlinJvm` — BUILD SUCCESSFUL
- `./gradlew :libs:engine:jvmTest --tests "com.smellouk.kamper.EngineEventTest"` — 12 tests passing, 0 failed
- `./gradlew :libs:engine:jvmTest` — 41 tests passing (all engine tests, no regressions)
- Pre-existing detekt issues in `libs/modules/thermal/` are out of scope (documented in Plan 03 summary, not caused by this plan)

---

## Self-Check: PASSED

- [x] `libs/engine/src/commonMain/kotlin/com.smellouk.kamper/EventRecord.kt` — FOUND
- [x] `libs/engine/src/commonMain/kotlin/com.smellouk.kamper/EventToken.kt` — FOUND
- [x] `libs/engine/src/commonMain/kotlin/com.smellouk.kamper/KamperConfig.kt` contains `eventsEnabled` — VERIFIED
- [x] `libs/engine/src/commonMain/kotlin/com.smellouk.kamper/Engine.kt` contains `logEvent`, `startEvent`, `endEvent`, `measureEvent`, `dumpEvents`, `drainEvents` — VERIFIED
- [x] `libs/engine/src/commonTest/kotlin/com/smellouk/kamper/EngineEventTest.kt` has 12 tests, 0 @Ignore — VERIFIED
- [x] Commit 94a779f exists — VERIFIED
- [x] Commit f9d212a exists — VERIFIED
- [x] Commit e84288f exists — VERIFIED
