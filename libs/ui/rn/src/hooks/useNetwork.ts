// src/hooks/useNetwork.ts — single-metric hook (D-07).

import { useEffect, useState } from 'react';
import { Kamper } from '../Kamper';
import { _acquireEngine, _releaseEngine } from './useKamper';
import type { NetworkInfo } from '../types';

/**
 * useNetwork — subscribes to network events. Auto-manages engine lifecycle.
 * Mount: starts engine (or shares with other hooks). Unmount: stops engine if last ref.
 */
export function useNetwork(): NetworkInfo | null {
  const [data, setData] = useState<NetworkInfo | null>(null);

  useEffect(() => {
    _acquireEngine({ network: true });
    const sub = Kamper.on('network', (d: NetworkInfo) => {
      setData(d);
    });
    return () => {
      sub.remove();
      _releaseEngine();
    };
  }, []);

  return data;
}
