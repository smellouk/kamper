---
phase: 14
slug: react-native-package-library-engine-ui
status: verified
threats_open: 0
asvs_level: 1
created: 2026-04-27
---

# Phase 14 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| JS → KamperTurboModule.start (JSI) | Untrusted KamperConfig crosses JSI boundary | Boolean module-enable flags; validated by ReadableMap.flag() / SpecStartConfig |
| TurboModule.showOverlay → KamperUi | Privileged UIKit/overlay operation reachable from JS | Debug-only; guarded by BuildConfig.DEBUG (Android) and #if DEBUG (iOS) |
| Listener callbacks → emitOnXxx | 8 metric listeners run on Kamper background threads | Numeric metric payloads; WritableMap/NSDictionary construction is thread-safe |
| Jest moduleNameMapper → mock module | Test-only redirect; production bundle never resolves | Mock-only; no production path |

---

## Threat Register

| Threat ID | Category | Component | Disposition | Mitigation | Status |
|-----------|----------|-----------|-------------|------------|--------|
| T-12-W0-01 | Tampering | Jest mock vs Codegen spec | mitigate | Mock exports exactly 4 imperative fn + 8 emitters, 1:1 match with NativeKamperModule.ts spec; enforced by acceptance criteria grep count | closed |
| T-12-W0-02 | Information Disclosure | moduleNameMapper test redirect | accept | Redirect is jest.config.js test-time only; production Metro bundle never resolves the mock path | closed |
| T-12-04 | Information Disclosure | KamperUi.show() reachable from prod | accept | Library consumers calling show() from production is their responsibility; documented | closed |
| T-12-05 | Tampering | Invalid Context passed to show(context) | mitigate | KamperUi.kt (androidMain) line 21: `context.applicationContext as Application` cast; reactApplicationContext always provides valid applicationContext | closed |
| T-12-06 | DoS | Repeated show() calls | accept | Re-allocation is existing pre-phase behavior; documented | closed |
| T-12-07 | DoS | Event handler exceptions | accept | Handler exceptions don't propagate to native — React Native design guarantee | closed |
| T-12-08 | DoS | useIssues unbounded list growth | mitigate | useIssues.ts: `.slice(0, 100)` caps list; useKamper.ts applies same cap | closed |
| T-12-09 | Information Disclosure | showOverlay/hideOverlay reachable from prod JS | mitigate | Android: `BuildConfig.DEBUG` guard returns early in release builds. iOS: `#if DEBUG` compile-time guard strips overlay calls from release binary. Fixed in UAT verification pass. | closed |
| T-12-10 | Elevation | _acquireEngine/_releaseEngine internal helpers | accept | @internal JSDoc; not exported from index.ts barrel | closed |
| T-12-11 | Tampering | start(config: ReadableMap) malformed JS | mitigate | ReadableMap.flag() extension: hasKey() + getType(key)==ReadableType.Boolean before getBoolean(); non-boolean/missing → true (permissive default) | closed |
| T-12-12 | Information Disclosure | showOverlay in Android release builds | mitigate | kamper:ui:kmm declared as debugImplementation — KamperUi class absent from release APK; additionally guarded by T-12-09 BuildConfig.DEBUG early-return | closed |
| T-12-13 | DoS | UI thread blocking from Android showOverlay | mitigate | UiThreadUtil.runOnUiThread{} posts async to main thread — JS thread freed immediately | closed |
| T-12-14 | Spoofing | Module name "KamperModule" mismatch | mitigate | KamperTurboModule.NAME constant is single source of truth; KamperTurboPackage references NAME not a literal | closed |
| T-12-15 | Elevation | Untrusted JS → ANR detector chain | accept | IssuesModule: chainToPreviousHandler=false per existing demo pattern; documented | closed |
| T-12-16 | Tampering | iOS start config decoding | mitigate | Codegen SpecStartConfig is typed C++ struct with folly::Optional<bool> fields; flagOrTrue() defaults missing flags to YES | closed |
| T-12-17 | Information Disclosure | showOverlay in iOS App Store builds | mitigate | #if DEBUG guard (T-12-09 fix) strips overlay from release binary; JSDoc advisory in Kamper.ts | closed |
| T-12-18 | DoS | Main queue blocked by iOS showOverlay | mitigate | dispatch_async(dispatch_get_main_queue()) — non-blocking; JS thread freed immediately | closed |
| T-12-19 | Spoofing | iOS moduleName mismatch | mitigate | Single `@"KamperModule"` literal in `+ (NSString *)moduleName`; matches Android NAME and Codegen key | closed |
| T-12-20 | Tampering | _bridge double-init iOS | mitigate | `if (_bridge) return;` guard at top of start: makes it idempotent | closed |
| T-12-21 | Information Disclosure | iOS XCFramework symbol visibility | accept | App Store stripping is consumer responsibility; out of scope for this library | closed |

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-14-01 | T-12-W0-02 | moduleNameMapper is jest.config.js only; Metro bundler uses a separate resolution path | gsd-secure-phase | 2026-04-27 |
| AR-14-02 | T-12-04 | KamperUi public API is intentional; overlay is a debug tool, consumer responsibility | gsd-secure-phase | 2026-04-27 |
| AR-14-03 | T-12-06 | Re-allocation on repeated show() is pre-existing KamperUi behavior, not introduced by this phase | gsd-secure-phase | 2026-04-27 |
| AR-14-04 | T-12-07 | React Native guarantees handler exceptions are caught and logged by the JS runtime, not propagated | gsd-secure-phase | 2026-04-27 |
| AR-14-05 | T-12-10 | _acquireEngine/_releaseEngine are module-internal helpers; @internal + barrel exclusion is sufficient | gsd-secure-phase | 2026-04-27 |
| AR-14-06 | T-12-15 | chainToPreviousHandler=false matches existing demo pattern; ANR chain risk is pre-existing | gsd-secure-phase | 2026-04-27 |
| AR-14-07 | T-12-21 | XCFramework nm-visibility is a known iOS constraint; App Store bitcode stripping handles it | gsd-secure-phase | 2026-04-27 |

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-04-27 | 20 | 19 | 1 | gsd-security-auditor (agent ae0d71dc) |
| 2026-04-27 | 20 | 20 | 0 | gsd-secure-phase (T-12-09 fixed: BuildConfig.DEBUG + #if DEBUG guards added) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-04-27
