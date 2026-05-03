# Phase 26: flutter support package and demo - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-05-03
**Phase:** 26-flutter-support-package-and-demo
**Areas discussed:** Event delivery to Dart, Platform scope, Overlay inclusion, Monorepo dependency wiring, Dart API shape

---

## Event delivery to Dart

| Option | Description | Selected |
|--------|-------------|----------|
| EventChannel per metric | One EventChannel stream per module — cpuStream, fpsStream, etc. Native calls eventSink.success(payload) from addInfoListener. | ✓ |
| Single EventChannel, discriminated | One stream; each event has a 'type' field. More filtering needed on Dart side. | |
| MethodChannel polling | Consumer calls getCpuInfo() imperatively. Not recommended for real-time streaming. | |

**User's choice:** EventChannel per metric  
**Notes:** Confirmed with the code preview showing `Konitor.cpuStream.listen(...)` pattern.

---

## Platform scope

| Option | Description | Selected |
|--------|-------------|----------|
| Android + iOS | Mirrors RN parity. iOS uses XCFramework (cpu/fps/memory/network); jank/gc/thermal are Android-only. | ✓ |
| Android only | Defer iOS to a follow-up phase. | |

**User's choice:** Android + iOS  
**Notes:** Full parity with RN package scope.

---

## Overlay inclusion

| Option | Description | Selected |
|--------|-------------|----------|
| Include overlay, Android only | showOverlay/hideOverlay functional on Android; no-op on iOS. | |
| Include overlay, Android + iOS (no-op iOS) | Expose API on both; Android functional, iOS stub. | ✓ |
| Skip overlay | No overlay; consumers build their own using metric streams. | |

**User's choice:** Include on both platforms (Android functional, iOS no-op stub)  
**Notes:** User responded "not android only and iOS" — confirmed they want the API exposed on both, mirroring RN's behavior where iOS overlay is a no-op.

---

## Monorepo dependency wiring

| Option | Description | Selected |
|--------|-------------|----------|
| Local path (composite build, like RN) | implementation project(':libs:engine') etc., via includeBuild in root settings.gradle.kts. Root Gradle version used. | ✓ |
| Published Maven AAR | Depends on com.smellouk.konitor:engine:X.Y.Z. Cleaner for consumers but needs publish step. | |

**User's choice:** Local path composite build  
**Notes:** User also confirmed "one gradle version (root used for flutter)" — no separate Gradle wrapper in the plugin.

---

## Dart API shape

| Option | Description | Selected |
|--------|-------------|----------|
| Typed Dart model classes | Immutable classes CpuInfo, FpsInfo, etc. with factory CpuInfo.fromMap(). IDE-friendly, null-safe. | ✓ |
| Raw Map<String, dynamic> | Streams emit raw maps. No type safety. | |

**User's choice:** Typed Dart model classes  
**Notes:** Field names match the TypeScript types in libs/ui/rn/src/types.ts exactly.

---

## Claude's Discretion

- Channel naming convention (`com.smellouk.konitor/cpu` etc.) — standard Flutter plugin pattern
- Separation of EventChannels (per metric) vs single MethodChannel (imperative control) — mirrors RN split between event emitters and imperative methods

## Deferred Ideas

- pub.dev publishing / version tagging — release automation concern, separate phase
- Dart VM memory / GC stats (analogous to JS memory reporting in RN) — no direct equivalent in RN package; deferred
