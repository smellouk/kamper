package com.smellouk.kamper.cpu.repository.source

import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.cpu.repository.CpuInfoDto
import java.io.FileInputStream
import java.io.InputStream
import android.os.Process as ProcessPidProvider

/**
 * CPU usage for Android 8+ (Oreo).
 *
 * Global stats (total/user/sys/iowait) come from `top -n 2 -d 1`: two iterations
 * separated by 1 s produce a real delta instead of cumulative-since-boot values.
 *
 * App CPU is read from /proc/[pid]/stat ticks (reliable on all API levels, more
 * accurate than top's per-process %CPU column). The top per-process column is kept
 * as a fallback for environments where /proc/[pid]/stat is unavailable.
 *
 * When global stats are unavailable (some OEM top variants omit the summary line),
 * a synthetic totalTime is derived from elapsed wall-clock time and CPU count so
 * that appRatio can still be displayed.
 */
internal class ShellCpuInfoSource(
    private val logger: Logger
) : CpuInfoSource {

    private var cachedAppTicks: Long = -1L
    private var cachedUserTicks: Long = -1L
    private var cachedSysTicks: Long = -1L
    private var cachedSampleTimeMs: Long = -1L

    @RequiresApi(api = Build.VERSION_CODES.O)
    override fun getCpuInfoDto(): CpuInfoDto {
        val pid = ProcessPidProvider.myPid()

        val ticks = readProcPidTicks(pid)
        val currentAppTicks = ticks.total
        val currentUserTicks = ticks.user
        val currentSysTicks = ticks.sys
        val currentTimeMs = SystemClock.elapsedRealtime()
        val elapsedMs = if (cachedSampleTimeMs > 0L) {
            (currentTimeMs - cachedSampleTimeMs).coerceAtLeast(1L)
        } else {
            1000L
        }
        val appTicksDelta = if (cachedAppTicks >= 0L && currentAppTicks >= 0L) {
            (currentAppTicks - cachedAppTicks).coerceAtLeast(0L)
        } else {
            -1L
        }
        val userTicksDelta = if (cachedUserTicks >= 0L && currentUserTicks >= 0L) (currentUserTicks - cachedUserTicks).coerceAtLeast(0L) else -1L
        val sysTicksDelta = if (cachedSysTicks >= 0L && currentSysTicks >= 0L) (currentSysTicks - cachedSysTicks).coerceAtLeast(0L) else -1L
        Log.d("Kamper/CPU/Shell", "pid=$pid appTicksDelta=$appTicksDelta user=$userTicksDelta sys=$sysTicksDelta elapsedMs=$elapsedMs")
        cachedAppTicks = currentAppTicks
        cachedUserTicks = currentUserTicks
        cachedSysTicks = currentSysTicks
        cachedSampleTimeMs = currentTimeMs

        var process: Process? = null
        try {
            // Run 2 iterations: the first uses cumulative-since-boot stats and is discarded;
            // the second reports a real delta over the 1-second window between iterations.
            process = Runtime.getRuntime().exec("top -n 2 -d 1")
            val cmdOutputLines = process?.inputStream?.readAllLine()
                ?.map { it.replace(ANSI_ESCAPE_REGEX, "").trim() }
                ?.filter { it.isNotBlank() }
                ?: emptyList()

            Log.d("Kamper/CPU/Shell", "top output lines (${cmdOutputLines.size}):\n${cmdOutputLines.joinToString("\n")}")

            if (cmdOutputLines.isEmpty() && appTicksDelta < 0L) {
                return CpuInfoDto.INVALID
            }

            var cpuInfoUsageMap = emptyMap<String, Double>()
            var cpuLabelIndex = -1
            var cpuAppUsage = -1.0
            cmdOutputLines.forEach { line ->
                // 400%cpu   0%user   0%nice   0%sys 400%idle   0%iow   0%irq   0%sirq   0%host
                if (line.isCpuInfoUsageLine()) {
                    val parsed = line.toCpuInfoUsageMap()
                    if (parsed.isNotEmpty()) cpuInfoUsageMap = parsed
                }

                // [7m   PID USER         PR  NI VIRT  RES  SHR S[%CPU] %MEM     TIME+ ARGS
                if (line.isProcessLabelLine()) {
                    cpuLabelIndex = line.getCpuLabelIndex()
                }

                // 10062 u0_a149      10 -10  13G 150M  91M S  0.0   7.6   0:09.79 com.smellouk.k+
                if (line.isProcessAppDetailsLine(pid) && cpuLabelIndex != -1) {
                    cpuAppUsage = line.getCpuAppUsage(cpuLabelIndex)
                }
            }

            Log.d("Kamper/CPU/Shell", "cpuInfoUsageMap=$cpuInfoUsageMap cpuLabelIndex=$cpuLabelIndex cpuAppUsage=$cpuAppUsage")

            // App CPU: /proc/[pid]/stat ticks (preferred) or top per-process column (fallback)
            val appTime = when {
                appTicksDelta >= 0L -> {
                    // Convert jiffies to the same "% of one core" unit top uses.
                    // appTicksDelta / (HZ * intervalSeconds) * 100 gives % of one core.
                    (appTicksDelta.toDouble() / (TICKS_PER_SECOND * elapsedMs / 1000.0)) * 100.0
                }
                cpuAppUsage >= 0.0 -> cpuAppUsage
                else -> 0.0
            }

            // Detect restricted top: when all active components (user+sys+…) sum to 0 but cpu==idle,
            // top only sees the app's process namespace and its global summary is meaningless.
            val topActiveSum = listOf("user", "nice", "sys", "system", "iow", "irq", "sirq", "host")
                .sumOf { cpuInfoUsageMap[it] ?: 0.0 }
            val topGlobalReliable = cpuInfoUsageMap.isNotEmpty() && topActiveSum > 0.0

            Log.d("Kamper/CPU/Shell", "topGlobalReliable=$topGlobalReliable topActiveSum=$topActiveSum appTime=$appTime")

            return if (topGlobalReliable) {
                val total = cpuInfoUsageMap["cpu"]
                val user = cpuInfoUsageMap["user"]
                val sys = cpuInfoUsageMap["sys"] ?: cpuInfoUsageMap["system"]
                val idle = cpuInfoUsageMap["idle"]
                val iow = cpuInfoUsageMap["iow"]
                CpuInfoDto(
                    totalTime = total.normalize(),
                    userTime = user.normalize(),
                    systemTime = sys.normalize(),
                    idleTime = idle.normalize(),
                    ioWaitTime = iow.normalize(),
                    appTime = appTime
                )
            } else if (appTicksDelta >= 0L) {
                // top global stats unreliable or absent — synthesise from /proc/self/stat ticks.
                // user/sys breakdown comes from the process's own utime/stime.
                val numCores = Runtime.getRuntime().availableProcessors()
                val syntheticTotal = numCores.toDouble() * TICKS_PER_SECOND * (elapsedMs / 1000.0)
                val appTicks = appTicksDelta.toDouble()
                val userTicks = if (userTicksDelta >= 0L) userTicksDelta.toDouble() else 0.0
                val sysTicks = if (sysTicksDelta >= 0L) sysTicksDelta.toDouble() else 0.0
                CpuInfoDto(
                    totalTime = syntheticTotal,
                    userTime = userTicks,
                    systemTime = sysTicks,
                    idleTime = (syntheticTotal - appTicks).coerceAtLeast(0.0),
                    ioWaitTime = 0.0,
                    appTime = appTicks
                )
            } else {
                CpuInfoDto.INVALID
            }
        } catch (e: Exception) {
            logger.log(e.stackTraceToString())
        } finally {
            process?.destroy()
        }

        return CpuInfoDto.INVALID
    }

    // Read utime/stime/cutime/cstime from /proc/[pid]/stat (always accessible for own PID).
    private fun readProcPidTicks(pid: Int): ProcTicks = try {
        val parts = FileInputStream("/proc/$pid/stat")
            .bufferedReader()
            .use { it.readLine() }
            .split(" ")
        if (parts.size > CPU_PID_CS_TIME_INDEX) {
            val utime = parts[CPU_PID_U_TIME_INDEX].toLong()
            val stime = parts[CPU_PID_S_TIME_INDEX].toLong()
            val cutime = parts[CPU_PID_CU_TIME_INDEX].toLong()
            val cstime = parts[CPU_PID_CS_TIME_INDEX].toLong()
            ProcTicks(user = utime + cutime, sys = stime + cstime)
        } else {
            ProcTicks.INVALID
        }
    } catch (e: Exception) {
        ProcTicks.INVALID
    }

    private data class ProcTicks(val user: Long, val sys: Long) {
        val total: Long get() = user + sys
        companion object {
            val INVALID = ProcTicks(-1L, -1L)
        }
    }
}

