# Phase 15 — TV Manual Verification Checklist

**Created:** 2026-04-27
**Verified by:** [developer name]
**Verified on:** [datetime when complete]

---

## Purpose

This checklist walks the developer through manual verification of the TV overlay
adaptations delivered in Phase 15:

- Plan 15-01: tvOS targets (`tvosArm64`, `tvosSimulatorArm64`) + `tvosMain` actual `KamperUi.kt`
  with UIKit D-pad/Play-Pause handling.
- Plan 15-02: Android TV runtime branch — `FEATURE_LEANBACK` detection,
  `LeanbackComposeView` D-pad Select/MENU_LONG_PRESS, 1.5x density override, 48dp overscan.
- Plan 15-03 Task 2: `kamper-react-native.podspec` extended with
  `s.tvos.deployment_target = '13.0'` (D-19).

Automated tests cannot verify visual rendering, D-pad focus ring appearance, density
scaling, or remote-button timing. This checklist covers those behaviors.

---

## Prerequisites

- [ ] Plans 15-01 and 15-02 merged to the working branch (verify with
  `grep tvosArm64 kamper/ui/kmm/build.gradle.kts && grep FEATURE_LEANBACK kamper/ui/kmm/src/androidMain/kotlin/com/smellouk/kamper/ui/AndroidOverlayManager.kt`).
- [ ] `./gradlew :kamper:ui:kmm:assembleDebug :kamper:ui:kmm:testDebugUnitTest` is green
  (Plan 15-03 Task 1 build gate — Android targets pass; tvOS compile blocked by CMP 1.9.x
  klib limitation which is a known deferred issue, not a regression).
- [ ] macOS with Xcode 15+ and a tvOS Simulator runtime >= 17.0 installed
  (`xcodebuild -showsdks | grep tvOS`).
- [ ] Android Studio with an Android TV system image installed — e.g.
  "Android TV (1080p) API 34" AVD — or a physical Fire TV device with developer mode
  and ADB enabled.

---

## Section A — Apple TV (tvOS Simulator)

Build a Kamper-bearing target for tvOS and run it on the tvOS Simulator.
The existing `samples/apple` target can be configured for tvOS, or create a
minimal tvOS app target that imports the `kamper:ui:kmm` library.

Open the simulator via Xcode — Hardware > Show Apple TV Remote gives you
on-screen D-pad + Select + Play/Pause + Back buttons for interaction.

### A.1  Initial chip render

- [ ] **A.1.1 (D-01, D-13):** With `KamperUiConfig.position = ChipPosition.BOTTOM_END`
  (default for TV), the chip appears in the bottom-right corner with a visible
  margin from the screen edge — approximately 48pt (the `TV_OVERSCAN_DP = 48.0`
  constant) on a 1920x1080 simulator window. The chip is clearly inside the
  title-safe area.
- [ ] **A.1.2 (D-01):** The chip cannot be dragged. Attempting to swipe or pan
  via the simulator's Apple TV Remote trackpad surface does NOT move the chip.
  It stays fixed at its corner.
- [ ] **A.1.3 (D-01, D-13):** Switching `KamperUiConfig.position` to `TOP_START`
  and relaunching shows the chip in the top-left corner with the same 48pt
  overscan offset from both the top and left edges.

### A.2  D-pad Select two-step flow (D-02)

Open the Apple TV Remote (Hardware > Show Apple TV Remote) or use the simulator
keyboard arrow keys and Return/Enter to navigate.

- [ ] **A.2.1 (D-02):** With the chip in `ChipState.PEEK` (icon + small metric
  values visible), press D-pad Select (Return key) once. The chip transitions
  to `ChipState.EXPANDED` — all enabled metric rows (CPU, FPS, memory, network,
  etc.) become visible in the overlay.
- [ ] **A.2.2 (D-02):** With the chip expanded, press D-pad Select a second time.
  The `KamperPanel` modal slides in over the current view using
  `UIModalPresentationOverCurrentContext`. The full panel with detailed metrics
  is visible.
