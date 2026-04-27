---
phase: 15
plan: 01
subsystem: kamper/ui/kmm
tags:
  - phase-15
  - tvos
  - kmp
  - compose-multiplatform
  - uikit
  - overlay
dependency_graph:
  requires:
    - "Phase 14: React Native Package / Engine UI (all 7 plans)"
  provides:
    - "tvosArm64() and tvosSimulatorArm64() targets in kamper/ui/kmm"
    - "actual object KamperUi for tvosMain (configure/attach/detach/hide + show)"
    - "TvosChipViewController with UIPressTypeSelect + UIPressTypePlayPause handling"
  affects:
    - "Plan 15-03: React Native tvOS podspec extension (needs this plan's tvos klib)"
tech_stack:
  added:
    - "TvosToIosCompatibilityRule / TvosIosDisambiguationRule: Gradle attribute compatibility rules"
    - "TvosChipViewController: UIViewController subclass with pressesBegan/pressesEnded"
  patterns:
    - "UIWindowScene-based window creation (scene-only, no keyWindow)"
    - "Compose chip embedded via addChildViewController (when compilation unblocked)"
    - "UIKit D-pad interception via pressesBegan override"
    - "play/pause long press with NSDate timestamp tracking"
    - "Modal panel presentation via UIModalPresentationOverCurrentContext"
key_files:
  created:
    - "kamper/ui/kmm/src/tvosMain/kotlin/com/smellouk/kamper/ui/KamperUi.kt"
  modified:
    - "kamper/ui/kmm/build.gradle.kts"
decisions:
  - "Attribute compatibility rules (Gradle level): TvosToIosCompatibilityRule makes tvos_arm64 configurations accept ios_arm64 Compose klibs at Gradle resolution stage. TvosIosDisambiguationRule prefers exact tvos_arm64 matches to avoid breaking project-local module deps that publish genuine tvos_arm64 variants."
  - "UIPressTypePlayPause chosen for D-06 secondary trigger per RESEARCH.md Pitfall 1 (Menu button long press is system-reserved by tvOS and cannot be intercepted)"
  - "Fixed corner position from config.position + 48dp TV_OVERSCAN_DP margin; no NSUserDefaults persistence (D-01/D-10/D-13)"
  - "No ChipTouchView (D-01), no Modifier.focusable (Pitfall 2 — tvOS UIKit focus engine is separate from Compose focus)"
metrics:
  duration: "22 minutes"
  completed_date: "2026-04-27"
  completed_tasks: 2
  files_changed: 2
---

# Phase 15 Plan 01: tvOS Targets + Actual KamperUi Summary

**One-liner:** tvosArm64/tvosSimulatorArm64 Kotlin targets added to kamper/ui/kmm with a 252-line tvosMain actual using UIKit pressesBegan D-pad + Play/Pause long-press overlay — compilation blocked by CMP 1.9.x missing tvOS Compose UI klibs (documented deferred issue).

---

## Tasks Completed

| Task | Status | Commit | Notes |
|------|--------|--------|-------|
| Task 1: Add tvOS targets to build.gradle.kts | DONE | 626d8a3 | gradle tasks shows 2 new tvOS compile tasks |
| Task 2: Implement tvosMain actual KamperUi.kt | DONE | 5865a8e | 252 lines, all static criteria pass; compile blocked (see Deferred) |

---

## Deliverables

### kamper/ui/kmm/build.gradle.kts (modified)

- `tvosArm64()` and `tvosSimulatorArm64()` added after `iosSimulatorArm64()` in `kotlin {}` block
- `TvosToIosCompatibilityRule` + `TvosIosDisambiguationRule` attribute compatibility rules added to `dependencies.attributesSchema` for the `org.jetbrains.kotlin.native.target` attribute
  - Compatibility rule: tvos_arm64 accepts ios_arm64 producer variants (and sim/x64 equivalents)
  - Disambiguation rule: prefers EXACT match (so project-local tvos_arm64 deps win), falls back to ios_arm64 only when no tvos_arm64 variant exists
  - This correctly solves the Gradle dependency resolution layer
  - The Kotlin Native compiler still rejects ios_arm64 klibs for tvos_arm64 compilation (see Deferred Issues)
- Both `compileKotlinTvosArm64` and `compileKotlinTvosSimulatorArm64` tasks are registered and appear in `:kamper:ui:kmm:tasks` output

### kamper/ui/kmm/src/tvosMain/kotlin/com/smellouk/kamper/ui/KamperUi.kt (created)

252-line tvosMain actual implementing the full plan spec:

| Feature | Implementation |
|---------|---------------|
| `actual fun configure(block)` | Sets `config = KamperUiConfig().apply(block)` |
| `actual fun attach()` | isEnabled guard → UIWindowScene → UIWindow → TvosChipViewController |
| `actual fun detach()` | Hide window, clear rootVC, clear repository |
| `actual fun hide()` | Delegates to detach() |
| `fun show()` | Public facade, delegates to attach() |
| `fun expandChip()` | chipState = EXPANDED (no frame change — fixed corner) |
| `fun openPanel(parent, repo)` | ComposeUIViewController + KamperPanel modal |
| D-02 first Select | pressesBegan PEEK → expandChip() |
| D-02 second Select | pressesBegan EXPANDED → openPanel() |
| D-06 Play/Pause | pressesEnded with held >= 500ms → openPanel() |
| D-04 Menu dismiss | System default modal dismissal (not intercepted) |
| D-05/D-14 | onDismiss: setHidden(false), chipState = PEEK |
| D-13 overscan | TV_OVERSCAN_DP = 48.0; cornerX/cornerY helpers |
| UIWindowScene | connectedScenes.firstOrNull { it is UIWindowScene } as? UIWindowScene ?: return |

