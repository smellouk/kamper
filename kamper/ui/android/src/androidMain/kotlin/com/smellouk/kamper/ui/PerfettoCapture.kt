package com.smellouk.kamper.ui

import android.content.Context
import java.io.File
import java.util.concurrent.TimeUnit

internal class PerfettoCapture(private val context: Context) {
    private var process: Process? = null
    private var _traceFile: File? = null
    private var stderrOutput = ""
    private var stderrThread: Thread? = null

    val isAvailable: Boolean by lazy { File("/system/bin/perfetto").exists() }

    /** Returns an error string if the process failed to start, null on success. */
    fun start(): String? {
        if (!isAvailable) return "perfetto binary not found at /system/bin/perfetto"
        if (process != null) return null

        val traceOut = File(context.cacheDir, "kamper_${System.currentTimeMillis()}.perfetto-trace")
        val configFile = File(context.cacheDir, "kamper_config.pbtx")
        stderrOutput = ""

        return try {
            configFile.writeText(CONFIG)
            val p = ProcessBuilder(
                "/system/bin/perfetto",
                "--txt", "--config", configFile.absolutePath,
                "--out", traceOut.absolutePath
            ).start()

            val buf = StringBuilder()
            stderrThread = Thread {
                try { buf.append(p.errorStream.bufferedReader().readText()) }
                catch (_: Exception) {}
            }.apply { isDaemon = true; start() }

            // Give perfetto 800ms to either start recording or fail
            if (p.waitFor(800, TimeUnit.MILLISECONDS)) {
                // Exited already — failure
                stderrThread?.join(500)
                stderrThread = null
                stderrOutput = buf.toString().trim()
                "perfetto exited (code ${p.exitValue()}): $stderrOutput"
            } else {
                _traceFile = traceOut
                process = p
                stderrThread?.let { t ->
                    // Keep reference so we can join in stop()
                    stderrThread = t
                }
                null
            }
        } catch (e: Exception) {
            "Cannot run perfetto: ${e.message}"
        }
    }

    fun stop() {
        val p = process ?: return
        process = null
        val pid = reflectPid(p)
        if (pid > 0) {
            try { Runtime.getRuntime().exec(arrayOf("kill", "-15", pid.toString())).waitFor() }
            catch (_: Exception) {}
        }
        if (!p.waitFor(4, TimeUnit.SECONDS)) p.destroyForcibly()
        stderrThread?.join(1000)
        stderrThread = null
    }

    fun traceFile(): File? = _traceFile?.takeIf { it.exists() && it.length() > 0L }

    fun lastError(): String = stderrOutput

    private fun reflectPid(p: Process): Int = try {
        val f = p.javaClass.getDeclaredField("pid")
        f.isAccessible = true
        f.getInt(p)
    } catch (_: Exception) { -1 }

    private companion object {
        val CONFIG = """
buffers {
  size_kb: 65536
  fill_policy: RING_BUFFER
}
data_sources {
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
