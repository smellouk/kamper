---
phase: 22-manual-testing-all-demo-platforms-one-by-one
reviewed: 2026-04-30T00:00:00Z
depth: standard
files_reviewed: 24
files_reviewed_list:
  - demos/android/src/main/java/com/smellouk/kamper/android/MainActivity.kt
  - demos/compose/src/androidMain/kotlin/com/smellouk/kamper/compose/KamperSetup.kt
  - demos/compose/src/commonMain/kotlin/com/smellouk/kamper/compose/App.kt
  - demos/compose/src/commonMain/kotlin/com/smellouk/kamper/compose/KamperState.kt
  - demos/compose/src/commonMain/kotlin/com/smellouk/kamper/compose/ui/tabs/JankTab.kt
  - demos/compose/src/commonMain/kotlin/com/smellouk/kamper/compose/ui/tabs/NetworkTab.kt
  - demos/compose/src/desktopMain/kotlin/com/smellouk/kamper/compose/KamperSetup.kt
  - demos/compose/src/iosMain/kotlin/com/smellouk/kamper/compose/KamperSetup.kt
  - demos/compose/src/wasmJsMain/kotlin/com/smellouk/kamper/compose/KamperSetup.kt
  - demos/react-native/App.tsx
  - demos/react-native/metro.config.js
  - demos/web/src/jsMain/kotlin/com/smellouk/kamper/web/ui/CpuSection.kt
  - demos/web/src/jsMain/kotlin/com/smellouk/kamper/web/ui/GcSection.kt
  - demos/web/src/jsMain/kotlin/com/smellouk/kamper/web/ui/IssuesSection.kt
  - demos/web/src/jsMain/kotlin/com/smellouk/kamper/web/ui/JankSection.kt
  - demos/web/src/jsMain/kotlin/com/smellouk/kamper/web/ui/NetworkSection.kt
  - demos/web/src/jsMain/kotlin/com/smellouk/kamper/web/ui/ThermalSection.kt
  - libs/modules/cpu/build.gradle.kts
  - libs/modules/cpu/src/macosMain/kotlin/com/smellouk/kamper/cpu/repository/source/MacosCpuInfoSource.kt
  - libs/modules/cpu/src/nativeInterop/cinterop/cpuInfo.def
  - libs/modules/gc/src/jsMain/kotlin/com/smellouk/kamper/gc/repository/GcInfoRepositoryImpl.kt
  - libs/modules/thermal/src/jsMain/kotlin/com/smellouk/kamper/thermal/repository/ThermalInfoRepositoryImpl.kt
  - libs/modules/thermal/src/macosMain/kotlin/com/smellouk/kamper/thermal/repository/ThermalInfoRepositoryImpl.kt
  - libs/modules/thermal/src/nativeInterop/cinterop/thermalState.def
findings:
  critical: 1
  warning: 4
  info: 2
  total: 7
status: issues_found
---

# Phase 22: Code Review Report

**Reviewed:** 2026-04-30T00:00:00Z
**Depth:** standard
**Files Reviewed:** 24
**Status:** issues_found

## Summary

This phase covers the demo smoke-test implementations across Android, Compose Multiplatform (Android/iOS/Desktop/WasmJS), React Native, Web (JS), and the two new native library implementations (macOS CPU via Mach ticks, macOS Thermal via SMC, JS GC/Thermal stubs).

The library-side implementations (MacosCpuInfoSource, ThermalInfoRepositoryImpl macOS/JS, GcInfoRepositoryImpl JS) are generally sound and follow the D-06 safety rule. The critical defect is a wrong field name in the React Native demo that will produce a runtime crash whenever Jank data arrives. Three further warnings cover missing INVALID guards in the Compose demos, an unsafe C cinterop struct initialization, and a busy-wait on the UI/main coroutine dispatcher.

---

## Critical Issues

### CR-01: Wrong field name `jankyRatio` in React Native JankTab crashes at runtime

**File:** `demos/react-native/App.tsx:588`
**Issue:** The React Native `JankTab` component reads `jank.jankyRatio`, but the `JankInfo` type exported by `react-native-kamper` mirrors the Kotlin data class, whose field is `jankyFrameRatio`. The field `jankyRatio` does not exist. When a `JankData` object arrives from the native bridge, accessing `.jankyRatio` returns `undefined`, and calling `.toFixed(1)` on `undefined` throws `TypeError: Cannot read properties of undefined`. This crashes the Jank tab whenever it receives data.

