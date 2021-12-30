package com.smellouk.kamper.memory.repository.source

import android.app.ActivityManager
import android.content.Context
import android.os.Process
import com.smellouk.kamper.api.Logger
import com.smellouk.kamper.memory.repository.MemoryInfoDto
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class MemoryInfoSourceTest {
    private val context = mockk<Context>()
    private val logger = mockk<Logger>(relaxed = true)

    private val classToTest: MemoryInfoSource by lazy {
        MemoryInfoSource(
            context, logger
        )
    }

    @Before
    fun setup() {
        mockkObject(RuntimeWrapper.Companion)
        mockkObject(PssInfoWrapper.Companion)
        mockkObject(RamInfoWrapper.Companion)
        mockkStatic(Process::class)
        every { Process.myPid() } returns PID
    }

    @After
    fun tearDown() {
        unmockkObject(RuntimeWrapper.Companion)
        unmockkObject(PssInfoWrapper.Companion)
        unmockkObject(RamInfoWrapper.Companion)
        unmockkStatic(Process::class)
    }

    @Test
    fun `getMemoryInfoDto should return invalid memory info when activityManager is null`() {
        every { context.getSystemService(Context.ACTIVITY_SERVICE) } returns null

        val dto = classToTest.getMemoryInfoDto()

        assertEquals(MemoryInfoDto.INVALID, dto)
    }

    @Test
    fun `getMemoryInfoDto should return invalid memory info when and exception is thrown`() {
        val activityManager = mockk<ActivityManager>(relaxed = true).apply {
            every { getProcessMemoryInfo(any()) } throws Exception("ANY_EXCEPTION")
        }
        every { context.getSystemService(Context.ACTIVITY_SERVICE) } returns activityManager

        val dto = classToTest.getMemoryInfoDto()

        assertEquals(MemoryInfoDto.INVALID, dto)
    }

    @Test
    fun `getMemoryInfoDto should return memory info`() {
        every {
            context.getSystemService(Context.ACTIVITY_SERVICE)
        } returns mockk<ActivityManager>()
        mockRuntimeMemory()
        mockPssInfoMemory()
        mockRamInfoMemory()

        val dto = classToTest.getMemoryInfoDto()

        assertEquals(EXPECTED_DTO, dto)
    }

    @Test
    fun `getMemoryInfoDto should return memory info with null pss info`() {
        every {
            context.getSystemService(Context.ACTIVITY_SERVICE)
        } returns mockk<ActivityManager>()
        mockRuntimeMemory()
        every { PssInfoWrapper.getPssInfo(any()) } returns null
        mockRamInfoMemory()

        val dto = classToTest.getMemoryInfoDto()

        assertEquals(EXPECTED_DTO_WITH_NULL_PSS, dto)
    }

    private fun mockRuntimeMemory() {
        every { RuntimeWrapper.getRuntimeInfo() } returns mockk<RuntimeWrapper>().apply {
            every { freeMemory } returns MEMORY_IN_BYTE
            every { maxMemory } returns MEMORY_IN_BYTE
            every { allocatedInBytes } returns MEMORY_IN_BYTE
        }
    }

    private fun mockPssInfoMemory() {
        every { PssInfoWrapper.getPssInfo(any()) } returns mockk<PssInfoWrapper>().apply {
            every { totalPss } returns MEMORY_IN_BYTE
            every { dalvikPss } returns MEMORY_IN_BYTE
            every { nativePss } returns MEMORY_IN_BYTE
            every { otherPss } returns MEMORY_IN_BYTE
        }
    }

    private fun mockRamInfoMemory() {
        every { RamInfoWrapper.getRamInfo(any()) } returns mockk<RamInfoWrapper>().apply {
            every { availMem } returns MEMORY_IN_BYTE
            every { totalMem } returns MEMORY_IN_BYTE
            every { threshold } returns MEMORY_IN_BYTE
            every { lowMemory } returns true
        }
    }
}

private const val PID = 1234
private const val MEMORY_IN_BYTE = 1024 * 1024L
private val EXPECTED_DTO = MemoryInfoDto(
    // App
    freeMemoryInBytes = MEMORY_IN_BYTE,
    maxMemoryInBytes = MEMORY_IN_BYTE,
    allocatedInBytes = MEMORY_IN_BYTE,
    // PSS
    totalPssInBytes = MEMORY_IN_BYTE,
    dalvikPssInBytes = MEMORY_IN_BYTE,
    nativePssInBytes = MEMORY_IN_BYTE,
    otherPssInBytes = MEMORY_IN_BYTE,
    // Ram
    availableRamInBytes = MEMORY_IN_BYTE,
    totalRamInBytes = MEMORY_IN_BYTE,
    lowRamThresholdInBytes = MEMORY_IN_BYTE,
    isLowMemory = true
)
private val EXPECTED_DTO_WITH_NULL_PSS = MemoryInfoDto(
    // App
    freeMemoryInBytes = MEMORY_IN_BYTE,
    maxMemoryInBytes = MEMORY_IN_BYTE,
    allocatedInBytes = MEMORY_IN_BYTE,
    // PSS
    totalPssInBytes = null,
    dalvikPssInBytes = null,
    nativePssInBytes = null,
    otherPssInBytes = null,
    // Ram
    availableRamInBytes = MEMORY_IN_BYTE,
    totalRamInBytes = MEMORY_IN_BYTE,
    lowRamThresholdInBytes = MEMORY_IN_BYTE,
    isLowMemory = true
)
