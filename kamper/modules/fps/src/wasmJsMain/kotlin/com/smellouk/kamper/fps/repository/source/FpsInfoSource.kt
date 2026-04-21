package com.smellouk.kamper.fps.repository.source

import com.smellouk.kamper.fps.repository.FpsInfoDto

internal class FpsInfoSource {
    init {
        JsFpsTimer.setFrameListener { }
    }

    fun getFpsInfoDto(): FpsInfoDto {
        val count = JsFpsTimer.getFrameCount()
        if (count == 0) return FpsInfoDto.INVALID
        val startMs = JsFpsTimer.getStartMs()
        val lastMs = JsFpsTimer.getLastMs()
        JsFpsTimer.resetState()
        return FpsInfoDto(
            currentFrameCount = count,
            startFrameTimeInSeconds = startMs / 1000.0,
            currentFrameTimeInSeconds = lastMs / 1000.0
        )
    }
}
