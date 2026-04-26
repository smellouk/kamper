---
phase: 09-missing-features
reviewed: 2026-04-26T00:00:00Z
depth: standard
files_reviewed: 14
files_reviewed_list:
  - kamper/api/src/androidMain/kotlin/com/smellouk/kamper/api/PlatformTime.kt
  - kamper/api/src/commonMain/kotlin/com/smellouk/kamper/api/IWatcher.kt
  - kamper/api/src/commonMain/kotlin/com/smellouk/kamper/api/Info.kt
  - kamper/api/src/commonMain/kotlin/com/smellouk/kamper/api/Performance.kt
  - kamper/api/src/commonMain/kotlin/com/smellouk/kamper/api/PlatformTime.kt
  - kamper/api/src/commonMain/kotlin/com/smellouk/kamper/api/Watcher.kt
  - kamper/engine/src/commonMain/kotlin/com.smellouk.kamper/Engine.kt
  - kamper/engine/src/commonMain/kotlin/com.smellouk.kamper/ValidationInfo.kt
  - kamper/modules/cpu/src/androidMain/kotlin/com/smellouk/kamper/cpu/repository/CpuInfoRepositoryImpl.kt
  - kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/KamperConfigReceiver.kt
  - kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/ModuleLifecycleManager.kt
  - kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/KamperUiState.kt
  - kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/ActivityTab.kt
  - kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/PanelComponents.kt
findings:
  critical: 2
  warning: 5
  info: 4
  total: 11
status: issues_found
---

# Phase 09: Code Review Report

**Reviewed:** 2026-04-26T00:00:00Z
**Depth:** standard
**Files Reviewed:** 14
**Status:** issues_found

## Summary

This phase delivered Engine.validate() health-check (FEAT-03), CPU UNSUPPORTED detection (FEAT-01), a BroadcastReceiver remote control (FEAT-02), FPS/Jank/GC/Thermal UI modules, issue persistence, and shared Compose panel components.

The core API layer (IWatcher, Info, Watcher, PlatformTime implementations) is clean. The Engine validate() logic is correct. Two blockers are found: a silent operator-precedence bug in the CPU /proc/stat accessibility check that makes the fallback condition permanently incorrect, and a thread-safety gap on the FPS mutable state fields written from two threads. Five warnings cover a misleading ValidationInfo design, a state initialization problem in KamperUiState.EMPTY, unguarded applySettings access to state.value, debug Log.d statements left in production CPU code, and a deserialize-then-split fragility on newline separators. Four info items round out the findings.

---

## Critical Issues

### CR-01: Operator-precedence bug makes /proc/stat non-zero check always false

**File:** `kamper/modules/cpu/src/androidMain/kotlin/com/smellouk/kamper/cpu/repository/CpuInfoRepositoryImpl.kt:71`

**Issue:** In `ProcStatAccessibilityProvider.isAccessible()`, the expression

```kotlin
val result = parts.size >= 5 && parts.drop(1).any { it.toLongOrNull() ?: 0L > 0L }
```

is parsed by the Kotlin compiler as:

```kotlin
it.toLongOrNull() ?: (0L > 0L)
```

because `>` has higher precedence than `?:`. `0L > 0L` is always `false`. When `toLongOrNull()` returns `null` (i.e. the token is not a number), the elvis result is `false`. When `toLongOrNull()` returns a non-null `Long`, the expression evaluates to that `Long` value — but the `any {}` lambda must return `Boolean`, so the compiler actually coerces it through Kotlin's `Long.equals(Boolean)` overload resolution path, which will evaluate the Long as a truthy object reference (always non-null = true). The net effect is that the check is unreliable: it returns `true` for any non-null parse result regardless of value (zero or not), defeating the stated intent to require at least one positive value to confirm /proc/stat is readable and contains real data.

The intended expression — "at least one token parses as a positive Long" — requires parentheses:

```kotlin
val result = parts.size >= 5 && parts.drop(1).any { (it.toLongOrNull() ?: 0L) > 0L }
```

**Impact:** A device whose /proc/stat is readable but returns all zeros (e.g. certain emulators, locked-down builds) will be treated as having a working /proc/stat source, so the shell fallback is never engaged, and CPU samples will always show 0%. Conversely the opposite scenario (real data, misclassified) can also occur depending on compiler resolution. Either way the capability probe is broken.

**Fix:**
```kotlin
val result = parts.size >= 5 && parts.drop(1).any { (it.toLongOrNull() ?: 0L) > 0L }
```

---

