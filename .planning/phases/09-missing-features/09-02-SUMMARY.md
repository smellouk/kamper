---
phase: 09-missing-features
plan: "02"
subsystem: kamper-ui-android
tags: [feat-02, broadcast-receiver, adb, android, intent, security]
dependency_graph:
  requires: []
  provides: [KamperConfigReceiver, com.smellouk.kamper.CONFIGURE intent handler]
  affects: [kamper/ui/android/src/androidMain/AndroidManifest.xml]
tech_stack:
  added: []
  patterns: [BroadcastReceiver delegation, AndroidManifest <receiver> entry, MockK mockkObject for Android singletons]
key_files:
  created:
    - kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/KamperConfigReceiver.kt
    - (replaced stub) kamper/ui/android/src/androidTest/kotlin/com/smellouk/kamper/ui/KamperConfigReceiverTest.kt
  modified:
    - kamper/ui/android/src/androidMain/AndroidManifest.xml
decisions:
  - "Used mockk<Context>(relaxed=true) instead of ApplicationProvider — androidx.test.core not in project classpath; Context is unused by the receiver body"
  - "Used FQN com.smellouk.kamper.ui.KamperConfigReceiver in manifest (not leading-dot shorthand) — the manifest has no package attribute so leading-dot would resolve incorrectly"
  - "Detekt pre-existing 109-issue failure is out of scope — detekt excludes **/android/ paths so KamperConfigReceiver is not analyzed; failure predates this plan"
metrics:
  duration: "4 minutes"
  completed: "2026-04-26"
  tasks_completed: 2
  tasks_total: 2
  files_created: 2
  files_modified: 1
---

# Phase 09 Plan 02: KamperConfigReceiver (FEAT-02) Summary

**One-liner:** BroadcastReceiver toggling Kamper.start/stop via ADB-sendable `com.smellouk.kamper.CONFIGURE` intent with `android:exported="false"` and safe missing-extra default.

## Tasks Completed

| Task | Name | Commit | Files |
|------|------|--------|-------|
| 1 | Create KamperConfigReceiver + manifest entry | f70899a | KamperConfigReceiver.kt (new), AndroidManifest.xml |
| 2 | Add KamperConfigReceiverTest (3 tests) | 4872212 | KamperConfigReceiverTest.kt (stub replaced) |

## What Was Built

### KamperConfigReceiver

A minimal `BroadcastReceiver` (~15 lines) that reads the `enabled` Boolean extra from an inbound intent and delegates to `Kamper.start()` or `Kamper.stop()`:

```
adb shell am broadcast -a com.smellouk.kamper.CONFIGURE --ez enabled false
```

Key design properties:
- **Safe default:** `getBooleanExtra("enabled", true)` — missing or malformed extra defaults to enabling monitoring, not disabling it (D-07, T-7-02 mitigated).
- **No held state:** receiver is a pure delegation shim; nothing is stored.
- **Package-public class:** required for OS instantiation; cannot be `internal`.
- **No custom permission:** `android:exported="false"` restricts callers to same-UID and ADB shell.

### Manifest Entry

Registered inside `<application>` with fully-qualified name and action filter:

```xml
<receiver
    android:name="com.smellouk.kamper.ui.KamperConfigReceiver"
    android:exported="false">
    <intent-filter>
        <action android:name="com.smellouk.kamper.CONFIGURE" />
    </intent-filter>
</receiver>
```

### KamperConfigReceiverTest

Replaced Wave 0 stub with 3 instrumented tests covering:
1. `enabled=true` → `Kamper.start()` called exactly once, `stop()` never.
2. `enabled=false` → `Kamper.stop()` called exactly once, `start()` never.
3. Missing extra → `Kamper.start()` called exactly once (safe default per D-07).

## androidTest Source Set

The `kamper/ui/android` module already had an `androidTest` source set populated (evidenced by `AndroidOverlayManagerTest.kt`). The root `build.gradle.kts` `afterEvaluate` block auto-configures `androidInstrumentedTest` with MockK. No build.gradle.kts modification was needed.

## Instrumented Test Run

No connected device was available in the executor environment. The closest automatable signal — `compileDebugAndroidTestKotlin` — exits 0 (all 3 test methods compile cleanly). Manual validation on a connected device before the phase ships is required per plan acceptance criteria.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Replaced ApplicationProvider with mockk<Context>**
- **Found during:** Task 2 compile step
- **Issue:** `androidx.test.core.app.ApplicationProvider` is not in the project classpath — `Unresolved reference 'test'` and `Unresolved reference 'ApplicationProvider'` compile errors.
- **Fix:** Replaced `ApplicationProvider.getApplicationContext<Context>()` with `mockk<Context>(relaxed = true)`. The receiver body does not use `context` at all, so a relaxed mock is functionally equivalent for these tests.
- **Files modified:** `KamperConfigReceiverTest.kt`
- **Commit:** 4872212

### Pre-existing Issues (Out of Scope)

**Detekt 109-issue failure** — present on the base commit before any plan changes (verified by stash+run). The detekt task excludes `**/android/` paths so `KamperConfigReceiver.kt` is not analyzed. This failure is deferred to the team's existing backlog.

## Known Stubs

None — all three test methods are fully implemented with real MockK assertions.

## Threat Flags

No new threat surface beyond the plan's declared `<threat_model>`. The `<receiver>` entry with `android:exported="false"` mitigates T-7-01 (Spoofing/Tampering). T-7-02 (malformed extra) mitigated by safe default. T-7-03 (DoS intent flooding) mitigated by existing `Watcher.startWatching` idempotency guard.

## Self-Check

**Created files exist:**
- `kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/KamperConfigReceiver.kt` — FOUND
- `kamper/ui/android/src/androidTest/kotlin/com/smellouk/kamper/ui/KamperConfigReceiverTest.kt` — FOUND

**Commits exist:**
- `f70899a` feat(09-02): add KamperConfigReceiver + manifest entry for FEAT-02 — FOUND
- `4872212` feat(09-02): add KamperConfigReceiverTest — 3 tests for FEAT-02 receiver — FOUND

## Self-Check: PASSED
