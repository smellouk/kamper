// src/Kamper.ts — Imperative API singleton (D-07).
// Wraps the Codegen TurboModule with a clean, typed surface.
// Hooks layer (./hooks/*) calls into this module — single shared engine instance.

import NativeKamperModule from './NativeKamperModule';
import type {
  CpuInfo,
  FpsInfo,
  MemoryInfo,
  NetworkInfo,
  IssueInfo,
  JankInfo,
  GcInfo,
  ThermalInfo,
  KamperConfig,
} from './types';

/**
 * EventMap — JS-friendly event-name → payload mapping.
 * Maps to Codegen EventEmitter properties on NativeKamperModule:
 *   'cpu'     → onCpu
 *   'fps'     → onFps
 *   'memory'  → onMemory
 *   'network' → onNetwork
 *   'issue'   → onIssue
 *   'jank'    → onJank
 *   'gc'      → onGc
 *   'thermal' → onThermal
 */
export type KamperEventMap = {
  cpu: CpuInfo;
  fps: FpsInfo;
  memory: MemoryInfo;
  network: NetworkInfo;
  issue: IssueInfo;
  jank: JankInfo;
  gc: GcInfo;
  thermal: ThermalInfo;
};

export type KamperSubscription = { remove(): void };

// Map JS event name → TurboModule property name (capitalized "on" + Pascal-case).
const EVENT_TO_PROP: Record<keyof KamperEventMap, string> = {
  cpu: 'onCpu',
  fps: 'onFps',
  memory: 'onMemory',
  network: 'onNetwork',
  issue: 'onIssue',
  jank: 'onJank',
  gc: 'onGc',
  thermal: 'onThermal',
};

/**
 * Kamper — imperative API for the React Native bridge.
 *
 * Usage:
 *   Kamper.start({ cpu: true, fps: true });
 *   const sub = Kamper.on('cpu', info => console.log(info));
 *   sub.remove();
 *   Kamper.stop();
 *   Kamper.showOverlay();  // Native debug overlay
 */
export const Kamper = {
  /**
   * Start the engine with optional per-module config (D-08).
   * If config is omitted, all available modules start.
   * On iOS, only cpu/fps/memory/network are emitted regardless of config
   * (XCFramework only exports those 4 modules — see CONTEXT.md).
   */
  start(config?: KamperConfig): void {
    NativeKamperModule.start(config ?? {});
  },

  /** Stop the engine. Idempotent. */
  stop(): void {
    NativeKamperModule.stop();
  },

  /**
   * Subscribe to a metric event. Returns a subscription handle whose
   * `.remove()` method unsubscribes (mirrors React Native's standard EventSubscription).
   */
  on<K extends keyof KamperEventMap>(
    event: K,
    handler: (payload: KamperEventMap[K]) => void,
  ): KamperSubscription {
    const propName = EVENT_TO_PROP[event];
    // Codegen EventEmitter<T> properties are callable as a function returning a subscription.
    // Cast through `unknown` because the spec's EventEmitter type is not callable in plain TS.
    const emitter = (NativeKamperModule as unknown as Record<string, (h: unknown) => KamperSubscription>)[propName];
    return emitter(handler as unknown);
  },

  /**
   * Convenience: remove a subscription. Equivalent to calling `.remove()` on the returned handle.
   * Provided for symmetry with Kamper.on() per D-07.
   */
  off(subscription: KamperSubscription): void {
    subscription.remove();
  },

  /** Show the native Kamper overlay (D-10, D-12). Typically called inside `__DEV__`. */
  showOverlay(): void {
    NativeKamperModule.showOverlay();
  },

  /** Hide the native Kamper overlay (D-10, D-12). */
  hideOverlay(): void {
    NativeKamperModule.hideOverlay();
  },
} as const;

export type KamperApi = typeof Kamper;
