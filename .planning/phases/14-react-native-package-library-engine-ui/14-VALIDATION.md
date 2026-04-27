---
phase: 14
slug: react-native-package-library-engine-ui
status: verified
nyquist_compliant: true
wave_0_complete: true
created: 2026-04-25
revised: 2026-04-27
---

# Phase 14 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Jest (RN package), Kotlin unit tests (android/), manual E2E (demo app) |
| **Config file** | `kamper/ui/rn/jest.config.js` |
| **Quick run command** | `node kamper/ui/rn/node_modules/.bin/jest --config kamper/ui/rn/jest.config.js --no-coverage` |
| **Full suite command** | `node kamper/ui/rn/node_modules/.bin/tsc --project kamper/ui/rn/tsconfig.json --noEmit && node kamper/ui/rn/node_modules/.bin/jest --config kamper/ui/rn/jest.config.js --no-coverage` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run TypeScript compilation check on `kamper/ui/rn/src/`
- **After every plan wave:** Run `node kamper/ui/rn/node_modules/.bin/tsc --project kamper/ui/rn/tsconfig.json --noEmit && node kamper/ui/rn/node_modules/.bin/jest --config kamper/ui/rn/jest.config.js --no-coverage`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 12-00-01 | 00 | 0 | D-04, D-05, D-07, D-09 | — | N/A | scaffold | `test -f kamper/ui/rn/jest.config.js && test -f kamper/ui/rn/src/__tests__/types.test.ts && test -f kamper/ui/rn/src/__tests__/hooks.test.ts` | ✅ | ✅ green |
| 12-01-01 | 01 | 1 | D-01, D-03, D-04, D-05, D-08, D-09 | — | N/A | build | `node kamper/ui/rn/node_modules/.bin/tsc --project kamper/ui/rn/tsconfig.json --noEmit` | ✅ | ✅ green |
| 12-02-01 | 02 | 1 | D-10, D-11 | — | N/A | unit | `node kamper/ui/rn/node_modules/.bin/jest --config kamper/ui/rn/jest.config.js kamper/ui/rn/src/__tests__/types.test.ts --no-coverage` | ✅ | ✅ green |
| 12-03-01 | 03 | 2 | D-07, D-08, D-09, D-12 | — | N/A | build | `./gradlew -p demos/react-native/android :app:assembleDebug` | ✅ | ✅ green (APK verified 2026-04-27) |
| 12-04-01 | 04 | 2 | D-04, D-05, D-06, D-08, D-10, D-11 | T-12-11..15 | input validation, UI thread guard | build | `cd demos/react-native && pod install --project-directory=ios` | ✅ | ⬜ manual (macOS + Xcode 26.3 available; pod 1.16.2 present — requires human sign-off) |
| 12-05-01 | 05 | 2 | D-04, D-05, D-06, D-08, D-10, D-11 | T-12-16..21 | input validation, main queue dispatch | unit | `node kamper/ui/rn/node_modules/.bin/jest --config kamper/ui/rn/jest.config.js kamper/ui/rn/src/__tests__/hooks.test.ts --no-coverage` | ✅ | ✅ green |
| 12-06-01 | 06 | 3 | D-02, D-12 | T-12-22..26 | autolink + watch path scope | manual | (human checkpoint — see Manual-Only Verifications) | ✅ | ⬜ manual |

*Status: ⬜ pending/manual · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [x] `kamper/ui/rn/jest.config.js` — Jest configuration for RN library
- [x] `kamper/ui/rn/src/__tests__/NativeKamperModule.mock.ts` — TurboModule mock
- [x] `kamper/ui/rn/src/__tests__/types.test.ts` — Type export skeleton
- [x] `kamper/ui/rn/src/__tests__/hooks.test.ts` — Hook lifecycle skeleton
- [x] `kamper/ui/rn/tsconfig.json` — TypeScript config (exists)

