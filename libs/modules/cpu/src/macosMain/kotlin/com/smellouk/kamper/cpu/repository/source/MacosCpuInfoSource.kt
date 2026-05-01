@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.smellouk.kamper.cpu.repository.source

import com.smellouk.kamper.cpu.cinterop.KamperCpuTicks
import com.smellouk.kamper.cpu.cinterop.kamper_fill_cpu_ticks
import com.smellouk.kamper.cpu.cinterop.kamper_get_logical_cpu_count
import com.smellouk.kamper.cpu.repository.CpuInfoDto
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.posix.CLOCK_MONOTONIC
import platform.posix.RUSAGE_SELF
import platform.posix.clock_gettime
import platform.posix.getrusage
import platform.posix.rusage
import platform.posix.timespec

internal class MacosCpuInfoSource : CpuInfoSource {
    // System-wide CPU tick state (Mach host_statistics counters since boot)
    private var prevUser: Long = 0L
    private var prevSys: Long = 0L
    private var prevIdle: Long = 0L
    private var prevNice: Long = 0L
    private var cpuInitialized = false

    // App CPU state (getrusage delta)
    private var prevCpuUs: Long = -1L
    private var prevWallUs: Long = -1L
    private val numCpus: Int by lazy { kamper_get_logical_cpu_count() }

    override fun getCpuInfoDto(): CpuInfoDto = memScoped {
        val ticks = alloc<KamperCpuTicks>()
        if (kamper_fill_cpu_ticks(ticks.ptr) != 0) {
            return@memScoped CpuInfoDto.INVALID
        }

        val user = ticks.user
        val sys = ticks.sys
        val idle = ticks.idle
        val nice = ticks.nice

        if (!cpuInitialized) {
            prevUser = user; prevSys = sys; prevIdle = idle; prevNice = nice
            cpuInitialized = true
            sampleAppCpuPct() // prime wall-clock baseline
            return@memScoped CpuInfoDto.INVALID
        }

        val dUser = (user - prevUser).coerceAtLeast(0L)
        val dSys = (sys - prevSys).coerceAtLeast(0L)
        val dIdle = (idle - prevIdle).coerceAtLeast(0L)
        val dNice = (nice - prevNice).coerceAtLeast(0L)
        val dTotal = dUser + dSys + dIdle + dNice

        prevUser = user; prevSys = sys; prevIdle = idle; prevNice = nice

        if (dTotal == 0L) return@memScoped CpuInfoDto.INVALID

        val userPct = dUser.toDouble() / dTotal * 100.0
        val sysPct = dSys.toDouble() / dTotal * 100.0
        val idlePct = dIdle.toDouble() / dTotal * 100.0
        val appPct = sampleAppCpuPct()

        CpuInfoDto(
            totalTime = 100.0,
            userTime = userPct,
            systemTime = sysPct,
            idleTime = idlePct,
            ioWaitTime = 0.0,
            appTime = appPct
        )
    }

    private fun sampleAppCpuPct(): Double = memScoped {
        val usage = alloc<rusage>()
        if (getrusage(RUSAGE_SELF, usage.ptr) != 0) return@memScoped 0.0

        val cpuUs = usage.ru_utime.tv_sec * MICROS_PER_SEC + usage.ru_utime.tv_usec.toLong() +
            usage.ru_stime.tv_sec * MICROS_PER_SEC + usage.ru_stime.tv_usec.toLong()
        val ts = alloc<timespec>()
        clock_gettime(CLOCK_MONOTONIC.toUInt(), ts.ptr)
        val wallUs = ts.tv_sec * MICROS_PER_SEC + ts.tv_nsec / NANOS_PER_MICRO

        val result = if (prevCpuUs < 0L) {
            0.0
        } else {
            val dCpu = (cpuUs - prevCpuUs).coerceAtLeast(0L)
            val dWall = (wallUs - prevWallUs).coerceAtLeast(1L)
            // Normalize by core count: KN spawns multiple worker threads whose combined
            // CPU time can exceed wall time on a multi-core Mac, inflating the ratio.
            (dCpu.toDouble() / (dWall.toDouble() * numCpus) * 100.0).coerceIn(0.0, 100.0)
        }
        prevCpuUs = cpuUs
        prevWallUs = wallUs
        result
    }

    private companion object {
        const val MICROS_PER_SEC = 1_000_000L
        const val NANOS_PER_MICRO = 1_000L
    }
}
