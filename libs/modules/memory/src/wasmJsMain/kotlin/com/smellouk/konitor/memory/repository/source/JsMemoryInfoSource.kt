package com.smellouk.konitor.memory.repository.source

import com.smellouk.konitor.memory.repository.MemoryInfoDto

@JsFun("() => { var m = window.performance && window.performance.memory; return m ? m.usedJSHeapSize : -1; }")
private external fun jsGetUsedJSHeapSize(): Double

@JsFun("() => { var m = window.performance && window.performance.memory; return m ? m.totalJSHeapSize : -1; }")
private external fun jsGetTotalJSHeapSize(): Double

@JsFun("() => { var m = window.performance && window.performance.memory; return m ? m.jsHeapSizeLimit : -1; }")
private external fun jsGetJSHeapSizeLimit(): Double

internal class JsMemoryInfoSource {
    fun getMemoryInfoDto(): MemoryInfoDto {
        val used = jsGetUsedJSHeapSize().toLong().takeIf { it >= 0 }
            ?: return MemoryInfoDto.INVALID
        val total = jsGetTotalJSHeapSize().toLong().coerceAtLeast(0L)
        val limit = jsGetJSHeapSizeLimit().toLong().coerceAtLeast(0L)

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
