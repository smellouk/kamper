package com.smellouk.konitor.gpu

import com.smellouk.konitor.gpu.repository.source.OshiGpuInfoSource
import kotlin.test.Test
import kotlin.test.assertNotNull

@Suppress("IllegalIdentifier")
class OshiGpuInfoSourceTest {

    @Test
    fun `OshiGpuInfoSource never throws and never returns null`() {
        val source = OshiGpuInfoSource()
        val info = source.getInfo()
        // OSHI provides no utilization API per 23-RESEARCH; valid outcomes are:
        //   - GpuInfo.UNSUPPORTED (no GPU on host)
        //   - GpuInfo(utilization = -1.0, usedMemoryMb = -1.0, totalMemoryMb = vramMb)  (partial data, D-02)
        //   - GpuInfo.INVALID (caught exception)
        assertNotNull(info)
    }
}
