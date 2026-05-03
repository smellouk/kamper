package com.smellouk.konitor.fps.repository.source

import com.smellouk.konitor.api.nanosToSeconds
import com.smellouk.konitor.fps.repository.FpsInfoDto
import kotlin.concurrent.Volatile

internal class FpsInfoSource {
    @Volatile private var currentFrameCount: Int = 0
    @Volatile private var startFrameTimeNanos: Long = 0
    @Volatile private var currentFrameTimeNanos: Long = 0

    internal val frameListener: (Long) -> Unit = { frameTimeNanos ->
        currentFrameCount++
        if (startFrameTimeNanos == 0L) {
            startFrameTimeNanos = frameTimeNanos
        }
        currentFrameTimeNanos = frameTimeNanos
    }

    init {
        IosFpsTimer.addFrameListener(frameListener)
    }

    fun getFpsInfoDto(): FpsInfoDto = if (currentFrameCount == 0) {
        FpsInfoDto.INVALID
    } else {
        FpsInfoDto(
            currentFrameCount = currentFrameCount,
            startFrameTimeInSeconds = startFrameTimeNanos.nanosToSeconds(),
            currentFrameTimeInSeconds = currentFrameTimeNanos.nanosToSeconds()
        ).also {
            startFrameTimeNanos = 0
            currentFrameTimeNanos = 0
            currentFrameCount = 0
        }
    }
}
