package com.smellouk.kamper.cpu.repository.source

import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.cpu.repository.CpuInfoDto
import java.io.FileInputStream
import android.os.Process as ProcessPidProvider

/**
 * CPU usage for Android 8+ (Oreo).
 *
 * Global stats (total/user/system/idle/iowait) come from /proc/stat: the first line
 * is read on each sample and a delta is computed against the previous cached reading.
 * On the first call no delta is available so CpuInfoDto.INVALID is returned; the
 * second and subsequent calls yield real delta-based percentages.
 *
 * App CPU is read from /proc/[pid]/stat ticks (utime+stime+cutime+cstime).
 *
 * If /proc/stat is unreadable (e.g., SELinux denial on a locked-down OEM ROM)
 * getCpuInfoDto() returns CpuInfoDto.INVALID — no retry, no subprocess fallback.
 */
internal class ShellCpuInfoSource(
    private val logger: Logger
) : CpuInfoSource {

    private var cachedAppTicks: Long = -1L
    private var cachedUserTicks: Long = -1L
    private var cachedSysTicks: Long = -1L
    private var cachedSampleTimeMs: Long = -1L

    private var cachedProcStatTotal: Long = -1L
    private var cachedProcStatUser: Long = -1L
    private var cachedProcStatSystem: Long = -1L
    private var cachedProcStatIdle: Long = -1L
    private var cachedProcStatIowait: Long = -1L

    @RequiresApi(api = Build.VERSION_CODES.O)
    override fun getCpuInfoDto(): CpuInfoDto {
        val pid = ProcessPidProvider.myPid()
        val ticks = readProcPidTicks(pid)
        val currentTimeMs = SystemClock.elapsedRealtime()
        val elapsedMs = computeElapsedMs(currentTimeMs)
        val appTicksDelta = computeDelta(cachedAppTicks, ticks.total)
        updatePidTicksCache(ticks, currentTimeMs)

        val snapshot = readProcStat()
        val globalValid = cachedProcStatTotal >= 0L && snapshot != ProcStatSnapshot.INVALID

        if (snapshot == ProcStatSnapshot.INVALID && appTicksDelta < 0L) {
            return CpuInfoDto.INVALID
        }

        val deltas = computeGlobalDeltas(snapshot, globalValid)
        updateProcStatCache(snapshot)

        Log.d(
            "Kamper/CPU/Shell",
            "pid=$pid total=${deltas.total} user=${deltas.user} sys=${deltas.system} " +
                "idle=${deltas.idle} iowait=${deltas.iowait} appDelta=$appTicksDelta elapsedMs=$elapsedMs"
        )

        if (!globalValid) {
            return CpuInfoDto.INVALID
        }

        return buildCpuInfoDto(deltas, appTicksDelta, elapsedMs)
    }

    private fun computeElapsedMs(currentTimeMs: Long): Long =
        if (cachedSampleTimeMs > 0L) {
            (currentTimeMs - cachedSampleTimeMs).coerceAtLeast(1L)
        } else {
            DEFAULT_ELAPSED_MS
        }

    private fun computeDelta(cached: Long, current: Long): Long =
        if (cached >= 0L && current >= 0L) (current - cached).coerceAtLeast(0L) else -1L

    private fun updatePidTicksCache(ticks: ProcTicks, currentTimeMs: Long) {
        cachedAppTicks = ticks.total
        cachedUserTicks = ticks.user
        cachedSysTicks = ticks.sys
        cachedSampleTimeMs = currentTimeMs
    }

    private fun computeGlobalDeltas(snapshot: ProcStatSnapshot, globalValid: Boolean): GlobalDeltas =
        GlobalDeltas(
            total = if (globalValid) (snapshot.total - cachedProcStatTotal).coerceAtLeast(0L) else -1L,
            user = if (globalValid) (snapshot.userPlusNice - cachedProcStatUser).coerceAtLeast(0L) else -1L,
            system = if (globalValid) (snapshot.system - cachedProcStatSystem).coerceAtLeast(0L) else -1L,
            idle = if (globalValid) (snapshot.idle - cachedProcStatIdle).coerceAtLeast(0L) else -1L,
            iowait = if (globalValid) (snapshot.iowait - cachedProcStatIowait).coerceAtLeast(0L) else -1L
        )

    private fun updateProcStatCache(snapshot: ProcStatSnapshot) {
        if (snapshot != ProcStatSnapshot.INVALID) {
            cachedProcStatTotal = snapshot.total
            cachedProcStatUser = snapshot.userPlusNice
            cachedProcStatSystem = snapshot.system
            cachedProcStatIdle = snapshot.idle
            cachedProcStatIowait = snapshot.iowait
        }
    }

    private fun buildCpuInfoDto(deltas: GlobalDeltas, appTicksDelta: Long, elapsedMs: Long): CpuInfoDto {
        val appTime = if (appTicksDelta >= 0L) {
            (appTicksDelta.toDouble() / (TICKS_PER_SECOND * elapsedMs / MS_PER_SECOND)) * PERCENT_FACTOR
        } else {
            0.0
        }
        return CpuInfoDto(
            totalTime = deltas.total.toDouble(),
            userTime = deltas.user.toDouble(),
            systemTime = deltas.system.toDouble(),
            idleTime = deltas.idle.toDouble(),
            ioWaitTime = deltas.iowait.toDouble(),
            appTime = appTime
        )
    }

    // Read utime/stime/cutime/cstime from /proc/[pid]/stat (always accessible for own PID).
    private fun readProcPidTicks(pid: Int): ProcTicks = try {
        val line = ShellProcFileReader.getPidStatLine(pid)
        val parts = line.split(" ")
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
        Log.d("Kamper/CPU/Shell", "/proc/$pid/stat read failed: ${e.message}")
        ProcTicks.INVALID
    }

    private fun readProcStat(): ProcStatSnapshot = try {
        val line = ShellProcFileReader.getStatLine()
        val parts = line.trim().split("\\s+".toRegex())
        if (parts.size > CPU_STAT_STEAL_INDEX) {
            ProcStatSnapshot(
                user = parts[CPU_STAT_USER_INDEX].toLong(),
                nice = parts[CPU_STAT_NICE_INDEX].toLong(),
                system = parts[CPU_STAT_SYSTEM_INDEX].toLong(),
                idle = parts[CPU_STAT_IDLE_INDEX].toLong(),
                iowait = parts[CPU_STAT_IOWAIT_INDEX].toLong(),
                irq = parts[CPU_STAT_IRQ_INDEX].toLong(),
                softirq = parts[CPU_STAT_SOFTIRQ_INDEX].toLong(),
                steal = parts[CPU_STAT_STEAL_INDEX].toLong()
            )
        } else {
            ProcStatSnapshot.INVALID
        }
    } catch (e: Exception) {
        Log.d("Kamper/CPU/Shell", "/proc/stat read failed: ${e.message}")
        ProcStatSnapshot.INVALID
    }

    private data class ProcTicks(val user: Long, val sys: Long) {
        val total: Long get() = user + sys
        companion object {
            val INVALID = ProcTicks(-1L, -1L)
        }
    }

    private data class ProcStatSnapshot(
        val user: Long,
        val nice: Long,
        val system: Long,
        val idle: Long,
        val iowait: Long,
        val irq: Long,
        val softirq: Long,
        val steal: Long
    ) {
        val total: Long get() = user + nice + system + idle + iowait + irq + softirq + steal
        val userPlusNice: Long get() = user + nice
        companion object {
            val INVALID = ProcStatSnapshot(-1L, -1L, -1L, -1L, -1L, -1L, -1L, -1L)
        }
    }

    private data class GlobalDeltas(
        val total: Long,
        val user: Long,
        val system: Long,
        val idle: Long,
        val iowait: Long
    )
}

