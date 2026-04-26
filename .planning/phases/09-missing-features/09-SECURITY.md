---
phase: 09
slug: missing-features
status: verified
threats_open: 0
asvs_level: 1
created: 2026-04-26
---

# Phase 09 — Security

> Per-phase security contract: threat register, accepted risks, and audit trail.

---

## Trust Boundaries

| Boundary | Description | Data Crossing |
|----------|-------------|---------------|
| ADB shell → kamper library process | ADB shell runs as `system_shell` UID and is allowed to bypass `exported=false`. This is intentional — that is exactly the toggle channel FEAT-02 enables. | Boolean intent extra (enabled on/off) |
| In-process UI → Kamper engine | KamperUiState is `internal`; overlay renders only on debug builds (FLAG_DEBUGGABLE). | In-process boolean flags, no PII |
| Watcher coroutine → Performance fields | Watcher dispatches sample callbacks on mainDispatcher; Performance fields are `@Volatile`. | Wall-clock millisecond timestamp, no PII |

---

## Threat Register

| Threat ID | Category | Component | Disposition | Mitigation | Status |
|-----------|----------|-----------|-------------|------------|--------|
| T-7-01 | Spoofing / Tampering | `<receiver>` declaration in AndroidManifest.xml | mitigate | `android:exported="false"` on the `<receiver>` element. Verified at `AndroidManifest.xml:27`. | closed |
| T-7-02 | Tampering | KamperConfigReceiver.onReceive() Intent extras | mitigate | `intent.getBooleanExtra("enabled", true)` returns `true` safe default when extra is missing or wrong type. Verified at `KamperConfigReceiver.kt:27`. | closed |
| T-7-03 | Denial of Service (intent flooding) | KamperConfigReceiver → Kamper.start() / Kamper.stop() | mitigate | `if (job?.isActive == true) return` guard at `Watcher.kt:25–27` makes `startWatching()` idempotent. | closed |
| T-7-04 | Tampering (low) | Public sentinel constants in Info subclasses | accept | See Accepted Risks Log. | closed |
| T-7-05 | Information Disclosure (low) | Performance.lastValidSampleAt @Volatile field | accept | See Accepted Risks Log. | closed |
| T-7-06 | Information Disclosure | Intent action string `com.smellouk.kamper.CONFIGURE` | accept | See Accepted Risks Log. | closed |
| T-7-07 | Tampering | `platformSupported` cache field in CpuInfoRepositoryImpl | accept | See Accepted Risks Log. | closed |
| T-7-08 | Denial of Service | Repeated UNSUPPORTED probe in CpuInfoRepositoryImpl | mitigate | `platformSupported: Boolean? = null` cache at `CpuInfoRepositoryImpl.kt:18`; early-return guard at line 26 bypasses all OS calls once cache is set. | closed |
| T-7-09 | Information Disclosure | Diagnostic Log.d entry mentioning "platformSupported=false" | accept | See Accepted Risks Log. | closed |
| T-7-10 | Information Disclosure (low) | "Unsupported" label visible in Kamper overlay | accept | See Accepted Risks Log. | closed |
| T-7-11 | Tampering | KamperUiState.cpuUnsupported boolean field | accept | See Accepted Risks Log. | closed |
| T-7-12 | Tampering | Performance.lastValidSampleAt concurrent writes from Watcher coroutine | accept | See Accepted Risks Log. | closed |
| T-7-13 | Denial of Service | Misbehaving onSampleDelivered callback consumer | accept | See Accepted Risks Log. | closed |
| T-7-14 | Information Disclosure | Wall-clock timestamp in Performance.lastValidSampleAt | accept | See Accepted Risks Log. | closed |
| T-7-15 | Tampering | Reading lastValidSampleAt / installedAt across threads inside validate() | mitigate | Both fields are `@Volatile` (`Performance.kt:18,24`). Reads observe the most recent write from any thread. | closed |
| T-7-16 | Denial of Service | Misbehaving ValidationInfo listener inside validate() | accept | See Accepted Risks Log. | closed |
| T-7-17 | Information Disclosure | Problem strings include module class simpleName | accept | See Accepted Risks Log. | closed |
| T-7-18 | Race | Two threads call validate() concurrently | accept | See Accepted Risks Log. | closed |

