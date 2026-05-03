package com.smellouk.konitor.cpu.repository.source

import android.os.SystemClock
import android.util.Log
import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.cpu.repository.CpuInfoDto
import android.os.Process as ProcessPidProvider

/**
 * Fallback CPU source for API 26+ devices where SELinux blocks /proc/stat.
 *
 * Reads /proc/<pid>/stat (always accessible) and computes per-process tick deltas.
 * utime/cutime deltas map to userTime; stime/cstime deltas map to systemTime.
 * These reflect the app's own user vs kernel CPU split, not system-wide totals.
 * System-wide ioWait is unavailable without /proc/stat and stays 0.
 * totalTime is synthesized from availableProcessors × 100 ticks/sec × elapsed time.
 *
 * Returns INVALID on the first call (no delta yet).
 */
internal class ShellCpuInfoSource(
    private val logger: Logger
) : CpuInfoSource {

    private var cachedUserTicks: Long = -1L
    private var cachedSysTicks: Long = -1L
    private var cachedTimeMs: Long = -1L

    override fun getCpuInfoDto(): CpuInfoDto {
        val pid = ProcessPidProvider.myPid()

        val pidStatLine = try {
            ProcFileReader.getCpuProcPidStatTime(pid)
                .also { Log.d(TAG, "/proc/$pid/stat: $it") }
        } catch (e: Exception) {
            Log.e(TAG, "/proc/$pid/stat read failed: ${e.javaClass.simpleName}: ${e.message}")
            return CpuInfoDto.INVALID
        }

        val parts = pidStatLine.trim().split(" ")
        if (parts.size < EXPECTED_PID_STAT_SIZE) {
            Log.w(TAG, "/proc/$pid/stat: unexpected field count ${parts.size}")
            return CpuInfoDto.INVALID
        }

        val utime = parts[UTIME_INDEX].toLongOrNull() ?: return CpuInfoDto.INVALID
        val stime = parts[STIME_INDEX].toLongOrNull() ?: return CpuInfoDto.INVALID
        val cutime = parts[CUTIME_INDEX].toLongOrNull() ?: return CpuInfoDto.INVALID
        val cstime = parts[CSTIME_INDEX].toLongOrNull() ?: return CpuInfoDto.INVALID
        val userTicks = utime + cutime
        val sysTicks = stime + cstime

        val currentTimeMs = SystemClock.elapsedRealtime()

        return if (cachedUserTicks < 0L) {
            cachedUserTicks = userTicks
            cachedSysTicks = sysTicks
            cachedTimeMs = currentTimeMs
            Log.d(TAG, "first sample: userTicks=$userTicks sysTicks=$sysTicks (caching, returning INVALID)")
            CpuInfoDto.INVALID
        } else {
            val userDelta = (userTicks - cachedUserTicks).coerceAtLeast(0L).toDouble()
            val sysDelta = (sysTicks - cachedSysTicks).coerceAtLeast(0L).toDouble()
            val appDelta = userDelta + sysDelta
            val elapsedMs = (currentTimeMs - cachedTimeMs).coerceAtLeast(1L)
            val numCores = Runtime.getRuntime().availableProcessors()
            // 100 jiffies/sec per core is standard Linux clock tick rate
            val totalTicks = (elapsedMs / 10.0) * numCores

            Log.d(TAG, "userDelta=$userDelta sysDelta=$sysDelta appDelta=$appDelta totalTicks=$totalTicks elapsedMs=$elapsedMs numCores=$numCores")

            cachedUserTicks = userTicks
            cachedSysTicks = sysTicks
            cachedTimeMs = currentTimeMs

            CpuInfoDto(
                totalTime = totalTicks,
                userTime = userDelta,
                systemTime = sysDelta,
                idleTime = (totalTicks - appDelta).coerceAtLeast(0.0),
                ioWaitTime = 0.0,
                appTime = appDelta
            )
        }
    }
}

private const val TAG = "Konitor/CPU/Shell"
private const val EXPECTED_PID_STAT_SIZE = 17
private const val UTIME_INDEX = 13
private const val STIME_INDEX = 14
private const val CUTIME_INDEX = 15
private const val CSTIME_INDEX = 16