internal object ShellProcFileReader {
    // Visible only for testing
    internal fun getStatLine(): String = FileInputStream("/proc/stat")
        .bufferedReader()
        .use { it.readLine() }

    // Visible only for testing
    internal fun getPidStatLine(pid: Int): String = FileInputStream("/proc/$pid/stat")
        .bufferedReader()
        .use { it.readLine() }
}

private const val TICKS_PER_SECOND = 100.0
private const val MS_PER_SECOND = 1000.0
private const val PERCENT_FACTOR = 100.0
private const val DEFAULT_ELAPSED_MS = 1000L

private const val CPU_PID_U_TIME_INDEX = 13
private const val CPU_PID_S_TIME_INDEX = 14
private const val CPU_PID_CU_TIME_INDEX = 15
private const val CPU_PID_CS_TIME_INDEX = 16

private const val CPU_STAT_USER_INDEX = 1
private const val CPU_STAT_NICE_INDEX = 2
private const val CPU_STAT_SYSTEM_INDEX = 3
private const val CPU_STAT_IDLE_INDEX = 4
private const val CPU_STAT_IOWAIT_INDEX = 5
private const val CPU_STAT_IRQ_INDEX = 6
private const val CPU_STAT_SOFTIRQ_INDEX = 7
private const val CPU_STAT_STEAL_INDEX = 8