### CR-02: FPS mutable fields written across two threads without synchronization

**File:** `kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/ModuleLifecycleManager.kt:177-219`

**Issue:** `fpsActive`, `fpsFrameCount`, and `fpsWindowStartNanos` are plain `var` fields:

```kotlin
private var fpsFrameCount = 0
private var fpsWindowStartNanos = 0L
private var fpsActive = false
```

`stopFps()` (lines 213-221) writes all three fields from whichever thread the caller is on (typically the settings coroutine or main thread outside the Choreographer loop). `startFps()` (lines 205-210) writes `fpsActive = true` and then posts to the main looper — but the write itself may be on a non-main thread. The `doFrame` callback (lines 182-202) runs on the Choreographer thread (main thread) and reads `fpsActive` to decide whether to re-register.

There is a race between `stopFps()` writing `fpsActive = false` (on thread A) and `doFrame` reading `fpsActive` (on the main thread). Without `@Volatile` or synchronization, the write may never be visible to `doFrame`, causing the callback to keep re-registering itself indefinitely after `stopFps()` is called. This is an infinite-execution bug that will drain battery and CPU even after the user disables FPS monitoring.

Additionally, `fpsFrameCount` and `fpsWindowStartNanos` are reset in `stopFps()` from a non-main thread while `doFrame` may be mid-flight on the main thread reading or writing the same fields — a classic unsynchronized shared-state bug.

**Fix:** Mark the three fields `@Volatile`, or confine all writes to them to the main thread (post the reset work to `Handler(Looper.getMainLooper())`):

```kotlin
@Volatile private var fpsFrameCount = 0
@Volatile private var fpsWindowStartNanos = 0L
@Volatile private var fpsActive = false
```

Or post the reset inside `stopFps()` to the main looper:

```kotlin
fun stopFps() {
    fpsActive = false
    Handler(Looper.getMainLooper()).post {
        Choreographer.getInstance().removeFrameCallback(fpsCallback)
        fpsFrameCount = 0
        fpsWindowStartNanos = 0L
    }
    state.update { it.copy(fpsPeak = 0, fpsLow = Int.MAX_VALUE, fpsHistory = emptyList()) }
}
```

The existing `removeFrameCallback` call is already inside a `Handler.post`, so the guard is partially there but the field resets are outside it.

---

## Warnings

### WR-01: ValidationInfo.EMPTY and ValidationInfo.INVALID are identical — misleading API

**File:** `kamper/engine/src/commonMain/kotlin/com.smellouk.kamper/ValidationInfo.kt:15-16`

**Issue:**

```kotlin
val EMPTY = ValidationInfo(emptyList())
val INVALID = ValidationInfo(emptyList())
```

Both `EMPTY` and `INVALID` are `data class` instances with identical contents. Because `ValidationInfo` is a `data class`, `EMPTY == INVALID` evaluates to `true`. Any code that checks `if (info == ValidationInfo.INVALID)` to detect an "invalid" state will also match `EMPTY` (an empty but valid result), producing incorrect logic for any future consumer that tries to distinguish the two. The conventional use of `INVALID` in the rest of the codebase (e.g. `CpuInfo.INVALID` uses sentinel values like `-1.0` that cannot be confused with real data) is not followed here.

**Fix:** Either remove `INVALID` entirely (since `Engine.validate()` never emits it as a no-op sentinel — it always emits real results), or give it a structurally distinct value. If the intent is to distinguish "the listener was never called" from "validate() returned empty", use a named constant with an explanatory comment and avoid the duplicate:

```kotlin
data class ValidationInfo(val problems: List<String>) : Info {
    companion object {
        val EMPTY = ValidationInfo(emptyList())
        // INVALID is intentionally the same shape; used only as a placeholder
        // where the Info type system requires a non-null sentinel.
        // Do NOT compare with == against EMPTY.
    }
}
```

Or simply remove `INVALID` since it is unused at every call site.

---

### WR-02: KamperUiState.EMPTY defaults engineRunning=true — state mismatch before initialise()

**File:** `kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/KamperUiState.kt:19, 41`

**Issue:** `KamperUiState.EMPTY` is the initial value of the `MutableStateFlow` created in `KamperUiRepository` (line 16). It has `engineRunning = true` as its default. However, the engine has not been started yet at construction time — `ModuleLifecycleManager.initialise()` is called in the `init {}` block of `KamperUiRepository`, but there is a window (however brief) where subscribers see `engineRunning = true` when the engine is not running. More practically, `applySettings()` reads `state.value.engineRunning` (line 396) to decide whether to restart the engine. If `applySettings()` is somehow called before `initialise()` completes (e.g. during tests or abnormal lifecycle), it will call `engine.stop()` and `engine.start()` on an engine that has never been started, which is benign today but brittle.

