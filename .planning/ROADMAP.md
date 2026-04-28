# Roadmap: Kamper

## Overview

Kamper v1.0 takes an existing KMP performance monitoring library from a rough brownfield state to a stable, well-documented, fully-tested, and open-source-ready release. Work spans history cleanup, documentation, bug fixes, lifecycle hardening, performance improvements, architecture refactoring, build modernization, platform expansion, and release automation across 20 phases.

## Phases

**Phase Numbering:**
- Integer phases (1–20): Planned milestone work
- Decimal phases: Urgent insertions (marked INSERTED) — none yet

- [x] **Phase 1: Git History Clean** — Rewrite all 89 commit messages to Conventional Commits spec using git filter-repo
- [x] **Phase 2: Codebase Documentation** — Create PRINCIPLES.md, 4 ADRs, annotate CONCERNS.md with 27 phase resolution links
- [x] **Phase 3: Bug Fixes — Coroutine Safety** — Fix CrashDetector handler restoration (5 platforms), KamperUiRepository mutex, iOS isActive loop
- [x] **Phase 4: Fragile Lifecycle Hardening** — Harden AndroidOverlayManager, FpsChoreographer exception safety, AnrDetector stop race
- [x] **Phase 5: CPU Performance Recording Buffer** — Add ring buffer for CPU sample recording and replay
- [x] **Phase 6: KamperUiRepository Refactor** — Background dispatcher, split classes, settings tests
- [x] **Phase 7: KamperPanel Refactor** — Eliminate unnecessary Compose recompositions
- [x] **Phase 8: Security, Docs & Scaling** — Security policy, API docs, capacity limits
- [x] **Phase 9: Missing Features** — Implement platform features deferred from earlier phases
- [x] **Phase 10: Test Coverage** — Close unit and instrumented test coverage gaps (completed 2026-04-26)
- [x] **Phase 11: Migrate buildSrc to Composite Build** — Replace buildSrc with convention plugins (completed 2026-04-26)
- [x] **Phase 12: Kotlin-Gradle Monorepo Consolidation** — Kotlin-first Gradle structure consolidation (completed 2026-04-26)
- [x] **Phase 13: Stack Alignment & Dependency Unification** — Single version catalog for all dependencies (completed 2026-04-27)
- [x] **Phase 14: React Native Package** — Create and publish RN wrapper for Kamper engine and UI (completed 2026-04-27)
- [x] **Phase 15: Adjust Kamper UI for React Native** — Align UI layer to support the RN package (completed 2026-04-27)
- [x] **Phase 16: Release Automation** — GitHub Releases, changelogs, multi-registry publishing (completed 2026-04-28)
- [ ] **Phase 17: Medium Article Series** — Android performance bottlenecks series using Kamper
- [x] **Phase 18: Service Integrations** — Kamper integration points for Sentry and similar services (completed 2026-04-28)
- [x] **Phase 19: Claude-Friendly Repo** — Research and define Claude skill offerings for the project (completed 2026-04-28)
- [ ] **Phase 20: Open Source Cleanup** — Contribution guidelines, open source readiness

## Phase Details

### Phase 1: Git History Clean
**Goal**: Rewrite all commit messages to Conventional Commits spec — no emojis, no resolves #N footers, lowercase after colon, scoped by module
**Depends on**: Nothing
**Requirements**: CC-01
**Success Criteria** (what must be TRUE):
  1. All 89 commits follow `<type>(<scope>): <description>` format
  2. No emojis remain in any commit message
  3. `git log --oneline` reads as a clean changelog
**Plans**: 3 plans

Plans:
- [x] 01-01: Research and build Python message-callback script with full mapping table
- [x] 01-02: Dry-run preview — generate old→new mapping for human review
- [x] 01-03: Execute filter-repo rewrite, create backup branch, force-push

