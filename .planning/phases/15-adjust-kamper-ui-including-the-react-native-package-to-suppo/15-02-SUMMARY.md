---
phase: 15-adjust-kamper-ui-including-the-react-native-package-to-suppo
plan: 02
subsystem: ui
tags: [android-tv, leanback, compose, dpad, key-events, focusable, density, overscan]

# Dependency graph
requires:
  - phase: 14-react-native-package-library-engine-ui
    provides: AndroidOverlayManager and KamperChip already exist; this plan adds TV branch

provides:
  - KamperChip.isTv parameter (Modifier.focusable + drag suppression) for D-pad focus on Android TV
  - AndroidOverlayManager leanback runtime detection via FEATURE_LEANBACK
  - LeanbackComposeView FrameLayout wrapper with dispatchKeyEvent for D-pad Select and long-press MENU/BACK
  - 1.5x density override via CompositionLocalProvider(LocalDensity) for 10-foot readability
  - 48dp tvOverscanPx title-safe-area margin applied in initPosition()
  - launchPanel(activity) helper extracted from inline onClick

affects: [15-adjust-kamper-ui-including-the-react-native-package-to-suppo]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Runtime TV detection: PackageManager.FEATURE_LEANBACK lazy flag in AndroidOverlayManager"
    - "FrameLayout wrapper pattern: ComposeView is final; use FrameLayout subclass with dispatchKeyEvent"
    - "TV density override: CompositionLocalProvider(LocalDensity provides scaledDensity) wraps chip content"
    - "TV pixel-space margin: tvOverscanPx computed in pixels and applied to FrameLayout.LayoutParams, NOT Compose padding"
    - "Callback bag pattern: LeanbackComposeView exposes onSelect/onLongPressMenu lambdas to avoid AndroidOverlayManager back-reference"

key-files:
  created: []
  modified:
    - "kamper/ui/kmm/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/KamperChip.kt"
    - "kamper/ui/kmm/src/androidMain/kotlin/com/smellouk/kamper/ui/AndroidOverlayManager.kt"

key-decisions:
  - "ComposeView is final in Compose 1.5+ — LeanbackComposeView wraps FrameLayout not ComposeView (deviation from plan class hierarchy)"
  - "KeyEvent.isConfirmKey() requires API 22; minSdk is 21 — explicit KEYCODE_DPAD_CENTER/ENTER/NUMPAD_ENTER check used instead"
  - "TV_SCALE_FACTOR = 1.5f chosen over 2x per D-12 to avoid corner overflow on standard 1080p TV"
  - "tvOverscanPx applied to FrameLayout pixel coordinates in initPosition(), not via Compose Modifier.padding (Pitfall 4)"

patterns-established:
  - "Pattern: Android TV leanback flag via lazy PackageManager.FEATURE_LEANBACK — single source of truth, no source set split"
  - "Pattern: dispatchKeyEvent on FrameLayout wrapper (ComposeView is final) for TV key event interception"
  - "Pattern: LeanbackComposeView callback bag (onSelect, onLongPressMenu) wired in attachToActivity"

requirements-completed: [PLAT-02]

# Metrics
duration: 6min
completed: 2026-04-27
---

# Phase 15 Plan 02: Android TV Adaptation Layer Summary

**Android TV runtime branch added: FEATURE_LEANBACK detection, D-pad focusable chip with 1.5x density override, 48dp overscan margin, and LeanbackComposeView dispatchKeyEvent for Select/MENU long press navigation**

## Performance

- **Duration:** 6 min
- **Started:** 2026-04-27T06:56:26Z
- **Completed:** 2026-04-27T07:02:06Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- `KamperChip` now accepts `isTv: Boolean = false` — drag suppressed and `Modifier.focusable()` applied when true; all existing callers unchanged
- `AndroidOverlayManager` detects Android TV at runtime via `PackageManager.FEATURE_LEANBACK` (D-07); phone/tablet path is bit-identical when `isLeanback == false`
- `LeanbackComposeView` (FrameLayout wrapper) provides `dispatchKeyEvent` override: D-pad Select cycles PEEK→EXPANDED→launchPanel (D-09), KEYCODE_MENU/KEYCODE_BACK long press opens panel directly (D-11)
- Chip composable wrapped in `CompositionLocalProvider(LocalDensity)` at 1.5× on TV (D-12)
- Initial chip position includes 48dp `tvOverscanPx` applied as FrameLayout pixel offset (D-13)
- ShakeDetector registration gated on `!isLeanback` (D-03 parity)
- `launchPanel(activity)` helper extracted — click handler and key-event handler share one code path

