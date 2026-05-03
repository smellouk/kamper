// src/hooks/useJank.ts — single-metric hook (D-07).

import { useEffect, useState } from 'react';
import { Konitor } from '../Konitor';
import { _acquireEngine, _releaseEngine } from './useKonitor';
import type { JankInfo } from '../types';

/**
 * useJank — subscribes to jank events. Auto-manages engine lifecycle.
 * Mount: starts engine (or shares with other hooks). Unmount: stops engine if last ref.
 */
export function useJank(): JankInfo | null {
  const [data, setData] = useState<JankInfo | null>(null);

  useEffect(() => {
    _acquireEngine({ jank: true });
    const sub = Konitor.on('jank', (d: JankInfo) => {
      setData(d);
    });
    return () => {
      sub.remove();
      _releaseEngine();
    };
  }, []);

  return data;
}