### Phase 2: Codebase Documentation
**Goal**: Complete codebase documentation — PRINCIPLES.md with 7 engineering principles, 4 Nygard ADRs, CONCERNS.md annotated with phase resolution links
**Depends on**: Phase 1
**Requirements**: DOC-01
**Success Criteria** (what must be TRUE):
  1. PRINCIPLES.md exists with all 7 principles (D-01..D-07), each with "How to apply" and "Violation indicator"
  2. 4 ADR files exist in adr/ with Status/Context/Decision/Consequences sections
  3. CONCERNS.md has 27 "Resolved by: Phase N" annotations
**Plans**: 3 plans

Plans:
- [x] 02-01: Annotate CONCERNS.md + refresh STACK.md date
- [x] 02-02: Enhance ARCHITECTURE.md, CONVENTIONS.md, TESTING.md
- [x] 02-03: Create PRINCIPLES.md and 4 ADR files

### Phase 3: Bug Fixes — Coroutine Safety
**Goal**: Fix 3 confirmed defects — CrashDetector handler not restored on stop (all platforms), settings load/save missing mutex (Android + Apple), iOS shake-detection unsafe while(true)
**Depends on**: Phase 2
**Requirements**: BUG-01, BUG-02, DEBT-01
**Success Criteria** (what must be TRUE):
  1. After CrashDetector.stop(), previous exception handler is always restored on all 5 platforms
  2. loadSettings()/saveSettings() are mutex-protected on Android and Apple
  3. iOS shake-detection coroutine respects isActive and exits cleanly on cancel
**Plans**: 2 plans

Plans:
- [x] 03-01: Fix CrashDetector handler restoration on Android, JVM, iOS, macOS, tvOS
- [x] 03-02: Fix KamperUiRepository mutex + iOS isActive loop

### Phase 4: Fragile Lifecycle Hardening
**Goal**: Three Android-only hardening fixes — no leaked overlay views, FPS collection survives listener exceptions, ANrDetector stop race eliminated
**Depends on**: Phase 3
**Requirements**: FRAG-01, FRAG-02, FRAG-03
**Success Criteria** (what must be TRUE):
  1. After detachFromActivity(), no overlay view remains tracked even if removeView() throws
  2. A listener exception in doFrame() does not silently kill FPS collection
  3. onIssue() is never fired after AnrDetector.stop() returns
**Plans**: 3 plans

Plans:
- [x] 04-01: AndroidOverlayManager view tracking + try-catch (FRAG-01)
- [x] 04-02: FpsChoreographer AtomicBoolean + exception safety (FRAG-02)
- [x] 04-03: AnrDetector stop race elimination (FRAG-03)

### Phase 5: CPU Performance Recording Buffer
**Goal**: Add a ring buffer to record CPU performance samples for replay and analysis
**Depends on**: Phase 4
**Requirements**: PERF-01
**Success Criteria** (what must be TRUE):
  1. CPU samples are stored in a bounded ring buffer with configurable capacity
  2. Buffer contents can be replayed or exported on demand
  3. Buffer adds no measurable overhead to CPU monitoring loop
**Plans**: 2 plans

Plans:
- [x] 05-01: Ring buffer implementation and integration with CpuInfoSource
- [x] 05-02: Replay API and buffer configuration

### Phase 6: KamperUiRepository Refactor
**Goal**: Refactor KamperUiRepository to use background dispatcher, split into focused classes, and add settings tests
**Depends on**: Phase 5
**Requirements**: ARCH-01
**Success Criteria** (what must be TRUE):
  1. KamperUiRepository uses background dispatcher for all IO operations
  2. Class split into focused single-responsibility classes
  3. Settings load/save paths covered by unit tests
**Plans**: 6 plans

Plans:
- [x] 06-01: Extract settings repository
- [x] 06-02: Extract overlay state management
- [x] 06-03: Migrate to background dispatcher
- [x] 06-04: Add settings unit tests
- [x] 06-05: Add overlay state tests
- [x] 06-06: Integration verification

### Phase 7: KamperPanel Refactor
**Goal**: Refactor Kamper overlay Compose architecture to eliminate unnecessary recompositions
**Depends on**: Phase 6
**Requirements**: PERF-02
**Success Criteria** (what must be TRUE):
  1. KamperPanel no longer recomposes the entire tree on each metric update
  2. Each metric section recomposes only when its own data changes
  3. Recomposition count measurably reduced (verified via Layout Inspector)
