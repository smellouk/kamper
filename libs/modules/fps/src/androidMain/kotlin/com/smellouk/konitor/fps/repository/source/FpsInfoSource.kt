package com.smellouk.konitor.fps.repository.source

import com.smellouk.konitor.api.nanosToSeconds
import com.smellouk.konitor.fps.repository.FpsInfoDto

internal class FpsInfoSource(choreographer: FpsChoreographer) {
    @Volatile private var currentFrameCount: Int = 0
    @Volatile private var startFrameTimeNanos: Long = 0
    @Volatile private var currentFrameTimeNanos: Long = 0

    // Visible only for testing
    internal val frameListener: FpsChoreographerFrameListener = { frameTimeNanos ->
        currentFrameCount++
        if (startFrameTimeNanos == 0L) {
            startFrameTimeNanos = frameTimeNanos
        }
        currentFrameTimeNanos = frameTimeNanos
    }

    init {
        choreographer.setFrameListener(frameListener)
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
