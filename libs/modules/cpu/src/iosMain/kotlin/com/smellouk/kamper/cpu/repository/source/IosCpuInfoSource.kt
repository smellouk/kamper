package com.smellouk.kamper.cpu.repository.source

import com.smellouk.kamper.cpu.repository.CpuInfoDto
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.ULongVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.value
import platform.darwin.CPU_STATE_IDLE
import platform.darwin.CPU_STATE_MAX
import platform.darwin.CPU_STATE_NICE
import platform.darwin.CPU_STATE_SYSTEM
import platform.darwin.CPU_STATE_USER
import platform.darwin.KERN_SUCCESS
import platform.darwin.PROCESSOR_CPU_LOAD_INFO
import platform.darwin.THREAD_BASIC_INFO
import platform.darwin.THREAD_BASIC_INFO_COUNT
import platform.darwin.TH_USAGE_SCALE
import platform.darwin.host_processor_info
import platform.darwin.mach_host_self
import platform.darwin.mach_task_self_
import platform.darwin.natural_tVar
import platform.darwin.task_threads
import platform.darwin.thread_basic_info
import platform.darwin.thread_info
import platform.darwin.vm_deallocate

@OptIn(ExperimentalForeignApi::class)
internal class IosCpuInfoSource : CpuInfoSource {
    private var prevUser = 0L
    private var prevSystem = 0L
    private var prevIdle = 0L
    private var prevNice = 0L
    private var initialized = false
    private var coreCount = 1

    override fun getCpuInfoDto(): CpuInfoDto {
        val ticks = readCpuTicks() ?: return CpuInfoDto.INVALID

        return if (!initialized) {
            prevUser = ticks.user
            prevSystem = ticks.system
            prevIdle = ticks.idle
            prevNice = ticks.nice
            initialized = true
            CpuInfoDto.INVALID
        } else {
            val dUser = (ticks.user - prevUser).toDouble()
            val dSystem = (ticks.system - prevSystem).toDouble()
            val dIdle = (ticks.idle - prevIdle).toDouble()
            val dNice = (ticks.nice - prevNice).toDouble()
            val total = dUser + dSystem + dIdle + dNice

            prevUser = ticks.user
            prevSystem = ticks.system
            prevIdle = ticks.idle
            prevNice = ticks.nice

            if (total <= 0) CpuInfoDto.INVALID
            else {
                val appRatio = readAppCpuRatio()
                CpuInfoDto(
                    totalTime = total,
                    userTime = dUser + dNice,
                    systemTime = dSystem,
                    idleTime = dIdle,
                    ioWaitTime = 0.0,
                    appTime = appRatio * total
                )
            }
        }
    }

    private fun readAppCpuRatio(): Double = memScoped {
        val threadList = alloc<CPointerVar<UIntVar>>()
        val threadCount = alloc<UIntVar>()

        val kr = task_threads(
            mach_task_self_,
            threadList.ptr.reinterpret(),
            threadCount.ptr
        )
        if (kr != KERN_SUCCESS) return@memScoped 0.0

        val count = threadCount.value.toInt()
        val threads = threadList.value ?: return@memScoped 0.0

        var totalUsage = 0L
        for (i in 0 until count) {
            val info = alloc<thread_basic_info>()
            val infoCount = alloc<UIntVar>()
            infoCount.value = THREAD_BASIC_INFO_COUNT
            val r = thread_info(
                threads[i],
                THREAD_BASIC_INFO.toUInt(),
                info.ptr.reinterpret(),
                infoCount.ptr
            )
            if (r == KERN_SUCCESS && info.cpu_usage > 0) {
                totalUsage += info.cpu_usage
            }
        }

        val listAddr = threadList.reinterpret<ULongVar>().value
        val listSize = (count.toLong() * INT_SIZE_BYTES).toULong()
        vm_deallocate(mach_task_self_, listAddr, listSize)

        totalUsage.toDouble() / (TH_USAGE_SCALE.toDouble() * coreCount.coerceAtLeast(1))
    }

    private companion object {
        const val INT_SIZE_BYTES = 4L
    }

    private data class CpuTicks(
        val user: Long,
        val system: Long,
        val idle: Long,
        val nice: Long
    )

    private fun readCpuTicks(): CpuTicks? = memScoped {
        val processorCount = alloc<natural_tVar>()
        val processorInfo = alloc<CPointerVar<IntVar>>()
        val processorInfoCount = alloc<UIntVar>()

        val kr = host_processor_info(
            mach_host_self(),
            PROCESSOR_CPU_LOAD_INFO,
            processorCount.ptr,
            processorInfo.ptr.reinterpret(),
            processorInfoCount.ptr
        )

        if (kr != KERN_SUCCESS) return@memScoped null

        val count = processorCount.value.toInt()
        coreCount = count
        val info = processorInfo.value ?: return@memScoped null

        var user = 0L; var sys = 0L; var idle = 0L; var nice = 0L
        for (i in 0 until count) {
            val base = i * CPU_STATE_MAX
            user += info[base + CPU_STATE_USER].toLong()
            sys += info[base + CPU_STATE_SYSTEM].toLong()
            idle += info[base + CPU_STATE_IDLE].toLong()
            nice += info[base + CPU_STATE_NICE].toLong()
        }

        val infoAddress = processorInfo.reinterpret<ULongVar>().value
        val infoSize = (processorInfoCount.value.toLong() * INT_SIZE_BYTES).toULong()
        vm_deallocate(mach_task_self_, infoAddress, infoSize)

        CpuTicks(user, sys, idle, nice)
    }
}
