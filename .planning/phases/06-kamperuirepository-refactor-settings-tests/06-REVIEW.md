---
phase: 06-kamperuirepository-refactor-settings-tests
reviewed: 2026-04-26T00:00:00Z
depth: standard
files_reviewed: 14
files_reviewed_list:
  - kamper/ui/android/build.gradle.kts
  - kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/AndroidPreferencesStore.kt
  - kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/KamperUiRepository.kt
  - kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/ModuleLifecycleManager.kt
  - kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/RecordingManager.kt
  - kamper/ui/android/src/androidUnitTest/kotlin/com/smellouk/kamper/ui/RecordingManagerTest.kt
  - kamper/ui/android/src/appleMain/kotlin/com/smellouk/kamper/ui/ApplePreferencesStore.kt
  - kamper/ui/android/src/appleMain/kotlin/com/smellouk/kamper/ui/KamperUiRepository.kt
  - kamper/ui/android/src/appleMain/kotlin/com/smellouk/kamper/ui/ModuleLifecycleManager.kt
  - kamper/ui/android/src/appleMain/kotlin/com/smellouk/kamper/ui/RecordingManager.kt
  - kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/PreferencesStore.kt
  - kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/SettingsRepository.kt
  - kamper/ui/android/src/commonTest/kotlin/com/smellouk/kamper/ui/FakePreferencesStore.kt
  - kamper/ui/android/src/commonTest/kotlin/com/smellouk/kamper/ui/SettingsRepositoryTest.kt
findings:
  critical: 3
  warning: 5
  info: 3
  total: 11
status: issues_found
---

# Phase 06: Code Review Report

**Reviewed:** 2026-04-26T00:00:00Z
**Depth:** standard
**Files Reviewed:** 14
**Status:** issues_found

## Summary

This phase introduces `SettingsRepository` (a persistence layer backed by `PreferencesStore`), refactors `KamperUiRepository` to delegate settings I/O through it, and adds unit tests for both `SettingsRepository` and `RecordingManager`. The two platform-specific `ModuleLifecycleManager` implementations are also included.

Three critical defects were found: a persistent-key mismatch between platforms that silently drops all saved issues on Android after any cross-platform migration; a listener-leak that fires stale callbacks after a module is reinstalled; and the Apple `RecordingManager` being a no-op stub that makes `maxRecordingSamples` and all recording-related APIs silently non-functional on iOS. Five further warnings cover correctness and robustness risks.

---

## Critical Issues

### CR-01: Android PREF_ISSUES key differs from Apple — persisted issues are silently lost

**File:** `kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/ModuleLifecycleManager.kt:42`

**Issue:** The Android implementation uses the key `"issues_list"` while the Apple implementation uses `"kamper_issues_list"` (appleMain `ModuleLifecycleManager.kt:41`). Because `PreferencesStore` is platform-specific, this means:

1. Any migration or shared-store scenario silently reads from the wrong key and returns an empty string, discarding all persisted issues.
2. More immediately: the constant names are identical (`PREF_ISSUES`) but resolve to different string values, making the divergence invisible during code review unless both files are open side-by-side.
3. The correct, namespaced form is `"kamper_issues_list"`. The Android value should be updated to match.

**Fix:**
```kotlin
// androidMain/ModuleLifecycleManager.kt line 42 — change to match Apple
private const val PREF_ISSUES = "kamper_issues_list"
```

---

### CR-02: Info listeners are added on every reinstall but never removed — duplicate callbacks accumulate

**File:** `kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/ModuleLifecycleManager.kt:204-310`

**Issue:** `Engine.uninstall()` calls `mapListeners.remove(I::class)`, which removes the entire listener bucket for that info type. Immediately after, each `install*()` helper calls `engine.addInfoListener(listener)`, which tries to add the listener to `mapListeners[I::class]`. However, the `Engine.addInfoListener` implementation only succeeds when the key **already exists** in `mapListeners`:

```kotlin
// Engine.kt:57-62
fun <I : Info> addInfoListener(listener: InfoListener<I>): Engine {
    mapListeners[I::class]?.add(listener)       // ← add to existing bucket
        ?: logger.log("Can't add listener ...")  // ← silently fails if absent
    return this
}
```

