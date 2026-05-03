// src/hooks/useGc.ts — single-metric hook (D-07).

import { useEffect, useState } from 'react';
import { Konitor } from '../Konitor';
import { _acquireEngine, _releaseEngine } from './useKonitor';
import type { GcInfo } from '../types';

/**
 * useGc — subscribes to gc events. Auto-manages engine lifecycle.
 * Mount: starts engine (or shares with other hooks). Unmount: stops engine if last ref.
 */
export function useGc(): GcInfo | null {
  const [data, setData] = useState<GcInfo | null>(null);

  useEffect(() => {
    _acquireEngine({ gc: true });
    const sub = Konitor.on('gc', (d: GcInfo) => {
      setData(d);
    });
    return () => {
      sub.remove();
      _releaseEngine();
    };
  }, []);

  return data;
}
