/**
 * Wave 0 skeleton — type export verification (per D-09).
 *
 * RED state until Plan 01 ships kamper/react-native/src/types.ts.
 * Once Plan 01 lands, all 9 imports resolve and the suite goes GREEN.
 *
 * These tests intentionally check structural shape rather than runtime
 * behavior — TypeScript interfaces have no runtime presence, so we use
 * type-level `satisfies` patterns and shape-assertions on plain objects
 * that conform to each interface.
 */

import type {
  CpuInfo,
  FpsInfo,
  MemoryInfo,
  NetworkInfo,
  IssueInfo,
  JankInfo,
  GcInfo,
  ThermalInfo,
  KamperConfig,
} from '../types';

describe('react-native-kamper public type exports (D-09)', () => {
  it('CpuInfo has 5 numeric fields', () => {
    const sample: CpuInfo = {
      totalUseRatio: 0,
      appRatio: 0,
      userRatio: 0,
      systemRatio: 0,
      ioWaitRatio: 0,
    };
    expect(Object.keys(sample)).toHaveLength(5);
  });

  it('FpsInfo carries fps as number', () => {
    const sample: FpsInfo = { fps: 0 };
    expect(typeof sample.fps).toBe('number');
  });

  it('MemoryInfo carries 4 numbers + isLowMemory boolean', () => {
    const sample: MemoryInfo = {
      heapAllocatedMb: 0,
      heapMaxMb: 0,
      ramUsedMb: 0,
      ramTotalMb: 0,
      isLowMemory: false,
    };
    expect(typeof sample.isLowMemory).toBe('boolean');
  });

  it('NetworkInfo carries rxMb + txMb', () => {
    const sample: NetworkInfo = { rxMb: 0, txMb: 0 };
    expect(Object.keys(sample).sort()).toEqual(['rxMb', 'txMb']);
  });

  it('IssueInfo allows id/type/severity/message/timestamp + optional duration/thread', () => {
    const sample: IssueInfo = {
      id: 'x',
      type: 't',
      severity: 's',
      message: 'm',
      timestamp: 0,
    };
    expect(sample.durationMs).toBeUndefined();
    expect(sample.threadName).toBeUndefined();
  });

  it('JankInfo carries droppedFrames + jankyRatio + worstFrameMs', () => {
    const sample: JankInfo = {
      droppedFrames: 0,
      jankyRatio: 0,
      worstFrameMs: 0,
    };
    expect(Object.keys(sample)).toHaveLength(3);
  });

  it('GcInfo carries gcCountDelta + gcPauseMsDelta + gcCount', () => {
    const sample: GcInfo = {
      gcCountDelta: 0,
      gcPauseMsDelta: 0,
      gcCount: 0,
    };
    expect(Object.keys(sample)).toHaveLength(3);
  });

  it('ThermalInfo carries state string + isThrottling boolean', () => {
    const sample: ThermalInfo = { state: 'NORMAL', isThrottling: false };
    expect(typeof sample.state).toBe('string');
    expect(typeof sample.isThrottling).toBe('boolean');
  });

  it('KamperConfig accepts all 8 module flags as optional booleans (D-08)', () => {
    const empty: KamperConfig = {};
    const full: KamperConfig = {
      cpu: true, fps: true, memory: true, network: true,
      issues: true, jank: true, gc: true, thermal: true,
    };
    expect(Object.keys(empty)).toHaveLength(0);
    expect(Object.keys(full)).toHaveLength(8);
  });
});
