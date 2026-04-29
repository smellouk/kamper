/**
 * NativeKamperModule mock — Wave 0 test scaffold.
 *
 * Replaces the real TurboModule import in tests. Each method is a `jest.fn()`
 * so individual tests can assert call counts and arguments. Each EventEmitter
 * property is also a `jest.fn()` returning a subscription handle whose
 * `.remove()` is itself a `jest.fn()` — this matches the Codegen
 * `EventEmitter<T>` contract (callable, returns subscription).
 *
 * Plan 01 creates the real `kamper/react-native/src/NativeKamperModule.ts`.
 * Plan 03 hooks consume it through `Kamper.ts`. In tests, Jest's
 * moduleNameMapper redirects all `./NativeKamperModule` imports to this file.
 */

type Subscription = { remove: jest.Mock };
const makeEmitter = (): jest.Mock<Subscription, [unknown]> =>
  jest.fn(() => ({ remove: jest.fn() }));

export const start = jest.fn();
export const stop = jest.fn();
export const showOverlay = jest.fn();
export const hideOverlay = jest.fn();

export const onCpu = makeEmitter();
export const onFps = makeEmitter();
export const onMemory = makeEmitter();
export const onNetwork = makeEmitter();
export const onIssue = makeEmitter();
export const onJank = makeEmitter();
export const onGc = makeEmitter();
export const onThermal = makeEmitter();

/**
 * Default export shape mirrors `TurboModuleRegistry.getEnforcing<Spec>('KamperModule')`
 * — a single object that exposes both imperative methods and EventEmitter properties.
 */
const NativeKamperModule = {
  start,
  stop,
  showOverlay,
  hideOverlay,
  onCpu,
  onFps,
  onMemory,
  onNetwork,
  onIssue,
  onJank,
  onGc,
  onThermal,
};

export default NativeKamperModule;

/**
 * Test-only helper — clears all mock call records.
 * Call from `beforeEach` in any suite.
 */
export function _resetNativeKamperModuleMocks(): void {
  [start, stop, showOverlay, hideOverlay,
   onCpu, onFps, onMemory, onNetwork,
   onIssue, onJank, onGc, onThermal].forEach(fn => fn.mockClear());
}
