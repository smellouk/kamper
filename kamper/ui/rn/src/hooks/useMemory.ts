// src/hooks/useMemory.ts — single-metric hook (D-07).

import { useEffect, useState } from 'react';
import { Kamper } from '../Kamper';
import { _acquireEngine, _releaseEngine } from './useKamper';
import type { MemoryInfo } from '../types';

/**
 * useMemory — subscribes to memory events. Auto-manages engine lifecycle.
 * Mount: starts engine (or shares with other hooks). Unmount: stops engine if last ref.
 */
export function useMemory(): MemoryInfo | null {
  const [data, setData] = useState<MemoryInfo | null>(null);

  useEffect(() => {
    _acquireEngine({ memory: true });
    const sub = Kamper.on('memory', (d: MemoryInfo) => {
      setData(d);
    });
    return () => {
      sub.remove();
      _releaseEngine();
    };
  }, []);

  return data;
}