Meanwhile `Engine.install()` creates the bucket (`mapListeners[I::class] = mutableListOf()`) **before** `initialize()` is called. Because the install helpers call `engine.install(mod)` first and then `engine.addInfoListener(listener)`, the ordering is:

1. `install()` creates empty bucket
2. `initialize()` captures the listener list by reference
3. `addInfoListener()` adds the listener to the bucket

This works on the **first** install. On **reinstall** (config change path in `applySettings`), `uninstall()` removes the bucket, then `install()` creates a new empty bucket that `initialize()` captures, and then `addInfoListener()` adds the listener — so far correct.

The actual regression is that `engine.clear()` (called from `ModuleLifecycleManager.clear()`) wipes `mapListeners` entirely, but `clear()` does **not** null the module references (`cpuModule`, etc. are set to null only in `clear()`). On a subsequent `initialise()` call after `clear()` the engine is a freshly constructed instance (it is constructed at class init time, not per `initialise()`), which means all is fine there.

However, the genuinely broken path is the **config-change reinstall in `applySettings`**: after `uninstallCpu()` removes the bucket and `installCpu()` re-creates it, `engine.start()` is unconditionally called at line 372 `if (state.value.engineRunning) engine.start()`. If the engine was already running (modules are actively polling), `engine.start()` is called a second time on the same `performanceList`, restarting already-running modules without stopping them first. The module's `start()` is called twice, which depending on the module implementation can register duplicate OS callbacks or duplicate timers.

**Fix:**
```kotlin
// In applySettings, replace the final engine.start() call with:
if (state.value.engineRunning) {
    engine.stop()   // stop currently running instances cleanly
    engine.start()  // restart with the updated module set
}
```

---

### CR-03: Apple RecordingManager is a no-op stub — maxRecordingSamples and all recording APIs are silently non-functional on iOS

**File:** `kamper/ui/android/src/appleMain/kotlin/com/smellouk/kamper/ui/RecordingManager.kt:7-18`

**Issue:** The Apple `RecordingManager` implements every method as `Unit` and `exportTrace()` always returns `ByteArray(0)`. The `maxRecordingSamples` property exists in `KamperUiRepository` (appleMain) and is passed in from the call site (`KamperUi.kt:76`), but the `RecordingManager` ignores it entirely. All of `startRecording()`, `stopRecording()`, `clearRecording()`, `record()`, and `exportTrace()` are dead. No samples are ever collected on iOS. `isRecording` is always `false`. This is not an intentional "not yet implemented" state — the public API surface (`KamperUiRepository`) exposes `startRecording()` / `exportTrace()` as if they work.

This also means the `recordingManager` field in the Apple `KamperUiRepository` does not receive `maxRecordingSamples`, so even if recording were wired up, the cap would be ignored.

**Fix:** Implement a real `RecordingManager` for Apple using a common implementation, or at minimum extract the recording logic to `commonMain` and share it:

```kotlin
// appleMain/RecordingManager.kt — replace stub with a real implementation
internal class RecordingManager(
    private val maxSamples: Int = 4_200
) {
    private val recordingBuffer = ArrayDeque<RecordedSample>()
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    private val _recordingSampleCount = MutableStateFlow(0)
    val recordingSampleCount: StateFlow<Int> = _recordingSampleCount.asStateFlow()

    private fun nowNs(): Long = platform.Foundation.NSDate.timeIntervalSinceReferenceDate
        .let { (it * 1_000_000_000).toLong() }

    fun record(trackId: Int, value: Double) {
        if (!_isRecording.value) return
        if (recordingBuffer.size >= maxSamples) recordingBuffer.removeFirst()
        recordingBuffer.addLast(RecordedSample(nowNs(), trackId, value))
        _recordingSampleCount.value = recordingBuffer.size
    }

    fun startRecording() {
        recordingBuffer.clear()
        _recordingSampleCount.value = 0
        _isRecording.value = true
    }

    fun stopRecording() { _isRecording.value = false }
    fun exportTrace(): ByteArray = PerfettoExporter.export(recordingBuffer.toList())
    fun clearRecording() {
        recordingBuffer.clear()
        _recordingSampleCount.value = 0
        _isRecording.value = false
    }
}
```

Alternatively, move `RecordingManager` to `commonMain` using `expect/actual` only for the clock source.

---

## Warnings

### WR-01: issuesList is mutated from multiple threads without synchronization (Android)