// Visible only for testing
internal fun InputStream.readAllLine(): List<String> =
    bufferedReader(Charsets.UTF_8).useLines { lines ->
        lines.filter { line ->
            line.isNotBlank()
        }.map { line ->
            line.trim()
        }.toList()
    }

// Visible only for testing
// Matches toybox format:   "800%cpu  44%user  0%nice  42%sys  713%idle  0%iow  0%irq  0%host"
// and old toolbox format:  "User 44%, System 42%, IOW 0%, IRQ 0%"
internal fun String.isCpuInfoUsageLine(): Boolean =
    matches("^\\d+%\\w+.*\\d+%\\w+.*".toRegex()) ||
        matches("(?i).*user\\s+\\d+%.*system\\s+\\d+%.*".toRegex())

// Visible only for testing
internal fun String.toCpuInfoUsageMap(): Map<String, Double> = try {
    // Reject multi-line input: a CPU summary line is always a single line.
    if (contains('\n') || contains('\r')) return emptyMap()

    if (matches("^\\d+%\\w+.*".toRegex())) {
        // Toybox: "800%cpu  44%user  0%nice  42%sys  713%idle  0%iow  0%irq  0%host"
        lowercase()
            .split("\\s+".toRegex())
            .map { token -> token.replace("[^\\w%]".toRegex(), "") }
            .map { token -> token.split("%") }
            .filter { parts -> parts.size >= 2 }
            .map { parts -> parts[1] to parts[0].toDouble() }
            .toMap()
    } else {
        // Old toolbox: "User 44%, System 42%, IOW 0%, IRQ 0%"
        val result = mutableMapOf<String, Double>()
        "(\\w+)\\s+(\\d+(?:\\.\\d+)?)%".toRegex(RegexOption.IGNORE_CASE)
            .findAll(this)
            .forEach { match ->
                val key = match.groupValues[1].lowercase()
                val value = match.groupValues[2].toDoubleOrNull() ?: return@forEach
                val normalized = when (key) {
                    "system" -> "sys"
                    "iow", "iowait" -> "iow"
                    else -> key
                }
                result[normalized] = value
            }
        if (result.isNotEmpty()) {
            val numCores = Runtime.getRuntime().availableProcessors()
            val active = (result["user"] ?: 0.0) + (result["sys"] ?: 0.0) +
                (result["iow"] ?: 0.0) + (result["irq"] ?: 0.0)
            val total = numCores.toDouble() * 100.0
            result["cpu"] = total
            result["idle"] = (total - active).coerceAtLeast(0.0)
        }
        result
    }
} catch (ignore: Throwable) {
    emptyMap()
}

