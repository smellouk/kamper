// src/hooks/useKamper.ts — Umbrella hook + shared engine ref-counting (D-07).
//
// Single-metric hooks (useCpu, useFps, ...) import _acquireEngine / _releaseEngine
// from this file to share one engine instance per JS process.

import { useEffect, useState } from 'react';
import { Kamper } from '../Kamper';
import type {
  CpuInfo, FpsInfo, MemoryInfo, NetworkInfo,
  IssueInfo, JankInfo, GcInfo, ThermalInfo, KamperConfig,
} from '../types';

// ─── Module-level shared ref counter ──────────────────────────────────────
// One engine per JS process. start() runs on first acquire(), stop() on last release().

let activeRefs = 0;
let activeConfig: KamperConfig = {};

/**
 * @internal — used by single-metric hooks (useCpu, useFps, etc.).
 * Acquires a reference on the engine. Calls Kamper.start() if this is the first ref.
 * Returns the acquired config so callers can compose with sibling hooks.
 */
export function _acquireEngine(config: KamperConfig): void {
  if (activeRefs === 0) {
    activeConfig = { ...config };
    Kamper.start(activeConfig);
  } else {
    // Merge new flags into running config — already-true flags stay true.
    activeConfig = { ...activeConfig, ...config };
    // NOTE: We do NOT restart the engine on config diff. Adding a hook for a
    // module that wasn't enabled at start time is a known limitation — document
    // in JSDoc. Workaround: pass full config to useKamper() at app root.
  }
  activeRefs += 1;
}

/**
 * @internal — used by single-metric hooks.
 * Releases a reference. Calls the engine stop when refs hit zero.
 */
export function _releaseEngine(): void {
  activeRefs = Math.max(0, activeRefs - 1);
  if (activeRefs === 0) {
    Kamper.stop();
    activeConfig = {};
  }
}

/**
 * useKamper — umbrella hook. Subscribes to all 8 metric events according
 * to the supplied config. Mount auto-starts the engine; unmount stops it.
 *
 * Usage:
 *   const state = useKamper({ cpu: true, fps: true, memory: true });
 *   state.cpu  // CpuInfo | null
 *   state.fps  // FpsInfo | null
 *
 * For a single metric, prefer the dedicated hook: useCpu(), useFps(), etc.
 */
export interface KamperState {
  cpu: CpuInfo | null;
  fps: FpsInfo | null;
  memory: MemoryInfo | null;
  network: NetworkInfo | null;
  issues: IssueInfo[];
  jank: JankInfo | null;
  gc: GcInfo | null;
  thermal: ThermalInfo | null;
}

const EMPTY_STATE: KamperState = {
  cpu: null, fps: null, memory: null, network: null,
  issues: [], jank: null, gc: null, thermal: null,
};

export function useKamper(config?: KamperConfig): KamperState {
  const [state, setState] = useState<KamperState>(EMPTY_STATE);
  const cfg = config ?? {};

  useEffect(() => {
    _acquireEngine(cfg);

    const subs = [
      Kamper.on('cpu',     (d: CpuInfo)     => setState(s => ({ ...s, cpu: d }))),
      Kamper.on('fps',     (d: FpsInfo)     => setState(s => ({ ...s, fps: d }))),
      Kamper.on('memory',  (d: MemoryInfo)  => setState(s => ({ ...s, memory: d }))),
      Kamper.on('network', (d: NetworkInfo) => setState(s => ({ ...s, network: d }))),
      Kamper.on('issue',   (d: IssueInfo)   => setState(s => ({ ...s, issues: [d, ...s.issues].slice(0, 100) }))),
      Kamper.on('jank',    (d: JankInfo)    => setState(s => ({ ...s, jank: d }))),
      Kamper.on('gc',      (d: GcInfo)      => setState(s => ({ ...s, gc: d }))),
      Kamper.on('thermal', (d: ThermalInfo) => setState(s => ({ ...s, thermal: d }))),
    ];

    return () => {
      subs.forEach(sub => sub.remove());
      _releaseEngine();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return state;
}
