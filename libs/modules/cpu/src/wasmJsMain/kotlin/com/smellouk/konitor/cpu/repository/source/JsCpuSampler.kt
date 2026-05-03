package com.smellouk.konitor.cpu.repository.source

@JsFun("""() => {
    if (window.__konitorCpu) return;
    window.__konitorCpu = { load: 0.0, running: false, handle: 0, expected: 0 };
}""")
private external fun jsCpuInit()

@JsFun("""() => {
    var s = window.__konitorCpu;
    if (!s || s.running) return;
    s.running = true;
    s.expected = performance.now() + 50;
    function tick() {
        if (!s.running) return;
        var now = performance.now();
        var drift = Math.max(0.0, now - s.expected);
        var instant = Math.min(1.0, drift / 50.0);
        s.load = s.load * 0.75 + instant * 0.25;
        s.expected = now + 50;
        s.handle = setTimeout(tick, 50);
    }
    s.handle = setTimeout(tick, 50);
}""")
private external fun jsCpuStart()

@JsFun("""() => {
    var s = window.__konitorCpu;
    if (!s) return;
    s.running = false;
    s.load = 0.0;
    clearTimeout(s.handle);
}""")
private external fun jsCpuStop()

@JsFun("() => { var s = window.__konitorCpu; return s ? s.load : 0.0; }")
private external fun jsCpuLoad(): Double

internal object JsCpuSampler {
    val loadEstimate: Double get() = jsCpuLoad()

    fun ensureStarted() {
        jsCpuInit()
        jsCpuStart()
    }

    fun stop() {
        jsCpuStop()
    }
}
