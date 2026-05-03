class KonitorConfig {
  final bool? cpu, fps, memory, network, issues, jank, gc, thermal, gpu;

  const KonitorConfig({
    this.cpu,
    this.fps,
    this.memory,
    this.network,
    this.issues,
    this.jank,
    this.gc,
    this.thermal,
    this.gpu,
  });

  Map<String, dynamic> toMap() => {
        if (cpu != null) 'cpu': cpu,
        if (fps != null) 'fps': fps,
        if (memory != null) 'memory': memory,
        if (network != null) 'network': network,
        if (issues != null) 'issues': issues,
        if (jank != null) 'jank': jank,
        if (gc != null) 'gc': gc,
        if (thermal != null) 'thermal': thermal,
        if (gpu != null) 'gpu': gpu,
      };
}
