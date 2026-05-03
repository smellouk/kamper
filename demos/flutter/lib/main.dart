import 'dart:async';
import 'dart:io' show Platform, ProcessInfo;
import 'dart:math' show cos, sin, pi;
import 'package:flutter/foundation.dart' show compute;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:http/http.dart' as http;
import 'package:konitor_flutter/konitor_flutter.dart';

// Catppuccin Mocha palette
const _base     = Color(0xFF1E1E2E);
const _mantle   = Color(0xFF181825);
const _surface0 = Color(0xFF313244);
const _surface1 = Color(0xFF45475A);
const _overlay1 = Color(0xFF7F849C);
const _text     = Color(0xFFCDD6F4);
const _muted    = Color(0xFF6C7086);
const _blue     = Color(0xFF89B4FA);
const _green    = Color(0xFFA6E3A1);
const _yellow   = Color(0xFFF9E2AF);
const _peach    = Color(0xFFFAB387);
const _mauve    = Color(0xFFCBA6F7);
const _teal     = Color(0xFF94E2D5);
const _red      = Color(0xFFF38BA8);

const _animPalette = [_blue, _green, _yellow, _peach, _mauve, _teal];

void main() {
  runApp(const KonitorDemoApp());
}

// Top-level function required by compute()
Uint8List _allocAndTouch(int size) {
  final buf = Uint8List(size);
  for (var i = 0; i < size; i += 4096) {
    buf[i] = 0xAA;
  }
  return buf;
}

// ── Event entry with wall-clock timestamp ──────────────────────────────────────
class _EventEntry {
  final UserEventInfo info;
  final int wallClockMs;
  _EventEntry(this.info, this.wallClockMs);
}

// ── FPS rotating-dots animation ────────────────────────────────────────────────
class _FpsAnimation extends StatefulWidget {
  const _FpsAnimation();

  @override
  State<_FpsAnimation> createState() => _FpsAnimationState();
}

class _FpsAnimationState extends State<_FpsAnimation>
    with SingleTickerProviderStateMixin {
  late final AnimationController _ctrl;

  @override
  void initState() {
    super.initState();
    _ctrl = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 4),
    )..repeat();
  }

  @override
  void dispose() {
    _ctrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    const size      = 220.0;
    const center    = size / 2;
    const outerR    = size * 0.30;
    const innerR    = size * 0.15;
    const outerDotR = size * 0.040;
    const innerDotR = size * 0.020;
    const centerDotR = size * 0.013;

    return AnimatedBuilder(
      animation: _ctrl,
      builder: (context, _) {
        final outerAngle = _ctrl.value * 2 * pi;
        final innerAngle = -_ctrl.value * 2 * pi * 1.5;
        return SizedBox(
          width: size,
          height: size,
          child: Stack(
            children: [
              Container(
                width: size,
                height: size,
                decoration: const BoxDecoration(
                  color: _mantle,
                  shape: BoxShape.circle,
                ),
              ),
              for (var i = 0; i < 6; i++)
                Positioned(
                  left: center + outerR * cos((i / 6) * 2 * pi + outerAngle) - outerDotR,
                  top:  center + outerR * sin((i / 6) * 2 * pi + outerAngle) - outerDotR,
                  child: Container(
                    width: outerDotR * 2,
                    height: outerDotR * 2,
                    decoration: BoxDecoration(
                      color: _animPalette[i],
                      shape: BoxShape.circle,
                    ),
                  ),
                ),
              for (var i = 0; i < 6; i++)
                Positioned(
                  left: center + innerR * cos((i / 6) * 2 * pi + innerAngle) - innerDotR,
                  top:  center + innerR * sin((i / 6) * 2 * pi + innerAngle) - innerDotR,
                  child: Opacity(
                    opacity: 0.7,
                    child: Container(
                      width: innerDotR * 2,
                      height: innerDotR * 2,
                      decoration: BoxDecoration(
                        color: _animPalette[(i + 2) % 6],
                        shape: BoxShape.circle,
                      ),
                    ),
                  ),
                ),
              Positioned(
                left: center - centerDotR,
                top:  center - centerDotR,
                child: Container(
                  width: centerDotR * 2,
                  height: centerDotR * 2,
                  decoration: const BoxDecoration(
                    color: _surface1,
                    shape: BoxShape.circle,
                  ),
                ),
              ),
            ],
          ),
        );
      },
    );
  }
}

