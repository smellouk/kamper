class NetworkInfo {
  final double rxMb;
  final double txMb;

  const NetworkInfo({required this.rxMb, required this.txMb});

  factory NetworkInfo.fromMap(Map<dynamic, dynamic> m) => NetworkInfo(
        rxMb: (m['rxMb'] as num).toDouble(),
        txMb: (m['txMb'] as num).toDouble(),
      );
}