- [ ] **A.2.3 (D-04):** Press the Apple TV Remote Back/Menu button. The
  `KamperPanel` modal dismisses. The system handles dismissal — no custom
  in-panel close button is needed.
- [ ] **A.2.4 (D-05, D-14):** After panel dismissal, the chip is back at its
  original corner in `ChipState.PEEK` state — collapsed to icon + small values.

### A.3  Play/Pause secondary trigger (D-06)

This uses the Play/Pause button on the Siri Remote, NOT the Menu button.
Menu/Back long press is system-reserved on tvOS (exits to home screen) and
cannot be intercepted — Play/Pause is the correct secondary trigger per D-06.

- [ ] **A.3.1 (D-06):** With the chip in `ChipState.PEEK`, press and hold the
  Play/Pause button for at least 500ms then release. The `KamperPanel` opens
  directly without first transitioning through `ChipState.EXPANDED`. The long
  press threshold is 500ms (`pressesEnded` checks elapsed time >= 500ms).
- [ ] **A.3.2 (D-06):** A short tap of Play/Pause (less than 500ms) does NOT
  open the panel. The chip remains in PEEK state.

### A.4  Shake suppression (D-03)

tvOS devices have no accelerometer — shake detection must be a no-op.

- [ ] **A.4.1 (D-03):** No shake-detection log lines appear in the simulator
  console (Xcode debug console). The `tvosMain` actual for `TvosSupport.kt`
  provides empty `startShakeDetection()` / `stopShakeDetection()` — no
  `CoreMotion` or accelerometer APIs are invoked.

---

## Section B — Android TV (emulator or device)

Boot an Android TV emulator ("Android TV (1080p) API 34" AVD or higher) or
connect a Fire TV device with developer mode + ADB enabled.

Install the `samples/android` APK (or any Kamper-bearing demo APK built with
`./gradlew :samples:android:assembleDebug`).

ADB key injection commands used below:

- D-pad Up: `adb shell input keyevent 19`
- D-pad Down: `adb shell input keyevent 20`
- D-pad Left: `adb shell input keyevent 21`
- D-pad Right: `adb shell input keyevent 22`
- D-pad Select (OK): `adb shell input keyevent 23`
- KEYCODE_MENU: `adb shell input keyevent 82`
- KEYCODE_BACK: `adb shell input keyevent 4`
- Long press: `adb shell input keyevent --longpress <keycode>`

### B.1  Leanback detection and initial chip render (D-07, D-13)

- [ ] **B.1.1 (D-07):** Launch the demo app. Verify via `adb logcat` that the
  `FEATURE_LEANBACK` detection path activates. The `AndroidOverlayManager`
  lazily evaluates `context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)`.
  Add a temporary `Log.d("KamperTV", "isLeanback=$isLeanback")` if needed,
  or check that a `LeanbackComposeView` is attached (instead of a plain
  `ComposeView`).
- [ ] **B.1.2 (D-13):** The chip appears at the configured corner (default
  `BOTTOM_END`) with a ~48dp margin from the screen edge — visibly inside the
  title-safe area. The `tvOverscanPx` offset is applied as a `FrameLayout`
  pixel-space offset in `initPosition()`, independent of Compose density.
- [ ] **B.1.3 (D-12):** Chip text and icons appear noticeably larger than on a
  phone — approximately 1.5x the mobile size. This is the `TV_SCALE_FACTOR = 1.5f`
  applied via `CompositionLocalProvider(LocalDensity)` wrapping the
  `LeanbackComposeView` content.

### B.2  D-pad focus and Select two-step flow (D-08, D-09)

- [ ] **B.2.1 (D-08):** Press D-pad direction keys
  (`adb shell input keyevent 19/20/21/22`) to navigate. Eventually the chip
  receives focus, indicated by a visible BLUE focus ring border (2dp,
  `Color(0xFF1976D2)`) around the chip — `Modifier.focusable()` is applied
  on the `KamperChip` when `isTv = true`.
