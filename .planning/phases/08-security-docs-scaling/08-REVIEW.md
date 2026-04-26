---
phase: 08-security-docs-scaling
reviewed: 2026-04-26T13:55:35Z
depth: standard
files_reviewed: 54
files_reviewed_list:
  - CAPACITY.md
  - README.md
  - SECURITY.md
  - kamper/engine/src/androidMain/kotlin/com/smellouk/kamper/Kamper.kt
  - kamper/engine/src/commonMain/kotlin/com.smellouk.kamper/KamperConfig.kt
  - kamper/engine/src/commonTest/kotlin/com/smellouk/kamper/KamperConfigBuilderTest.kt
  - kamper/engine/src/jsMain/kotlin/com/smellouk/kamper/Kamper.kt
  - kamper/engine/src/jvmMain/kotlin/com/smellouk/kamper/Kamper.kt
  - kamper/engine/src/wasmJsMain/kotlin/com/smellouk/kamper/Kamper.kt
  - kamper/modules/cpu/src/androidMain/kotlin/com/smellouk/kamper/cpu/Module.kt
  - kamper/modules/cpu/src/commonMain/kotlin/com/smellouk/kamper/cpu/CpuConfig.kt
  - kamper/modules/cpu/src/commonTest/kotlin/com/smellouk/kamper/cpu/CpuConfigBuilderTest.kt
  - kamper/modules/cpu/src/iosMain/kotlin/com/smellouk/kamper/cpu/Module.kt
  - kamper/modules/cpu/src/jsMain/kotlin/com/smellouk/kamper/cpu/Module.kt
  - kamper/modules/cpu/src/jvmMain/kotlin/com/smellouk/kamper/cpu/Module.kt
  - kamper/modules/cpu/src/macosMain/kotlin/com/smellouk/kamper/cpu/Module.kt
  - kamper/modules/cpu/src/tvosMain/kotlin/com/smellouk/kamper/cpu/Module.kt
  - kamper/modules/cpu/src/wasmJsMain/kotlin/com/smellouk/kamper/cpu/Module.kt
  - kamper/modules/fps/src/androidMain/kotlin/com/smellouk/kamper/fps/Module.kt
  - kamper/modules/fps/src/commonMain/kotlin/com/smellouk/kamper/fps/FpsConfig.kt
  - kamper/modules/fps/src/commonTest/kotlin/com/smellouk/kamper/fps/FpsConfigBuilderTest.kt
  - kamper/modules/fps/src/iosMain/kotlin/com/smellouk/kamper/fps/Module.kt
  - kamper/modules/fps/src/jsMain/kotlin/com/smellouk/kamper/fps/Module.kt
  - kamper/modules/fps/src/jvmMain/kotlin/com/smellouk/kamper/fps/Module.kt
  - kamper/modules/fps/src/macosMain/kotlin/com/smellouk/kamper/fps/Module.kt
  - kamper/modules/fps/src/tvosMain/kotlin/com/smellouk/kamper/fps/Module.kt
  - kamper/modules/fps/src/wasmJsMain/kotlin/com/smellouk/kamper/fps/Module.kt
  - kamper/modules/gc/src/androidMain/kotlin/com/smellouk/kamper/gc/Module.kt
  - kamper/modules/gc/src/commonMain/kotlin/com/smellouk/kamper/gc/GcConfig.kt
  - kamper/modules/issues/src/androidMain/kotlin/com/smellouk/kamper/issues/Module.kt
  - kamper/modules/issues/src/commonMain/kotlin/com/smellouk/kamper/issues/IssuesConfig.kt
  - kamper/modules/issues/src/iosMain/kotlin/com/smellouk/kamper/issues/Module.kt
  - kamper/modules/issues/src/jsMain/kotlin/com/smellouk/kamper/issues/Module.kt
  - kamper/modules/issues/src/jvmMain/kotlin/com/smellouk/kamper/issues/Module.kt
  - kamper/modules/issues/src/macosMain/kotlin/com/smellouk/kamper/issues/Module.kt
  - kamper/modules/issues/src/tvosMain/kotlin/com/smellouk/kamper/issues/Module.kt
  - kamper/modules/issues/src/wasmJsMain/kotlin/com/smellouk/kamper/issues/Module.kt
  - kamper/modules/jank/src/androidMain/kotlin/com/smellouk/kamper/jank/Module.kt
  - kamper/modules/jank/src/commonMain/kotlin/com/smellouk/kamper/jank/JankConfig.kt
  - kamper/modules/jank/src/jvmMain/kotlin/com/smellouk/kamper/jank/Module.kt
  - kamper/modules/network/src/androidMain/kotlin/com/smellouk/kamper/network/Module.kt
  - kamper/modules/network/src/commonMain/kotlin/com/smellouk/kamper/network/NetworkConfig.kt
  - kamper/modules/network/src/commonTest/kotlin/com/smellouk/kamper/network/NetworkConfigBuilderTest.kt
  - kamper/modules/network/src/iosMain/kotlin/com/smellouk/kamper/network/Module.kt
  - kamper/modules/network/src/jsMain/kotlin/com/smellouk/kamper/network/Module.kt
  - kamper/modules/network/src/jvmMain/kotlin/com/smellouk/kamper/network/Module.kt
  - kamper/modules/network/src/macosMain/kotlin/com/smellouk/kamper/network/Module.kt
  - kamper/modules/network/src/tvosMain/kotlin/com/smellouk/kamper/network/Module.kt
  - kamper/modules/network/src/wasmJsMain/kotlin/com/smellouk/kamper/network/Module.kt
  - kamper/modules/thermal/src/androidMain/kotlin/com/smellouk/kamper/thermal/Module.kt
  - kamper/modules/thermal/src/commonMain/kotlin/com/smellouk/kamper/thermal/ThermalConfig.kt
  - kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/RecordingManager.kt
