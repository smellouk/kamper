---
phase: 07-kamper-panel-refactor-recomposition-fix
reviewed: 2026-04-26T00:00:00Z
depth: standard
files_reviewed: 7
files_reviewed_list:
  - kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/ActivityTab.kt
  - kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/IssuesTab.kt
  - kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/KamperPanel.kt
  - kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/PanelComponents.kt
  - kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/PerfettoTab.kt
  - kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/SettingsTab.kt
  - kamper/ui/android/src/commonTest/kotlin/com/smellouk/kamper/ui/compose/KamperPanelTest.kt
findings:
  critical: 0
  warning: 6
  info: 4
  total: 10
status: issues_found
---

# Phase 7: Code Review Report

**Reviewed:** 2026-04-26
**Depth:** standard
**Files Reviewed:** 7
**Status:** issues_found

## Summary

This phase extracted shared Compose composables into `PanelComponents.kt`, split `ActivityTab.kt` and `SettingsTab.kt` out of the monolithic `KamperPanel.kt`, and introduced `derivedStateOf` recomposition optimisation. The structural goals are achieved and the coordinator reduction is real. Six warnings and four info items were found; no blockers. The most consequential findings are: (1) `derivedStateOf` in `ActivityTab` does not provide the claimed scoping benefit due to how Kotlin property delegation works with a single upstream state capture, making the recomposition optimisation largely inert; (2) the infinite `while(true)` coroutine loop in `RecordingBadge` has no cooperative cancellation point before the first `delay`, making it structurally non-cancellable on the iteration that catches the cancellation exception; (3) substantial logic duplication exists between `IssuesTab.kt` and `PanelComponents.kt` for colour/name mappings; and (4) the test suite tests only lambda shapes, not the routing logic it claims to guard.

---

## Warnings

### WR-01: `derivedStateOf` in `ActivityTab` does not scope recomposition as documented

**File:** `kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/ActivityTab.kt:27-42`

**Issue:** Every `derivedStateOf` lambda reads from `s`, which is itself a `by state.collectAsState()` delegation. When any field of `KamperUiState` changes, `state` emits a new object, `s` is reassigned, and `ActivityTab` recomposes unconditionally — at which point every `derivedStateOf` block also re-evaluates. The derived values are structurally correct (they do cache their last result and skip child recomposition when the specific extracted value has not changed), but `ActivityTab` itself still recomposes on every emission regardless of which metric changed. The code comment at line 23–26 calls the `remember` wrapper "mandatory" and warns against its omission, but the per-field `derivedStateOf` wrapper only prevents child recomposition when the parent is already recomposing — which is every emission cycle. To actually scope recomposition to the specific `MetricCard` that cares about one field, each card would need to receive a `() -> T` lambda (stable producer), or each field would need to be in a separate `StateFlow`. As written, the optimisation eliminates only redundant downstream lambda re-execution, not the `ActivityTab` parent recomposition, which defeats the stated PERF-03 intent.

**Fix:** Either (a) pass stable lambdas `cpuPercent = { s.cpuPercent }` directly to `MetricCard` and have `MetricCard` use `derivedStateOf` internally against its own collector, or (b) restructure `MetricCard` to accept `StateFlow<Float>` and call `collectAsState()` inside the card, so each card independently subscribes. Option (a) is the lighter change:

```kotlin
// In ActivityTab, remove the derivedStateOf block entirely.
// Pass lambdas captured from 's' directly to MetricCard.
MetricCard(
    title    = "CPU",
    current  = { "${s.cpuPercent.formatDp(1)}%" },  // stable lambda
    fraction = { (s.cpuPercent / 100f).coerceIn(0f, 1f) },
    ...
)
// In MetricCard, use derivedStateOf against the lambda:
val current by remember { derivedStateOf { currentProvider() } }
```

---

### WR-02: `while(true)` timer loop in `RecordingBadge` is not safely cancellable before first iteration

**File:** `kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/PanelComponents.kt:621`

**Issue:** The loop body is `while (true) { delay(1_000); elapsed++ }`. Structured concurrency cancels coroutines cooperatively: when the `LaunchedEffect` is cancelled (e.g., recording stops or the composable leaves composition), the cancellation exception is thrown at the `delay` suspension point. This is correct for all iterations after the first. However, if `isRecording` flips to `false` between the moment `elapsed = 0` is set and the first `delay(1_000)` call, the `LaunchedEffect(isRecording)` with the new `false` key will restart the effect — resetting `elapsed` — before the old coroutine is cancelled, because the old coroutine has not yet reached a suspension point. In practice this is a very tight race and unlikely to produce visible bugs, but the pattern is fragile. More concretely: after a very fast start-stop, `elapsed` can briefly read `0` even though the display still shows `REC` for one frame.

