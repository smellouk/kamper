class MemoryInfo {
  final double heapAllocatedMb;
  final double heapMaxMb;
  final double ramUsedMb;
  final double ramTotalMb;
  final bool isLowMemory;

  const MemoryInfo({
    required this.heapAllocatedMb,
    required this.heapMaxMb,
    required this.ramUsedMb,
    required this.ramTotalMb,
    required this.isLowMemory,
  });

  factory MemoryInfo.fromMap(Map<dynamic, dynamic> m) => MemoryInfo(
        heapAllocatedMb: (m['heapAllocatedMb'] as num).toDouble(),
        heapMaxMb: (m['heapMaxMb'] as num).toDouble(),
        ramUsedMb: (m['ramUsedMb'] as num).toDouble(),
        ramTotalMb: (m['ramTotalMb'] as num).toDouble(),
        isLowMemory: m['isLowMemory'] as bool,
      );
}