**File:** `kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/ModuleLifecycleManager.kt:99-107`

**Issue:** `issuesList` is a plain `mutableListOf()`. The `issuesListener` lambda is invoked by the engine's data-delivery thread (not the main thread). `clearIssues()` can be called from any thread the caller chooses. Both mutate `issuesList` concurrently without any lock or `@GuardedBy` guarantee. A `ConcurrentModificationException` or lost update is possible under normal usage.

**Fix:**
```kotlin
// Replace mutableListOf() with a thread-safe structure, or guard with a mutex:
private val issuesMutex = Mutex()

private val issuesListener: InfoListener<IssueInfo> = listener@{ info ->
    if (info == IssueInfo.INVALID) return@listener
    // Requires a coroutine scope; alternatively use synchronized(issuesList):
    synchronized(issuesList) {
        issuesList.add(0, info.issue)
        if (issuesList.size > MAX_ISSUES) issuesList.removeAt(issuesList.size - 1)
        saveIssues()
    }
    state.update { s ->
        s.copy(issues = issuesList.toList(), unreadIssueCount = s.unreadIssueCount + 1)
    }
}

fun clearIssues() {
    synchronized(issuesList) {
        issuesList.clear()
        saveIssues()
    }
    state.update { it.copy(issues = emptyList(), unreadIssueCount = 0) }
}
```

The same issue exists in `appleMain/ModuleLifecycleManager.kt:104-111` and `147-151`.

---

### WR-02: Apple KamperUiRepository ignores maxRecordingSamples when constructing RecordingManager

**File:** `kamper/ui/android/src/appleMain/kotlin/com/smellouk/kamper/ui/KamperUiRepository.kt:13`

**Issue:** The `actual` constructor accepts `maxRecordingSamples: Int = 4_200` and the call site (`KamperUi.kt:76`) passes the user-configured value, but `RecordingManager()` is constructed with no arguments. Even after CR-03 is fixed and `RecordingManager` gains a real implementation with a `maxSamples` parameter, it will still use its own default rather than the caller-supplied cap.

**Fix:**
```kotlin
// appleMain/KamperUiRepository.kt line 13
private val recordingManager = RecordingManager(maxSamples = maxRecordingSamples)
```

---

### WR-03: SettingsRepository.clear() silently swallows in-flight saves — no error surfaced

**File:** `kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/SettingsRepository.kt:108-110`

**Issue:** `clear()` cancels the coroutine scope. If a `saveSettingsSync` coroutine launched by `updateSettings()` is in flight at the moment `clear()` is called, the save is abandoned silently. The in-memory `StateFlow` has already been updated, but the store is not written, creating a divergence between the live state and what will be reloaded on the next `SettingsRepository` construction. The test at `SettingsRepositoryTest.kt:109-115` actually verifies this divergence exists and treats it as correct — but the callers of `KamperUiRepository.clear()` (app teardown / overlay dismiss) may not expect to lose the last settings write.

This is acceptable if `clear()` is always called after the UI is destroyed and no further persistence is needed, but the contract is undocumented. At minimum, the method should flush pending saves before cancelling.

**Fix:**
```kotlin
fun clear() {
    // Ensure any in-flight save completes before cancelling.
    // If this is called from a coroutine context, use runBlocking or
    // call a synchronous flush instead.
    scope.cancel()
}
// OR: expose a suspend fun clearSuspending() that calls join() on pending jobs first.
```

At minimum, add a KDoc comment on `clear()` documenting that pending saves are abandoned.

---

### WR-04: fpsLow is never reset to Int.MAX_VALUE when FPS module restarts (Android)

**File:** `kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/ModuleLifecycleManager.kt:191-198`

**Issue:** `stopFps()` resets `fpsFrameCount` and `fpsWindowStartNanos`, but does not reset `state.fpsLow` or `state.fpsPeak`. When `startFps()` is called again (e.g. after a settings change that toggles FPS off then on), the old peak/low values persist and distort the new measurement window. The initial `KamperUiState.EMPTY` sets `fpsLow = Int.MAX_VALUE` and `fpsPeak = 0`, which are the correct sentinel values; they should be restored on restart.