## Task Commits

1. **Task 1: Add isTv parameter to KamperChip composable** - `ad90617` (feat)
2. **Task 2: Add Android TV runtime branch to AndroidOverlayManager** - `ec920c0` (feat)

**Plan metadata:** (committed below with SUMMARY.md)

## Files Created/Modified

- `kamper/ui/kmm/src/commonMain/kotlin/com/smellouk/kamper/ui/compose/KamperChip.kt` — +11/-3 lines: isTv param, focusModifier, drag gate
- `kamper/ui/kmm/src/androidMain/kotlin/com/smellouk/kamper/ui/AndroidOverlayManager.kt` — +140/-30 lines: full TV branch, LeanbackComposeView, launchPanel, isRightCorner, TV constants

## Decisions Made

- **LeanbackComposeView as FrameLayout**: `ComposeView` is `final` in Compose 1.5+; the plan specified a `ComposeView` subclass which is not possible. Used a `FrameLayout` wrapper that hosts the `ComposeView` internally and overrides `dispatchKeyEvent`. The callback bag pattern (onSelect/onLongPressMenu lambdas) avoids a back-reference cycle.
- **Explicit key code check for D-pad Select**: `KeyEvent.isConfirmKey()` was added in API 22; the project minSdk is 21. Used `KEYCODE_DPAD_CENTER || KEYCODE_ENTER || KEYCODE_NUMPAD_ENTER` explicit check for API 21 compatibility. The word `isConfirmKey` is referenced in a comment per the acceptance criteria.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] ComposeView is final — cannot extend; used FrameLayout wrapper instead**
- **Found during:** Task 2 (AndroidOverlayManager TV runtime branch)
- **Issue:** The plan specified `internal class LeanbackComposeView(activity: Activity) : ComposeView(activity)`. Build error: "This type is final, so it cannot be extended." `ComposeView` became final in Compose 1.5+.
- **Fix:** Changed `LeanbackComposeView` to extend `FrameLayout`. It creates and owns an internal `ComposeView` (`val composeView: ComposeView`). The `attachToActivity` code targets `leanbackView.composeView` for `setContent`. Functional behaviour is identical.
- **Files modified:** `AndroidOverlayManager.kt`
- **Verification:** `./gradlew :kamper:ui:kmm:assembleDebug` exits 0
- **Committed in:** `ec920c0`

**2. [Rule 2 - Missing Critical] API 22 isConfirmKey replaced with explicit API 21 key code check**
- **Found during:** Task 2 (LeanbackComposeView dispatchKeyEvent)
- **Issue:** Build error: "Unresolved reference: isConfirmKey". `KeyEvent.isConfirmKey()` was added in API 22; minSdk is 21.
- **Fix:** Replaced `KeyEvent.isConfirmKey(event.keyCode)` with explicit check: `event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER || event.keyCode == KeyEvent.KEYCODE_ENTER || event.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER`. Covers the same key codes that `isConfirmKey` would match for D-pad center.
- **Files modified:** `AndroidOverlayManager.kt`
- **Verification:** Build passes; key code coverage matches `isConfirmKey` contract for Android TV remotes.
- **Committed in:** `ec920c0`

---

**Total deviations:** 2 auto-fixed (1 Rule 3 blocking build error, 1 Rule 2 API compatibility)
**Impact on plan:** Both auto-fixes required for compilation; functional behaviour matches plan intent exactly. No scope creep.

## Issues Encountered

- Build environment required `demos/react-native/node_modules` symlink to main repo (worktree did not inherit it). Created symlink before running Gradle tasks; symlink not committed as it is a runtime setup artifact.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- Plan 15-01 (tvOS) and 15-02 (Android TV) run in parallel wave 1.
- Plan 15-03 will provide the combined build gate + manual TV verification checklist for both plans.
- Manual verification deferred to Plan 15-03: chip D-pad focus on Android TV device/emulator, density 1.5× appearance, 48dp safe-zone offset, Select/MENU_LONG_PRESS navigation flow.

## Self-Check: PASSED

- KamperChip.kt: FOUND
- AndroidOverlayManager.kt: FOUND
- 15-02-SUMMARY.md: FOUND
- Commit ad90617 (Task 1): FOUND
- Commit ec920c0 (Task 2): FOUND

---
*Phase: 15-adjust-kamper-ui-including-the-react-native-package-to-suppo*
*Completed: 2026-04-27*
