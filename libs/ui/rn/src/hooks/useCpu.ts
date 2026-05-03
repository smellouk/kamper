// src/hooks/useCpu.ts — single-metric hook (D-07).

import { useEffect, useState } from 'react';
import { Konitor } from '../Konitor';
import { _acquireEngine, _releaseEngine } from './useKonitor';
import type { CpuInfo } from '../types';

/**
 * useCpu — subscribes to cpu events. Auto-manages engine lifecycle.
 * Mount: starts engine (or shares with other hooks). Unmount: stops engine if last ref.
 */
export function useCpu(): CpuInfo | null {
  const [data, setData] = useState<CpuInfo | null>(null);

  useEffect(() => {
    _acquireEngine({ cpu: true });
    const sub = Konitor.on('cpu', (d: CpuInfo) => {
      setData(d);
    });
    return () => {
      sub.remove();
      _releaseEngine();
    };
  }, []);

  return data;
}
