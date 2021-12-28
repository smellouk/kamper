package com.smellouk.kamper.fps.repository.source

import com.smellouk.kamper.api.nanosToSeconds
import com.smellouk.kamper.fps.repository.FpsInfoDto

internal class FpsInfoSource(choreographer: FpsChoreographer) {
    private var currentFrameCount: Int = 0
    private var startFrameTimeNanos: Long = 0
    private var currentFrameTimeNanos: Long = 0

    init {
        choreographer.setDoFrameListener { frameTimeNanos ->
            currentFrameCount++
            if (startFrameTimeNanos == 0L) {
                startFrameTimeNanos = frameTimeNanos
            }
            currentFrameTimeNanos = frameTimeNanos
        }
    }

    fun getFpsInfoRaw(): FpsInfoDto {
        val fpsInfo = FpsInfoDto(
            currentFrameCount = currentFrameCount,
            startFrameTimeInSeconds = startFrameTimeNanos.nanosToSeconds(),
            currentFrameTimeInSeconds = currentFrameTimeNanos.nanosToSeconds()
        )
        startFrameTimeNanos = 0
        currentFrameTimeNanos = 0
        currentFrameCount = 0
        return fpsInfo
    }
}
