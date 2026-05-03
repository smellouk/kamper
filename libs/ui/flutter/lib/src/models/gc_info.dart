class GcInfo {
  final double gcCountDelta;
  final double gcPauseMsDelta;
  final double gcCount;

  const GcInfo({
    required this.gcCountDelta,
    required this.gcPauseMsDelta,
    required this.gcCount,
  });

  factory GcInfo.fromMap(Map<dynamic, dynamic> m) => GcInfo(
        gcCountDelta: (m['gcCountDelta'] as num).toDouble(),
        gcPauseMsDelta: (m['gcPauseMsDelta'] as num).toDouble(),
        gcCount: (m['gcCount'] as num).toDouble(),
      );
}
