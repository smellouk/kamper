package com.smellouk.konitor.fps.repository.source

import com.smellouk.konitor.fps.repository.FpsInfoDto

internal class FpsInfoSource {
    private var currentFrameCount = 0
    private var startFrameTimeMs = 0.0
    private var currentFrameTimeMs = 0.0

    internal val frameListener: (Double) -> Unit = { timestampMs ->
        currentFrameCount++
        if (startFrameTimeMs == 0.0) {
            startFrameTimeMs = timestampMs
        }
        currentFrameTimeMs = timestampMs
    }

    init {
        JsFpsTimer.setFrameListener(frameListener)
    }

    fun getFpsInfoDto(): FpsInfoDto = if (currentFrameCount == 0) {
        FpsInfoDto.INVALID
    } else {
        FpsInfoDto(
            currentFrameCount = currentFrameCount,
            startFrameTimeInSeconds = startFrameTimeMs / 1000.0,
            currentFrameTimeInSeconds = currentFrameTimeMs / 1000.0
        ).also {
            startFrameTimeMs = 0.0
            currentFrameTimeMs = 0.0
            currentFrameCount = 0
        }
    }
}