// ── App ────────────────────────────────────────────────────────────────────────
class KonitorDemoApp extends StatelessWidget {
  const KonitorDemoApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: Platform.isAndroid ? 'K|Android|FL' : 'K|iOS|FL',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        brightness: Brightness.dark,
        scaffoldBackgroundColor: _base,
        colorScheme: const ColorScheme.dark(
          surface: _surface0,
          primary: _blue,
          secondary: _mauve,
          error: _red,
          onSurface: _text,
          onPrimary: _base,
        ),
        textTheme: const TextTheme(
          bodyMedium: TextStyle(color: _text),
          bodySmall: TextStyle(color: _overlay1),
        ),
        appBarTheme: const AppBarTheme(
          backgroundColor: _mantle,
          foregroundColor: _text,
          elevation: 0,
        ),
        tabBarTheme: const TabBarTheme(
          labelColor: _blue,
          unselectedLabelColor: _muted,
          indicatorColor: _blue,
        ),
        elevatedButtonTheme: ElevatedButtonThemeData(
          style: ElevatedButton.styleFrom(
            backgroundColor: _blue,
            foregroundColor: _base,
          ),
        ),
      ),
      home: const HomePage(),
    );
  }
}

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  bool _running = false;
  CpuInfo? _cpu;
  FpsInfo? _fps;
  MemoryInfo? _memory;
  NetworkInfo? _network;
  double _peakRxMb = 0;
  double _peakTxMb = 0;
  List<IssueInfo> _issues = [];
  JankInfo? _jank;
  GcInfo? _gc;
  ThermalInfo? _thermal;
  GpuInfo? _gpu;
  bool _gpuUnavailable = false;
  Timer? _gpuAvailTimer;
  List<_EventEntry> _events = [];
  final List<StreamSubscription<dynamic>> _subs = [];
  bool _networkFetching = false;
  bool _cpuStress = false;
  bool _gpuStress = false;
  bool _thermalStress = false;
  String _spanStatus = '';
  final _eventController = TextEditingController();
  final Map<String, int> _activeEvents = {};

  // Memory alloc state
  Uint8List? _allocatedChunk; // holds reference to prevent GC collection
  String _memAllocMsg = '';

  static const _tabs = [
    'CPU', 'GPU', 'FPS', 'MEMORY', 'EVENTS',
    'NETWORK', 'ISSUES', 'JANK', 'GC', 'THERMAL',
  ];

  @override
  void initState() {
    super.initState();
    SystemChrome.setSystemUIOverlayStyle(
      const SystemUiOverlayStyle(statusBarBrightness: Brightness.dark),
    );
    // Auto-start monitoring on launch
    _startMonitoring();
  }

  @override
  void dispose() {
    _gpuAvailTimer?.cancel();
    for (final sub in _subs) { sub.cancel(); }
    _subs.clear();
    _eventController.dispose();
    super.dispose();
  }

  Future<void> _startMonitoring() async {
    await Konitor.start();
    await Konitor.showOverlay();
    setState(() => _running = true);

    // GPU availability watchdog — fires if GPU data never arrives (e.g. emulator)
    if (Platform.isAndroid) {
      _gpuAvailTimer = Timer(const Duration(seconds: 5), () {
        if (mounted && _gpu == null) {
          setState(() => _gpuUnavailable = true);
        }
      });
    }

    _subs.addAll([
      Konitor.cpuStream.listen((d) => setState(() => _cpu = d)),
      Konitor.fpsStream.listen((d) => setState(() => _fps = d)),
      Konitor.memoryStream.listen((d) => setState(() => _memory = d)),
      Konitor.networkStream.listen((d) => setState(() {
            _network = d;
            if (d.rxMb > _peakRxMb) _peakRxMb = d.rxMb;
            if (d.txMb > _peakTxMb) _peakTxMb = d.txMb;
          })),
      Konitor.issuesStream
          .listen((d) => setState(() => _issues = [d, ..._issues])),
      if (Platform.isAndroid) ...[
        Konitor.jankStream.listen((d) => setState(() => _jank = d)),
        Konitor.gcStream.listen((d) => setState(() => _gc = d)),
        Konitor.thermalStream.listen((d) => setState(() => _thermal = d)),
        Konitor.gpuStream.listen((d) {
          _gpuAvailTimer?.cancel();
          setState(() {
            _gpu = d;
            _gpuUnavailable = false;
          });
        }),
      ],
      Konitor.userEventStream.listen((d) => setState(() => _events = [
            _EventEntry(d, DateTime.now().millisecondsSinceEpoch),
            ..._events,
          ])),
    ]);
  }

  Future<void> _stopMonitoring() async {
    await Konitor.stop();
    await Konitor.hideOverlay();
    _gpuAvailTimer?.cancel();
    setState(() {
      _running = false;
      _gpuUnavailable = false;
      _peakRxMb = 0;
      _peakTxMb = 0;
    });
    for (final sub in _subs) { sub.cancel(); }
    _subs.clear();
  }

  void _simulateCpuStress(bool active) {
    setState(() => _cpuStress = active);
    if (active) {
      Future(() {
        var sum = 0.0;
        for (var i = 0; i < 50000000; i++) {
          sum += i * 3.14159;
        }
        debugPrint('stress sum: $sum');
        if (mounted && _cpuStress) _simulateCpuStress(true);
      });
    }
  }

  void _simulateGpuStress(bool active) => setState(() => _gpuStress = active);

  void _simulateThermalStress(bool active) {
    setState(() => _thermalStress = active);
    _simulateCpuStress(active);
  }

  void _simulateJank() {
    Konitor.simulateJank();
  }

  void _simulateGc() {
    final _ = List.generate(1000000, (_) => Object());
    setState(() {});
    debugPrint('GC pressure allocated: ${_.length} objects');
  }

  bool _allocating = false;

  int _currentRss() {
    try {
      return ProcessInfo.currentRss;
    } catch (_) {
      return 0;
    }
  }

  Future<void> _allocMemory() async {
    if (_allocating) return;
    final rssBefore = _currentRss();
    setState(() {
      _allocating = true;
      _memAllocMsg = 'Allocating…';
    });
    try {
      // compute() runs on a worker isolate — UI stays responsive.
      final chunk = await compute(_allocAndTouch, 32 * 1024 * 1024);
      if (!mounted) return;
      _allocatedChunk = chunk;
      final rssAfter = _currentRss();
      final deltaMb = ((rssAfter - rssBefore) / (1024 * 1024)).toStringAsFixed(1);
      setState(() {
        _allocating = false;
        _memAllocMsg = 'Allocated 32 MB (RSS +$deltaMb MB)';
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _allocating = false;
        _memAllocMsg = 'Error: $e';
      });
    }
  }

  void _freeMemory() {
    _allocatedChunk = null;
    setState(() {
      _memAllocMsg = 'Released — awaiting GC';
    });
    Future.delayed(const Duration(seconds: 3), () {
      if (mounted) setState(() => _memAllocMsg = '');
    });
  }

  Future<void> _testDownload() async {
    setState(() => _networkFetching = true);
    try {
      final response = await http.get(
        Uri.parse('https://speed.cloudflare.com/__down?bytes=5000000'),
      );
      if (response.statusCode != 200 && mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Download failed')),
        );
      }
    } catch (_) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Error: network test failed')),
        );
      }
    } finally {
      if (mounted) setState(() => _networkFetching = false);
    }
  }

  // ── Shared helpers ─────────────────────────────────────────────────────────

  Widget _placeholder() => Center(
        child: Text(
          _running ? 'Waiting for data…' : '—',
          style: const TextStyle(color: _overlay1, fontSize: 14),
        ),
      );

  Widget _metricRow(
    String label,
    double value,
    Color barColor,
    String display,
  ) =>
      Padding(
        padding: const EdgeInsets.symmetric(vertical: 4),
        child: Row(
          children: [
            SizedBox(
              width: 80,
              child: Text(
                label,
                style: const TextStyle(color: _text, fontSize: 13),
              ),
            ),
            Expanded(
              child: LinearProgressIndicator(
                value: value.clamp(0.0, 1.0),
                backgroundColor: _surface1,
                valueColor: AlwaysStoppedAnimation<Color>(barColor),
                minHeight: 7,
                borderRadius: BorderRadius.circular(4),
              ),
            ),
            const SizedBox(width: 8),
            SizedBox(
              width: 72,
              child: Text(
                display,
                textAlign: TextAlign.right,
                style: const TextStyle(
                  fontFamily: 'monospace',
                  fontSize: 12,
                  color: _text,
                ),
              ),
            ),
          ],
        ),
      );

  Widget _sectionTitle(String title, {bool first = false}) => Padding(
        padding: EdgeInsets.only(bottom: 8, top: first ? 0 : 20),
        child: Text(
          title,
          style: const TextStyle(
            color: _text,
            fontSize: 14,
            fontWeight: FontWeight.w700,
          ),
        ),
      );

  Widget _footer(Widget child) => ColoredBox(
        color: _mantle,
        child: SafeArea(
          top: false,
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
            child: child,
          ),
        ),
      );

  Widget _btn({
    required String label,
    required VoidCallback? onPressed,
    bool active = false,
  }) =>
      ElevatedButton(
        onPressed: onPressed,
        style: ElevatedButton.styleFrom(
          backgroundColor: active ? _surface1 : _blue,
          foregroundColor: active ? _text : _base,
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
          tapTargetSize: MaterialTapTargetSize.shrinkWrap,
          textStyle: const TextStyle(
            fontSize: 12,
            fontWeight: FontWeight.w700,
            letterSpacing: 0.5,
          ),
        ),
        child: Text(label),
      );

  // ── CPU TAB ────────────────────────────────────────────────────────────────

  Widget _buildCpuTab() {
    final cpu = _cpu;
    return Column(
      children: [
        Expanded(
          child: cpu == null
              ? _placeholder()
              : SingleChildScrollView(
                  padding: const EdgeInsets.fromLTRB(16, 20, 16, 12),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      _sectionTitle('CPU USAGE', first: true),
                      _metricRow('Total',   cpu.totalUseRatio, _blue,   '${(cpu.totalUseRatio * 100).toStringAsFixed(1)}%'),
                      _metricRow('App',     cpu.appRatio,      _green,  '${(cpu.appRatio * 100).toStringAsFixed(1)}%'),
                      _metricRow('User',    cpu.userRatio,     _yellow, '${(cpu.userRatio * 100).toStringAsFixed(1)}%'),
                      _metricRow('System',  cpu.systemRatio,   _peach,  '${(cpu.systemRatio * 100).toStringAsFixed(1)}%'),
                      _metricRow('IO Wait', cpu.ioWaitRatio,   _mauve,  '${(cpu.ioWaitRatio * 100).toStringAsFixed(1)}%'),
                    ],
                  ),
                ),
        ),
        _footer(
          SizedBox(
            width: double.infinity,
            child: _btn(
              label: _cpuStress ? 'STOP CPU LOAD' : 'START CPU LOAD',
              onPressed: () => _simulateCpuStress(!_cpuStress),
              active: _cpuStress,
            ),
          ),
        ),
      ],
    );
  }

  // ── GPU TAB (Android only) ─────────────────────────────────────────────────

  Widget _notAvailableScreen(String label) => Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text(
              'N/A',
              style: TextStyle(
                fontSize: 80,
                fontFamily: 'monospace',
                color: _red,
                fontWeight: FontWeight.w700,
              ),
            ),
            Text(
              label,
              style: const TextStyle(fontSize: 12, color: _overlay1),
            ),
            const SizedBox(height: 16),
            const Text(
              'Not available on this device',
              style: TextStyle(color: _red, fontSize: 14),
            ),
          ],
        ),
      );

  Widget _buildGpuTab() {
    final gpu = _gpu;
    if (_gpuUnavailable) return _notAvailableScreen('gpu usage %');
    if (gpu == null) return _placeholder();

    final utilStr = gpu.utilization >= 0 ? '${gpu.utilization.toStringAsFixed(1)}%' : '—';
    return Column(
      children: [
        Expanded(
          child: SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(16, 20, 16, 12),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Center(
                  child: Column(
                    children: [
                      Text(
                        utilStr,
                        style: const TextStyle(
                          fontSize: 80,
                          fontFamily: 'monospace',
                          color: _mauve,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      const Text(
                        'GPU usage %',
                        style: TextStyle(fontSize: 12, color: _overlay1),
                      ),
                    ],
                  ),
                ),
                _sectionTitle('FREQUENCY'),
                _metricRow(
                  'Cur Freq',
                  gpu.curFreqKhz >= 0 && gpu.maxFreqKhz > 0
                      ? gpu.curFreqKhz / gpu.maxFreqKhz
                      : 0.0,
                  _mauve,
                  gpu.curFreqKhz >= 0
                      ? '${(gpu.curFreqKhz / 1000).toStringAsFixed(0)} MHz'
                      : '—',
                ),
                _metricRow(
                  'Max Freq',
                  1.0,
                  _surface1,
                  gpu.maxFreqKhz >= 0
                      ? '${(gpu.maxFreqKhz / 1000).toStringAsFixed(0)} MHz'
                      : '—',
                ),
                _sectionTitle('MEMORY'),
                _metricRow(
                  'Used',
                  gpu.totalMemoryMb > 0 ? gpu.usedMemoryMb / gpu.totalMemoryMb : 0.0,
                  _peach,
                  gpu.usedMemoryMb >= 0 ? '${gpu.usedMemoryMb.toStringAsFixed(0)} MB' : '—',
                ),
                _metricRow(
                  'Total',
                  1.0,
                  _surface1,
                  gpu.totalMemoryMb >= 0 ? '${gpu.totalMemoryMb.toStringAsFixed(0)} MB' : '—',
                ),
                _sectionTitle('BREAKDOWN'),
                _metricRow('App',      gpu.appUtilization / 100.0,      _mauve, gpu.appUtilization >= 0 ? '${gpu.appUtilization.toStringAsFixed(1)}%' : 'N/A'),
                _metricRow('Renderer', gpu.rendererUtilization / 100.0, _blue,  gpu.rendererUtilization >= 0 ? '${gpu.rendererUtilization.toStringAsFixed(1)}%' : 'N/A'),
                if (_gpuStress) ...[
                  const SizedBox(height: 16),
                  const _GpuStressCanvas(),
                ],
              ],
            ),
          ),
        ),
        _footer(
          SizedBox(
            width: double.infinity,
            child: _btn(
              label: _gpuStress ? 'STOP STRESS' : 'STRESS GPU',
              onPressed: _running ? () => _simulateGpuStress(!_gpuStress) : null,
              active: _gpuStress,
            ),
          ),
        ),
      ],
    );
  }

  // ── FPS TAB ────────────────────────────────────────────────────────────────

  Widget _buildFpsTab() {
    final fpsData = _fps;
    final fpsValue = fpsData?.fps;
    final fpsColor = fpsValue == null
        ? _muted
        : fpsValue >= 55
            ? _green
            : fpsValue >= 30
                ? _yellow
                : _red;

    return Column(
      children: [
        Expanded(
          child: SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(16, 20, 16, 12),
            child: Column(
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  crossAxisAlignment: CrossAxisAlignment.baseline,
                  textBaseline: TextBaseline.alphabetic,
                  children: [
                    Text(
                      fpsValue != null ? '$fpsValue' : '—',
                      style: TextStyle(
                        fontSize: 80,
                        fontFamily: 'monospace',
                        color: fpsColor,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    const SizedBox(width: 6),
                    const Text(
                      'fps',
                      style: TextStyle(fontSize: 16, color: _overlay1),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
                const _FpsAnimation(),
              ],
            ),
          ),
        ),
        _footer(
          const Center(
            child: Text(
              'Choreographer-based frame measurement',
              style: TextStyle(fontSize: 11, color: _overlay1),
            ),
          ),
        ),
      ],
    );
  }

  // ── MEMORY TAB ─────────────────────────────────────────────────────────────

  Widget _buildMemoryTab() {
    final mem = _memory;
    return Column(
      children: [
        Expanded(
          child: mem == null
              ? _placeholder()
              : SingleChildScrollView(
                  padding: const EdgeInsets.fromLTRB(16, 20, 16, 12),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      _sectionTitle('NATIVE — HEAP', first: true),
                      _metricRow(
                        'Heap',
                        mem.heapMaxMb > 0 ? mem.heapAllocatedMb / mem.heapMaxMb : 0.0,
                        _green,
                        '${mem.heapAllocatedMb.toStringAsFixed(1)} MB',
                      ),
                      _metricRow('Max Heap', 1.0, _surface1, '${mem.heapMaxMb.toStringAsFixed(1)} MB'),
                      _sectionTitle('NATIVE — SYSTEM RAM'),
                      _metricRow(
                        'RAM',
                        mem.ramTotalMb > 0 ? mem.ramUsedMb / mem.ramTotalMb : 0.0,
                        _blue,
                        '${mem.ramUsedMb.toStringAsFixed(1)} MB',
                      ),
                      Padding(
                        padding: const EdgeInsets.only(top: 4),
                        child: Text(
                          '${mem.ramUsedMb.toStringAsFixed(1)} MB / ${mem.ramTotalMb.toStringAsFixed(0)} MB total',
                          style: const TextStyle(
                            fontSize: 11,
                            fontFamily: 'monospace',
                            color: _overlay1,
                          ),
                        ),
                      ),
                      _sectionTitle('DART VM'),
                      Builder(builder: (_) {
                        final rssMb = _currentRss() / (1024 * 1024);
                        return _metricRow(
                          'RSS',
                          (rssMb / 512).clamp(0.0, 1.0),
                          _mauve,
                          '${rssMb.toStringAsFixed(1)} MB',
                        );
                      }),
                      if (mem.isLowMemory)
                        Container(
                          margin: const EdgeInsets.only(top: 16),
                          padding: const EdgeInsets.all(12),
                          decoration: BoxDecoration(
                            color: _surface0,
                            borderRadius: BorderRadius.circular(8),
                            border: Border.all(color: _yellow),
                          ),
                          child: const Row(
                            children: [
                              Icon(Icons.warning_amber_rounded, color: _yellow, size: 18),
                              SizedBox(width: 8),
                              Text('⚠ Low Memory', style: TextStyle(color: _yellow)),
                            ],
                          ),
                        ),
                      if (_memAllocMsg.isNotEmpty)
                        Padding(
                          padding: const EdgeInsets.only(top: 12),
                          child: Text(
                            _memAllocMsg,
                            style: const TextStyle(
                              fontSize: 12,
                              fontFamily: 'monospace',
                              color: _overlay1,
                            ),
                          ),
                        ),
                    ],
                  ),
                ),
        ),
        _footer(
          Row(
            children: [
              Expanded(
                child: _btn(
                  label: _allocating ? 'ALLOCATING…' : 'ALLOC 32 MB',
                  onPressed: _allocating ? null : _allocMemory,
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: _btn(
                  label: 'FREE / GC',
                  onPressed: _allocatedChunk != null && !_allocating ? _freeMemory : null,
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  // ── EVENTS TAB ─────────────────────────────────────────────────────────────

  String _fmtEventTime(int ms) {
    final dt = DateTime.fromMillisecondsSinceEpoch(ms);
    final h  = dt.hour.toString().padLeft(2, '0');
    final m  = dt.minute.toString().padLeft(2, '0');
    final s  = dt.second.toString().padLeft(2, '0');
    return '$h:$m:$s';
  }

  Widget _buildEventsTab() {
    return Column(
      children: [
        // Event log — takes remaining space
        Expanded(
          child: _events.isEmpty
              ? const Center(
                  child: Text(
                    'No events logged',
                    style: TextStyle(color: _overlay1, fontSize: 14),
                  ),
                )
              : ListView.builder(
                  itemCount: _events.length,
                  itemBuilder: (context, index) {
                    final entry    = _events[index];
                    final event    = entry.info;
                    final hasDur   = event.durationMs != null;
                    return Container(
                      margin: const EdgeInsets.symmetric(horizontal: 0, vertical: 0),
                      decoration: const BoxDecoration(
                        color: _surface0,
                        border: Border(
                          bottom: BorderSide(color: _mantle, width: 1),
                        ),
                      ),
                      child: Row(
                        children: [
                          Container(
                            width: 4,
                            height: 44,
                            color: hasDur ? _blue : _green,
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Text(
                              event.name,
                              style: const TextStyle(
                                fontFamily: 'monospace',
                                fontSize: 13,
                                color: _text,
                              ),
                            ),
                          ),
                          if (hasDur)
                            Text(
                              '(${event.durationMs!.toStringAsFixed(0)}ms)',
                              style: const TextStyle(
                                fontFamily: 'monospace',
                                fontSize: 11,
                                color: _blue,
                              ),
                            ),
                          const SizedBox(width: 8),
                          Text(
                            _fmtEventTime(entry.wallClockMs),
                            style: const TextStyle(
                              fontFamily: 'monospace',
                              fontSize: 11,
                              color: _overlay1,
                            ),
                          ),
                          const SizedBox(width: 12),
                        ],
                      ),
                    );
                  },
                ),
        ),
        // Footer with preset buttons + custom input
        ColoredBox(
          color: _mantle,
          child: SafeArea(
            top: false,
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  for (final name in ['user_login', 'purchase', 'screen_view'])
                    _btn(
                      label: name.toUpperCase(),
                      onPressed: () => Konitor.logEvent(name),
                    ),
                  Builder(builder: (_) {
                    final isRecording = _activeEvents.containsKey('video_playback');
                    return _btn(
                      label: isRecording ? 'Recording…' : 'video_playback',
                      onPressed: isRecording
                          ? null
                          : () async {
                              final tokenId =
                                  await Konitor.startEvent('video_playback');
                              setState(
                                () => _activeEvents['video_playback'] = tokenId,
                              );
                              Future.delayed(const Duration(seconds: 2), () async {
                                await Konitor.endEvent(tokenId);
                                if (mounted) {
                                  setState(
                                    () => _activeEvents.remove('video_playback'),
                                  );
                                }
                              });
                            },
                      active: isRecording,
                    );
                  }),
                  _btn(
                    label: 'CLEAR',
                    onPressed: _events.isEmpty
                        ? null
                        : () => setState(() => _events = []),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _eventController,
                      style: const TextStyle(
                        color: _text,
                        fontSize: 12,
                        fontFamily: 'monospace',
                      ),
                      decoration: InputDecoration(
                        hintText: 'custom event name…',
                        hintStyle: const TextStyle(color: _overlay1, fontSize: 12),
                        filled: true,
                        fillColor: _surface0,
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(8),
                          borderSide: BorderSide.none,
                        ),
                        contentPadding: const EdgeInsets.symmetric(
                          horizontal: 10,
                          vertical: 8,
                        ),
                      ),
                      onChanged: (_) => setState(() {}),
                      onSubmitted: (v) {
                        final name = v.trim();
                        if (name.isNotEmpty) {
                          Konitor.logEvent(name);
                          _eventController.clear();
                          setState(() {});
                        }
                      },
                    ),
                  ),
                  const SizedBox(width: 8),
                  _btn(
                    label: 'LOG',
                    onPressed: _eventController.text.trim().isEmpty
                        ? null
                        : () {
                            Konitor.logEvent(_eventController.text.trim());
                            _eventController.clear();
                            setState(() {});
                          },
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    ),
      ],
    );
  }

  // ── NETWORK TAB ────────────────────────────────────────────────────────────

  Widget _buildNetworkTab() {
    final net = _network;
    return Column(
      children: [
        Expanded(
          child: net == null
              ? _placeholder()
              : SingleChildScrollView(
                  padding: const EdgeInsets.fromLTRB(16, 20, 16, 12),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      _sectionTitle('SYSTEM TRAFFIC (PER INTERVAL)', first: true),
                      _metricRow(
                        'Download',
                        (net.rxMb / _peakRxMb.clamp(0.001, double.infinity))
                            .clamp(0.0, 1.0),
                        _teal,
                        _fmtMb(net.rxMb),
                      ),
                      Padding(
                        padding: const EdgeInsets.only(left: 80, bottom: 6),
                        child: Text(
                          '${_fmtMb(net.rxMb)}/interval   peak ${_fmtMb(_peakRxMb)}',
                          style: const TextStyle(
                            fontSize: 11,
                            fontFamily: 'monospace',
                            color: _overlay1,
                          ),
                        ),
                      ),
                      _metricRow(
                        'Upload',
                        (net.txMb / _peakTxMb.clamp(0.001, double.infinity))
                            .clamp(0.0, 1.0),
                        _mauve,
                        _fmtMb(net.txMb),
                      ),
                      Padding(
                        padding: const EdgeInsets.only(left: 80, bottom: 6),
                        child: Text(
                          '${_fmtMb(net.txMb)}/interval   peak ${_fmtMb(_peakTxMb)}',
                          style: const TextStyle(
                            fontSize: 11,
                            fontFamily: 'monospace',
                            color: _overlay1,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
        ),
        _footer(
          SizedBox(
            width: double.infinity,
            child: _btn(
              label: _networkFetching ? 'FETCHING 5 MB…' : 'TEST DOWNLOAD',
              onPressed: _networkFetching ? null : _testDownload,
            ),
          ),
        ),
      ],
    );
  }

  String _fmtMb(double v) {
    if (v >= 1)    return '${v.toStringAsFixed(3)} MB';
    if (v >= 0.01) return '${(v * 1000).toStringAsFixed(0)} KB';
    return '< 10 KB';
  }

  // ── ISSUES TAB (Android only) ──────────────────────────────────────────────

  static const _severityColors = {
    'CRITICAL': _red,
    'ERROR': _peach,
    'WARNING': _yellow,
    'INFO': _green,
  };

  void _triggerSlowSpan() {
    Konitor.logEvent('issue_slow_span');
    setState(() => _spanStatus = 'Blocking main thread 1.2 s…');
    // simulateSlowSpan blocks the Android main thread for 1.2 s, which
    // exceeds the 1 s ANR threshold so konitor fires an Issue event.
    Konitor.simulateSlowSpan().then((_) {
      if (mounted) {
        setState(() => _spanStatus = 'Done — check Issues tab');
        Future.delayed(const Duration(seconds: 5),
            () { if (mounted) setState(() => _spanStatus = ''); });
      }
    });
  }

  void _triggerCrash() {
    Konitor.logEvent('issue_crash_trigger');
    // Crash on a native thread so konitor's UncaughtExceptionHandler fires.
    Konitor.simulateCrash();
  }

  Widget _buildIssuesTab() {
    return Column(
      children: [
        Expanded(
          child: _issues.isEmpty
              ? Center(
                  child: Text(
                    _running ? 'No issues detected' : '—',
                    style: const TextStyle(color: _overlay1, fontSize: 14),
                  ),
                )
              : ListView.builder(
                  itemCount: _issues.length,
                  itemBuilder: (context, index) {
                    final issue = _issues[index];
                    final sevColor =
                        _severityColors[issue.severity.toUpperCase()] ?? _blue;
                    return Container(
                      decoration: const BoxDecoration(
                        color: _surface0,
                        border: Border(bottom: BorderSide(color: _mantle)),
                      ),
                      child: IntrinsicHeight(
                        child: Row(
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: [
                            Container(width: 4, color: sevColor),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Padding(
                                padding: const EdgeInsets.symmetric(vertical: 8),
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Row(
                                      children: [
                                        Container(
                                          padding: const EdgeInsets.symmetric(
                                              horizontal: 5, vertical: 1),
                                          decoration: BoxDecoration(
                                            border: Border.all(color: sevColor),
                                            borderRadius: BorderRadius.circular(3),
                                          ),
                                          child: Text(
                                            issue.type,
                                            style: TextStyle(
                                              color: sevColor,
                                              fontSize: 10,
                                              fontWeight: FontWeight.w700,
                                              fontFamily: 'monospace',
                                            ),
                                          ),
                                        ),
                                        const SizedBox(width: 6),
                                        Text(
                                          issue.severity,
                                          style: TextStyle(
                                            color: sevColor,
                                            fontSize: 11,
                                          ),
                                        ),
                                        const Spacer(),
                                        Text(
                                          _fmtTimestamp(issue.timestamp.toInt()),
                                          style: const TextStyle(
                                            fontFamily: 'monospace',
                                            fontSize: 11,
                                            color: _overlay1,
                                          ),
                                        ),
                                        const SizedBox(width: 12),
                                      ],
                                    ),
                                    const SizedBox(height: 4),
                                    Text(
                                      issue.message,
                                      style: const TextStyle(
                                        color: _text,
                                        fontSize: 12,
                                      ),
                                      maxLines: 2,
                                    ),
                                  ],
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    );
                  },
                ),
        ),
        if (_spanStatus.isNotEmpty)
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
            child: Text(
              _spanStatus,
              style: const TextStyle(
                fontSize: 12,
                fontFamily: 'monospace',
                color: _yellow,
              ),
            ),
          ),
        _footer(
          Row(
            children: [
              _btn(label: 'SLOW SPAN', onPressed: _triggerSlowSpan),
              const SizedBox(width: 8),
              _btn(label: 'CRASH', onPressed: _triggerCrash),
              const SizedBox(width: 8),
              _btn(
                label: 'CLEAR',
                onPressed: _issues.isEmpty
                    ? null
                    : () => setState(() => _issues = []),
              ),
            ],
          ),
        ),
      ],
    );
  }

  String _fmtTimestamp(int ms) {
    final s = (ms ~/ 1000) % 86400;
    final h = s ~/ 3600;
    final m = (s % 3600) ~/ 60;
    final sc = s % 60;
    return '${h.toString().padLeft(2,'0')}:${m.toString().padLeft(2,'0')}:${sc.toString().padLeft(2,'0')}';
  }

  // ── JANK TAB (Android only) ────────────────────────────────────────────────

  Widget _buildJankTab() {
    final jank = _jank;
    return Column(
      children: [
        Expanded(
          child: SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(16, 20, 16, 12),
            child: jank == null
                ? _placeholder()
                : Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Center(
                        child: Column(
                          children: [
                            Text(
                              '${jank.droppedFrames}',
                              style: const TextStyle(
                                fontSize: 80,
                                fontFamily: 'monospace',
                                color: _mauve,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                            const Text(
                              'dropped frames / window',
                              style: TextStyle(fontSize: 12, color: _overlay1),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 16),
                      _sectionTitle('STATS'),
                      _metricRow(
                        'Janky ratio',
                        jank.jankyRatio,
                        _yellow,
                        '${(jank.jankyRatio * 100).toStringAsFixed(1)}%',
                      ),
                      _metricRow(
                        'Worst frame',
                        (jank.worstFrameMs / 1000.0).clamp(0.0, 1.0),
                        _peach,
                        '${jank.worstFrameMs.toStringAsFixed(0)} ms',
                      ),
                    ],
                  ),
          ),
        ),
        _footer(
          SizedBox(
            width: double.infinity,
            child: _btn(
              label: 'SIMULATE JANK',
              onPressed: _simulateJank,
            ),
          ),
        ),
      ],
    );
  }

  // ── GC TAB (Android only) ──────────────────────────────────────────────────

  Widget _buildGcTab() {
    final gc = _gc;
    return Column(
      children: [
        Expanded(
          child: gc == null
              ? _placeholder()
              : SingleChildScrollView(
                  padding: const EdgeInsets.fromLTRB(16, 20, 16, 12),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      _sectionTitle('NATIVE — GC (ART)', first: true),
                      _buildGcRow('Events/interval', gc.gcCountDelta.toStringAsFixed(0), _yellow),
                      _buildGcRow('Pause delta', '${gc.gcPauseMsDelta.toStringAsFixed(0)} ms', _yellow),
                      _buildGcRow('Total count', gc.gcCount.toStringAsFixed(0), _text),
                    ],
                  ),
                ),
        ),
        _footer(
          SizedBox(
            width: double.infinity,
            child: _btn(
              label: 'SIMULATE GC',
              onPressed: _simulateGc,
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildGcRow(String label, String value, Color color) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 5),
        child: Row(
          children: [
            SizedBox(
              width: 130,
              child: Text(label, style: const TextStyle(color: _text, fontSize: 13)),
            ),
            Text(value, style: TextStyle(fontFamily: 'monospace', fontSize: 12, color: color)),
          ],
        ),
      );

  // ── THERMAL TAB (Android only) ─────────────────────────────────────────────

  static const _thermalColors = {
    'NONE': _green,
    'LIGHT': _green,
    'MODERATE': _yellow,
    'SEVERE': _peach,
    'CRITICAL': _peach,
    'EMERGENCY': _peach,
    'SHUTDOWN': _peach,
  };

  Widget _buildThermalTab() {
    final thermal = _thermal;
    final isUnsupported = thermal?.state == 'UNSUPPORTED';

    if (isUnsupported) {
      return Column(
        children: [
          Expanded(child: _notAvailableScreen('thermal state')),
          _footer(SizedBox(
            width: double.infinity,
            child: _btn(
              label: _thermalStress ? 'STOP CPU STRESS' : 'START CPU STRESS',
              onPressed: () => _simulateThermalStress(!_thermalStress),
              active: _thermalStress,
            ),
          )),
        ],
      );
    }

    final stateColor = thermal != null
        ? (_thermalColors[thermal.state] ?? _overlay1)
        : _overlay1;

    return Column(
      children: [
        Expanded(
          child: thermal == null
              ? _placeholder()
              : SingleChildScrollView(
                  padding: const EdgeInsets.fromLTRB(16, 20, 16, 12),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Center(
                        child: Column(
                          children: [
                            Text(
                              thermal.state,
                              style: TextStyle(
                                fontSize: 48,
                                fontFamily: 'monospace',
                                color: stateColor,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                            const Text(
                              'thermal state',
                              style: TextStyle(fontSize: 12, color: _overlay1),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 16),
                      _sectionTitle('STATUS'),
                      _buildGcRow(
                        'Throttling',
                        thermal.isThrottling ? 'YES' : 'NO',
                        thermal.isThrottling ? _peach : _green,
                      ),
                      if (thermal.temperatureC >= 0)
                        _buildGcRow(
                          'Battery temp',
                          '${thermal.temperatureC.toStringAsFixed(1)} °C',
                          thermal.temperatureC > 40 ? _peach : _text,
                        ),
                    ],
                  ),
                ),
        ),
        _footer(
          SizedBox(
            width: double.infinity,
            child: _btn(
              label: _thermalStress ? 'STOP CPU STRESS' : 'START CPU STRESS',
              onPressed: () => _simulateThermalStress(!_thermalStress),
              active: _thermalStress,
            ),
          ),
        ),
      ],
    );
  }

  // ── TAB VIEWS ──────────────────────────────────────────────────────────────

  List<Widget> _buildTabViews() {
    final isAndroid = Platform.isAndroid;
    return [
      _buildCpuTab(),
      isAndroid ? _buildGpuTab() : _notAvailableScreen('gpu usage %'),
      _buildFpsTab(),
      _buildMemoryTab(),
      _buildEventsTab(),
      _buildNetworkTab(),
      _buildIssuesTab(),
      isAndroid ? _buildJankTab() : _notAvailableScreen('jank detection'),
      isAndroid ? _buildGcTab() : _notAvailableScreen('gc monitoring'),
      isAndroid ? _buildThermalTab() : _notAvailableScreen('thermal state'),
    ];
  }

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: _tabs.length,
      child: Scaffold(
        backgroundColor: _base,
        appBar: AppBar(
          backgroundColor: _mantle,
          leading: Center(
            child: Container(
              width: 8,
              height: 8,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: _running ? _green : _surface1,
              ),
            ),
          ),
          title: Text(
            Platform.isAndroid ? 'K|Android|FL' : 'K|iOS|FL',
            style: const TextStyle(
              color: _blue,
              fontWeight: FontWeight.w700,
              fontSize: 16,
            ),
          ),
          actions: [
            Padding(
              padding: const EdgeInsets.only(right: 8),
              child: TextButton(
                onPressed: _running ? _stopMonitoring : _startMonitoring,
                style: TextButton.styleFrom(
                  backgroundColor: _running ? _surface1 : _blue,
                  foregroundColor: _running ? _text : _base,
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(8),
                  ),
                  textStyle: const TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.w700,
                    letterSpacing: 0.5,
                  ),
                ),
                child: Text(_running ? 'STOP' : 'START'),
              ),
            ),
          ],
          bottom: TabBar(
            isScrollable: true,
            tabAlignment: TabAlignment.start,
            padding: EdgeInsets.zero,
            indicatorColor: _blue,
            indicatorWeight: 2.0,
            labelColor: _blue,
            unselectedLabelColor: _muted,
            labelStyle: const TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.w600,
              letterSpacing: 0.5,
            ),
            unselectedLabelStyle: const TextStyle(
              fontSize: 12,
              fontWeight: FontWeight.w600,
              letterSpacing: 0.5,
            ),
            tabs: _tabs.map((t) => Tab(text: t)).toList(),
          ),
        ),
        body: TabBarView(
          children: _buildTabViews(),
        ),
      ),
    );
  }
}

// ── GPU Stress Canvas ──────────────────────────────────────────────────────────
class _GpuStressCanvas extends StatefulWidget {
  const _GpuStressCanvas();

  @override
  State<_GpuStressCanvas> createState() => _GpuStressCanvasState();
}

class _GpuStressCanvasState extends State<_GpuStressCanvas>
    with TickerProviderStateMixin {
  static const _ballCount  = 30;
  static const _ballSize   = 50.0;
  static const _boxSize    = 280.0;
  static const _colors     = [_blue, _green, _yellow, _peach, _mauve, _teal, _red];

  late final List<AnimationController> _xCtrl;
  late final List<AnimationController> _yCtrl;
  late final List<Animation<double>>   _xAnim;
  late final List<Animation<double>>   _yAnim;

  @override
  void initState() {
    super.initState();
    const maxPos = _boxSize - _ballSize;
    _xCtrl = [];
    _yCtrl = [];
    _xAnim = [];
    _yAnim = [];
    for (var i = 0; i < _ballCount; i++) {
      final dur  = Duration(milliseconds: 800 + (i * 137) % 1200);
      final yDur = Duration(milliseconds: ((800 + (i * 137) % 1200) * 0.7).round());

      final xc = AnimationController(vsync: this, duration: dur)
        ..repeat(reverse: true);
      final yc = AnimationController(vsync: this, duration: yDur)
        ..repeat(reverse: true);
      _xCtrl.add(xc);
      _yCtrl.add(yc);
      _xAnim.add(Tween<double>(begin: 0, end: maxPos).animate(
        CurvedAnimation(parent: xc, curve: Curves.easeInOut),
      ));
      _yAnim.add(Tween<double>(begin: 0, end: maxPos).animate(
        CurvedAnimation(parent: yc, curve: Curves.easeInOut),
      ));
    }
  }

  @override
  void dispose() {
    for (final c in _xCtrl) { c.dispose(); }
    for (final c in _yCtrl) { c.dispose(); }
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      width: _boxSize,
      height: _boxSize,
      decoration: BoxDecoration(
        color: const Color(0xFF0D0D1A),
        borderRadius: BorderRadius.circular(8),
      ),
      clipBehavior: Clip.hardEdge,
      child: AnimatedBuilder(
        animation: Listenable.merge([..._xCtrl, ..._yCtrl]),
        builder: (context, _) {
          return Stack(
            children: [
              for (var i = 0; i < _ballCount; i++)
                Positioned(
                  left: _xAnim[i].value,
                  top:  _yAnim[i].value,
                  child: Container(
                    width: _ballSize,
                    height: _ballSize,
                    decoration: BoxDecoration(
                      color: _colors[i % _colors.length].withOpacity(0.85),
                      shape: BoxShape.circle,
                    ),
                  ),
                ),
            ],
          );
        },
      ),
    );
  }
}