**Plans**: 5 plans

Plans:
- [x] 07-01: Analyze recomposition hotspots
- [x] 07-02: Extract stable state holders per metric
- [x] 07-03: Refactor CPU section
- [x] 07-04: Refactor FPS/memory/network sections
- [x] 07-05: Verify recomposition reduction

### Phase 8: Security, Docs & Scaling
**Goal**: Add security policy, complete API documentation, document scaling limits and capacity
**Depends on**: Phase 7
**Requirements**: SEC-01, DOC-02
**Success Criteria** (what must be TRUE):
  1. SECURITY.md exists with vulnerability reporting policy
  2. All public API types have KDoc documentation
  3. CAPACITY.md documents known scaling limits per module
**Plans**: 5 plans (3 executed; 2 gap-closure plans added after VERIFICATION found 0/3 ROADMAP must-haves and 4 unfixed code-review findings)

Plans:
- [x] 08-01: Existing — SEC-01/SEC-02/SEC-03 inline security framing (KDoc + README + CrashDetector log)
- [x] 08-02: Existing — SCALE-01 issues capacity + DroppedIssueEvent
- [x] 08-03: Existing — SCALE-02 Perfetto streaming gzip export
- [x] 08-04: Gap closure — SECURITY.md, CAPACITY.md, KDoc sweep across 15 public Config types (SEC-01, DOC-02)
- [x] 08-05: Gap closure — Code review fixes CR-01..CR-04 (README examples, Builder singletons, RecordingManager thread-safety)

### Phase 9: Missing Features
**Goal**: Implement platform features deferred from earlier phases
**Depends on**: Phase 8
**Requirements**: FEAT-01
**Success Criteria** (what must be TRUE):
  1. All previously deferred platform features are implemented or explicitly out-of-scoped
  2. Feature parity between Android and JVM where applicable
**Plans**: 6 plans

Plans:
- [x] 09-01: UNSUPPORTED sentinel constants for all Info subclasses + Performance.lastValidSampleAt (FEAT-01)
- [x] 09-02: KamperConfigReceiver BroadcastReceiver with android:exported=false (FEAT-02)
- [x] 09-03: CpuInfoRepositoryImpl one-time UNSUPPORTED capability probe (FEAT-01)
- [x] 09-04: UI UNSUPPORTED tile rendering — KamperUiState + MetricCard gray styling (FEAT-01 UI)
- [x] 09-05: FEAT-03 plumbing — currentApiTimeMs + onSampleDelivered + Performance.installedAt
- [x] 09-06: Engine.validate() health-check API with per-module staleness detection (FEAT-03)

### Phase 10: Test Coverage
**Goal**: Systematically close unit and instrumented test coverage gaps across all modules
**Depends on**: Phase 9
**Requirements**: TEST-01
**Success Criteria** (what must be TRUE):
  1. All critical paths in commonMain covered by unit tests
  2. All platform actuals covered by platform-specific tests
  3. No TODO() stubs remain in test files
**Plans**: 3 plans

Plans:
- [x] 10-01: Audit coverage gaps and prioritize
- [x] 10-02: Unit test coverage for commonMain paths
- [x] 10-03: Instrumented test coverage for Android paths

### Phase 11: Migrate buildSrc to Composite Build
**Goal**: Replace buildSrc with composite build convention plugins
**Depends on**: Phase 10
**Requirements**: BUILD-01
**Success Criteria** (what must be TRUE):
  1. buildSrc directory removed; convention plugins in build-logic/
  2. All modules use convention plugins for shared build config
  3. Build cache hit rate maintained or improved
**Plans**: 4 plans

Plans:
- [x] 11-01: Create build-logic module structure
- [x] 11-02: Migrate Config.kt and Libs.kt
- [x] 11-03: Migrate module-level build scripts to plugins
- [x] 11-04: Remove buildSrc and verify build

