// src/hooks/useThermal.ts — single-metric hook (D-07).

import { useEffect, useState } from 'react';
import { Kamper } from '../Kamper';
import { _acquireEngine, _releaseEngine } from './useKamper';
import type { ThermalInfo } from '../types';

/**
 * useThermal — subscribes to thermal events. Auto-manages engine lifecycle.
 * Mount: starts engine (or shares with other hooks). Unmount: stops engine if last ref.
 */
export function useThermal(): ThermalInfo | null {
  const [data, setData] = useState<ThermalInfo | null>(null);

  useEffect(() => {
    _acquireEngine({ thermal: true });
    const sub = Kamper.on('thermal', (d: ThermalInfo) => {
      setData(d);
    });
    return () => {
      sub.remove();
      _releaseEngine();
    };
  }, []);

  return data;
}
