# Changelog

## 0.1.0

- Initial release: Flutter plugin for Konitor performance monitoring
- Android: CPU, FPS, Memory, Network, Issues, Jank, GC, Thermal, GPU modules via EventChannel
- iOS: CPU, FPS, Memory, Network modules via FlutterEventChannel (Android-only modules are no-ops)
- MethodChannel control: start, stop, showOverlay, hideOverlay, logEvent, startEvent, endEvent
