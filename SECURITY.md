# Security Policy

## Reporting a Vulnerability

If you discover a security vulnerability in Kamper, please report it privately to:

- **Email:** sidali.mellouk@zattoo.com
- **Subject line:** `[Kamper Security] <short description>`

Please include:

- A clear description of the issue
- Steps to reproduce, including a minimal Kamper configuration if relevant
- The Kamper version (or commit SHA) where the issue was observed
- Affected platform target (Android, JVM, iOS, macOS, tvOS, JS, WasmJS)
- Any proof-of-concept or screenshots, if applicable

**Do not** open a public GitHub issue for security-sensitive reports. Public issues are appropriate
only after a fix has been released.

## Response Timeline

| Stage | Target |
|-------|--------|
| Acknowledgement of report | 5 business days |
| Initial triage and severity assessment | 10 business days |
| Patch released or mitigation published | Best-effort, dependent on severity |

Kamper is a single-maintainer open-source project; please be patient if the response is slower than
a commercial product. Critical vulnerabilities (remote code execution, data exfiltration paths) are
prioritized over informational findings.

## Supported Versions

Kamper is pre-1.0. Only the latest released minor version on the `main` branch is supported.

| Version | Supported |
|---------|-----------|
| latest minor on `main` | ✅ |
| older versions | ❌ |

Once Kamper reaches 1.0, this table will be updated to declare a window of supported versions.

## Security-Relevant Configuration Notes

Kamper is a developer-facing performance monitoring library. The items below are intentionally
**convenience features** — they are documented here so library consumers shipping to production can
make an informed decision about each.

### SharedPreferences plain-text storage

Kamper UI persists its configuration (panel toggles, polling intervals, threshold values) in
plain-text `SharedPreferences` under the file name `kamper_ui_prefs`. Issue history is similarly
persisted. This data is sandboxed to your application's private storage and is not readable by
other apps on a non-rooted device, but it is **not encrypted at rest**.

