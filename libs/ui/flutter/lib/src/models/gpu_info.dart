class GpuInfo {
  final double utilization;
  final double usedMemoryMb;
  final double totalMemoryMb;
  final double curFreqKhz;
  final double maxFreqKhz;
  final double appUtilization;
  final double rendererUtilization;
  final double tilerUtilization;
  final double computeUtilization;

  const GpuInfo({
    required this.utilization,
    required this.usedMemoryMb,
    required this.totalMemoryMb,
    required this.curFreqKhz,
    required this.maxFreqKhz,
    required this.appUtilization,
    required this.rendererUtilization,
    required this.tilerUtilization,
    required this.computeUtilization,
  });

  factory GpuInfo.fromMap(Map<dynamic, dynamic> m) => GpuInfo(
        utilization: (m['utilization'] as num).toDouble(),
        usedMemoryMb: (m['usedMemoryMb'] as num).toDouble(),
        totalMemoryMb: (m['totalMemoryMb'] as num).toDouble(),
        curFreqKhz: (m['curFreqKhz'] as num).toDouble(),
        maxFreqKhz: (m['maxFreqKhz'] as num).toDouble(),
        appUtilization: (m['appUtilization'] as num).toDouble(),
        rendererUtilization: (m['rendererUtilization'] as num).toDouble(),
        tilerUtilization: (m['tilerUtilization'] as num).toDouble(),
        computeUtilization: (m['computeUtilization'] as num).toDouble(),
      );
}
