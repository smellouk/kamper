// src/hooks/useIssues.ts — single-metric hook (D-07).

import { useEffect, useState } from 'react';
import { Konitor } from '../Konitor';
import { _acquireEngine, _releaseEngine } from './useKonitor';
import type { IssueInfo } from '../types';

/**
 * useIssues — accumulates issue events into a capped list (max 100).
 * Auto-manages engine lifecycle.
 */
export function useIssues(): IssueInfo[] {
  const [data, setData] = useState<IssueInfo[]>([]);

  useEffect(() => {
    _acquireEngine({ issues: true });
    const sub = Konitor.on('issue', (d: IssueInfo) => {
      setData(prev => [d, ...prev].slice(0, 100));
    });
    return () => {
      sub.remove();
      _releaseEngine();
    };
  }, []);

  return data;
}
