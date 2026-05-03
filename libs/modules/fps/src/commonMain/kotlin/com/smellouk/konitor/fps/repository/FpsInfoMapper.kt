package com.smellouk.konitor.fps.repository

import com.smellouk.konitor.fps.FpsInfo
import kotlin.math.roundToInt

internal class FpsInfoMapper {
    fun map(dto: FpsInfoDto): FpsInfo = with(dto) {
        if (this == FpsInfoDto.INVALID) {
            return FpsInfo.INVALID
        }

        if (currentFrameCount < 2) return FpsInfo.INVALID
        val duration = currentFrameTimeInSeconds - startFrameTimeInSeconds
        if (duration <= 0.0) return FpsInfo.INVALID
        val fps = (currentFrameCount - 1) / duration
        return FpsInfo(fps.roundToInt())
    }
}