The deeper problem is that `EMPTY` misrepresents the pre-start state to any observer. The Compose UI will show "ENGINE RUNNING" before the first frame renders the real state.

**Fix:** Change the EMPTY constant's `engineRunning` to `false` (the engine is not running before `initialise()` is called) and set it to `true` only in `initialise()`:

```kotlin
val EMPTY = KamperUiState(
    ...
    engineRunning = false  // not started yet
)
```

`ModuleLifecycleManager.initialise()` already calls `state.update { it.copy(engineRunning = true) }`, so the state will be correct after init.

---

### WR-03: applySettings() reads state.value.engineRunning outside atomic update — TOCTOU

**File:** `kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/ModuleLifecycleManager.kt:396-399`

**Issue:**

```kotlin
if (state.value.engineRunning) {
    engine.stop()
    engine.start()
}
```

`state.value.engineRunning` is read as a snapshot at one point, then `engine.stop()` and `engine.start()` are called. Between the read and the actual engine call, another coroutine (e.g. `startEngine()` or `stopEngine()`) could change the engine state. If `stopEngine()` runs concurrently between these two lines, the engine will be stopped by both callers and started once, or a `start()` call will happen on a logically-stopped engine. The `MutableStateFlow.value` read is not an atomic lock on the engine state.

**Fix:** Guard the engine restart with the actual engine state, not the StateFlow snapshot. Since `ModuleLifecycleManager` has sole ownership of the `engine` instance, tracking engine-running state in a separate `@Volatile` boolean owned by the manager (not the UI state) would be safer:

```kotlin
@Volatile private var engineRunning = false

fun startEngine() {
    engine.start()
    engineRunning = true
    state.update { it.copy(engineRunning = true) }
}

fun stopEngine() {
    engine.stop()
    engineRunning = false
    state.update { it.copy(engineRunning = false) }
}

// In applySettings():
if (engineRunning) {
    engine.stop()
    engine.start()
}
```

---

### WR-04: Android Log.d calls left unconditionally in production CPU code

**File:** `kamper/modules/cpu/src/androidMain/kotlin/com/smellouk/kamper/cpu/repository/CpuInfoRepositoryImpl.kt:30, 37, 44, 54, 69, 72, 75`

**Issue:** Seven `Log.d("Kamper/CPU", ...)` calls are present with no debug-build guard. These fire on every call to `getInfo()` — which is invoked on the CPU polling interval (typically every 1–2 seconds). `Log.d` output appears in logcat in production builds (it is stripped only when ProGuard/R8 rules explicitly remove it), and the messages include the CPU source selection logic, DTO content, and mapped info values. This is a quality defect and causes unnecessary logcat noise in user builds. All other Kamper modules route through the `Logger` abstraction so the host app controls output; the CPU repository bypasses this contract.

**Fix:** Replace all `Log.d` calls with the `logger` abstraction (injectable `Logger`) already used by `Watcher` and `Performance`. If `CpuInfoRepositoryImpl` does not currently receive a `Logger`, inject one in the constructor. As a minimum, guard with `BuildConfig.DEBUG`:

```kotlin
if (BuildConfig.DEBUG) Log.d("Kamper/CPU", "source=$source ...")
```

---

### WR-05: Issue serialization newline delimiter collides with per-record separator

**File:** `kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/ModuleLifecycleManager.kt:164` and `kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/IssueSerializer.kt:9-19`

**Issue:** Issues are persisted by joining serialized records with `"\n"` (newline):

```kotlin
preferencesStore.putString(PREF_ISSUES, issuesList.joinToString("\n") { it.serialize() })
```

`Issue.serialize()` uses `` (ASCII Unit Separator) as the intra-record field delimiter and `` (Record Separator) for details map entries — both correct. However, the `pctEncode()` function only encodes characters with `code < 0x20`, which includes `\n` (code = 0x0A, decimal 10). So newlines inside field values (e.g. a stack trace containing `\n`) **will be encoded** as `%0A` by `pctEncode()`.

The risk is on the **outer delimiter**: if any field contains a literal `\n` that somehow bypasses `pctEncode()` (e.g. future refactoring adds a non-encoded field), a single serialized record would span multiple lines and `loadPersistedIssues()` would parse it as two records, causing a corrupt deserialization. Currently the code is safe because all string fields pass through `pctEncode()`, but the design is fragile: a human-readable but control-character-based inter-record separator (`` or ``) would be more robust than `\n`.

