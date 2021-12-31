package com.smellouk.kamper.network.repository

import com.smellouk.kamper.network.NetworkInfo
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("IllegalIdentifier")
class NetworkInfoMapperTest {
    private val classToTest = NetworkInfoMapper()

    @Test
    fun `map dto should return invalid network info when dto is invalid`() {
        val networkInfo = classToTest.map(NetworkInfoDto.INVALID)

        assertEquals(NetworkInfo.INVALID, networkInfo)
    }

    @Test
    fun `map dto should return not supported when dto not supported`() {
        val networkInfo = classToTest.map(NetworkInfoDto.NOT_SUPPORTED)

        assertEquals(NetworkInfo.NOT_SUPPORTED, networkInfo)
    }

    @Test
    fun `map dto should return valid network info`() {
        val networkInfo = classToTest.map(
            NetworkInfoDto(
                rxTotalInBytes = TRAFFIC_IN_BYTES,
                txTotalInBytes = TRAFFIC_IN_BYTES,
                rxUidInBytes = TRAFFIC_IN_BYTES,
                txUidInBytes = TRAFFIC_IN_BYTES
            )
        )

        assertEquals(EXPECTED_NETWORK_INFO, networkInfo)
    }
}

private const val TRAFFIC_IN_BYTES = 1024 * 1024L
private val EXPECTED_NETWORK_INFO = NetworkInfo(
    rxSystemTotalInMb = 1F,
    txSystemTotalInMb = 1F,
    rxAppInMb = 1F,
    txAppInMb = 1F
)
