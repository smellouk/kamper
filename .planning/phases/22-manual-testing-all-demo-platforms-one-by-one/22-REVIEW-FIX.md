---
phase: 22-manual-testing-all-demo-platforms-one-by-one
fixed_at: 2026-04-30T00:00:00Z
review_path: .planning/phases/22-manual-testing-all-demo-platforms-one-by-one/22-REVIEW.md
iteration: 1
findings_in_scope: 5
fixed: 5
skipped: 0
status: all_fixed
---

# Phase 22: Code Review Fix Report

**Fixed at:** 2026-04-30T00:00:00Z
**Source review:** .planning/phases/22-manual-testing-all-demo-platforms-one-by-one/22-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 5
- Fixed: 5
- Skipped: 0

## Fixed Issues

### CR-01: Wrong field name `jankyRatio` in React Native JankTab

**Files modified:** `demos/react-native/App.tsx`
**Commit:** c769274
**Applied fix:** Changed `jank.jankyRatio` to `jank.jankyFrameRatio` at line 588. The field `jankyRatio` does not exist on the JankInfo type — `jankyFrameRatio` matches the Kotlin data class declaration in `JankInfo.kt`.

---

### WR-01: Compose KamperSetup listeners pass INVALID/UNSUPPORTED info without guarding

**Files modified:** `demos/compose/src/androidMain/kotlin/com/smellouk/kamper/compose/KamperSetup.kt`, `demos/compose/src/desktopMain/kotlin/com/smellouk/kamper/compose/KamperSetup.kt`, `demos/compose/src/iosMain/kotlin/com/smellouk/kamper/compose/KamperSetup.kt`, `demos/compose/src/wasmJsMain/kotlin/com/smellouk/kamper/compose/KamperSetup.kt`
**Commit:** 67a977f
**Applied fix:** Added `if (info != XxxInfo.INVALID)` guards before each `scope.launch { ... }` call for CpuInfo, FpsInfo, MemoryInfo, NetworkInfo, GcInfo, and ThermalInfo listeners. For JankInfo, guarded against both `JankInfo.INVALID` and `JankInfo.UNSUPPORTED`. IssueInfo listener left without guard, consistent with the Android demo reference (MainActivity.kt line 101).

---

### WR-02: C cinterop struct stale fields before second SMC call

**Files modified:** `libs/modules/thermal/src/nativeInterop/cinterop/thermalState.def`
**Commit:** f76c5ba
**Applied fix:** Added `memset(&in, 0, sizeof(in));` before the second `IOConnectCallStructMethod` call and re-assigned all three fields (`in.key`, `in.keyInfo.dataSize`, `in.data8`) after the reset. This ensures no stale data from the first call's response leaks into the second kernel call.

---

### WR-03: Jank simulation in Compose JankTab busy-loops on `Dispatchers.Main`

**Files modified:** `demos/compose/src/commonMain/kotlin/com/smellouk/kamper/compose/ui/tabs/JankTab.kt`
**Commit:** 67c09b0
**Applied fix:** Removed the explicit `Dispatchers.Main` argument from `scope.launch(Dispatchers.Main)`, leaving it as `scope.launch`. Also removed the now-unused `import kotlinx.coroutines.Dispatchers` line. The `rememberCoroutineScope` scope already targets Main, so the explicit dispatcher was redundant.

---

### WR-04: `IssuesSection.render()` sets `bar.className` twice — dead assignment

**Files modified:** `demos/web/src/jsMain/kotlin/com/smellouk/kamper/web/ui/IssuesSection.kt`
**Commit:** be56264
**Applied fix:** Removed the redundant `bar.className = "issue-bar"` line 84. Additionally, since `bar` was no longer referenced after its `also` block, the `val bar =` assignment was converted to an anonymous statement (dropping the variable binding entirely), consistent with the same pattern used for other elements in the same render loop that are not referenced after creation.

---

_Fixed: 2026-04-30T00:00:00Z_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
