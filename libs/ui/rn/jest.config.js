/**
 * Jest configuration for react-native-kamper (Wave 0).
 *
 * - preset: @react-native/jest-preset (provided transitively by react-native peerDep
 *   via the consumer app's node_modules in dev workflow; resolution falls back to
 *   the demo's node_modules when running `yarn jest` from kamper/react-native/).
 * - moduleNameMapper: redirects `./NativeKamperModule` imports to the test mock so
 *   tests do not require the real TurboModule registry to be wired.
 * - transformIgnorePatterns: ensures kamper/react-native/src is transformed
 *   (default RN preset ignores node_modules but we need to compile our TS).
 * - testEnvironment: 'node' is sufficient — no DOM needed for unit tests; RN preset
 *   already supplies the matchers we use.
 */
module.exports = {
  preset: '@react-native/jest-preset',
  rootDir: '.',
  testMatch: ['<rootDir>/src/__tests__/**/*.test.ts'],
  moduleFileExtensions: ['ts', 'tsx', 'js', 'jsx', 'json'],
  moduleNameMapper: {
    // Tests import './NativeKamperModule' (relative to src/), and we redirect
    // that to the mock. Relative-path mapping uses ^ anchor for safety.
    '^\\./NativeKamperModule$': '<rootDir>/src/__tests__/NativeKamperModule.mock.ts',
    '^\\.\\./NativeKamperModule$': '<rootDir>/src/__tests__/NativeKamperModule.mock.ts',
  },
  transformIgnorePatterns: [
    'node_modules/(?!(react-native|@react-native|@react-native-community)/)',
  ],
  // No watchAll / watch mode — sampling spec from VALIDATION.md requires single-shot runs.
};
