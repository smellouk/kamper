package com.smellouk.kamper.network.repository.source

import android.net.TrafficStats
import android.os.Process
import com.smellouk.kamper.network.repository.NetworkInfoDto
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class NetworkInfoSourceTest {
    private val classToTest = NetworkInfoSource(mockk(relaxed = true))

    @Before
    fun setup() {
        mockkStatic(TrafficStats::class)

        mockkStatic(Process::class)
        every { Process.myUid() } returns MY_UUID
    }

    @After
    fun tearDown() {
        unmockkStatic(TrafficStats::class)
        unmockkStatic(Process::class)
    }

    @Test
    fun `getNetworkInfoDto should return invalid dto when cache is not initialized`() {
        mockTraffic(ONE_MEGABYTE_TRAFFIC_IN_BYTES)

        val dto = classToTest.getNetworkInfoDto()

        assertEquals(NetworkInfoDto.INVALID, dto)
    }

    @Test
    fun `getNetworkInfoDto should return not supported dto when device does not supports TrafficStats`() {
        mockTraffic(NOT_SUPPORTED)

        val dto = classToTest.getNetworkInfoDto()

        assertEquals(NetworkInfoDto.NOT_SUPPORTED, dto)
    }

    @Test
    fun `getNetworkInfoDto should return valid dto when cache is available`() {
        classToTest.cachedDto = CACHED_DTO
        mockTraffic(ONE_MEGABYTE_TRAFFIC_IN_BYTES * 3)

        val dto = classToTest.getNetworkInfoDto()

        assertEquals(EXPECTED_DTO, dto)
    }

    private fun mockTraffic(trafficInBytes: Long) {
        every { TrafficStats.getTotalRxBytes() } returns trafficInBytes
        every { TrafficStats.getTotalTxBytes() } returns trafficInBytes
        every { TrafficStats.getUidRxBytes(MY_UUID) } returns trafficInBytes
        every { TrafficStats.getUidTxBytes(MY_UUID) } returns trafficInBytes
    }
}

private const val MY_UUID = 128797234
private const val ONE_MEGABYTE_TRAFFIC_IN_BYTES = 1024 * 1024L
private const val NOT_SUPPORTED = -1L

private val CACHED_DTO = NetworkInfoDto(
    rxTotalInBytes = ONE_MEGABYTE_TRAFFIC_IN_BYTES,
    txTotalInBytes = ONE_MEGABYTE_TRAFFIC_IN_BYTES,
    rxUidInBytes = ONE_MEGABYTE_TRAFFIC_IN_BYTES,
    txUidInBytes = ONE_MEGABYTE_TRAFFIC_IN_BYTES
)
private val EXPECTED_DTO = NetworkInfoDto(
    rxTotalInBytes = ONE_MEGABYTE_TRAFFIC_IN_BYTES * 2,
    txTotalInBytes = ONE_MEGABYTE_TRAFFIC_IN_BYTES * 2,
    rxUidInBytes = ONE_MEGABYTE_TRAFFIC_IN_BYTES * 2,
    txUidInBytes = ONE_MEGABYTE_TRAFFIC_IN_BYTES * 2
)
