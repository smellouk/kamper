package com.smellouk.konitor.memory.repository.source

import com.smellouk.konitor.memory.repository.MemoryInfoDto
import kotlinx.browser.window

internal class JsMemoryInfoSource {
    fun getMemoryInfoDto(): MemoryInfoDto {
        val memory: dynamic = window.asDynamic().performance.memory
            ?: return MemoryInfoDto.INVALID

        val used = (memory.usedJSHeapSize as? Double)?.toLong()
            ?: return MemoryInfoDto.INVALID
        val total = (memory.totalJSHeapSize as? Double)?.toLong() ?: 0L
        val limit = (memory.jsHeapSizeLimit as? Double)?.toLong() ?: 0L

        val threshold = if (limit > 0) limit / LOW_MEMORY_DIVISOR else 0L
        val available = maxOf(0L, limit - used)

        return MemoryInfoDto(
            maxMemoryInBytes = limit,
            allocatedInBytes = used,
            totalPssInKiloBytes = null,
            dalvikPssInKiloBytes = null,
            nativePssInKiloBytes = null,
            otherPssInKiloBytes = null,
            availableRamInBytes = available,
            totalRamInBytes = limit,
            lowRamThresholdInBytes = threshold,
            isLowMemory = threshold > 0 && available < threshold
        )
    }

    private companion object {
        const val LOW_MEMORY_DIVISOR = 10L
    }
}