Kamper does not store credentials, PII, or secrets. The only values written are numeric thresholds
and boolean toggles configured by the developer. If your app extends Kamper to store sensitive
threshold values (for example, a private API endpoint as part of a custom config), migrate the
backing store to [`EncryptedSharedPreferences`](https://developer.android.com/reference/androidx/security/crypto/EncryptedSharedPreferences)
from `androidx.security:security-crypto`. Kamper does not depend on `androidx.security:security-crypto`
by default — adding it is the consuming app's responsibility.

### Auto-initialization via `KamperUiInitProvider`

On Android, `KamperUiInitProvider` auto-initializes Kamper UI in debuggable builds via the
`FLAG_DEBUGGABLE` application flag. This is a development convenience — `FLAG_DEBUGGABLE` can be
spoofed on rooted devices and **must not be relied upon as a security control**.

To opt out of auto-initialization (for production builds, paid users, or sensitive environments),
disable the provider in your app's `AndroidManifest.xml`:

```xml
<provider
    android:name="com.smellouk.kamper.ui.KamperUiInitProvider"
    android:authorities="${applicationId}.kamper_ui_init"
    android:enabled="false"
    tools:replace="android:enabled" />
```

With auto-init disabled, call `KamperUi.attach(context)` and `KamperUi.configure { ... }` explicitly
from your `Application.onCreate()`.

### `CrashDetector` overrides the existing uncaught-exception handler

On Android, `CrashDetector` (when enabled via `CrashConfig.isEnabled = true`) installs a default
uncaught-exception handler. If your app already integrates Crashlytics, Sentry, Bugsnag, or another
crash reporter, Kamper's handler is installed on top of it. A warning is logged at INFO level via
`Log.w("CrashDetector", "replacing existing UncaughtExceptionHandler: <className>")` so the
displacement is observable in logcat.

To disable Kamper's crash handler, set `CrashConfig.isEnabled = false` when configuring
`IssuesModule` and rely solely on your existing crash reporter.

## Acknowledgement

Reporters who follow this policy and disclose responsibly will be acknowledged in the release notes
of the version that fixes the issue, unless they request anonymity.

---

## Phase 14 Security Audit — react-native-package-library-engine-ui

**Audit date:** 2026-04-27
**ASVS Level:** 1
**Auditor:** claude-sonnet-4-6 (automated, adversarial stance)
**Threats total:** 21 | **Closed:** 20 | **Open (BLOCKER):** 1

### Threat Verification

| Threat ID | Category | Disposition | Status | Evidence |
|-----------|----------|-------------|--------|----------|
| T-12-W0-01 | Tampering | mitigate | CLOSED | `NativeKamperModule.mock.ts` exports exactly 4 imperative fns (`start`, `stop`, `showOverlay`, `hideOverlay`) + 8 emitters (`onCpu` … `onThermal`), matching `NativeKamperModule.ts` Spec interface 1:1. |
| T-12-W0-02 | Information Disclosure | accept | CLOSED | Accepted risk; `moduleNameMapper` in `jest.config.js` is scoped to test execution only. |
| T-12-04 | Information Disclosure | accept | CLOSED | Accepted risk; documented in threat register. |
| T-12-05 | Tampering | mitigate | CLOSED | `KamperUi.show(context)` delegates to `attach(context)` which calls `context.applicationContext as Application` (`KamperUi.kt` Android line 21). `reactApplicationContext` always carries a valid `applicationContext`; the cast is safe for the sole TurboModule caller. |
| T-12-06 | DoS | accept | CLOSED | Accepted risk; documented in threat register. |
| T-12-07 | DoS | accept | CLOSED | Accepted risk; documented in threat register. |
| T-12-08 | DoS | mitigate | CLOSED | `useIssues.ts` line 18: `[d, ...prev].slice(0, 100)`. Also capped in `useKamper.ts` line 89. |
| T-12-09 | Information Disclosure | mitigate | **OPEN** | See Open Threats section below. |
| T-12-10 | Elevation | accept | CLOSED | `_acquireEngine`/`_releaseEngine` carry `@internal` JSDoc and are NOT re-exported from `src/index.ts` (grep confirms no export). |
| T-12-11 | Tampering | mitigate | CLOSED | `KamperTurboModule.kt` lines 60-65: `ReadableMap?.flag()` guards with `hasKey()` + `getType(key) == ReadableType.Boolean` before calling `getBoolean()`; non-boolean → `true`. |
| T-12-12 | Information Disclosure | mitigate | CLOSED | `android/build.gradle` line 60 (monorepo branch): `debugImplementation project(':kamper:ui:kmm')`. Line 73 (autolink branch): `debugImplementation "com.smellouk.kamper:kmm:$v"`. Both branches scope `kamper-ui` to debug variant only. |
| T-12-13 | DoS | mitigate | CLOSED | `KamperTurboModule.kt` lines 179, 185: `UiThreadUtil.runOnUiThread { ... }` wraps both `showOverlay` and `hideOverlay`. |
| T-12-14 | Spoofing | mitigate | CLOSED | `KamperTurboModule.kt` line 191: `const val NAME = "KamperModule"`. All references in `KamperTurboPackage.kt` use `KamperTurboModule.NAME`. Single source of truth confirmed. |
| T-12-15 | Elevation | accept | CLOSED | Accepted risk; `chainToPreviousHandler = false` is documented existing pattern. |
| T-12-16 | Tampering | mitigate | CLOSED | `KamperTurboModule.mm` line 33: `static BOOL flagOrTrue(folly::Optional<bool> opt)` returns `YES` when option absent. Called for all 4 iOS flags at lines 41-44. |
| T-12-17 | Information Disclosure | mitigate | CLOSED | Declared mitigation is JSDoc `__DEV__` guidance + compile-time strip deferred to consumer. `Kamper.ts` line 104 carries the JSDoc note. No runtime guard was claimed for this threat — the description explicitly states "compile-time strip deferred." |
| T-12-18 | DoS | mitigate | CLOSED | `KamperTurboModule.mm` lines 123, 129: `dispatch_async(dispatch_get_main_queue(), ^{ ... })` for both `showOverlay` and `hideOverlay`. |
| T-12-19 | Spoofing | mitigate | CLOSED | `KamperTurboModule.mm` line 145: single `@"KamperModule"` literal in `+ (NSString *)moduleName`. |
| T-12-20 | Tampering | mitigate | CLOSED | `KamperTurboModule.mm` line 39: `if (_bridge) return;` at top of `start:` method prevents double-init. |
| T-12-21 | Information Disclosure | accept | CLOSED | Accepted risk; App Store symbol stripping is consumer responsibility. |

### Open Threats — BLOCKER

#### T-12-09 — Information Disclosure — showOverlay reachable in prod JS / native

**Gap:** The declared mitigation is "JSDoc `__DEV__` guard documented; **native guard in TurboModule**." The JSDoc hint exists at `Kamper.ts` line 104. However, no runtime native guard is present in either `KamperTurboModule.kt` (Android) or `KamperTurboModule.mm` (iOS). Both `showOverlay()` implementations execute unconditionally regardless of build type. The Android path additionally depends on `debugImplementation` for the `kamper-ui-kmm` artifact (T-12-12), which means `KamperUi.show()` will throw a `NoClassDefFoundError` at runtime in release builds — an uncontrolled crash rather than a guard.

**Required fix (choose one):**
1. Add `if (BuildConfig.DEBUG) return` (Android) and `#if DEBUG` / `NS_BLOCK_ASSERTIONS` (iOS) inside `showOverlay()` in both TurboModule implementations.
2. Or revise the threat disposition for T-12-09 from `mitigate` to `accept`, documenting that the only protection is the JSDoc advisory and that the Android release crash (NoClassDefFoundError) is accepted behavior.

**Files to modify (implementation team only — this audit is read-only):**
- `kamper/ui/rn/android/src/main/java/com/smellouk/kamper/rn/KamperTurboModule.kt` — `showOverlay()` / `hideOverlay()`
- `kamper/ui/rn/ios/KamperTurboModule.mm` — `showOverlay` / `hideOverlay`

### Accepted Risks Log

| Threat ID | Risk Summary | Rationale |
|-----------|-------------|-----------|
| T-12-W0-02 | `moduleNameMapper` redirects to mock at test-time | Redirect is in `jest.config.js`, never in the production bundle |
| T-12-04 | `KamperUi.show()` callable from production code | Library consumers bear responsibility; documented in threat register |
| T-12-06 | Repeated `show()` calls cause re-allocation | Existing behavior; no regression introduced |
| T-12-07 | Event handler exceptions visible to native | React Native design; handlers do not propagate to native thread |
| T-12-10 | `_acquireEngine`/`_releaseEngine` not in public export | `@internal` annotation + excluded from `index.ts` barrel |
| T-12-15 | `start()` ANR detector overrides uncaught-exception handler | `chainToPreviousHandler = false`; documented existing pattern |
| T-12-21 | iOS XCFramework may expose internal symbols in App Store builds | App Store symbol stripping is consumer responsibility |

### Unregistered Flags

None. All threat flags from the implementation are mapped to registered threat IDs above.
