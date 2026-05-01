// src/NativeKamperModule.ts — Codegen TurboModule spec.
// Source of truth: generates Android NativeKamperModuleSpec (Java abstract) +
// iOS NativeKamperModuleSpecBase (ObjC++) + JSI glue.
// Pattern: D-05 (TurboModule) + Codegen EventEmitter properties.

import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';
import type { EventEmitter } from 'react-native/Libraries/Types/CodegenTypes';

// ─── Codegen-compatible payload types ─────────────────────────────────────
// Codegen requires plain object types defined inline in the spec file.
// These mirror src/types.ts but cannot import — Codegen processes this file
// standalone and rejects external type references in emitter payload types.

export type CpuPayload = {
  totalUseRatio: number;
  appRatio: number;
  userRatio: number;
  systemRatio: number;
  ioWaitRatio: number;
};

export type FpsPayload = {
  fps: number;
};

export type MemoryPayload = {
  heapAllocatedMb: number;
  heapMaxMb: number;
  ramUsedMb: number;
  ramTotalMb: number;
  isLowMemory: boolean;
};

export type NetworkPayload = {
  rxMb: number;
  txMb: number;
};

export type IssuePayload = {
  id: string;
  type: string;
  severity: string;
  message: string;
  timestamp: number;
  durationMs?: number;
  threadName?: string;
};

export type JankPayload = {
  droppedFrames: number;
  jankyRatio: number;
  worstFrameMs: number;
};

export type GcPayload = {
  gcCountDelta: number;
  gcPauseMsDelta: number;
  gcCount: number;
};

export type ThermalPayload = {
  state: string;
  isThrottling: boolean;
  temperatureC: number;
};

export type JsMemoryPayload = {
  usedMb: number;
  totalMb: number;
};

export type JsGcPayload = {
  gcCount: number;
  gcPauseMs: number;
  gcCountDelta: number;
  gcPauseMsDelta: number;
};

// ─── TurboModule Spec ─────────────────────────────────────────────────────────

export interface Spec extends TurboModule {
  // Imperative methods — Codegen generates abstract Kotlin / ObjC++ overrides.
  start(config: Object): void;
  stop(): void;
  showOverlay(): void;
  hideOverlay(): void;

  // JS bridge write methods — push data from Hermes into Kamper
  reportJsMemory(usedMb: number, totalMb: number): void;
  reportJsGc(count: number, pauseMs: number): void;
  reportCrash(message: string, stack: string, isFatal: boolean): void;

  // Span tracking — routes JS spans through IssueSpans so SlowSpanDetector fires
  beginSpan(label: string, thresholdMs: number): void;
  endSpan(label: string): void;

  // Codegen emitter properties — generate `emitOnCpu(WritableMap)` style
  // methods on the spec base class for both Android (Kotlin) and iOS (ObjC++).
  // Use Codegen EventEmitter (not the legacy event emitter pattern deprecated for TurboModules).
  readonly onCpu: EventEmitter<CpuPayload>;
  readonly onFps: EventEmitter<FpsPayload>;
  readonly onMemory: EventEmitter<MemoryPayload>;
  readonly onNetwork: EventEmitter<NetworkPayload>;
  readonly onIssue: EventEmitter<IssuePayload>;
  readonly onJank: EventEmitter<JankPayload>;
  readonly onGc: EventEmitter<GcPayload>;
  readonly onThermal: EventEmitter<ThermalPayload>;
  readonly onJsMemory: EventEmitter<JsMemoryPayload>;
  readonly onJsGc: EventEmitter<JsGcPayload>;
}

// Module name 'KamperModule' MUST match Android `KamperTurboModule.NAME`,
// iOS `+ moduleName`, and JS consumers' import key.
export default TurboModuleRegistry.getEnforcing<Spec>('KamperModule');
