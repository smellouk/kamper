package com.smellouk.konitor.fps.repository

import com.smellouk.konitor.fps.FpsInfo
import com.smellouk.konitor.fps.repository.source.FpsInfoSource

internal class FpsInfoRepositoryImpl(
    private val fpsSource: FpsInfoSource,
    private val fpsInfoMapper: FpsInfoMapper
) : FpsInfoRepository {
    override fun getInfo(): FpsInfo = fpsInfoMapper.map(fpsSource.getFpsInfoDto())
}