findings:
  critical: 2
  warning: 4
  info: 0
  total: 6
status: issues_found
---

# Phase 08 (Wave 2): Code Review Report

**Reviewed:** 2026-04-26T13:55:35Z
**Depth:** standard
**Files Reviewed:** 54
**Status:** issues_found

## Summary

This wave delivered: SECURITY.md and CAPACITY.md at repo root, KDoc added to all public Config data
classes, `object Builder` singletons converted to `class Builder` across all 47+ call sites,
`RecordingManager.recordingBuffer` synchronized under `bufferLock`, and README fixes for the
IssuesModule quick-start and `IssueInfo` listener examples.

The Builder-class conversion is complete — no `object Builder` references remain anywhere in the
codebase. The KDoc additions are accurate. SECURITY.md is correct and matches the actual code
(provider name, authority, and behavior verified). CAPACITY.md claims about IssuesWatcher
(FIFO eviction, totalDropped reset on `startWatching` and `clean()`, listener delivery unaffected
by the cap) all check out against the implementation.

Two blockers were found. The README still contains a Jank module example that will not compile
because it references properties that do not exist on `JankConfig.Builder`. The
`RecordingManager.startRecording()` sets `_isRecording.value = true` outside the synchronized
block, creating a race window against concurrent `clearRecording()` that leaves the buffer in a
non-empty state after a clear. Four warnings cover a misleading `CrashConfig.persistToDisk`
default, a silent no-op parameter in the iOS `IssuesModule` builder, a redundant unused import in
`iosMain` CpuModule, and inconsistent `@KamperDslMarker` annotation across native platform targets.

---

## Critical Issues

### CR-01: README JankModule example uses nonexistent `JankConfig.Builder` properties — will not compile

**File:** `README.md:231-234`

**Issue:** The documented `JankModule { ... }` DSL block assigns `frameThresholdMs` and
`consecutiveFrameThreshold`, neither of which exists on `JankConfig.Builder`. The only
configurable property on `JankConfig.Builder` (besides the standard `isEnabled`, `intervalInMs`,
`logger`) is `jankThresholdMs`.

```kotlin
// README — does not compile:
install(JankModule {
    frameThresholdMs          = 32   // DOES NOT EXIST on JankConfig.Builder
    consecutiveFrameThreshold = 3    // DOES NOT EXIST on JankConfig.Builder
})
```

Any developer copying this snippet will get an unresolved-reference compile error. The properties
`frameThresholdMs` and `consecutiveFramesThreshold` belong to `DroppedFramesConfig` inside
`IssuesModule`, not to `JankModule`.

**Fix:**
```kotlin
install(JankModule {
    jankThresholdMs = 16   // flag frames slower than 16 ms (one frame at 60 Hz)
})
```