**Fix:** Add a cancellation check before the delay, or use `isActive`:

```kotlin
LaunchedEffect(isRecording) {
    elapsed = 0
    if (isRecording) {
        while (isActive) { delay(1_000); elapsed++ }
    }
}
```

`isActive` is a property on `CoroutineScope` available inside `LaunchedEffect` blocks; checking it turns the loop into a properly cooperative structure.

---

### WR-03: Logic duplication between `IssuesTab.kt` and `PanelComponents.kt` — `severityColor`, `typeColor`, `typeShortName`

**File:** `kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/IssuesTab.kt:176-205` and `kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/PanelComponents.kt:477-505`

**Issue:** The `severityColor`, `typeColor` (as `typeChipColor`), and `typeShortName` `when` blocks are duplicated verbatim. The comment on line 468–469 of `PanelComponents.kt` acknowledges this, noting that "Plan 04 will remove the originals from IssuesTab.kt." That removal did not happen in this phase. Both copies must be kept in sync: adding a new `IssueType` or `Severity` value requires updating two independent `when` expressions. If only one is updated the other silently exhausts the sealed class without a compile error (because neither `when` is used as an expression in an assignment that requires exhaustiveness at these call sites — the `@Composable` `when` in `IssueTab.severityColor` returns `Color` so it is exhaustive, but divergence between the two files will compile fine).

**Fix:** Make `IssueDetailDialog` call the private helpers in `IssuesTab.kt` (requires making them internal), or consolidate all four helpers into `PanelComponents.kt` as `internal` functions and delete the duplicates from `IssuesTab.kt` in this phase rather than deferring.

---

### WR-04: `fmtTime` timestamp conversion is incorrect — milliseconds treated as wall-clock seconds incorrectly

**File:** `kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/IssuesTab.kt:220-224` and `kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/PanelComponents.kt:508-512`

**Issue:** Both copies compute `val sec = (ms / 1000) % 86400`. This yields the number of elapsed seconds modulo one day, which — given that `ms` is a `System.currentTimeMillis()`-style epoch millisecond value — produces the number of seconds since midnight UTC, not local time. For a user in UTC+2 at 01:30 local time, the displayed time will read "23:30:00" (the UTC value). The function is named `fmtTime` and renders in the Issues list under a "Time" field, leading users to read it as local time. This is a display correctness bug, not a localisation preference — the value is actively wrong.

**Fix:** Convert epoch millis to local time using the platform's calendar API, or use `java.text.SimpleDateFormat` / `java.time.LocalTime` (on Android API 26+) with `TimeZone.getDefault()`:

```kotlin
private fun fmtTime(ms: Long): String {
    val cal = java.util.Calendar.getInstance() // uses default (local) timezone
    cal.timeInMillis = ms
    val h = cal.get(java.util.Calendar.HOUR_OF_DAY)
    val m = cal.get(java.util.Calendar.MINUTE)
    val s = cal.get(java.util.Calendar.SECOND)
    return "${h.toString().padStart(2,'0')}:${m.toString().padStart(2,'0')}:${s.toString().padStart(2,'0')}"
}
```

Both copies (IssuesTab.kt and PanelComponents.kt) must be fixed.

---

### WR-05: `KamperPanel` scrim `clickable` calls `onDismiss` but inner panel uses `clickable(enabled = false) {}` — double-dismiss not guarded

**File:** `kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/KamperPanel.kt:69-88`

**Issue:** The outer `Box` (the scrim) has `.clickable(onClick = onDismiss)` at line 71. The inner panel column has `.clickable(enabled = false) {}` at line 87, intended to stop click propagation. Compose's `clickable` with `enabled = false` does consume the pointer event and prevents propagation in Compose's pointer-input chain — this is the correct pattern. However, a rapid double-tap on the scrim area (not the panel) can invoke `onDismiss` twice because there is no `enabled` guard on the outer clickable. If `onDismiss` has side effects (e.g., popping a back-stack entry), a double invocation may cause a crash or unexpected navigation state.

**Fix:** Use a one-shot guard or check dismissal state before acting:

```kotlin
var dismissed by remember { mutableStateOf(false) }
// ...
.clickable {
    if (!dismissed) { dismissed = true; onDismiss() }
}
```

Alternatively, callers of `KamperPanel` should ensure `onDismiss` is idempotent, but that is an implicit contract not documented at the call site.

---