### Phase 12: Kotlin-Gradle Monorepo Consolidation
**Goal**: Kotlin-first Gradle consolidation for monorepo structure
**Depends on**: Phase 11
**Requirements**: BUILD-02
**Success Criteria** (what must be TRUE):
  1. All Kamper-owned Gradle scripts use Kotlin DSL exclusively; demos/react-native/android/ Groovy DSL files are React Native framework-owned and intentionally preserved per D-11
  2. Shared build logic consolidated with no duplication
  3. Build structure documented in STRUCTURE.md
**Plans**: 5 plans

Plans:
- [x] 12-01: Audit remaining Groovy DSL usage
- [x] 12-02: Migrate to Kotlin DSL
- [x] 12-03: Consolidate shared build logic
- [x] 12-04: Gap closure — Fix ROADMAP SC-1 carve-out for RN composite Groovy files (BUILD-02)
- [x] 12-05: Gap closure — Update STRUCTURE.md to reflect build-logic, composite builds, CC, PREFER_SETTINGS (BUILD-02)

### Phase 13: Stack Alignment & Dependency Unification
**Goal**: Unify all dependency versions into a single version catalog (libs.versions.toml)
**Depends on**: Phase 12
**Requirements**: BUILD-03
**Success Criteria** (what must be TRUE):
  1. libs.versions.toml contains all dependency versions
  2. No version hardcoding in module build scripts
  3. Dependency conflicts resolved across all modules
**Plans**: 5 plans

