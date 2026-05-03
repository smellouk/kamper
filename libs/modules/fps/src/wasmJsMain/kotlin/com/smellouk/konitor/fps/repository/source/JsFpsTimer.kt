package com.smellouk.konitor.fps.repository.source

@JsFun("""() => {
    if (window.__konitorFps) return;
    window.__konitorFps = { running: false, handle: 0, count: 0, startMs: 0.0, lastMs: 0.0 };
}""")
private external fun jsFpsInit()

@JsFun("""() => {
    var s = window.__konitorFps;
    if (!s || s.running) return;
    s.running = true;
    s.count = 0; s.startMs = 0.0; s.lastMs = 0.0;
    function tick(ts) {
        if (!s.running) return;
        s.count++;
        if (s.startMs === 0.0) s.startMs = ts;
        s.lastMs = ts;
        s.handle = requestAnimationFrame(tick);
    }
    s.handle = requestAnimationFrame(tick);
}""")
private external fun jsFpsStart()

@JsFun("""() => {
    var s = window.__konitorFps;
    if (!s) return;
    s.running = false;
    cancelAnimationFrame(s.handle);
}""")
private external fun jsFpsStop()

@JsFun("() => { var s = window.__konitorFps; return s ? s.count : 0; }")
private external fun jsFpsGetCount(): Int

@JsFun("() => { var s = window.__konitorFps; return s ? s.startMs : 0.0; }")
private external fun jsFpsGetStartMs(): Double

@JsFun("() => { var s = window.__konitorFps; return s ? s.lastMs : 0.0; }")
private external fun jsFpsGetLastMs(): Double

@JsFun("() => { var s = window.__konitorFps; if (s) { s.count = 0; s.startMs = 0.0; s.lastMs = 0.0; } }")
private external fun jsFpsReset()

internal object JsFpsTimer {
    @Suppress("UNUSED_PARAMETER")
    fun setFrameListener(listener: (Double) -> Unit) {
        jsFpsInit()
    }

    fun start() {
        jsFpsInit()
        jsFpsStart()
    }

    fun stop() {
        jsFpsStop()
    }

    fun clean() {
        jsFpsStop()
        jsFpsReset()
    }

    fun getFrameCount(): Int = jsFpsGetCount()
    fun getStartMs(): Double = jsFpsGetStartMs()
    fun getLastMs(): Double = jsFpsGetLastMs()
    fun resetState() = jsFpsReset()
}
