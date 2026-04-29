package com.smellouk.kamper.network.repository

import com.smellouk.kamper.network.NetworkInfo
import com.smellouk.kamper.network.repository.source.JvmNetworkInfoSource

internal class NetworkInfoRepositoryImpl(
    private val networkInfoSource: JvmNetworkInfoSource,
    private val networkInfoMapper: NetworkInfoMapper
) : NetworkInfoRepository {
    override fun getInfo(): NetworkInfo = networkInfoMapper.map(networkInfoSource.getNetworkInfoDto())
}