*Status: open · closed*
*Disposition: mitigate (implementation required) · accept (documented risk) · transfer (third-party)*

---

## Accepted Risks Log

| Risk ID | Threat Ref | Rationale | Accepted By | Date |
|---------|------------|-----------|-------------|------|
| AR-01 | T-7-04 | Sentinel constants are public immutable `val` declarations. A consumer could compare against UNSUPPORTED to bypass UI rendering, but this is the intended contract — no security boundary exists. | gsd-security-auditor | 2026-04-26 |
| AR-02 | T-7-05 | `Performance.lastValidSampleAt` is `internal` (kamper.api package) and exposes only a wall-clock millisecond. Consumers cannot read it through public API. No PII. | gsd-security-auditor | 2026-04-26 |
| AR-03 | T-7-06 | The action string `com.smellouk.kamper.CONFIGURE` is documented public knowledge. Knowing the action grants no capability — only same-UID or ADB processes can dispatch it (T-7-01 mitigation covers this). | gsd-security-auditor | 2026-04-26 |
| AR-04 | T-7-07 | `platformSupported` is `private var` inside an `internal class` — unreachable from outside the module. An attacker controlling the device could send malformed `/proc/stat` content, but they already have root in that scenario. | gsd-security-auditor | 2026-04-26 |
| AR-05 | T-7-09 | `Log.d` is filtered at Android's logging level in release builds. The message contains no PII. Aligns with existing `Log.d("Kamper/CPU", ...)` pattern. | gsd-security-auditor | 2026-04-26 |
| AR-06 | T-7-10 | The overlay is debug-build only by default (KamperUiInitProvider gates on FLAG_DEBUGGABLE). The "Unsupported" label reveals only that an OS capability is absent — not user data, credentials, or crash details. | gsd-security-auditor | 2026-04-26 |
| AR-07 | T-7-11 | `KamperUiState.cpuUnsupported` is `internal var`, mutated only by `cpuListener` inside KamperUiRepository (single owner). External code cannot reach `_state` (private). The flag gates UI rendering only; it does not control any security decision. | gsd-security-auditor | 2026-04-26 |
| AR-08 | T-7-12 | Concurrent writes to `@Volatile lastValidSampleAt` are atomic on JVM/Android per Kotlin volatile semantics. `kotlin.concurrent.Volatile` used in commonMain covers Native targets. No additional synchronization required. | gsd-security-auditor | 2026-04-26 |
| AR-09 | T-7-13 | The `onSampleDelivered` callback is `internal`-only; its sole consumer is `Performance.start()`'s lambda which performs only `lastValidSampleAt = currentApiTimeMs()` — neither operation can throw. Future external callbacks would require an explicit hardening pass. | gsd-security-auditor | 2026-04-26 |
| AR-10 | T-7-14 | `Performance.lastValidSampleAt` is `internal` and not exposed by any public API in this phase. Millisecond wall-clock resolution carries no PII. | gsd-security-auditor | 2026-04-26 |
| AR-11 | T-7-16 | ValidationInfo listeners run synchronously on the engine's invoking thread — consistent with how all other Info listeners fire. A future hardening could try-catch each invocation; out of scope for this phase. | gsd-security-auditor | 2026-04-26 |
| AR-12 | T-7-17 | Class simple names are already public via Kotlin reflection. Problem strings contain no PII, credentials, or environment details. | gsd-security-auditor | 2026-04-26 |
| AR-13 | T-7-18 | `performanceList` iteration inside `validate()` is read-only. Two concurrent calls could see slightly stale views but cannot corrupt state. Phase ships single-call-from-app-code semantics per D-13; future hardening could use a synchronized block. | gsd-security-auditor | 2026-04-26 |

*Accepted risks do not resurface in future audit runs.*

---

## Security Audit Trail

| Audit Date | Threats Total | Closed | Open | Run By |
|------------|---------------|--------|------|--------|
| 2026-04-26 | 18 | 18 | 0 | gsd-security-auditor (auto) |

---

## Sign-Off

- [x] All threats have a disposition (mitigate / accept / transfer)
- [x] Accepted risks documented in Accepted Risks Log
- [x] `threats_open: 0` confirmed
- [x] `status: verified` set in frontmatter

**Approval:** verified 2026-04-26
