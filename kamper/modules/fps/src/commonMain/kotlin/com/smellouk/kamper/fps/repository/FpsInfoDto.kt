package com.smellouk.kamper.fps.repository

internal class FpsInfoDto(
    val currentFrameCount: Int,
    val startFrameTimeInSeconds: Double,
    val currentFrameTimeInSeconds: Double
) {
    companion object {
        val INVALID = FpsInfoDto(
            -1,
            -1.0,
            -1.0
        )
    }
}