Plans:
- [x] 13-01: Audit all dependency declarations
- [x] 13-02: Create libs.versions.toml
- [x] 13-03: Migrate kamper/* modules
- [x] 13-04: Migrate samples/* and demos/*
- [x] 13-05: Verify and clean up conflicts

### Phase 14: React Native Package
**Goal**: Create and publish a React Native package wrapping the Kamper engine and UI
**Depends on**: Phase 13
**Requirements**: PLAT-01
**Success Criteria** (what must be TRUE):
  1. React Native package published to npm
  2. Android bridge exposes CPU, FPS, memory, network metrics
  3. KamperPanel rendered in RN app via native view
**Plans**: 7 plans

Plans:
- [x] 14-00: Architecture design and RN bridge approach
- [x] 14-01: RN package scaffolding
- [x] 14-02: Android bridge — engine API
- [x] 14-03: Android bridge — UI (KamperPanel)
- [x] 14-04: iOS bridge — engine API
- [x] 14-05: RN JavaScript API layer
- [x] 14-06: Publish to npm + documentation

### Phase 15: Adjust Kamper UI for React Native
**Goal**: Align Kamper UI layer to properly support the React Native package
**Depends on**: Phase 14
**Requirements**: PLAT-02
**Success Criteria** (what must be TRUE):
  1. KamperPanel works correctly when embedded in a React Native app
  2. No visual regressions in native Android/iOS usage
**Plans**: 3 plans

Plans:
- [x] 15-01: Identify UI adjustments needed for RN embedding
- [x] 15-02: Implement adjustments
- [x] 15-03: Cross-platform visual verification

### Phase 16: Release Automation
**Goal**: Automated GitHub Releases, changelogs, and multi-registry publishing (Maven Central + npm)
**Depends on**: Phase 15
**Requirements**: REL-01
**Success Criteria** (what must be TRUE):
  1. Git tag triggers automated GitHub Release with changelog
  2. KMP artifacts published to Maven Central automatically
  3. RN package published to npm automatically
**Plans**: 8 plans

Plans:
- [x] 16-00: Release strategy and tooling selection
- [x] 16-01: Changelog generation (git-cliff or similar)
- [x] 16-02: GitHub Actions release workflow
- [x] 16-03: Maven Central publishing setup
- [x] 16-04: Signing and secrets configuration
- [x] 16-05: npm publishing workflow
- [x] 16-06: Version bump automation
- [x] 16-07: End-to-end release dry-run

### Phase 17: Medium Article Series
**Goal**: Write and publish Medium article series on Android performance bottlenecks using Kamper
**Depends on**: Phase 16
**Requirements**: DOC-03
**Success Criteria** (what must be TRUE):
  1. At least 5 articles published covering CPU, FPS, memory, network, and crash monitoring
  2. Each article includes working Kamper code samples
  3. Articles cross-link to GitHub repo and npm package
**Plans**: 5 plans

Plans:
- [ ] 17-01: Article outline and series structure
- [ ] 17-02: CPU + FPS articles
- [ ] 17-03: Memory + network articles
- [ ] 17-04: Crash/ANR/jank article
- [ ] 17-05: Publication and promotion

### Phase 18: Service Integrations
**Goal**: Design and implement Kamper integration points for Sentry and similar observability services
**Depends on**: Phase 17
**Requirements**: INT-01
**Success Criteria** (what must be TRUE):
  1. Kamper metrics can be forwarded to Sentry as breadcrumbs or custom metrics
  2. Integration is optional and zero-cost when not used
  3. Integration guide documented
**Plans**: 5 plans

Plans:
- [x] 18-01: Integration API design
- [x] 18-02: Sentry integration implementation
- [x] 18-03: Generic integration hook for other services
- [x] 18-04: Integration tests
- [x] 18-05: Documentation and examples

### Phase 19: Claude-Friendly Repo
**Goal**: Make the repo Claude-friendly — research and define Claude skill offerings for the project
**Depends on**: Phase 18
**Requirements**: OSS-01
**Success Criteria** (what must be TRUE):
  1. CLAUDE.md exists with accurate project context
  2. At least 1 Claude skill defined for Kamper-specific workflows
**Plans**: 3 plans

Plans:
- [x] 19-01-PLAN.md — Create root CLAUDE.md (Quick Start + Build & Test + Module Patterns + Commit & PR Rules + Architecture & Key Files)
- [x] 19-02-PLAN.md — Create kamper-new-module skill (full 8-platform module skeleton scaffolder)
- [x] 19-03-PLAN.md — Create kamper-check + kamper-module-review skills and extend .claude/settings.json Gradle allowlist

### Phase 20: Open Source Cleanup
**Goal**: Contribution guidelines, issue templates, open source readiness checklist completed
**Depends on**: Phase 19
**Requirements**: OSS-02
**Success Criteria** (what must be TRUE):
  1. CONTRIBUTING.md covers setup, coding standards, PR process
  2. GitHub issue and PR templates in place
  3. All TODOs and internal references removed from public-facing files
**Plans**: TBD

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Git History Clean | 3/3 | Complete | 2026-04-25 |
| 2. Codebase Documentation | 3/3 | Complete | 2026-04-25 |
| 3. Bug Fixes — Coroutine Safety | 2/2 | Complete | 2026-04-25 |
| 4. Fragile Lifecycle Hardening | 3/3 | Complete | 2026-04-26 |
| 5. CPU Recording Buffer | 2/2 | Complete | 2026-04-26 |
| 6. KamperUiRepository Refactor | 6/6 | Complete | 2026-04-26 |
| 6. KamperUiRepository Refactor | 6/6 | Complete | 2026-04-26 |
| 7. KamperPanel Refactor | 5/5 | Complete | 2026-04-26 |
| 8. Security, Docs & Scaling | 5/5 | Complete | 2026-04-26 |
| 9. Missing Features | 0/6 | Not started | - |
| 10. Test Coverage | 3/3 | Complete   | 2026-04-26 |
| 11. Migrate buildSrc | 4/4 | Complete    | 2026-04-26 |
| 12. Monorepo Consolidation | 5/5 | Complete   | 2026-04-26 |
| 13. Dependency Unification | 5/5 | Complete    | 2026-04-27 |
| 14. React Native Package | 7/7 | Complete    | 2026-04-27 |
| 15. Kamper UI for RN | 3/3 | Complete   | 2026-04-27 |
| 16. Release Automation | 8/8 | Complete    | 2026-04-28 |
| 17. Medium Articles | 0/5 | Not started | - |
| 18. Service Integrations | 6/6 | Complete    | 2026-04-28 |
| 19. Claude-Friendly Repo | 3/3 | Complete    | 2026-04-28 |
| 20. Open Source Cleanup | 0/TBD | Not started | - |
