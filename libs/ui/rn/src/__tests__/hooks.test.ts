/**
 * Wave 0 skeleton — hook mount/unmount lifecycle (per D-07).
 *
 * RED state until Plan 03 ships konitor/react-native/src/hooks/*.ts and
 * konitor/react-native/src/Konitor.ts.
 * Once Plans 01 + 03 land, all imports resolve and the suite goes GREEN.
 *
 * Strategy: mock the underlying NativeKonitorModule (already done globally
 * via jest moduleNameMapper) and assert that:
 *   - mounting a hook starts the engine exactly once via Konitor.start()
 *   - mounting two hooks does NOT start the engine a second time (ref count)
 *   - unmounting the LAST active hook calls Konitor.stop()
 *
 * react-test-renderer's `renderHook` is provided by @testing-library/react-hooks
 * via the RN jest preset stack. If unavailable, tests fall back to manual
 * effect simulation by calling the hook inside a stub component.
 */

import { renderHook } from '@testing-library/react-native';
import {
  _resetNativeKonitorModuleMocks,
  start as nativeStart,
  stop as nativeStop,
} from './NativeKonitorModule.mock';
import { useKonitor } from '../hooks/useKonitor';
import { useCpu } from '../hooks/useCpu';
import { useFps } from '../hooks/useFps';
import { useMemory } from '../hooks/useMemory';
import { useNetwork } from '../hooks/useNetwork';
import { useIssues } from '../hooks/useIssues';
import { useJank } from '../hooks/useJank';
import { useGc } from '../hooks/useGc';
import { useThermal } from '../hooks/useThermal';

beforeEach(() => {
  _resetNativeKonitorModuleMocks();
});

describe('useKonitor umbrella hook (D-07)', () => {
  it('mount calls native start() exactly once', () => {
    const { unmount } = renderHook(() => useKonitor({ cpu: true }));
    expect(nativeStart).toHaveBeenCalledTimes(1);
    unmount();
  });

  it('unmount calls native stop() when ref count hits zero', () => {
    const { unmount } = renderHook(() => useKonitor({ cpu: true }));
    unmount();
    expect(nativeStop).toHaveBeenCalledTimes(1);
  });
});

describe.each([
  ['useCpu',     useCpu],
  ['useFps',     useFps],
  ['useMemory',  useMemory],
  ['useNetwork', useNetwork],
  ['useIssues',  useIssues],
  ['useJank',    useJank],
  ['useGc',      useGc],
  ['useThermal', useThermal],
])('single-metric hook %s lifecycle (D-07)', (_label, hook) => {
  it('mount triggers native start() exactly once', () => {
    const { unmount } = renderHook(() => hook());
    expect(nativeStart).toHaveBeenCalledTimes(1);
    unmount();
  });

  it('unmount triggers native stop() when ref count hits zero', () => {
    const { unmount } = renderHook(() => hook());
    unmount();
    expect(nativeStop).toHaveBeenCalledTimes(1);
  });
});

describe('shared engine ref counting across hooks (Claude\'s Discretion)', () => {
  it('mounting a second hook does NOT call native start() a second time', () => {
    const r1 = renderHook(() => useCpu());
    expect(nativeStart).toHaveBeenCalledTimes(1);
    const r2 = renderHook(() => useFps());
    expect(nativeStart).toHaveBeenCalledTimes(1);
    r1.unmount();
    // First unmount should NOT stop yet — second hook still active.
    expect(nativeStop).toHaveBeenCalledTimes(0);
    r2.unmount();
    // Last unmount stops engine.
    expect(nativeStop).toHaveBeenCalledTimes(1);
  });
});
