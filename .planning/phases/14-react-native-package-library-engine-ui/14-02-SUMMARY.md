---
phase: 14
plan: "02"
subsystem: kamper-ui
tags: [kmp, expect-actual, public-api, kotlin-native, react-native]
dependency_graph:
  requires: []
  provides: [KamperUi.show(Context), KamperUi.hide(), KamperUi.show()]
  affects: [kamper/react-native/android (Plan 04), kamper/react-native/ios (Plan 05)]
tech_stack:
  added: []
  patterns: [expect-actual-platform-extension, kotlin-internal-jvm-mangling]
key_files:
  created: []
  modified:
    - kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/KamperUi.kt
    - kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/KamperUi.kt
    - kamper/ui/android/src/appleMain/kotlin/com/smellouk/kamper/ui/KamperUi.kt
    - kamper/ui/android/src/androidUnitTest/kotlin/com/smellouk/kamper/ui/KamperUiPublicFacadeTest.kt
decisions:
  - "hide() declared on expect object (symmetric, no-arg both platforms); show() is platform-only because Android takes Context and iOS is no-arg"
  - "show(context: Context) added as non-actual public member on androidMain actual (Kotlin allows actuals to have extra members)"
  - "show() added as non-actual public member on appleMain actual for Kotlin/Native ObjC binding"
  - "TDD approach: reflection-based unit tests verify public API surface without requiring Android runtime"
  - "Kotlin internal JVM mangling: internal fun attach(context) compiles to attach$<module_name> — test updated to use startsWith check"
metrics:
  duration: "~10 minutes"
  completed: "2026-04-27T04:24:00Z"
  tasks_completed: 1
  files_modified: 4
---

# Phase 14 Plan 02: KamperUi Public show/hide Facades Summary

**One-liner:** Added `show(Context)` and `show()` platform-specific facades plus symmetric `hide()` expect/actual to `KamperUi`, enabling cross-module overlay control for the React Native TurboModule layer.

## What Was Built

Three files in the `kamper:ui:android` KMP module were updated to expose public API facades that the React Native TurboModule (Plans 04 and 05) needs to show/hide the Kamper overlay from a separate Gradle module:

| File | Change |
|------|--------|
| `commonMain/KamperUi.kt` | Added `fun hide()` to `expect object KamperUi` with asymmetry design note |
| `androidMain/KamperUi.kt` | Added `fun show(context: Context) = attach(context)` (non-actual) + `actual fun hide() = detach()` |
| `appleMain/KamperUi.kt` | Added `fun show() = attach()` (non-actual) + `actual fun hide() = detach()` |
| `androidUnitTest/KamperUiPublicFacadeTest.kt` | 5 reflection-based unit tests verifying public API surface + preservation of internal API |

## Design Decisions

### Why `show()` is NOT on `expect object KamperUi`

The two platforms require different parameter shapes:
- Android: `fun show(context: Context)` — Context is required to attach into the app window
- iOS: `fun show()` — no parameter; UIApplication.sharedApplication provides the root ViewController

`commonMain` cannot reference `android.content.Context`. Rather than compromise with `Any`, `show()` is declared as a non-`actual` public member on each platform's actual object. Kotlin allows `actual object` to have additional members beyond what `expect` declares — this is the established pattern.

### Why `hide()` IS on `expect object KamperUi`

`hide()` takes no parameters on either platform. Declaring it on `expect` allows any commonMain consumer (not just the TurboModule) to hide the overlay. This is the symmetric design — show diverges, hide converges.

### TDD Approach