- [ ] **B.2.2 (D-09):** With the chip focused, press D-pad Select
  (`adb shell input keyevent 23` or the remote OK button). The chip transitions
  from `ChipState.PEEK` to `ChipState.EXPANDED` (all metric rows visible).
- [ ] **B.2.3 (D-09):** Press D-pad Select a second time. `KamperPanelActivity`
  launches as a separate Activity (the `launchPanel(activity)` helper dispatches
  an Intent). The full panel appears fullscreen.
- [ ] **B.2.4 (D-14):** Press the system Back button
  (`adb shell input keyevent 4`) to dismiss `KamperPanelActivity`. The Activity
  finishes and the chip returns to `ChipState.PEEK` at its corner.
- [ ] **B.2.5 (D-08):** Drag attempts via touch input on a touch-enabled emulator
  do NOT move the chip. On TV, `detectDragGestures` is suppressed (the
  `focusModifier` replaces the drag gesture modifier when `isTv = true` in
  `KamperChip.kt`).

### B.3  Long press secondary trigger (D-11)

Use ADB to inject long press key events. A long press is `KeyEvent.ACTION_DOWN`
with `repeatCount > 0`, which ADB `--longpress` produces.

- [ ] **B.3.1 (D-11 primary):** Long press KEYCODE_MENU:
  `adb shell input keyevent --longpress 82`.
  `KamperPanelActivity` launches directly — the chip does NOT need to be
  focused or expanded first.
- [ ] **B.3.2 (D-11 fallback):** Long press KEYCODE_BACK:
  `adb shell input keyevent --longpress 4`.
  `KamperPanelActivity` also launches. (Fire TV remotes often lack KEYCODE_MENU;
  KEYCODE_BACK is the fallback trigger for those devices.)
- [ ] **B.3.3 (regression):** A short press of KEYCODE_BACK
  (`adb shell input keyevent 4`) dismisses `KamperPanelActivity` if open,
  or performs standard back navigation otherwise — no regression in normal
  Back behavior.

### B.4  Shake detection disabled on TV (D-03 parity)

- [ ] **B.4.1 (D-03):** Confirm via `adb logcat | grep -i "SensorManager\|ShakeDetector"`
  that `SensorManager.registerListener` for `Sensor.TYPE_ACCELEROMETER` is NOT
  called from the `ShakeDetector` path after `KamperUi.show(context)` on an
  Android TV device. The `if (!isLeanback)` guard in `AndroidOverlayManager`
  skips shake detector registration on TV.

---

## Section C — React Native consumer (OPTIONAL)

This section verifies that the podspec change in Plan 15-03 Task 2 (D-19) makes
the pod installable on a tvOS consumer target. Skip if you do not have a scratch
RN tvOS Podfile handy — it is not required for Phase 15 sign-off.

- [ ] **C.1 (D-19):** In a scratch directory, create a minimal `Podfile` targeting
  `:tvos, '13.0'` that includes
  `pod 'react-native-kamper', :path => '/path/to/kamper/ui/rn'`.
  Run `pod install`. Expected: install succeeds (the pod is recognised as
  compatible with the tvOS platform). `pod install` failing with a
  platform-mismatch error is a regression against D-19.

  Note: actually building/running the consumer's tvOS app on a Simulator is NOT
  required in this phase — the XCFramework does not yet include a tvOS slice
  (deferred to Phase 16+). `pod install` success alone validates D-19.

---

## Sign-off

- [ ] Section A complete (Apple TV Simulator — all A.1 through A.4 items)
- [ ] Section B complete (Android TV emulator or device — all B.1 through B.4 items)
- [ ] Section C complete or explicitly skipped (optional RN consumer pod install)
- [ ] No deviations from D-01..D-14 / D-19 observed; OR deviations recorded in
  `.planning/phases/15-adjust-kamper-ui-including-the-react-native-package-to-suppo/15-03-SUMMARY.md`

**Verified by:** ___________________________
**Date:** ___________________________