**Fix:** Use a control-character inter-record separator that `pctEncode()` will also encode if it ever appears in field data, making the format self-consistent. Replace `"\n"` with `""` (Group Separator, code 0x1D < 0x20, so it is encoded by `pctEncode()`):

```kotlin
// serialize
preferencesStore.putString(PREF_ISSUES, issuesList.joinToString("") { it.serialize() })

// deserialize
return raw.split('').mapNotNull { it.deserializeIssue() }.toMutableList()
```

---

## Info

### IN-01: derivedStateOf wrappers in ActivityTab provide no recomposition benefit

**File:** `kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/ActivityTab.kt:27-44`

**Issue:** The composable reads `val s by state.collectAsState()` and then derives every field via `remember { derivedStateOf { s.xxx } }`. `s` is already a `State<KamperUiState>`, so reading `s.cpuPercent` inside a composable causes recomposition whenever `s` changes regardless of which field changed. The `derivedStateOf` lambda captures `s` directly (not `s.cpuPercent`), so it invalidates whenever `s` changes — at that point the derived value is recomputed but the recomposition scope is the `ActivityTab` itself, not each individual `MetricCard`. The pattern does not eliminate recompositions of `ActivityTab`; it only adds overhead from the derived state objects. The comment claiming "each MetricCard only recomposes when ITS value changes" is inaccurate.

To achieve per-card isolation, each `MetricCard` would need to receive a `State<Float>` (not a `Float`) and read it inside its own scope, or the derived states would need to be passed into individual child composables as lambda parameters.

**Fix:** Either accept that `ActivityTab` recomposes whenever the parent state changes (which is fine for a debug panel) and remove the `derivedStateOf` wrappers, or refactor `MetricCard` to accept `() -> Float` lambdas and read inside them to defer recomposition. The current code is not incorrect but the comment creates a false sense of optimization.

---

### IN-02: ValidationInfo.EMPTY and ValidationInfo.INVALID are unused dead code

**File:** `kamper/engine/src/commonMain/kotlin/com.smellouk.kamper/ValidationInfo.kt:15-16`

**Issue:** Neither `ValidationInfo.EMPTY` nor `ValidationInfo.INVALID` is referenced anywhere in the codebase (confirmed by grep). The `EMPTY` convention is used as a return value when no problems exist, but `Engine.validate()` returns `emptyList<String>()` wrapped in a new `ValidationInfo(problems)` — it does not use the companion object. Dead companion values add API surface with no benefit.

**Fix:** Remove both `EMPTY` and `INVALID` from `ValidationInfo`, or at minimum add a `@Suppress("unused")` annotation if they are reserved for future external use.

---

### IN-03: KamperConfigReceiver action string is not exported as a constant

**File:** `kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/KamperConfigReceiver.kt:26`

**Issue:** The intent action `com.smellouk.kamper.CONFIGURE` is documented in a KDoc comment but is not declared as a public constant in the class or its companion. Callers building the intent in-process must hard-code the string or copy it from documentation. The test file (`KamperConfigReceiverTest.kt:64`) defines a private `ACTION` constant — a sign that the value is being duplicated.

**Fix:**
```kotlin
class KamperConfigReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_CONFIGURE = "com.smellouk.kamper.CONFIGURE"
        private const val EXTRA_ENABLED = "enabled"
    }
    ...
}
```

---

### IN-04: Memory fraction in ActivityTab hardcodes 512 MB ceiling

**File:** `kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/ActivityTab.kt:77`

**Issue:**

```kotlin
fraction = (memoryUsedMb / 512f).coerceIn(0f, 1f),
```

The progress bar fills to 100% at 512 MB. Modern Android devices (and especially tablets) commonly have 4–16 GB RAM. An app using 1 GB of heap will show a full bar at 512 MB, appearing to be "at maximum" when it has consumed only 25% of available memory. This is a magic number with no basis in any device specification.

**Fix:** Either expose this ceiling as a `KamperUiSettings` field, derive it from `ActivityManager.MemoryInfo.totalMem`, or document the magic number with a comment explaining the design choice:

```kotlin
// Memory bar ceiling: 512 MB is a reasonable upper-bound for heap use
// on phones; tablets or processes with large native heaps may saturate early.
private const val MEMORY_BAR_CEILING_MB = 512f
```

---

_Reviewed: 2026-04-26T00:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