Unit tests use reflection to verify the public API surface without requiring a real Android runtime (which isn't available in JVM unit tests). The test checks:
- `KamperUi.show(Context)` is public and exists
- `KamperUi.hide()` is public and exists  
- `internal fun attach(context: Context)` is preserved (may have mangled JVM name `attach$<module>`)
- `actual fun attach()` (no-arg) is preserved
- `actual fun detach()` is preserved

### Kotlin Internal JVM Mangling

Kotlin compiles `internal` visibility to a JVM-mangled method name (`attach$android_debug` in debug builds) to prevent cross-module bytecode access. The test was updated after RED-phase debugging to use `startsWith("attach")` for the internal overload reflection check.

## Compilation Verification

Both platform targets compile cleanly:
- `./gradlew :kamper:ui:android:compileDebugKotlinAndroid` — exits 0
- `./gradlew :kamper:ui:android:compileKotlinIosArm64` — exits 0

## API Surface Added

### Android (external module consumers)

```kotlin
// Call from any Gradle module that depends on kamper:ui:android
KamperUi.show(reactApplicationContext)  // attaches overlay
KamperUi.hide()                          // detaches overlay
```

### iOS (Kotlin/Native ObjC bindings)

```kotlin
// Kotlin side (called from react-native iOS TurboModule via ObjC bridge)
KamperUi.show()   // wraps attach()
KamperUi.hide()   // wraps detach()
```

ObjC symbol name for iOS TurboModule (Plan 05) will be discovered at build time. Per RESEARCH.md Open Question 2, likely `[KamperKamperUi show]` or `[KamperUiKt show]`.

## Internal Compatibility Preserved

`KamperUiInitProvider` calls `KamperUi.attach(ctx)` (the internal overload) — this is in the same Gradle module so `internal` access is still valid. No changes to this call site needed.

## Notes for Plans 04 and 05

- **Plan 04 (Android TurboModule):** Call `KamperUi.show(reactApplicationContext)` and `KamperUi.hide()` wrapped in `runOnUiThread { }` — overlay operations must happen on the main thread
- **Plan 05 (iOS TurboModule):** Call `KamperUi.show()` / `KamperUi.hide()` via Kotlin/Native ObjC binding — dispatch to main thread required (`DispatchQueue.main.async`)

## Commits

| Hash | Type | Description |
|------|------|-------------|
| 368120e | test | Add failing tests for KamperUi show/hide public facades (RED) |
| 43120db | feat | Add public show/hide facades to KamperUi expect/actual triple (GREEN) |

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed Kotlin internal JVM mangling in reflection test**
- **Found during:** GREEN phase test run
- **Issue:** Test checking for `internal fun attach(context: Context)` via `declaredMethods.find { it.name == "attach" }` returned null because Kotlin compiles `internal` to `attach$android_debug` (mangled name)
- **Fix:** Changed test to `it.name.startsWith("attach")` to handle the mangled name
- **Files modified:** `KamperUiPublicFacadeTest.kt`
- **Commit:** 43120db

**2. [Rule 3 - Blocking] Node modules symlink for RN demo composite build**
- **Found during:** Initial test run
- **Issue:** `settings.gradle.kts` includes `demos/react-native/android` as composite build; `node_modules` are gitignored and missing in worktree, causing Gradle configuration failure
- **Fix:** Created symlink `demos/react-native/node_modules -> main-repo/demos/react-native/node_modules`
- **Impact:** Worktree-local only; not committed (gitignored)

## Threat Flags

None. The changes expose `show(context)` and `hide()` as public methods on `KamperUi`. Per the plan's threat model:
- T-12-04: Already accepted — library consumers calling show() from prod is their responsibility
- T-12-05: Already mitigated — existing `context.applicationContext as Application` cast handles invalid Contexts
- T-12-06: Already accepted — repeated calls re-allocate but this is existing behavior

## Self-Check: PASSED

Files exist:
- kamper/ui/android/src/commonMain/kotlin/com/smellouk/kamper/ui/KamperUi.kt: FOUND
- kamper/ui/android/src/androidMain/kotlin/com/smellouk/kamper/ui/KamperUi.kt: FOUND
- kamper/ui/android/src/appleMain/kotlin/com/smellouk/kamper/ui/KamperUi.kt: FOUND
- kamper/ui/android/src/androidUnitTest/kotlin/com/smellouk/kamper/ui/KamperUiPublicFacadeTest.kt: FOUND

Commits exist:
- 368120e: FOUND (RED test commit)
- 43120db: FOUND (GREEN implementation commit)