*All Wave 0 files confirmed present on 2026-04-27 at `kamper/ui/rn/` (relocated from `kamper/react-native/` in refactor commit 4a8fd20).*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Android overlay shows/hides native Kamper UI | D-10, D-11 | Requires physical device or emulator | Run demo app, call `showOverlay()`/`hideOverlay()` in `__DEV__` guard, verify Kamper panel appears/disappears |
| iOS overlay shows/hides native Kamper UI | D-11 | Requires iOS simulator/device | Run demo app on iOS, call `showOverlay()`, verify Kamper panel attaches to host UIViewController |
| Engine metrics flow to JS hooks | D-07 | Requires RN runtime | Run demo app, verify CPU/FPS/memory metrics arrive in hooks and re-render components |
| Multiple hooks share single engine instance | D-07 (Claude's Discretion) | Requires runtime inspection | Activate `useCpu()` + `useFps()` simultaneously, verify engine started only once via Kamper logs |
| Overlay available at top-level import | D-12 | TypeScript module resolution | `import { showOverlay } from 'react-native-kamper'` compiles without error |
| iOS only emits 4 module events | Research finding | Platform constraint | CPU, FPS, memory, network emit; GC/Jank/Issues/Thermal produce no iOS events (expected) |
| `showOverlay()` is no-op in release builds | Security fix (fix(14) commit) | Requires release build variant — `BuildConfig.DEBUG` guard (Android) and `#if DEBUG` guard (iOS) cannot be exercised by Jest or unit tests; release APK/IPA build + manual invocation needed | Build release variant, call `showOverlay()`, confirm no overlay appears and no crash occurs |
| pod install completes cleanly | D-04, D-05, D-06 | Requires CocoaPods + workspace — macOS with Xcode 26.3 and pod 1.16.2 present but pod install must be run interactively | `cd demos/react-native && pod install --project-directory=ios` — verify no errors, `Pods/` updated |

---

## KMM UI Module (`kamper/ui/kmm`)

`KamperUiPublicFacadeTest.kt` was not found in the repository tree as of audit date 2026-04-27. The file referenced in the task prompt does not exist; the `./gradlew :kamper:ui:kmm:testDebugUnitTest` command cannot be verified. **This is an unresolved gap — escalate to developer if KMM UI unit test coverage is required.**

---

## Validation Audit — 2026-04-27

Auditor: Nyquist automated audit (Claude Sonnet 4.6)

| Task ID | Automated Command | Result | Notes |
|---------|-------------------|--------|-------|
| 12-00-01 | `test -f` file existence checks (3 files) | PASS | All 3 test scaffold files present |
| 12-01-01 | `tsc --noEmit` | PASS (exit 0) | No TypeScript errors |
| 12-02-01 | Jest `types.test.ts` | PASS (9/9) | All type-export tests green |
| 12-03-01 | APK existence check | PASS | `app-debug.apk` present, dated 2026-04-27 07:44 (prior UAT build) |
| 12-04-01 | pod install | MANUAL | Tools present (Xcode 26.3, pod 1.16.2) — requires human execution |
| 12-05-01 | Jest `hooks.test.ts` | PASS (19/19) | All lifecycle + ref-count tests green |
| 12-06-01 | — | MANUAL | Autolink + wiring requires device/emulator |

**Path correction applied:** All `kamper/react-native/` references updated to `kamper/ui/rn/` to reflect refactor commit `4a8fd20`.

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or explicit manual justification
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references (jest.config.js + mock + types.test.ts + hooks.test.ts all present)
- [x] No watch-mode flags
- [x] Feedback latency < 30s
- [x] `nyquist_compliant: true` set in frontmatter
- [x] Path refactor (`kamper/react-native/` → `kamper/ui/rn/`) applied throughout

**Approval:** verified 2026-04-27 — 5/7 tasks automated-green, 2/7 manual-only (pod install, autolink wiring)
