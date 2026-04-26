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
