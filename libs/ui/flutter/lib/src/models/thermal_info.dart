class ThermalInfo {
  final String state;
  final bool isThrottling;
  final double temperatureC;

  const ThermalInfo({
    required this.state,
    required this.isThrottling,
    required this.temperatureC,
  });

  factory ThermalInfo.fromMap(Map<dynamic, dynamic> m) => ThermalInfo(
        state: m['state'] as String,
        isThrottling: m['isThrottling'] as bool,
        temperatureC: (m['temperatureC'] as num).toDouble(),
      );
}