Cross-reference: `libs/modules/jank` → `JankInfo.kt` declares `val jankyFrameRatio: Float`. The Compose demo correctly uses `info.jankyFrameRatio` (JankTab.kt:78). The RN demo deviates at line 588.

**Fix:**
```tsx
// Line 588 — change:
<Text style={[s.metricValue, {color: C.mauve}]}>{(jank.jankyRatio * 100).toFixed(1)}%</Text>
// to:
<Text style={[s.metricValue, {color: C.mauve}]}>{(jank.jankyFrameRatio * 100).toFixed(1)}%</Text>
```

---

## Warnings

### WR-01: Compose KamperSetup listeners pass INVALID/UNSUPPORTED info to state without guarding

**File:** `demos/compose/src/androidMain/kotlin/com/smellouk/kamper/compose/KamperSetup.kt:41-48`
**Also:** `demos/compose/src/desktopMain/kotlin/com/smellouk/kamper/compose/KamperSetup.kt:36-43`, `demos/compose/src/iosMain/kotlin/com/smellouk/kamper/compose/KamperSetup.kt:36-43`, `demos/compose/src/wasmJsMain/kotlin/com/smellouk/kamper/compose/KamperSetup.kt:33-40`

**Issue:** All four Compose platform `initialize()` implementations pass every received info object directly to the state without checking for `INVALID` or `UNSUPPORTED` sentinels:

```kotlin
Kamper.addInfoListener<CpuInfo> { info -> scope.launch { cpuInfo = info } }
```

The contract in CLAUDE.md (D-06 and the INVALID Sentinel section) states: "Callers check for INVALID before processing data; listeners are never invoked with invalid readings." However, the engine's actual delivery guarantee depends on module implementation. The Android demo's `MainActivity.kt` already correctly guards every listener (lines 97–104). The Compose demos do not. This means `CpuInfo.INVALID`, `JankInfo.UNSUPPORTED`, etc. can be written to Compose state and flow into the UI tabs. The tabs themselves do have INVALID/UNSUPPORTED checks, so the worst visible outcome is a flash of the "—" placeholder — but it is a correctness violation of the sentinel contract and inconsistent with the reference Android demo.

**Fix:** Mirror the Android demo's guard pattern:
```kotlin
Kamper.addInfoListener<CpuInfo>     { info -> if (info != CpuInfo.INVALID) scope.launch { cpuInfo = info } }
Kamper.addInfoListener<JankInfo>    { info -> if (info != JankInfo.INVALID && info != JankInfo.UNSUPPORTED) scope.launch { jankInfo = info } }
// etc. for all listeners
```

---

### WR-02: C cinterop struct `KamperSMCParam` zero-initialised by compound literal but `key` field is re-assigned — padding bytes in `in` may leak to kernel

**File:** `libs/modules/thermal/src/nativeInterop/cinterop/thermalState.def:48-64`

**Issue:** In `kamper_smc_read_key`, the local `KamperSMCParam in` is zero-initialised with `= {0}` (C compound literal), which is correct. However, the second IOConnectCallStructMethod call at line 63 reuses the same `in` struct that was mutated during the first call (setting `in.keyInfo.dataSize` and `in.data8`). The `out` struct is reset with `memset(&out, 0, ...)` (line 61) but `in` is not. Fields set during the first call (e.g., `in.data8 = KAMPER_SMC_GET_KEY_INFO`) are still set when the second call is made with `in.data8 = KAMPER_SMC_READ_BYTES`. In isolation this is intentional, but `in.keyInfo` holds the response data from the first call with `dataType` and `dataSize` populated. The function only assigns `in.keyInfo.dataSize = dataSize` and `in.data8` before the second call; `in.keyInfo.dataType` retains its old value (actually from the local `dataType` variable, not the struct). This is actually fine for functionality, but the struct `vers`, `pLimitData`, `result`, `status`, and `bytes` fields carry stale data from the first response into the second kernel call. On a read-only IOConnectCallStructMethod the kernel ignores input bytes fields, so no exploitable path exists; however this is a code clarity/correctness smell that could become a real issue if the SMC call semantics change.

**Fix:** Reset `in` before the second call for defensive correctness:
```c
memset(&in, 0, sizeof(in));
in.key              = kamper_smc_encode_key(key);
in.keyInfo.dataSize = dataSize;
in.data8            = KAMPER_SMC_READ_BYTES;
```

