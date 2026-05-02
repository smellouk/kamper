// src/types.ts — Public TypeScript types exposed by react-native-kamper.
// Field names match canonical Kotlin emit payloads (see KamperModule.kt lines 49-113).

export interface CpuInfo {
  totalUseRatio: number;
  appRatio: number;
  userRatio: number;
  systemRatio: number;
  ioWaitRatio: number;
}

export interface FpsInfo {
  fps: number;
}

export interface MemoryInfo {
  heapAllocatedMb: number;
  heapMaxMb: number;
  ramUsedMb: number;
  ramTotalMb: number;
  isLowMemory: boolean;
}

export interface NetworkInfo {
  rxMb: number;
  txMb: number;
}

export interface IssueInfo {
  id: string;
  type: string;
  severity: string;
  message: string;
  timestamp: number;
  durationMs?: number;
  threadName?: string;
}

export interface JankInfo {
  droppedFrames: number;
  jankyRatio: number;
  worstFrameMs: number;
}

export interface GcInfo {
  gcCountDelta: number;
  gcPauseMsDelta: number;
  gcCount: number;
}

export interface GpuInfo {
  utilization: number;
  usedMemoryMb: number;
  totalMemoryMb: number;
  curFreqKhz: number;
  maxFreqKhz: number;
  appUtilization: number;
  rendererUtilization: number;
  tilerUtilization: number;
  computeUtilization: number;
}

export interface ThermalInfo {
  state: string;
  isThrottling: boolean;
  temperatureC: number;
}

export interface JsMemoryInfo {
  usedMb: number;
  totalMb: number;
}

export interface JsGcInfo {
  gcCount: number;
  gcPauseMs: number;
  gcCountDelta: number;
  gcPauseMsDelta: number;
}

/**
 * KamperConfig — per-module enable flags (D-08).
 * All flags default to `true` when undefined; pass `false` to disable a module.
 * Note: On iOS only cpu/fps/memory/network are available — issues/jank/gc/thermal
 * flags are silently ignored on iOS (XCFramework only exports 4 modules).
 */
export interface KamperConfig {
  cpu?: boolean;
  fps?: boolean;
  memory?: boolean;
  network?: boolean;
  issues?: boolean;
  jank?: boolean;
  gc?: boolean;
  thermal?: boolean;
  gpu?: boolean;
  jsMemory?: boolean;
  jsGc?: boolean;
  jsCrash?: boolean;
}
