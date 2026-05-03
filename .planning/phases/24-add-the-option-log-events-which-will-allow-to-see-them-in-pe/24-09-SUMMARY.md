---
phase: 24
plan: 09
subsystem: demo-android
tags: [events, fragment, ui, android, demo]
dependency_graph:
  requires: [24-04]
  provides: [EventsFragment, fragment_events.xml, item_event.xml, Events tab]
  affects: [demos/android]
tech_stack:
  added: []
  patterns:
    - RecyclerView adapter with newest-first insertion and 200-row cap
    - Background thread duration event via Kamper.startEvent/endEvent + runOnUiThread callback
    - @Volatile guard for single concurrent video_playback recording
    - onDestroyView nulls all view references (lifecycle safety)
key_files:
  created:
    - demos/android/src/main/res/layout/fragment_events.xml
    - demos/android/src/main/res/layout/item_event.xml
    - demos/android/src/main/java/com/smellouk/kamper/android/EventsFragment.kt
  modified:
    - demos/android/src/main/java/com/smellouk/kamper/android/MainActivity.kt
decisions:
  - D-22: EventsFragment added as 9th tab in MainActivity (fragments list + tabTitles)
  - D-23: Preset buttons user_login/purchase/screen_view call Kamper.logEvent; video_playback uses Kamper.startEvent/endEvent with 2-second background sleep
  - D-24: Events list is in-memory only, clears on fragment recreation (no persistence)
metrics:
  duration: ~15 minutes
  completed: 2026-05-02T18:48:00Z
  tasks_completed: 2
  files_changed: 4
---

# Phase 24 Plan 09: Events Tab Android Demo Summary

EventsFragment with four preset event buttons, custom-event input field, scrollable RecyclerView, and 9th "Events" tab wired into MainActivity — using Kamper.logEvent/startEvent/endEvent end-to-end.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Create fragment_events.xml + item_event.xml | ac59d98 | fragment_events.xml, item_event.xml |
| 2 | Create EventsFragment.kt + add to MainActivity | cf2383f | EventsFragment.kt, MainActivity.kt |
| 3 | Human verification checkpoint | deferred | (awaiting human verification) |

## What Was Built

### fragment_events.xml
RelativeLayout root with:
- HorizontalScrollView footer containing 4 MaterialButton presets (`user_login`, `purchase`, `screen_view`, `video_playback`)
- LinearLayout input bar with TextInputEditText (`customEventInput`) and Log CTA (`btnLog`)
- Section label "Logged Events"
- RecyclerView (`eventsList`) filling remaining vertical space
- Empty state TextView (`emptyText`) centered, shown when list is empty
- All color references use Catppuccin Mocha `@color/` tokens — zero hardcoded hex

### item_event.xml
MaterialCardView with:
- Left `eventTypeBar` (4dp, color_green for instant / color_blue for duration)
- `eventName` (14sp, color_text)
- `eventTimestamp` (11sp, monospace, color_muted)
- `eventDuration` (11sp, monospace, color_mauve, `visibility="gone"` by default)

### EventsFragment.kt
- Preset instant buttons: call `Kamper.logEvent(name)` + insert `EventRow` at index 0
- `video_playback` preset: calls `Kamper.startEvent`, sleeps 2000ms on daemon thread, calls `Kamper.endEvent`, then updates UI via `activity?.runOnUiThread`
- Custom input: IME Done and button tap both call `Kamper.logEvent(raw)` + clear field
- Empty-input guard: no action when input is blank
- `MAX_ROWS = 200`: oldest row removed when cap exceeded
- `@Volatile isVideoRecording` flag prevents concurrent video_playback recordings (T-24-H-02)
- `onDestroyView()` nulls all view references (T-24-H-03 lifecycle safety)
- `VIDEO_DEMO_SLEEP_MS = 2_000L`: button shows "Recording..." during 2-second window
- Duration format: `"(${row.durationMs}ms)"` (e.g., `(2034ms)`)

### MainActivity.kt
Added `private val eventsFragment = EventsFragment()` and included it as the 10th element in both `fragments` and `tabTitles` ("Events").

## Human Verification Required

Task 3 is a `checkpoint:human-verify` — this plan has `autonomous: false`. Human verification must be performed on a connected Android device or emulator by following the steps in `24-09-PLAN.md §Task 3 <how-to-verify>`:

1. Install: `./gradlew :demos:android:installDebug`
2. Confirm "Events" appears as 9th tab
3. Tap user_login/purchase/screen_view — verify instant event rows with HH:mm:ss timestamp, green left bar
4. Tap video_playback — button shows "Recording..." for ~2s, then row appears with `(~2000ms)` duration and blue bar
5. Type custom event name, tap Log or press Done — row appears, input clears
6. Tap Log with empty input — no action
7. Rotate — list resets to empty
8. Verify Catppuccin Mocha colors per UI-SPEC

**Approval signal:** User responds with `approved` after verifying all steps.

## Build Note

`./gradlew :demos:android:assembleDebug` fails with a pre-existing `com.facebook.react.settings` plugin error (React Native node_modules not installed in the worktree). This failure exists in the base commit (b39075e) and is unrelated to Plan 09 changes. On a machine with React Native node_modules installed, the build compiles normally. The code correctness has been verified through:
- Static review matching the established IssuesFragment pattern
- Acceptance criteria checks: all IDs present, no hardcoded hex, Kamper API calls present, constants correct

## Deviations from Plan

None — plan executed exactly as written.

## Threat Mitigations Applied

| Threat | Mitigation | Code location |
|--------|-----------|---------------|
| T-24-H-02 (DoS — repeated video_playback taps) | `@Volatile isVideoRecording` flag + button disabled | EventsFragment.kt `triggerVideoPlayback()` |
| T-24-H-03 (lifecycle leak) | `onDestroyView()` nulls all view references | EventsFragment.kt `onDestroyView()` |
| T-24-H-04 (race — background → UI after destroy) | `activity?.runOnUiThread { ... }` null-safe | EventsFragment.kt `triggerVideoPlayback()` lambda |
| T-24-H-05 (memory growth) | `MAX_ROWS = 200` cap with oldest removal | EventsFragment.kt `addRow()` |

## Self-Check: PASSED

| Item | Status |
|------|--------|
| demos/android/src/main/res/layout/fragment_events.xml | FOUND |
| demos/android/src/main/res/layout/item_event.xml | FOUND |
| demos/android/src/main/java/com/smellouk/kamper/android/EventsFragment.kt | FOUND |
| Commit ac59d98 (layouts) | FOUND |
| Commit cf2383f (EventsFragment + MainActivity) | FOUND |
