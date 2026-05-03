package com.smellouk.konitor.gpu

import com.smellouk.konitor.api.Logger
import com.smellouk.konitor.api.EMPTY
import com.smellouk.konitor.gpu.repository.source.DevfreqGpuInfoSource
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import kotlin.test.assertEquals

@Suppress("IllegalIdentifier")
class DevfreqGpuInfoSourceTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `getInfo returns INVALID when no mali sysfs dir exists`() {
        val source = DevfreqGpuInfoSource(Logger.EMPTY) { null }
        assertEquals(GpuInfo.INVALID, source.getInfo())
    }

    @Test
    fun `getInfo uses max_freq when available`() {
        val dir = tmp.newFolder("mali-maxfreq")
        dir.resolve("cur_freq").writeText(CUR_FREQ_HALF.toString())
        dir.resolve("max_freq").writeText(MAX_FREQ.toString())
        val source = DevfreqGpuInfoSource(Logger.EMPTY) { dir }
        val expected = (CUR_FREQ_HALF.toDouble() / MAX_FREQ.toDouble()) * PERCENT
        assertEquals(expected, source.getInfo().utilization, TOLERANCE)
    }

    @Test
    fun `getInfo falls back to available_frequencies when max_freq absent`() {
        val dir = tmp.newFolder("mali-avail")
        dir.resolve("cur_freq").writeText(CUR_FREQ_HIGH.toString())
        dir.resolve("available_frequencies")
            .writeText("$FREQ_MIN $FREQ_202 $FREQ_400 $CUR_FREQ_HIGH $MAX_FREQ")
        val source = DevfreqGpuInfoSource(Logger.EMPTY) { dir }
        val expected = (CUR_FREQ_HIGH.toDouble() / MAX_FREQ.toDouble()) * PERCENT
        assertEquals(expected, source.getInfo().utilization, TOLERANCE)
    }

    @Test
    fun `getInfo handles newline-separated available_frequencies`() {
        val dir = tmp.newFolder("mali-newline")
        dir.resolve("cur_freq").writeText(FREQ_400.toString())
        dir.resolve("available_frequencies").writeText("$FREQ_MIN\n$FREQ_400\n$MAX_FREQ\n")
        val source = DevfreqGpuInfoSource(Logger.EMPTY) { dir }
        val expected = (FREQ_400.toDouble() / MAX_FREQ.toDouble()) * PERCENT
        assertEquals(expected, source.getInfo().utilization, TOLERANCE)
    }

    @Test
    fun `getInfo returns partial data with memory when max freq not available`() {
        val dir = tmp.newFolder("mali-memonly")
        dir.resolve("cur_freq").writeText(FREQ_400.toString())
        dir.resolve("dma_buf_gpu_mem").writeText(DMA_BUF_BYTES.toString())
        dir.resolve("total_gpu_mem").writeText(TOTAL_MEM_BYTES.toString())
        // no max_freq or available_frequencies
        val source = DevfreqGpuInfoSource(Logger.EMPTY) { dir }
        val info = source.getInfo()
        assertEquals(UNKNOWN, info.utilization, TOLERANCE)
        assertEquals(DMA_BUF_BYTES / BYTES_PER_MB, info.usedMemoryMb, TOLERANCE)
        assertEquals(TOTAL_MEM_BYTES / BYTES_PER_MB, info.totalMemoryMb, TOLERANCE)
    }

    @Test
    fun `getInfo returns full info when freq and memory both available`() {
        val dir = tmp.newFolder("mali-full")
        dir.resolve("cur_freq").writeText(CUR_FREQ_HALF.toString())
        dir.resolve("max_freq").writeText(MAX_FREQ.toString())
        dir.resolve("dma_buf_gpu_mem").writeText(DMA_BUF_BYTES.toString())
        dir.resolve("total_gpu_mem").writeText(TOTAL_MEM_BYTES.toString())
        val source = DevfreqGpuInfoSource(Logger.EMPTY) { dir }
        val info = source.getInfo()
        val expectedPct = (CUR_FREQ_HALF.toDouble() / MAX_FREQ.toDouble()) * PERCENT
        assertEquals(expectedPct, info.utilization, TOLERANCE)
        assertEquals(DMA_BUF_BYTES / BYTES_PER_MB, info.usedMemoryMb, TOLERANCE)
        assertEquals(TOTAL_MEM_BYTES / BYTES_PER_MB, info.totalMemoryMb, TOLERANCE)
    }

    @Test
    fun `getInfo returns INVALID when dir exists but no readable files`() {
        val dir = tmp.newFolder("mali-empty")
        val source = DevfreqGpuInfoSource(Logger.EMPTY) { dir }
        assertEquals(GpuInfo.INVALID, source.getInfo())
    }

    @Test
    fun `getInfo clamps utilization to 100 when cur_freq exceeds max`() {
        val dir = tmp.newFolder("mali-clamp")
        dir.resolve("cur_freq").writeText(CUR_FREQ_OVER.toString())
        dir.resolve("max_freq").writeText(MAX_FREQ.toString())
        val source = DevfreqGpuInfoSource(Logger.EMPTY) { dir }
        assertEquals(MAX_PCT, source.getInfo().utilization, TOLERANCE)
    }

    private companion object {
        const val MAX_FREQ = 848_000_000L
        const val CUR_FREQ_HALF = 424_000_000L
        const val CUR_FREQ_HIGH = 650_000_000L
        const val CUR_FREQ_OVER = 900_000_000L
        const val FREQ_MIN = 151_000_000L
        const val FREQ_202 = 202_000_000L
        const val FREQ_400 = 400_000_000L
        const val DMA_BUF_BYTES = 244_498_432L
        const val TOTAL_MEM_BYTES = 567_934_976L
        const val BYTES_PER_MB = 1024.0 * 1024.0
        const val PERCENT = 100.0
        const val MAX_PCT = 100.0
        const val UNKNOWN = -1.0
        const val TOLERANCE = 0.01
    }
}
