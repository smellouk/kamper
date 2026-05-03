class FpsInfo {
  final int fps;

  const FpsInfo({required this.fps});

  factory FpsInfo.fromMap(Map<dynamic, dynamic> m) => FpsInfo(
        fps: (m['fps'] as num).toInt(),
      );
}
