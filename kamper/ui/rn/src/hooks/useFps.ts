// src/hooks/useFps.ts — single-metric hook (D-07).

import { useEffect, useState } from 'react';
import { Kamper } from '../Kamper';
import { _acquireEngine, _releaseEngine } from './useKamper';
import type { FpsInfo } from '../types';

/**
 * useFps — subscribes to fps events. Auto-manages engine lifecycle.
 * Mount: starts engine (or shares with other hooks). Unmount: stops engine if last ref.
 */
export function useFps(): FpsInfo | null {
  const [data, setData] = useState<FpsInfo | null>(null);

  useEffect(() => {
    _acquireEngine({ fps: true });
    const sub = Kamper.on('fps', (d: FpsInfo) => {
      setData(d);
    });
    return () => {
      sub.remove();
      _releaseEngine();
    };
  }, []);

  return data;
}
