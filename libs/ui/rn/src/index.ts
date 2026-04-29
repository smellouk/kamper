// src/index.ts — Public barrel export for `react-native-kamper`.
//
// Consumers import:
//   import { Kamper, useCpu, showOverlay } from 'react-native-kamper';
//   import type { CpuInfo, KamperConfig } from 'react-native-kamper';

// Imperative API (D-07)
export { Kamper } from './Kamper';
export type { KamperApi, KamperEventMap, KamperSubscription } from './Kamper';

// Hooks (D-07)
export { useKamper } from './hooks/useKamper';
export { useCpu } from './hooks/useCpu';
export { useFps } from './hooks/useFps';
export { useMemory } from './hooks/useMemory';
export { useNetwork } from './hooks/useNetwork';
export { useIssues } from './hooks/useIssues';
export { useJank } from './hooks/useJank';
export { useGc } from './hooks/useGc';
export { useThermal } from './hooks/useThermal';

// Types (D-09)
export type {
  CpuInfo,
  FpsInfo,
  MemoryInfo,
  NetworkInfo,
  IssueInfo,
  JankInfo,
  GcInfo,
  ThermalInfo,
  KamperConfig,
} from './types';

// Top-level convenience exports (D-12)
import { Kamper as _Kamper } from './Kamper';
export const showOverlay = (): void => _Kamper.showOverlay();
export const hideOverlay = (): void => _Kamper.hideOverlay();
