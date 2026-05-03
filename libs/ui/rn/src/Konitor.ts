// src/Konitor.ts — Imperative API singleton (D-07).
// Wraps the Codegen TurboModule with a clean, typed surface.
// Hooks layer (./hooks/*) calls into this module — single shared engine instance.

import NativeKonitorModule from './NativeKonitorModule';
import type {
  CpuInfo,
  FpsInfo,
  GpuInfo,
  MemoryInfo,
  NetworkInfo,
  IssueInfo,
  JankInfo,
  GcInfo,
  ThermalInfo,
  JsMemoryInfo,
  JsGcInfo,
  UserEventInfo,
  KonitorConfig,
} from './types';

/**
 * EventMap — JS-friendly event-name → payload mapping.
 * Maps to Codegen EventEmitter properties on NativeKonitorModule:
 *   'cpu'     → onCpu
 *   'fps'     → onFps
 *   'memory'  → onMemory
 *   'network' → onNetwork
 *   'issue'   → onIssue
 *   'jank'    → onJank
 *   'gc'      → onGc
 *   'thermal' → onThermal
 */
export type KonitorEventMap = {
  cpu: CpuInfo;
  fps: FpsInfo;
  gpu: GpuInfo;
  memory: MemoryInfo;
  network: NetworkInfo;
  issue: IssueInfo;
  jank: JankInfo;
  gc: GcInfo;
  thermal: ThermalInfo;
  jsMemory: JsMemoryInfo;
  jsGc: JsGcInfo;
  userEvent: UserEventInfo;
};

export type KonitorSubscription = { remove(): void };

// Map JS event name → TurboModule property name (capitalized "on" + Pascal-case).
const EVENT_TO_PROP: Record<keyof KonitorEventMap, string> = {
  cpu: 'onCpu',
  fps: 'onFps',
  gpu: 'onGpu',
  memory: 'onMemory',
  network: 'onNetwork',
  issue: 'onIssue',
  jank: 'onJank',
  gc: 'onGc',
  thermal: 'onThermal',
  jsMemory: 'onJsMemory',
  jsGc: 'onJsGc',
  userEvent: 'onUserEvent',
};

/**
 * Konitor — imperative API for the React Native bridge.
 *
 * Usage:
 *   Konitor.start({ cpu: true, fps: true });
 *   const sub = Konitor.on('cpu', info => console.log(info));
 *   sub.remove();
 *   Konitor.stop();
 *   Konitor.showOverlay();  // Native debug overlay
 */
let _jsPollingTimer: ReturnType<typeof setInterval> | null = null;

function startJsRuntimePolling(): void {
  if (_jsPollingTimer != null) return;
  _jsPollingTimer = setInterval(() => {
    const h = (global as any).HermesInternal?.getInstrumentedStats?.();
    if (!h) return;
    // Hermes uses js_* prefix. TypedArray backing stores are external (js_externalBytes),
    // not counted in js_allocatedBytes — add both for accurate "used" reporting.
    const usedBytes  = (h.js_allocatedBytes ?? 0) + (h.js_externalBytes ?? 0);
    const totalBytes = (h.js_heapSize ?? 0) + (h.js_externalBytes ?? 0);
    const usedMb  = usedBytes  / (1024 * 1024);
    const totalMb = totalBytes / (1024 * 1024);
    const gcCount = h.js_numGCs ?? 0;
    const pauseMs = (h.js_gcTime ?? 0) / 1000;
    NativeKonitorModule.reportJsMemory(usedMb, totalMb);
    NativeKonitorModule.reportJsGc(gcCount, pauseMs);
  }, 1000);
}

function stopJsRuntimePolling(): void {
  if (_jsPollingTimer != null) {
    clearInterval(_jsPollingTimer);
    _jsPollingTimer = null;
  }
}

function hookErrorHandler(): void {
  const prev = ErrorUtils.getGlobalHandler();
  ErrorUtils.setGlobalHandler((error: Error, isFatal?: boolean) => {
    NativeKonitorModule.reportCrash(
      error?.message ?? 'Unknown error',
      error?.stack ?? '',
      isFatal ?? false,
    );
    prev?.(error, isFatal);
  });
}

export const Konitor = {
  /**
   * Start the engine with optional per-module config (D-08).
   * If config is omitted, all available modules start.
   * On iOS, only cpu/fps/memory/network are emitted regardless of config
   * (XCFramework only exports those 4 modules — see CONTEXT.md).
   */
  start(config?: KonitorConfig): void {
    NativeKonitorModule.start(config ?? {});
    startJsRuntimePolling();
    hookErrorHandler();
  },

  /** Stop the engine. Idempotent. */
  stop(): void {
    stopJsRuntimePolling();
    NativeKonitorModule.stop();
  },

  /**
   * Subscribe to a metric event. Returns a subscription handle whose
   * `.remove()` method unsubscribes (mirrors React Native's standard EventSubscription).
   */
  on<K extends keyof KonitorEventMap>(
    event: K,
    handler: (payload: KonitorEventMap[K]) => void,
  ): KonitorSubscription {
    const propName = EVENT_TO_PROP[event];
    // Codegen EventEmitter<T> properties are callable as a function returning a subscription.
    // Cast through `unknown` because the spec's EventEmitter type is not callable in plain TS.
    const emitter = (NativeKonitorModule as unknown as Record<string, (h: unknown) => KonitorSubscription>)[propName];
    return emitter(handler as unknown);
  },

  /**
   * Convenience: remove a subscription. Equivalent to calling `.remove()` on the returned handle.
   * Provided for symmetry with Konitor.on() per D-07.
   */
  off(subscription: KonitorSubscription): void {
    subscription.remove();
  },

  /** Show the native Konitor overlay (D-10, D-12). Typically called inside `__DEV__`. */
  showOverlay(): void {
    NativeKonitorModule.showOverlay();
  },

  /** Hide the native Konitor overlay (D-10, D-12). */
  hideOverlay(): void {
    NativeKonitorModule.hideOverlay();
  },

  /** Open a named span. If it exceeds thresholdMs when closed, fires a SLOW_SPAN issue. */
  beginSpan(label: string, thresholdMs: number): void {
    NativeKonitorModule.beginSpan(label, thresholdMs);
  },

  /** Close a named span opened with beginSpan. */
  endSpan(label: string): void {
    NativeKonitorModule.endSpan(label);
  },

  /** Log a named event for tracing (e.g. Perfetto). */
  logEvent(name: string): void {
    NativeKonitorModule.logEvent(name);
  },

  /** Start a timed event; returns an opaque token ID to pass to endEvent. */
  startEvent(name: string): number {
    return NativeKonitorModule.startEvent(name);
  },

  /** End a timed event started with startEvent. */
  endEvent(tokenId: number): void {
    NativeKonitorModule.endEvent(tokenId);
  },
} as const;

export type KonitorApi = typeof Konitor;
