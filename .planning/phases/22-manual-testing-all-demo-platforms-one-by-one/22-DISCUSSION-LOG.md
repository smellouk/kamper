# Phase 22: Manual Testing All Demo Platforms - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-04-29
**Phase:** 22-manual-testing-all-demo-platforms-one-by-one
**Areas discussed:** Platform scope, Pass/fail criteria, Issue handling, Execution assistance

---

## Platform Scope

| Option | Description | Selected |
|--------|-------------|----------|
| Android | demos/android/ — Gradle build, emulator/device | ✓ |
| JVM | demos/jvm/ — Pure JVM CLI, no device needed | ✓ |
| Compose | demos/compose/ — Compose Multiplatform desktop | ✓ |
| iOS / macOS / Web / RN | All remaining platforms requiring special tooling | ✓ |

**User's choice:** All 7 platforms ("all")
**Notes:** No platforms excluded. iOS/macOS (Xcode), web (browser), and React Native (RN toolchain) all included despite requiring special setup.

---

## Pass/Fail Criteria

| Option | Description | Selected |
|--------|-------------|----------|
| Build compiles + app launches | Zero build errors AND app/process starts | |
| Build + metrics visible | App starts AND at least one metric shows live values | |
| Full smoke test | App starts, all modules report values, UI renders, no crashes in 30s | ✓ |

**User's choice:** Full smoke test
**Notes:** Build success alone is not sufficient. Metrics must show live values and the UI overlay must render. No ANRs or crashes during a 30-second run.

---

## Issue Handling

| Option | Description | Selected |
|--------|-------------|----------|
| Fix inline in this phase | Each plan tests + fixes before marking done | |
| Log and continue | Document failure, don't block phase | |
| Block plan execution | Plan blocked until demo passes smoke test | ✓ |

**User's choice:** "Block plan execution in this phase" (custom response)
**Notes:** Each plan is blocked until its demo passes. No plan advances to "done" with a failing demo. Fixes are applied inline within the same plan.

---

## Execution Assistance

| Option | Description | Selected |
|--------|-------------|----------|
| Claude runs build, you run app | Claude runs build commands, user launches app, Claude interprets | ✓ |
| Guided checklist per platform | Step-by-step checklist, user runs, Claude analyzes feedback | |
| Fully interactive session | Claude issues one command at a time, waits for output | |

**User's choice:** Claude runs build, you run app (recommended)
**Notes:** Claude executes all Gradle/npm/xcodebuild commands. User manually launches the app/emulator and reports observations. Claude interprets and diagnoses.

---

## Plan Shape

| Option | Description | Selected |
|--------|-------------|----------|
| One plan per platform | 7 plans, atomic, easy to retry individually | ✓ |
| Fast platforms first, then slow | Grouped by toolchain similarity (4 plans) | |
| One big plan | Single plan covering all 7 sequentially | |

**User's choice:** One plan per platform
**Notes:** 7 plans, ordered from fastest toolchain to most complex: jvm → android → compose → ios → macos → web → react-native.

---

## Claude's Discretion

- Grep pattern for detecting stale `:kamper:*` references
- Whether to add a summary plan (plan 08) for test results documentation
- Exact Gradle task names per demo (`:demos:jvm:run` vs platform-specific equivalents)
- Order of module verification within each smoke test

## Deferred Ideas

None — discussion stayed within phase scope.
