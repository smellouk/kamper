package com.smellouk.kamper.ui

import android.content.Context
import android.util.Log
import java.io.File
import java.util.concurrent.TimeUnit

internal class PerfettoCapture(private val context: Context) {
    private var process: Process? = null
    private var _traceFile: File? = null
    private var stderrOutput = ""
    private var stderrThread: Thread? = null

    val isAvailable: Boolean by lazy {
        val exists = File("/system/bin/perfetto").exists()
        Log.d(TAG, "isAvailable=$exists (binary path: /system/bin/perfetto)")
        exists
    }

    /** Returns an error string if the process failed to start, null on success. */
    fun start(): String? {
        Log.d(TAG, "start() called — isAvailable=$isAvailable, process=${process != null}")
        if (!isAvailable) return "perfetto binary not found at /system/bin/perfetto"
        if (process != null) return null

        val traceOut = File(context.cacheDir, "kamper_${System.currentTimeMillis()}.perfetto-trace")
        val configFile = File(context.cacheDir, "kamper_config.pbtx")
        Log.d(TAG, "cacheDir=${context.cacheDir}")
        Log.d(TAG, "traceOut=${traceOut.absolutePath}")
        stderrOutput = ""

        return try {
            configFile.writeText(CONFIG)
            Log.d(TAG, "config written to ${configFile.absolutePath}")

            val cmd = listOf(
                "/system/bin/perfetto",
                "--txt", "--config", configFile.absolutePath,
                "--out", traceOut.absolutePath
            )
            Log.d(TAG, "launching: ${cmd.joinToString(" ")}")

            val p = ProcessBuilder(cmd).start()
            Log.d(TAG, "process started, pid=${reflectPid(p)}")

            val buf = StringBuilder()
            stderrThread = Thread {
                try {
                    val text = p.errorStream.bufferedReader().readText()
                    buf.append(text)
                    if (text.isNotBlank()) Log.w(TAG, "perfetto stderr: $text")
                } catch (_: Exception) {}
            }.apply { isDaemon = true; start() }

            // Give perfetto 800ms to either start recording or fail
            val exited = p.waitFor(800, TimeUnit.MILLISECONDS)
            Log.d(TAG, "after 800ms wait — process exited=$exited" +
                    if (exited) " exitCode=${p.exitValue()}" else "")

            if (exited) {
                stderrThread?.join(500)
                stderrThread = null
                stderrOutput = buf.toString().trim()
                val msg = "perfetto exited (code ${p.exitValue()}): $stderrOutput"
                Log.e(TAG, msg)
                msg
            } else {
                _traceFile = traceOut
                process = p
                Log.i(TAG, "perfetto is running — recording to ${traceOut.name}")
                null
            }
        } catch (e: Exception) {
            val msg = "Cannot run perfetto: ${e.message}"
            Log.e(TAG, msg, e)
            msg
        }
    }

    fun stop() {
        val p = process ?: run { Log.d(TAG, "stop() — no active process"); return }
        process = null
        val pid = reflectPid(p)
        Log.d(TAG, "stop() — sending SIGTERM to pid=$pid")
        if (pid > 0) {
            try { Runtime.getRuntime().exec(arrayOf("kill", "-15", pid.toString())).waitFor() }
            catch (e: Exception) { Log.w(TAG, "SIGTERM failed: ${e.message}") }
        }
        val exited = p.waitFor(4, TimeUnit.SECONDS)
        Log.d(TAG, "after 4s wait — exited=$exited")
        if (!exited) {
            Log.w(TAG, "process did not exit — forcing kill")
            p.destroyForcibly()
        }
        stderrThread?.join(1000)
        stderrThread = null

        val f = _traceFile
        if (f != null) {
            Log.i(TAG, "trace file: ${f.absolutePath} — exists=${f.exists()} size=${f.length()} bytes")
        } else {
            Log.w(TAG, "no trace file reference")
        }
    }

    fun traceFile(): File? {
        val f = _traceFile
        return when {
            f == null -> { Log.w(TAG, "traceFile() — _traceFile is null"); null }
            !f.exists() -> { Log.w(TAG, "traceFile() — file does not exist: ${f.absolutePath}"); null }
            f.length() == 0L -> { Log.w(TAG, "traceFile() — file is empty: ${f.absolutePath}"); null }
            else -> { Log.i(TAG, "traceFile() — ${f.length()} bytes at ${f.absolutePath}"); f }
        }
    }

    fun lastError(): String = stderrOutput

    private fun reflectPid(p: Process): Int = try {
        val f = p.javaClass.getDeclaredField("pid")
        f.isAccessible = true
        f.getInt(p)
    } catch (_: Exception) { -1 }

    internal companion object {
        const val TAG = "Kamper.Perfetto"

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
