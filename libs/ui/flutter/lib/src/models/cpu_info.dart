class CpuInfo {
  final double totalUseRatio;
  final double appRatio;
  final double userRatio;
  final double systemRatio;
  final double ioWaitRatio;

  const CpuInfo({
    required this.totalUseRatio,
    required this.appRatio,
    required this.userRatio,
    required this.systemRatio,
    required this.ioWaitRatio,
  });

  factory CpuInfo.fromMap(Map<dynamic, dynamic> m) => CpuInfo(
        totalUseRatio: (m['totalUseRatio'] as num).toDouble(),
        appRatio: (m['appRatio'] as num).toDouble(),
        userRatio: (m['userRatio'] as num).toDouble(),
        systemRatio: (m['systemRatio'] as num).toDouble(),
        ioWaitRatio: (m['ioWaitRatio'] as num).toDouble(),
      );
}
