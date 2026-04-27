// src/hooks/useJank.ts — single-metric hook (D-07).

import { useEffect, useState } from 'react';
import { Kamper } from '../Kamper';
import { _acquireEngine, _releaseEngine } from './useKamper';
import type { JankInfo } from '../types';

/**
 * useJank — subscribes to jank events. Auto-manages engine lifecycle.
 * Mount: starts engine (or shares with other hooks). Unmount: stops engine if last ref.
 */
export function useJank(): JankInfo | null {
  const [data, setData] = useState<JankInfo | null>(null);

  useEffect(() => {
    _acquireEngine({ jank: true });
    const sub = Kamper.on('jank', (d: JankInfo) => {
      setData(d);
    });
    return () => {
      sub.remove();
      _releaseEngine();
    };
  }, []);

  return data;
}
