package com.smellouk.kamper.memory.repository.source

import com.smellouk.kamper.memory.repository.MemoryInfoDto
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.value
import platform.darwin.HOST_VM_INFO64
import platform.darwin.KERN_SUCCESS
import platform.darwin.MACH_TASK_BASIC_INFO
import platform.darwin.host_statistics64
import platform.darwin.mach_host_self
import platform.darwin.mach_task_basic_info
import platform.darwin.mach_task_self_
import platform.darwin.task_info
import platform.darwin.vm_statistics64
import platform.Foundation.NSProcessInfo

@OptIn(ExperimentalForeignApi::class)
internal class IosMemoryInfoSource {
    fun getMemoryInfoDto(): MemoryInfoDto {
        val totalRam = NSProcessInfo.processInfo.physicalMemory.toLong()
        val appMemory = readAppMemory()
        val availRam = readAvailableRam()
        val threshold = totalRam / LOW_MEMORY_THRESHOLD_DIVISOR

        return MemoryInfoDto(
            maxMemoryInBytes = totalRam / PROCESS_MAX_RATIO,
            allocatedInBytes = appMemory,
            totalPssInKiloBytes = null,
            dalvikPssInKiloBytes = null,
            nativePssInKiloBytes = null,
            otherPssInKiloBytes = null,
            availableRamInBytes = availRam,
            totalRamInBytes = totalRam,
            lowRamThresholdInBytes = threshold,
            isLowMemory = availRam > 0L && availRam < threshold
        )
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun readAppMemory(): Long = memScoped {
        val info = alloc<mach_task_basic_info>()
        val count = alloc<UIntVar>()
        count.value = (sizeOf<mach_task_basic_info>() / WORD_SIZE).toUInt()

        val kr = task_info(
            mach_task_self_,
            MACH_TASK_BASIC_INFO.toUInt(),
            info.ptr.reinterpret(),
            count.ptr
        )

        if (kr == KERN_SUCCESS) info.resident_size.toLong() else 0L
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun readAvailableRam(): Long = memScoped {
        val vmStats = alloc<vm_statistics64>()
        val count = alloc<UIntVar>()
        count.value = (sizeOf<vm_statistics64>() / WORD_SIZE).toUInt()

        val kr = host_statistics64(
            mach_host_self(),
            HOST_VM_INFO64,
            vmStats.ptr.reinterpret(),
            count.ptr
        )

        if (kr == KERN_SUCCESS) {
            (vmStats.free_count.toLong() + vmStats.inactive_count.toLong()) * PAGE_SIZE
        } else 0L
    }

    private companion object {
        const val LOW_MEMORY_THRESHOLD_DIVISOR = 20L
        const val PROCESS_MAX_RATIO = 4L
        const val PAGE_SIZE = 4096L
        const val WORD_SIZE = 4L
    }
}
