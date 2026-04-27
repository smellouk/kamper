const { getDefaultConfig, mergeConfig } = require('@react-native/metro-config');
const path = require('path');

/**
 * Metro configuration
 * https://reactnative.dev/docs/metro
 *
 * watchFolders: Metro must watch `kamper/ui/rn/` because it lives outside
 * the project root and Metro only follows symlinks/file: deps within watched paths.
 * resolver.nodeModulesPaths: ensures the demo's own node_modules win over any nested
 * node_modules that might shadow react-native (Pitfall 5 in 12-RESEARCH.md).
 *
 * @type {import('@react-native/metro-config').MetroConfig}
 */
const config = {
  watchFolders: [
    path.resolve(__dirname, '..', '..', 'kamper', 'ui', 'rn'),
  ],
  resolver: {
    nodeModulesPaths: [
      path.resolve(__dirname, 'node_modules'),
    ],
  },
};

module.exports = mergeConfig(getDefaultConfig(__dirname), config);