// Visible only for testing
internal fun String.isProcessLabelLine(): Boolean = contains("PID USER")

// Visible only for testing
// Strips ANSI escape sequences then locates the CPU column.
// The header token S[%CPU] represents two data columns (S = state, %CPU = usage),
// so the actual data index is +1 when the token is prefixed like "S[%CPU]".
internal fun String.getCpuLabelIndex(): Int {
    val stripped = replace(ANSI_ESCAPE_REGEX, "").trim()
    val tokens = stripped.split("\\s+".toRegex())
    val index = tokens.indexOfFirst { it.contains("CPU", ignoreCase = true) }
    if (index < 0) return -1
    val token = tokens[index]
    // S[%CPU] → state col + cpu col in data rows, so skip the state column
    return if (token.isNotEmpty() && token[0] != '%' && token[0] != '[') index + 1 else index
}

// Visible only for testing
internal fun String.isProcessAppDetailsLine(pid: Int): Boolean =
    replace(ANSI_ESCAPE_REGEX, "").trim().startsWith(pid.toString())

// Visible only for testing
internal fun String.getCpuAppUsage(cpuIndex: Int): Double {
    return split("\\s+".toRegex()).takeIf { params ->
        params.isNotEmpty() && params.size >= cpuIndex
    }?.elementAt(cpuIndex)?.toDouble() ?: -1.0
}

// Visible only for testing
// Any negative value (including the -1.0 sentinel) is treated as 0.
internal fun Double?.normalize(): Double = if (this == null || this < 0.0) 0.0 else this

// Broader ANSI stripping: CSI sequences (ESC[…m), character-set selections (ESC(B / ESC)B),
// and standalone ESC-letter pairs, to avoid leaving stray bytes that break regex matching.
private val ANSI_ESCAPE_REGEX =
    "\\u001B(?:\\[[\\d;?]*[A-Za-z]|[()][A-Z0-9a-z]|[A-Za-z])".toRegex()

private const val TICKS_PER_SECOND = 100.0

private const val CPU_PID_U_TIME_INDEX = 13
private const val CPU_PID_S_TIME_INDEX = 14
private const val CPU_PID_CU_TIME_INDEX = 15
private const val CPU_PID_CS_TIME_INDEX = 16
