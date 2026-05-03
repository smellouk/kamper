package com.smellouk.konitor.cpu.repository

import com.smellouk.konitor.cpu.CpuInfo
import com.smellouk.konitor.cpu.repository.source.CpuInfoSource
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class CpuInfoRepositoryImplUnsupportedTest {

    private val procCpuInfoSource = mockk<CpuInfoSource>()
    private val shellCpuInfoSource = mockk<CpuInfoSource>()
    private val cpuInfoMapper = mockk<CpuInfoMapper>(relaxed = true)

    private val classToTest = CpuInfoRepositoryImpl(
        procCpuInfoSource = procCpuInfoSource,
        shellCpuInfoSource = shellCpuInfoSource,
        cpuInfoMapper = cpuInfoMapper
    )

    @Before
    fun setup() {
        mockkObject(ApiLevelProvider)
        mockkObject(ProcStatAccessibilityProvider)
    }

    @After
    fun teardown() {
        unmockkObject(ApiLevelProvider)
        unmockkObject(ProcStatAccessibilityProvider)
    }

    @Test
    fun getInfo_should_return_UNSUPPORTED_when_procStat_blocked_and_shell_returns_INVALID() {
        every { ApiLevelProvider.getApiLevel() } returns API_26
        every { ProcStatAccessibilityProvider.isAccessible() } returns false
        every { shellCpuInfoSource.getCpuInfoDto() } returns CpuInfoDto.INVALID

        // First call is the warm-up (delta sources always return INVALID on baseline caching).
        // UNSUPPORTED is only confirmed on the second call when INVALID recurs.
        classToTest.getInfo()
        val result = classToTest.getInfo()

        assertEquals(CpuInfo.UNSUPPORTED, result)
    }

    @Test
    fun getInfo_should_not_retry_sources_after_UNSUPPORTED_is_cached() {
        every { ApiLevelProvider.getApiLevel() } returns API_26
        every { ProcStatAccessibilityProvider.isAccessible() } returns false
        every { shellCpuInfoSource.getCpuInfoDto() } returns CpuInfoDto.INVALID

        // Call 1: warm-up (INVALID, not cached as UNSUPPORTED yet).
        // Call 2: INVALID again → platformSupported=false cached.
        // Call 3: must short-circuit without touching any source.
        classToTest.getInfo()
        classToTest.getInfo()
        val thirdResult = classToTest.getInfo()

        assertEquals(CpuInfo.UNSUPPORTED, thirdResult)
        verify(exactly = 0) { procCpuInfoSource.getCpuInfoDto() }
        verify(exactly = 2) { shellCpuInfoSource.getCpuInfoDto() }
        verify(exactly = 2) { ApiLevelProvider.getApiLevel() }
        verify(exactly = 2) { ProcStatAccessibilityProvider.isAccessible() }
    }

    @Test
    fun getInfo_should_NOT_switch_to_UNSUPPORTED_when_shell_fallback_succeeds_with_INVALID_mapper_output() {
        every { ApiLevelProvider.getApiLevel() } returns API_26
        every { ProcStatAccessibilityProvider.isAccessible() } returns false
        every { shellCpuInfoSource.getCpuInfoDto() } returns mockk<CpuInfoDto>(relaxed = true)
        every { cpuInfoMapper.map(any()) } returns CpuInfo.INVALID

        val first = classToTest.getInfo()
        val second = classToTest.getInfo()

        assertEquals(CpuInfo.INVALID, first)
        assertEquals(CpuInfo.INVALID, second)
        // Shell source consulted on every call — cache NOT set to false (Pitfall 6 — self-correcting).
        verify(exactly = 2) { shellCpuInfoSource.getCpuInfoDto() }
    }
}

private const val API_26 = 26