---

### WR-03: Jank simulation in Compose JankTab busy-loops on `Dispatchers.Main`, blocking the UI thread

**File:** `demos/compose/src/commonMain/kotlin/com/smellouk/kamper/compose/ui/tabs/JankTab.kt:103-106`

**Issue:** The "SIMULATE JANK" button launches a coroutine on `Dispatchers.Main` and spins a busy loop for 200 ms:

```kotlin
scope.launch(Dispatchers.Main) {
    val end = TimeSource.Monotonic.markNow() + 200.milliseconds
    while (end.hasNotPassedNow()) {}
}
```

On the Android and iOS Compose targets this unconditionally freezes the main thread (and therefore the Choreographer/display link) for 200 ms, which is the intended effect of demonstrating jank. However, on the Desktop JVM target `Dispatchers.Main` maps to the Swing EDT — a 200 ms freeze will hang the entire JVM desktop window (no repaints, no input). The RN demo uses `setTimeout` to yield before the busy loop (App.tsx:568). A 200 ms EDT freeze is acceptable in a demo, but the explicit `Dispatchers.Main` dispatcher is not needed — `scope` (from `rememberCoroutineScope`) already runs on Main. The explicit override is misleading.

**Fix:** Remove the explicit dispatcher; the `rememberCoroutineScope` scope already targets Main:
```kotlin
scope.launch {
    val end = TimeSource.Monotonic.markNow() + 200.milliseconds
    while (end.hasNotPassedNow()) {}
}
```

---

### WR-04: `IssuesSection.render()` sets `bar.className` twice — dead assignment

**File:** `demos/web/src/jsMain/kotlin/com/smellouk/kamper/web/ui/IssuesSection.kt:80-84`

**Issue:** The `bar` element's `className` is set inside the `also` block (`it.className = "issue-bar"`) at line 80-82, then immediately reassigned redundantly at line 84 (`bar.className = "issue-bar"`). The second assignment is a dead write — it sets the identical value that was already set two lines earlier. This is either dead code from a copy-paste error or indicates that an intended second class name (e.g., a severity modifier) was forgotten.

```kotlin
val bar = (document.createElement("div") as HTMLElement).also {
    it.className = "issue-bar"          // line 80-82: set here
    it.style.backgroundColor = severityColor(issue.severity)
    row.appendChild(it)
}
bar.className = "issue-bar"             // line 84: dead redundant write
```

**Fix:** Remove the redundant line 84 assignment. If a severity-specific CSS class was intended (e.g., `"issue-bar issue-bar--${issue.severity.name.lowercase()}"`) that should be added here instead.

---

## Info

### IN-01: `NetworkSection.measureBandwidth()` captures Kotlin locals via JS closure in an `unsafeCast` pattern — variable capture semantics are not guaranteed

**File:** `demos/web/src/jsMain/kotlin/com/smellouk/kamper/web/ui/NetworkSection.kt:71-87`

**Issue:** The `measureBandwidth` function stores `statusEl` and `dlValue` into local `val`s (`statusElem`, `dlElem`) and then references them inside an inline `js("""...""")` string block. Kotlin/JS does not formally specify that IR-compiled local variables referenced by name inside `js("")` strings will be captured as closure variables; this relies on the Kotlin/JS IR compiler outputting the same JavaScript local variable name. It works in practice for the current compiler version, but is fragile across compiler upgrades.

**Fix:** This is acceptable for a demo, but a more robust pattern would be to use Kotlin coroutines with `window.fetch` Kotlin bindings, or at minimum add a comment documenting the Kotlin/JS IR variable capture assumption.

---

### IN-02: `CpuSection.stopCpuStress` passes `handle` by name into `js("clearInterval(handle)")` — same Kotlin/JS IR capture fragility

**File:** `demos/web/src/jsMain/kotlin/com/smellouk/kamper/web/ui/CpuSection.kt:86`

**Issue:** Same pattern as IN-01. The `handle` parameter is referenced by name inside the `js("clearInterval(handle)")` call. This works with the current Kotlin/JS IR compiler but is not a guaranteed contract.

**Fix:** Same as IN-01 — acceptable for demo code, but worth a comment. No code change required.

---

_Reviewed: 2026-04-30T00:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
