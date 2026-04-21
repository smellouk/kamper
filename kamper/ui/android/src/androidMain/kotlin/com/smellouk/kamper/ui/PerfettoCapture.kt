package com.smellouk.kamper.ui

import android.content.Context
import java.io.File
import java.util.concurrent.TimeUnit

internal class PerfettoCapture(private val context: Context) {
    private var process: Process? = null
    private var _traceFile: File? = null

    val isAvailable: Boolean by lazy { File("/system/bin/perfetto").exists() }

    fun start() {
        if (!isAvailable || process != null) return
        val f = File(context.cacheDir, "kamper_${System.currentTimeMillis()}.perfetto-trace")
        try {
            val p = ProcessBuilder(
                "/system/bin/perfetto", "--txt", "--config", "-", "--out", f.absolutePath
            ).redirectErrorStream(true).start()
            p.outputStream.bufferedWriter().use { it.write(CONFIG) }
            process = p
            _traceFile = f
        } catch (_: Exception) {}
    }

    fun stop() {
        val p = process ?: return
        process = null
        p.destroy()
        try { p.waitFor(2, TimeUnit.SECONDS) } catch (_: Exception) {}
    }

    fun traceFile(): File? = _traceFile?.takeIf { it.exists() && it.length() > 0L }

    private companion object {
        val CONFIG = """
buffers: { size_kb: 65536 fill_policy: RING_BUFFER }
data_sources: {
  config {
    name: "linux.ftrace"
    ftrace_config {
      ftrace_events: "ftrace/print"
      atrace_apps: "*"
      atrace_categories: "app"
    }
  }
}
duration_ms: 3600000
""".trimIndent()
    }
}