If the intent was to document frame-drop thresholds for the Issues module, move the example there.
Note also that `JankInfo.droppedFrames` (referenced at line 238) does exist and is valid.

---

### CR-02: `RecordingManager.clearRecording()` sets `_isRecording = false` outside `bufferLock` — leaves buffer non-empty after clear

**File:** `kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/RecordingManager.kt:64-70`

**Issue:** `clearRecording()` clears the buffer inside `bufferLock`, then sets `_isRecording.value
= false` **after** releasing the lock:

```kotlin
fun clearRecording() {
    synchronized(bufferLock) {
        recordingBuffer.clear()
        _recordingSampleCount.value = 0
    }
    _isRecording.value = false   // outside lock — window of inconsistency here
}
```

Between releasing `bufferLock` and writing `_isRecording = false`, a concurrent `record()` thread
can execute the entire fast path: it checks `_isRecording.value` (still `true`), acquires the lock
(now available), appends a sample to the cleared buffer, and increments `_recordingSampleCount` to
1. After `clearRecording()` returns, the buffer is not empty, `_recordingSampleCount` is 1, and
`isRecording` is `false` — a permanently inconsistent state. Subsequent `startRecording()` will
clear the stale sample, so this does not corrupt a future session, but any caller that reads
`recordingSampleCount` immediately after `clearRecording()` will observe a non-zero count for a
stopped recording.

The symmetric issue exists in `startRecording()`: `_isRecording.value = true` is set after
releasing `bufferLock`, creating a brief window where the buffer is cleared but `_isRecording` is
still `false`. This means the very first `record()` call of a new session can be silently skipped.
The impact is one missed sample, which is minor for a ring buffer context, but the ordering
contract is violated.

**Fix:** Move the `_isRecording` state change inside the synchronized block for both methods:

```kotlin
fun startRecording() {
    synchronized(bufferLock) {
        recordingBuffer.clear()
        _recordingSampleCount.value = 0
        _isRecording.value = true   // set inside lock so record() sees consistent state
    }
}

fun stopRecording() {
    _isRecording.value = false   // fine outside lock — just a flag; record() may fire one last time
}

fun clearRecording() {
    synchronized(bufferLock) {
        recordingBuffer.clear()
        _recordingSampleCount.value = 0
        _isRecording.value = false   // set inside lock — prevents stale record() writes post-clear
    }
}
```

Note that `stopRecording()` alone does not need to be inside the lock because it only sets a flag
that causes `record()` to return early; a trailing write from a concurrent `record()` is acceptable
there. The problem is specifically `clearRecording()`, where a post-clear write corrupts the count.

---

## Warnings

### WR-01: `CrashConfig.persistToDisk` defaults to `true` but is documented as a no-op — misleads integrators

**File:** `kamper/modules/issues/src/commonMain/kotlin/com/smellouk/kamper/issues/IssuesConfig.kt:143-151`

**Issue:** `CrashConfig` declares `val persistToDisk: Boolean = true`. The KDoc comment on the
same line says: "Reserved for a future release. Currently has no effect — crash reports are not
written to disk." Searching every actual `CrashDetector` implementation confirms that `persistToDisk`
is never read: the field is built into the config object but no code path ever checks it.

The problem is the default value of `true`. A developer who reads the field name and default value
(without reading the KDoc) will assume crash data is persisted to disk by default. The doc-to-code
mismatch becomes a trust-erosion issue when the developer discovers disk writes never happen.
Additionally, SECURITY.md (line 52-55) states "Issue history is similarly persisted" — while this
refers to SharedPreferences in the UI layer, not `CrashConfig.persistToDisk`, the juxtaposition
amplifies the confusion.

**Fix:** Change the default to `false` to match the no-op reality:
```kotlin
data class CrashConfig(
    // ...
    // Reserved for a future release. Currently has no effect.
    val persistToDisk: Boolean = false,
    // ...
)
```
Also update `CrashConfig.Builder` default accordingly. If/when the feature is implemented, flip the
default back to `true` in the same commit that adds the disk-write logic.

---

### WR-02: iOS `IssuesModule` builder accepts `slowStart: SlowStartConfig` but silently ignores it

**File:** `kamper/modules/issues/src/iosMain/kotlin/com/smellouk/kamper/issues/Module.kt:15-37`

**Issue:** The iOS `IssuesModule` factory function signature is:

