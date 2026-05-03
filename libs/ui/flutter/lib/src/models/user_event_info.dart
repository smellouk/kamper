class UserEventInfo {
  final String name;
  final double? durationMs;

  const UserEventInfo({required this.name, this.durationMs});

  factory UserEventInfo.fromMap(Map<dynamic, dynamic> m) => UserEventInfo(
        name: m['name'] as String,
        durationMs:
            m['durationMs'] != null ? (m['durationMs'] as num).toDouble() : null,
      );
}
