class IssueInfo {
  final String id;
  final String type;
  final String severity;
  final String message;
  final double timestamp;
  final double? durationMs;
  final String? threadName;

  const IssueInfo({
    required this.id,
    required this.type,
    required this.severity,
    required this.message,
    required this.timestamp,
    this.durationMs,
    this.threadName,
  });

  factory IssueInfo.fromMap(Map<dynamic, dynamic> m) => IssueInfo(
        id: m['id'] as String,
        type: m['type'] as String,
        severity: m['severity'] as String,
        message: m['message'] as String,
        timestamp: (m['timestamp'] as num).toDouble(),
        durationMs:
            m['durationMs'] != null ? (m['durationMs'] as num).toDouble() : null,
        threadName: m['threadName'] != null ? m['threadName'] as String : null,
      );
}
