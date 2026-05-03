package com.smellouk.konitor.gpu

import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.EMPTY
import com.smellouk.konitor.gpu.repository.source.PixelGpuInfoSource
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals

@Suppress("IllegalIdentifier")
class PixelGpuInfoSourceTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `getInfo returns INVALID on hosts without pixel gpu sysfs`() {
        val source = PixelGpuInfoSource(Logger.EMPTY)
        assertEquals(GpuInfo.INVALID, source.getInfo())
    }

    @Test
    fun `getInfo reads gpu_busy as direct utilization`() {
        val dir = tmp.newFolder("pixelgpu")
        dir.resolve("gpu_busy").writeText("$GPU_BUSY_PCT")
        val source = PixelGpuInfoSource(Logger.EMPTY, dir)
        assertEquals(GPU_BUSY_PCT.toDouble(), source.getInfo().utilization, TOLERANCE)
    }

    @Test
    fun `getInfo falls back to freq proxy when gpu_busy absent`() {
        val dir = tmp.newFolder("pixelgpu2")
        dir.resolve("gpu_freq").writeText(GPU_FREQ.toString())
        dir.resolve("gpu_max_freq").writeText(GPU_MAX_FREQ.toString())
        val source = PixelGpuInfoSource(Logger.EMPTY, dir)
        val expected = (GPU_FREQ.toDouble() / GPU_MAX_FREQ.toDouble()) * PERCENT
        assertEquals(expected, source.getInfo().utilization, TOLERANCE)
    }

    @Test
    fun `getInfo returns INVALID when no readable files exist`() {
        val dir = tmp.newFolder("pixelgpu3")
        val source = PixelGpuInfoSource(Logger.EMPTY, dir)
        assertEquals(GpuInfo.INVALID, source.getInfo())
    }

    private companion object {
        const val GPU_BUSY_PCT = 42L
        const val GPU_FREQ = 650_000_000L
        const val GPU_MAX_FREQ = 848_000_000L
        const val PERCENT = 100.0
        const val TOLERANCE = 0.01
    }
}
