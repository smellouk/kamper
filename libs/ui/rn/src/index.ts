// src/index.ts — Public barrel export for `react-native-konitor`.
//
// Consumers import:
//   import { Konitor, useCpu, showOverlay } from 'react-native-konitor';
//   import type { CpuInfo, KonitorConfig } from 'react-native-konitor';

// Imperative API (D-07)
export { Konitor } from './Konitor';
export type { KonitorApi, KonitorEventMap, KonitorSubscription } from './Konitor';

// Hooks (D-07)
export { useKonitor } from './hooks/useKonitor';
export type { KonitorState } from './hooks/useKonitor';
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
  GpuInfo,
  MemoryInfo,
  NetworkInfo,
  IssueInfo,
  JankInfo,
  GcInfo,
  ThermalInfo,
  JsMemoryInfo,
  JsGcInfo,
  UserEventInfo,
  KonitorConfig,
} from './types';

// Top-level convenience exports (D-12)
import { Konitor as _Konitor } from './Konitor';
export const showOverlay = (): void => _Konitor.showOverlay();
export const hideOverlay = (): void => _Konitor.hideOverlay();
