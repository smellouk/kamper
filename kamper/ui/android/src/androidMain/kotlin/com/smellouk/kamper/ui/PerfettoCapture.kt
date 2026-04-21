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
        // Try SIGTERM first so perfetto can flush its ring buffer
        val pid = reflectPid(p)
        if (pid > 0) {
            try { Runtime.getRuntime().exec(arrayOf("kill", "-15", pid.toString())).waitFor() }
            catch (_: Exception) {}
        }
        if (!p.waitFor(4, TimeUnit.SECONDS)) {
            p.destroyForcibly()
        }
    }

    fun traceFile(): File? = _traceFile?.takeIf { it.exists() && it.length() > 0L }

    private fun reflectPid(p: Process): Int = try {
        val f = p.javaClass.getDeclaredField("pid")
        f.isAccessible = true
        f.getInt(p)
    } catch (_: Exception) { -1 }

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
      atrace_categories: "view"
      atrace_categories: "res"
      atrace_categories: "am"
    }
  }
}
duration_ms: 3600000
""".trimIndent()
    }
}