**Fix:**
```kotlin
fun stopFps() {
    fpsActive = false
    Handler(Looper.getMainLooper()).post {
        Choreographer.getInstance().removeFrameCallback(fpsCallback)
    }
    fpsFrameCount = 0
    fpsWindowStartNanos = 0L
    // Reset derived state so stale peak/low don't bleed into the next window:
    state.update { it.copy(fpsPeak = 0, fpsLow = Int.MAX_VALUE, fpsHistory = emptyList()) }
}
```

---

### WR-05: RecordingManagerTest duplicates the DEFAULT_MAX_RECORDING_SAMPLES constant rather than referencing it

**File:** `kamper/ui/android/src/androidUnitTest/kotlin/com/smellouk/kamper/ui/RecordingManagerTest.kt:107`

**Issue:** The test defines `private const val MAX_RECORDING_SAMPLES = 4_200` locally. `RecordingManager` has `private const val DEFAULT_MAX_RECORDING_SAMPLES = 4_200`. If the default cap changes in `RecordingManager`, the test will silently stop testing the real boundary — it will still pass but will be testing the wrong value. This is a reliability defect in the test suite.

**Fix:** Make the constant accessible to tests (e.g. `internal` visibility) or pass the cap explicitly in the test:

```kotlin
// In RecordingManager.kt: change private to internal for testing
internal const val DEFAULT_MAX_RECORDING_SAMPLES = 4_200

// In RecordingManagerTest.kt: remove the duplicate constant and reference:
repeat(DEFAULT_MAX_RECORDING_SAMPLES + 10) { i -> ... }
assertEquals(DEFAULT_MAX_RECORDING_SAMPLES, classToTest.recordingSampleCount.value)
```

---

## Info

### IN-01: AndroidPreferencesStore performs redundant applicationContext cast

**File:** `kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/AndroidPreferencesStore.kt:7`

**Issue:** `(context.applicationContext as Application).getSharedPreferences(...)` — `applicationContext` returns a `Context`, not an `Application`, so the cast to `Application` is used purely to reach `getSharedPreferences`, which is already available on `Context`. The cast is unnecessary and throws `ClassCastException` in instrumentation test environments where the application context may not be an `Application` subclass.

**Fix:**
```kotlin
private val prefs = context.applicationContext
    .getSharedPreferences("kamper_ui_prefs", Context.MODE_PRIVATE)
```

The same redundant cast appears in `KamperUiRepository.kt:13` (`private val appContext = context.applicationContext as Application`). `MemoryModule(appContext)` requires a `Context`, not `Application`; the cast can be removed there too.

---

### IN-02: ApplePreferencesStore.putLong stores via setInteger which is NSInteger (32-bit on 32-bit devices)

**File:** `kamper/ui/android/src/appleMain/kotlin/com/smellouk/kamper/ui/ApplePreferencesStore.kt:18-19`

**Issue:** `NSUserDefaults.setInteger` uses `NSInteger`, which is 32-bit on 32-bit iOS hardware. All modern Apple devices are 64-bit, but `Long` in Kotlin/Native is always 64-bit. For large values (e.g. threshold in ms for ANR at 5_000 — fine, but any timestamp or epoch value would overflow). The safer API is `setDouble` / `doubleForKey` for 64-bit precision, or `setObject(value.toString(), key)` with a parse on read.

**Fix:**
```kotlin
override fun putLong(key: String, value: Long) {
    defaults.setDouble(value.toDouble(), key)
}

override fun getLong(key: String, default: Long): Long =
    if (defaults.objectForKey(key) != null) defaults.doubleForKey(key).toLong() else default
```

---

### IN-03: SettingsRepository.loadSettings() is a public suspend function never called from outside tests

**File:** `kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/SettingsRepository.kt:61-63`

**Issue:** `loadSettings()` is `suspend fun` with `internal` visibility, used only in `SettingsRepositoryTest` for the round-trip test. The constructor already calls `loadSettingsSync()` eagerly, so this function adds redundant re-read capability. It also overwrites `_settings` with a freshly loaded value, which could race with an in-flight `updateSettings()` that has updated the StateFlow but not yet persisted. This could cause the UI to briefly revert to the old saved value.

**Fix:** Either remove `loadSettings()` entirely (the round-trip test can be restructured to construct `repo2` after `advanceUntilIdle()` — the constructor calls `loadSettingsSync()` at that point), or make the method `private` and document the race.

---

_Reviewed: 2026-04-26T00:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