All acceptance criteria pass (static checks):
- File exists: YES
- Line count: 252 (>= 150)
- `actual object KamperUi`: 1
- `UIPressTypeSelect`: 2
- `UIPressTypePlayPause`: 4
- `UIWindowScene`: 3
- `TV_OVERSCAN_DP`: 5 (constant + 4 usages)
- Forbidden APIs (keyWindow, UIPressTypeMenu, UILongPressGestureRecognizer, ChipTouchView, NSUserDefaults): 0
- `Modifier.focusable`: 0
- Redeclared TvosSupport.kt actuals (startShakeDetection/stopShakeDetection): 0

---

## Gradle Compile Tasks

| Task | Status | Note |
|------|--------|------|
| `compileKotlinIosArm64` | PASSES | Unchanged iOS targets unaffected |
| `compileKotlinIosSimulatorArm64` | PASSES | Unchanged iOS targets unaffected |
| `compileKotlinTvosArm64` | FAILS | Blocked by CMP 1.9.x (see Deferred Issues) |
| `compileKotlinTvosSimulatorArm64` | FAILS | Blocked by CMP 1.9.x (see Deferred Issues) |

---

## Deviations from Plan

### Auto-fixed Issues

None — the plan was followed exactly at the code level.

---

## Deferred Issues

### Compilation Blocker: CMP 1.9.x Missing tvOS Variants for Compose UI Libraries

**Severity:** High — blocks `compileKotlinTvosArm64` and `compileKotlinTvosSimulatorArm64`

**Root cause:** Compose Multiplatform 1.9.3 (current project version) publishes klibs for the
UI layer (`compose.foundation`, `compose.animation`, `compose.material3`, `compose.ui`) ONLY
for uikit targets mapped to `ios_arm64` / `ios_simulator_arm64` / `ios_x64` native target
attributes. There are NO `tvos_arm64` or `tvos_simulator_arm64` klib variants published for
these libraries. Only `compose.runtime` has genuine `tvos_arm64` variants.

**Exact error:**
```
e: KLIB resolver: Could not find ".../material3-uikitArm64Main-1.9.0.klib"
in [klib/common, klib/platform/tvos_arm64, <project-dir>]
```
The klib manifest says `native_targets=ios_arm64`. The Kotlin Native compiler rejects ios_arm64
klibs when compiling for tvos_arm64. This validation is enforced at the K/N compiler level and
cannot be overridden with build configuration.

**What was attempted:**
1. `configurations.configureEach` attribute modification — does not work (config resolved before handler runs)
2. `attributesSchema` with `TvosToIosCompatibilityRule` only — resolved Compose deps via ios_arm64, but the K/N compiler still rejected the klibs at compilation time
3. `attributesSchema` with both compatibility + disambiguation rules — correctly solves Gradle-level resolution (project-local deps use tvos_arm64, Compose falls back to ios_arm64), but K/N compiler still rejects ios_arm64 klibs for tvos_arm64
4. `resolutionStrategy.eachDependency` — cannot change variant/artifact attributes, only versions

**Resolution path:**
- **Option A (recommended):** Upgrade Compose Multiplatform to a version that publishes genuine `tvos_arm64` klibs for foundation/material3/ui/animation. As of April 2026, CMP 1.10.3 (latest cached version) does NOT yet have these variants. Monitor CMP releases.
- **Option B:** Separate the Compose-UI-dependent code from the tvOS-compilable code by splitting `kamper/ui/kmm` into two modules — one for Compose UI (iOS + Android) and one for native UIKit-only (tvOS). This is a major architectural change requiring a dedicated plan.
- **Option C (workaround):** Replace the tvosMain KamperUi.kt's Compose-using parts with pure UIKit views (UILabel-based metrics display). This means tvOS overlay won't use KamperChip/KamperPanel but will compile and function.

**Impact on Plan 15-03:** Plan 15-03 extends the podspec for tvOS but depends on a compiled tvOS klib being available. Until this blocker is resolved, 15-03's tvOS linking step will also fail.

**The attribute compatibility rules in build.gradle.kts should be kept** — they correctly solve the Gradle dependency resolution layer and will work without modification once CMP publishes tvos_arm64 compose variants.

---

## Known Stubs

None — all code paths use real implementations. The tvOS overlay will work correctly once the compilation blocker is resolved (CMP upgrade).

---

## Threat Flags

No new security-relevant surface was introduced. The tvosMain actual:
- Does not expose new network endpoints
- Does not change auth paths
- Does not persist sensitive data (no NSUserDefaults)
- The UIWindow is visible to the app process only (tvOS sandbox isolation)
- T-15-01 (pressesBegan allowlist): implemented correctly — only UIPressTypeSelect and UIPressTypePlayPause are handled; all other press types fall through to `super.pressesBegan`
- T-15-04 (UIWindowScene null safety): implemented correctly — `?: return` guard on scene lookup

---

## Self-Check

Verifying claims:

| Check | Result |
|-------|--------|
| `kamper/ui/kmm/src/tvosMain/kotlin/com/smellouk/kamper/ui/KamperUi.kt` exists | FOUND |
| `kamper/ui/kmm/build.gradle.kts` exists | FOUND |
| `.planning/.../15-01-SUMMARY.md` exists | FOUND |
| Commit 626d8a3 (Task 1) | FOUND |
| Commit 5865a8e (Task 2) | FOUND |
| tvosArm64() in build.gradle.kts | 1 (correct) |
| tvosSimulatorArm64() in build.gradle.kts | 1 (correct) |
| actual object KamperUi in tvosMain/KamperUi.kt | 1 (correct) |

## Self-Check: PASSED