### WR-06: `KamperPanel` passes raw `StateFlow` references into `ActivityTab` instead of already-collected values, forcing a second `collectAsState` call per tab switch

**File:** `kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/KamperPanel.kt:140` and `ActivityTab.kt:17-21`

**Issue:** `KamperPanel` collects `state` and `settings` into `s` and `cfg` at lines 58–59, then passes the original `StateFlow` references down to `ActivityTab` (line 140), which calls `collectAsState()` again at lines 20–21. This creates two independent subscriptions to the same flows within the same composition scope. Both subscriptions will fire on every emission, causing both `KamperPanel` and `ActivityTab` to schedule recompositions. The `s` variable in `KamperPanel` is only used for `IssuesTab` (line 149) and `SettingsTab` (lines 152–153); it is never passed to `ActivityTab`. The double-subscription is not a correctness bug (both reads will return the same current value), but it is a structural inconsistency: either the coordinator should collect and pass plain values, or tabs should each collect independently — not both.

**Fix:** Since `KamperPanel` already collects `s` and `cfg`, pass plain values to `SettingsTab` and `IssuesTab` and keep `ActivityTab`'s self-collection. Or remove the collection from `KamperPanel` altogether and pass flows everywhere. Do not mix both approaches in the same composable.

---

## Info

### IN-01: `KamperPanel` indentation is misaligned — `KamperThemeProvider` body not indented

**File:** `kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/KamperPanel.kt:67-164`

**Issue:** The `KamperThemeProvider { ... }` call on line 67 wraps the entire `Box`, but the `Box` is not indented relative to the provider. The closing `}` at line 164 has a comment `// KamperThemeProvider` to compensate for the mismatch. This makes the nesting structure visually ambiguous and the comment is a code smell masking a formatting issue.

**Fix:** Indent the `Box` block one level inside `KamperThemeProvider`:

```kotlin
KamperThemeProvider(isDark = cfg.isDarkTheme) {
    Box(
        modifier = Modifier
            ...
    ) {
        ...
    }
}
```

---

### IN-02: `PanelComponents.kt` component numbering skips `11` and is out of order

**File:** `kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/PanelComponents.kt:756`

**Issue:** The section comments number components 1–10 and then 12–16, with `MetricCard` labeled `11` at line 756 — after 12–16. The out-of-order placement and the comment at line 469 (`// ── 12. IssueDetailDialog`) before `// ── 11. MetricCard` at line 756 indicate `MetricCard` was appended at the end rather than inserted in sequence. This is a maintenance hazard: future contributors may not realise `11` is `MetricCard` without reading the full file.

**Fix:** Reorder sections so `MetricCard` (11) appears before `IssueDetailDialog` (12), or renumber consistently after the final ordering is decided.

---

### IN-03: Test suite for `KamperPanel` tests only lambda type-shapes, not routing behaviour

**File:** `kamper/ui/android/src/commonTest/kotlin/com/smellouk/kamper/ui/compose/KamperPanelTest.kt:26-33`

**Issue:** The tab-routing tests (D-12, lines 38–61) duplicate the routing logic in a local lambda (`tabRouting`) and assert against that lambda — not against `KamperPanel.kt`'s actual `when(selectedTab)` block. If the production routing changes (e.g., tabs reordered or a fifth tab added), the test lambda must be manually kept in sync; it will not fail automatically. The D-11 tests (lines 65–121) exercise lambda invocation in isolation with no dependency on `KamperPanel` whatsoever. As written, the entire test class provides zero regression protection for `KamperPanel.kt` itself.

**Fix:** Either (a) acknowledge in comments that these are purely compile-time contract tests with no runtime link to the production composable, or (b) replace the routing mirror with a shared constant (e.g., a `tabNames: List<String>` exposed from `KamperPanel`'s companion or a sibling constants file) that both production code and tests read, so a mismatch is detected at compile time.

---

### IN-04: `showAdbGuide` `expect` property in `PerfettoTab.kt` has no `actual` declaration visible in the reviewed files

**File:** `kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/PerfettoTab.kt:190`

**Issue:** `internal expect val showAdbGuide: Boolean` is declared in `commonMain`, but its `actual` implementations are not in the reviewed file set. If the `actual` files were not updated as part of this refactor, the build will fail on all targets. This is flagged as info because the implementations likely exist elsewhere in the module and were out of scope for this phase, but the absence of the `actual` declarations from the changed-files list warrants explicit verification.

**Fix:** Confirm that `actual val showAdbGuide` declarations exist in both `androidMain` and any other target source sets, and that they were not accidentally lost during the refactor.

---

_Reviewed: 2026-04-26_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
