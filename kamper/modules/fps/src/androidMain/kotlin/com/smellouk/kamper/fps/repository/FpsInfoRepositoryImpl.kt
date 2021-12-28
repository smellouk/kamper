package com.smellouk.kamper.fps.repository

import com.smellouk.kamper.fps.FpsInfo
import com.smellouk.kamper.fps.repository.source.FpsInfoSource

internal class FpsInfoRepositoryImpl(
    private val fpsSource: FpsInfoSource,
    private val fpsInfoMapper: FpsInfoMapper
) : FpsInfoRepository {
    override fun getInfo(): FpsInfo =
        fpsInfoMapper.map(fpsSource.getFpsInfoRaw())
}
