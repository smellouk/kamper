const Metro = require('metro');
const { getDefaultConfig, mergeConfig } = require('@react-native/metro-config');
const path = require('path');

const dir = __dirname;
const config = mergeConfig(getDefaultConfig(dir), {
  watchFolders: [path.resolve(dir, '..', '..', 'libs', 'ui', 'rn')],
  resolver: { nodeModulesPaths: [path.resolve(dir, 'node_modules')] },
});

Metro.runServer(config, { host: 'localhost', port: 8081 })
  .then(() => console.log('Metro running on :8081'))
  .catch(e => { console.error('Metro error:', e.message); process.exit(1); });
