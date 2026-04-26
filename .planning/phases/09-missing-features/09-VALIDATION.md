---
phase: 9
slug: missing-features
status: verified
nyquist_compliant: false
nyquist_partial: true
wave_0_complete: true
automated_covered: 13
manual_only: 3
created: 2026-04-25
last_audited: 2026-04-26
---

# Phase 9 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | kotlin.test (commonTest) + JUnit 4 (androidTest via Mokkery/MockK) |
| **Config file** | per-module `build.gradle.kts` (no single root config) |
| **Quick run command** | `./gradlew :kamper:engine:jvmTest :kamper:modules:cpu:jvmTest` |
| **Full suite command** | `./gradlew :kamper:engine:allTests :kamper:modules:cpu:allTests :kamper:api:allTests :kamper:ui:android:connectedAndroidTest` |
| **Estimated runtime** | ~60 seconds (JVM targets); ~3 minutes (with connected Android tests) |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :kamper:engine:jvmTest :kamper:modules:cpu:jvmTest`
- **After every plan wave:** Run `./gradlew :kamper:engine:allTests :kamper:modules:cpu:allTests :kamper:api:allTests`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 60 seconds (JVM targets only; connected Android tests at wave boundary)

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 7-01-01 | 01 | 1 | FEAT-01 UNSUPPORTED constant (Wave 0 stub) | — | N/A | unit | `./gradlew :kamper:modules:cpu:jvmTest` | ✅ CpuInfoUnsupportedTest (3 tests) | ✅ green |
| 7-01-02 | 01 | 1 | FEAT-01 UNSUPPORTED constants (numeric Info subclasses) | — | N/A | unit | `./gradlew :kamper:modules:fps:jvmTest :kamper:modules:memory:jvmTest` | ✅ CpuInfoUnsupportedTest (3 tests) | ✅ green |
| 7-01-03 | 01 | 1 | FEAT-01 ThermalInfo/IssueInfo + Performance.lastValidSampleAt + CpuInfoUnsupportedTest | — | N/A | unit | `./gradlew :kamper:modules:cpu:jvmTest --tests "com.smellouk.kamper.cpu.CpuInfoUnsupportedTest"` | ✅ CpuInfoUnsupportedTest (3 tests) | ✅ green |
| 7-02-01 | 02 | 1 | FEAT-02 receiver class + manifest entry | T-7-01, T-7-02, T-7-03 | `android:exported="false"` blocks external intents | unit | `./gradlew :kamper:ui:android:assembleDebug :kamper:ui:android:compileDebugAndroidTestKotlin` | ✅ KamperConfigReceiver.kt + manifest | ✅ green |
| 7-02-02 | 02 | 1 | FEAT-02 KamperConfigReceiverTest (enabled=true/false/default) | T-7-01, T-7-02 | exported=false + safe default | androidTest | `./gradlew :kamper:ui:android:connectedDebugAndroidTest --tests "com.smellouk.kamper.ui.KamperConfigReceiverTest"` | ✅ KamperConfigReceiverTest (3 tests, compiles) | ⚠️ manual-only |
| 7-03-01 | 03 | 2 | FEAT-01 CpuInfoRepositoryImpl probe | — | N/A | unit | `./gradlew :kamper:modules:cpu:assembleDebug :kamper:modules:cpu:compileDebugAndroidTestKotlin` | ✅ CpuInfoRepositoryImpl.kt | ✅ green |
| 7-03-02 | 03 | 2 | FEAT-01 CpuInfoRepositoryImplUnsupportedTest | — | N/A | androidTest | `./gradlew :kamper:modules:cpu:connectedDebugAndroidTest --tests "com.smellouk.kamper.cpu.repository.CpuInfoRepositoryImplUnsupportedTest"` | ✅ CpuInfoRepositoryImplUnsupportedTest (3 tests, compiles) | ⚠️ manual-only |
| 7-04-01 | 04 | 2 | FEAT-01 UI state + listener wiring | — | N/A | unit | `./gradlew :kamper:ui:android:compileDebugKotlin :kamper:ui:android:assembleDebug` | ✅ KamperUiState.kt + ModuleLifecycleManager.kt | ✅ green |
| 7-04-02 | 04 | 2 | FEAT-01 MetricCard unsupported branch + ThermalState.UNSUPPORTED arm | — | N/A | unit | `./gradlew :kamper:ui:android:assembleDebug :kamper:ui:android:lintDebug` | ✅ PanelComponents.kt + ActivityTab.kt | ✅ green |
| 7-04-03 | 04 | 2 | FEAT-01 visual verification | — | N/A | manual | See Manual-Only Verifications | N/A | ⚠️ manual-only |
| 7-05-01 | 05 | 2 | FEAT-03 currentApiTimeMs expect/actual (7 platforms) | — | N/A | unit | `./gradlew :kamper:api:compileCommonMainKotlinMetadata :kamper:api:jvmTest` | ✅ PlatformTime.kt (8 actuals) | ✅ green |
| 7-05-02 | 05 | 2 | FEAT-03 IWatcher onSampleDelivered callback wiring | — | N/A | unit | `./gradlew :kamper:api:jvmTest` | ✅ WatcherTest (5 tests, incl. 2 new) | ✅ green |
| 7-05-03 | 05 | 2 | FEAT-03 WatcherTest onSampleDelivered fires/skips | — | N/A | unit | `./gradlew :kamper:api:jvmTest --tests "com.smellouk.kamper.api.WatcherTest"` | ✅ WatcherTest (5 tests) | ✅ green |
| 7-06-01 | 06 | 3 | FEAT-03 ValidationInfo + engineCurrentTimeMs (7 platforms) | — | N/A | unit | `./gradlew :kamper:engine:compileCommonMainKotlinMetadata :kamper:engine:jvmTest` | ✅ ValidationInfo.kt + EnginePlatformTime.kt | ✅ green |
| 7-06-02 | 06 | 3 | FEAT-03 Engine.validate() + init/clear seeding | — | N/A | unit | `./gradlew :kamper:engine:jvmTest` | ✅ EngineValidateTest (6 tests) + EngineTest (14 tests) | ✅ green |
| 7-06-03 | 06 | 3 | FEAT-03 EngineValidateTest + EngineTest extension | — | N/A | unit | `./gradlew :kamper:engine:jvmTest` | ✅ EngineValidateTest (6 tests) | ✅ green |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ manual-only · 🔶 flaky*

---

## Wave 0 Requirements

All Wave 0 test stub files have been populated with real test bodies during phase execution.

- [x] `kamper/engine/src/commonTest/kotlin/com/smellouk/kamper/EngineValidateTest.kt` — 6 tests, all passing
- [x] `kamper/modules/cpu/src/commonTest/kotlin/com/smellouk/kamper/cpu/CpuInfoUnsupportedTest.kt` — 3 tests, all passing
- [x] `kamper/modules/cpu/src/androidTest/kotlin/com/smellouk/kamper/cpu/repository/CpuInfoRepositoryImplUnsupportedTest.kt` — 3 tests, compiles, device required
- [x] `kamper/ui/android/src/androidTest/kotlin/com/smellouk/kamper/ui/KamperConfigReceiverTest.kt` — 3 tests, compiles, device required

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| KamperConfigReceiverTest (enabled=true/false/default) | FEAT-02 SC-2, T-7-02 | androidTest — requires connected device | `./gradlew :kamper:ui:android:connectedDebugAndroidTest --tests "com.smellouk.kamper.ui.KamperConfigReceiverTest"` — file exists and compiles; run pre-merge on device |
| CpuInfoRepositoryImplUnsupportedTest (probe + cache + pitfall-6) | FEAT-01 SC-1, T-7-08 | androidTest — requires connected device; `/proc/stat` behaviour is device-specific | `./gradlew :kamper:modules:cpu:connectedDebugAndroidTest --tests "com.smellouk.kamper.cpu.repository.CpuInfoRepositoryImplUnsupportedTest"` — file exists and compiles; run pre-merge on device |
| KamperPanel displays gray "Unsupported" tile | FEAT-01 SC-1, D-14/D-15 | Visual Compose UI — no screenshot test infra in place | Build app with mocked `CpuInfo.UNSUPPORTED` as CPU state; visually verify tile is gray with "Unsupported" label, no progress bar fill, distinct from "--" INVALID display |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references — stubs populated with real tests
- [x] No watch-mode flags
- [x] Feedback latency < 60s (JVM targets); androidTest at wave boundary
- [x] `nyquist_partial: true` set in frontmatter (13 automated, 3 manual-only)
- [x] `wave_0_complete: true` set in frontmatter

**Approval:** verified 2026-04-26 (13 automated green, 3 manual-only documented)

---

## Validation Audit 2026-04-26

| Metric | Count |
|--------|-------|
| Gaps found | 2 |
| Resolved (automated) | 0 |
| Moved to manual-only | 2 |
| Already manual | 1 |
| Total automated green | 13 |
