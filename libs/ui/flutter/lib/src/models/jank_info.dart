class JankInfo {
  final int droppedFrames;
  final double jankyRatio;
  final double worstFrameMs;

  const JankInfo({
    required this.droppedFrames,
    required this.jankyRatio,
    required this.worstFrameMs,
  });

  factory JankInfo.fromMap(Map<dynamic, dynamic> m) => JankInfo(
        droppedFrames: (m['droppedFrames'] as num).toInt(),
        jankyRatio: (m['jankyRatio'] as num).toDouble(),
        worstFrameMs: (m['worstFrameMs'] as num).toDouble(),
      );
}