```kotlin
fun IssuesModule(
    anr: AnrConfig = AnrConfig(),
    slowStart: SlowStartConfig = SlowStartConfig(),   // accepted but never used
    builder: IssuesConfig.Builder.() -> Unit = {}
): PerformanceModule<IssuesConfig, IssueInfo>
```

`slowStart` is passed to the function but `buildDetectors(config, anr)` never receives it and no
`SlowStartDetector` is constructed. A caller that configures `slowStart = SlowStartConfig(isEnabled
= true, coldStartThresholdMs = 1_500L)` on iOS receives no slow-start monitoring — the
configuration is silently dropped. The JVM `IssuesModule` does not expose a `slowStart` parameter
at all (correct for that platform), making the iOS API asymmetric: it accepts a parameter it cannot
honor.

**Fix:** Remove `slowStart` from the iOS signature:
```kotlin
fun IssuesModule(
    anr: AnrConfig = AnrConfig(),
    // slowStart removed — SlowStartDetector is not available on iOS
    builder: IssuesConfig.Builder.() -> Unit = {}
): PerformanceModule<IssuesConfig, IssueInfo>
```

If slow-start detection is planned for iOS in a future phase, add the parameter back when the
detector is implemented. Accepting a parameter that has no effect is a silent API contract violation.

---

### WR-03: `iosMain` `CpuModule.kt` imports `KamperDslMarker` but never uses it

**File:** `kamper/modules/cpu/src/iosMain/kotlin/com/smellouk/kamper/cpu/Module.kt:3`

**Issue:**
```kotlin
import com.smellouk.kamper.api.KamperDslMarker  // imported but not applied
```

The `CpuModule` builder function on iOS is not annotated with `@KamperDslMarker`, making the
import unused. This is likely a copy-paste artifact from the Android/JVM source where the
annotation is applied. The unused import does not cause a runtime error but it makes the IDE warn
and it obscures the intentional annotation gap.

**Fix:** Remove the unused import:
```kotlin
// Remove this line from iosMain Module.kt:
import com.smellouk.kamper.api.KamperDslMarker
```

---

### WR-04: `@KamperDslMarker` annotation missing from native platform `CpuModule`, `FpsModule`, and `NetworkModule` builder functions — inconsistent DSL safety

**File:** `kamper/modules/cpu/src/iosMain/kotlin/com/smellouk/kamper/cpu/Module.kt:19`
**File:** `kamper/modules/cpu/src/macosMain/kotlin/com/smellouk/kamper/cpu/Module.kt:20`
**File:** `kamper/modules/cpu/src/tvosMain/kotlin/com/smellouk/kamper/cpu/Module.kt:19`
**File:** `kamper/modules/fps/src/iosMain/kotlin/com/smellouk/kamper/fps/Module.kt:19`
**File:** `kamper/modules/fps/src/macosMain/kotlin/com/smellouk/kamper/fps/Module.kt:20`
**File:** `kamper/modules/fps/src/tvosMain/kotlin/com/smellouk/kamper/fps/Module.kt:19`
**File:** `kamper/modules/network/src/iosMain/kotlin/com/smellouk/kamper/network/Module.kt:19`
**File:** `kamper/modules/network/src/macosMain/kotlin/com/smellouk/kamper/network/Module.kt:20`
**File:** `kamper/modules/network/src/tvosMain/kotlin/com/smellouk/kamper/network/Module.kt:19`

**Issue:** The `CpuModule`, `FpsModule`, and `NetworkModule` builder functions on Android, JVM, JS,
and WasmJS carry `@KamperDslMarker` to prevent accidental lambda nesting in the Kamper DSL. The
identical builder functions on iOS, macOS, and tvOS do not. A developer using the multiplatform DSL
on a native target loses the scope-safety enforcement that prevents calling outer DSL receivers
from inside an inner lambda. The inconsistency can mask DSL misuse silently on native targets that
compiles cleanly on Android.

**Fix:** Add `@KamperDslMarker` to each affected builder function:
```kotlin
// iosMain, macosMain, tvosMain — for CpuModule, FpsModule, NetworkModule
@KamperDslMarker
@Suppress("FunctionNaming")
fun CpuModule(
    builder: CpuConfig.Builder.() -> Unit
): PerformanceModule<CpuConfig, CpuInfo> = ...
```

---

_Reviewed: 2026-04-26T13:55:35Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
