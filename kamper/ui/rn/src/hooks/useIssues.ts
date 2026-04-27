// src/hooks/useIssues.ts — single-metric hook (D-07).

import { useEffect, useState } from 'react';
import { Kamper } from '../Kamper';
import { _acquireEngine, _releaseEngine } from './useKamper';
import type { IssueInfo } from '../types';

/**
 * useIssues — accumulates issue events into a capped list (max 100).
 * Auto-manages engine lifecycle.
 */
export function useIssues(): IssueInfo[] {
  const [data, setData] = useState<IssueInfo[]>([]);

  useEffect(() => {
    _acquireEngine({ issues: true });
    const sub = Kamper.on('issue', (d: IssueInfo) => {
      setData(prev => [d, ...prev].slice(0, 100));
    });
    return () => {
      sub.remove();
      _releaseEngine();
    };
  }, []);

  return data;
}
