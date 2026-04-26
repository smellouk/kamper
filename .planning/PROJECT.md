# Kamper

## What This Is

Kamper is a Kotlin Multiplatform performance monitoring library for Android and JVM. It provides real-time CPU, FPS, memory, network, jank, GC, and thermal monitoring via a modular plugin architecture, surfacing metrics through an in-app Compose overlay panel and a programmatic listener API.

## Core Value

The monitoring must never crash or destabilize the host app — every metric collection failure is silently swallowed so Kamper is always safe to ship in production builds.

## Requirements

### Validated

- ✓ Plugin-based engine — `Kamper.install(XxxModule)` per-module installation — v1.0/Phase 01-03
- ✓ Conventional Commits history — all 89 commits rewritten — v1.0/Phase 01
- ✓ Codebase documentation — PRINCIPLES.md, 4 ADRs, CONCERNS.md — v1.0/Phase 02
- ✓ CrashDetector handler restoration — all 5 platforms — v1.0/Phase 03
- ✓ KamperUiRepository thread-safety — @Synchronized mutex — v1.0/Phase 03
- ✓ iOS shake-detection coroutine safety — isActive loop fix — v1.0/Phase 03
- ✓ Lifecycle hardening — overlay set-tracking, FPS AtomicBoolean, ANR volatile stop — v1.0/Phase 04
- ✓ CPU /proc/stat direct reads (PERF-01) + configurable buffer ceiling + amber 90%-full badge (PERF-02) — v1.0/Phase 05
- ✓ KamperUiRepository refactor — PreferencesStore/SettingsRepository/RecordingManager/ModuleLifecycleManager split, 22 unit tests (ARCH-01 DEBT-03 DEBT-04 TEST-01) — v1.0/Phase 06

### Active

- [ ] KamperPanel refactor — eliminate unnecessary Compose recompositions (Phase 07)
- [ ] Security & docs — security policy, API docs, capacity limits (Phase 08)
- [ ] Missing platform features — deferred from earlier phases (Phase 09)
- [ ] Test coverage — systematic unit + instrumented coverage gaps closed (Phase 10)
- [ ] Build modernization — composite build convention plugins, version catalog (Phases 11-13)
- [ ] React Native package — published RN wrapper for Kamper engine + UI (Phases 14-15)
- [ ] Release automation — GitHub Releases, changelogs, multi-registry publishing (Phase 16)
- [ ] Open source readiness — contribution guidelines, Claude-friendly repo (Phases 19-20)

### Out of Scope

- Full reactive streams API (Flow/RxJava) — listener pattern is the intentional design; see ADR-003
- Auto-discovery of modules — manual install is required; see ADR-001
- Breaking public API changes — frozen for v1.0; see ADR-004

## Context

Kamper is a brownfield KMP library with 89 commits of history now aligned to Conventional Commits. Three phases of stabilization work completed (history rewrite, codebase documentation, coroutine safety bug fixes). The next phase begins Android lifecycle hardening before moving to performance, refactoring, and platform expansion work.

## Constraints

- **Compatibility**: Min Android API 21 — no APIs above that baseline
- **API Stability**: Public API surface frozen for v1.0 — only additive changes permitted
- **Safety**: Every platform-specific call must be wrapped in try/catch — bugs must never crash the host app
- **KMP**: expect/actual pattern for platform code — commonMain stays pure Kotlin with no platform imports

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Plugin-based engine | Users install only needed modules; zero overhead for unused ones | ✓ Good |
| KMP expect/actual = Ports & Adapters | Platform APIs can only exist in actuals; commonMain stays clean | ✓ Good |
| Listener pattern over reactive streams | Zero dependency on reactive libraries; simpler host-app integration | ✓ Good |
| No breaking API changes in v1.0 | Library users pin versions; breakage forces migration | ✓ Good |
| git filter-repo for history rewrite | Single-pass Python callback; all 89 commits in one run | ✓ Good |

---
*Last updated: 2026-04-26 after Phase 03 completion*
