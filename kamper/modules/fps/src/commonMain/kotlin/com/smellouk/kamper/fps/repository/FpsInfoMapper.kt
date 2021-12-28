package com.smellouk.kamper.fps.repository

import com.smellouk.kamper.fps.FpsInfo
import kotlin.math.roundToInt

internal class FpsInfoMapper {
    fun map(dto: FpsInfoDto): FpsInfo = with(dto) {
        if (this == FpsInfoDto.INVALID) {
            return FpsInfo.INVALID
        }
        if (currentFrameCount < 1 || currentFrameTimeInSeconds < startFrameTimeInSeconds) {
            return FpsInfo.INVALID
        }
        val fps = currentFrameCount / (currentFrameTimeInSeconds - startFrameTimeInSeconds)
        return FpsInfo(fps.roundToInt() - 1)
    }
}
