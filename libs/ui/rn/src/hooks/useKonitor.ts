// src/hooks/useKonitor.ts — Umbrella hook + shared engine ref-counting (D-07).
//
// Single-metric hooks (useCpu, useFps, ...) import _acquireEngine / _releaseEngine
// from this file to share one engine instance per JS process.

import { useEffect, useState } from 'react';
import { Konitor } from '../Konitor';
import type {
  CpuInfo, FpsInfo, MemoryInfo, NetworkInfo,
  IssueInfo, JankInfo, GcInfo, ThermalInfo, KonitorConfig,
} from '../types';

// ─── Module-level shared ref counter ──────────────────────────────────────
// One engine per JS process. start() runs on first acquire(), stop() on last release().

let activeRefs = 0;
let activeConfig: KonitorConfig = {};

/**
 * @internal — used by single-metric hooks (useCpu, useFps, etc.).
 * Acquires a reference on the engine. Calls Konitor.start() if this is the first ref.
 * Returns the acquired config so callers can compose with sibling hooks.
 */
export function _acquireEngine(config: KonitorConfig): void {
  if (activeRefs === 0) {
    activeConfig = { ...config };
    Konitor.start(activeConfig);
  } else {
    // Merge new flags into running config — already-true flags stay true.
    activeConfig = { ...activeConfig, ...config };
    // NOTE: We do NOT restart the engine on config diff. Adding a hook for a
    // module that wasn't enabled at start time is a known limitation — document
    // in JSDoc. Workaround: pass full config to useKonitor() at app root.
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
    Konitor.stop();
    activeConfig = {};
  }
}

/**
 * useKonitor — umbrella hook. Subscribes to all 8 metric events according
 * to the supplied config. Mount auto-starts the engine; unmount stops it.
 *
 * Usage:
 *   const state = useKonitor({ cpu: true, fps: true, memory: true });
 *   state.cpu  // CpuInfo | null
 *   state.fps  // FpsInfo | null
 *
 * For a single metric, prefer the dedicated hook: useCpu(), useFps(), etc.
 */
export interface KonitorState {
  cpu: CpuInfo | null;
  fps: FpsInfo | null;
  memory: MemoryInfo | null;
  network: NetworkInfo | null;
  issues: IssueInfo[];
  jank: JankInfo | null;
  gc: GcInfo | null;
  thermal: ThermalInfo | null;
}

const EMPTY_STATE: KonitorState = {
  cpu: null, fps: null, memory: null, network: null,
  issues: [], jank: null, gc: null, thermal: null,
};

export function useKonitor(config?: KonitorConfig): KonitorState {
  const [state, setState] = useState<KonitorState>(EMPTY_STATE);
  const cfg = config ?? {};

  useEffect(() => {
    _acquireEngine(cfg);

    const subs = [
      Konitor.on('cpu',     (d: CpuInfo)     => setState(s => ({ ...s, cpu: d }))),
      Konitor.on('fps',     (d: FpsInfo)     => setState(s => ({ ...s, fps: d }))),
      Konitor.on('memory',  (d: MemoryInfo)  => setState(s => ({ ...s, memory: d }))),
      Konitor.on('network', (d: NetworkInfo) => setState(s => ({ ...s, network: d }))),
      Konitor.on('issue',   (d: IssueInfo)   => setState(s => ({ ...s, issues: [d, ...s.issues].slice(0, 100) }))),
      Konitor.on('jank',    (d: JankInfo)    => setState(s => ({ ...s, jank: d }))),
      Konitor.on('gc',      (d: GcInfo)      => setState(s => ({ ...s, gc: d }))),
      Konitor.on('thermal', (d: ThermalInfo) => setState(s => ({ ...s, thermal: d }))),
    ];

    return () => {
      subs.forEach(sub => sub.remove());
      _releaseEngine();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return state;
}
