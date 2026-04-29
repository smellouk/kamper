package com.smellouk.kamper.ui

internal data class RecordedSample(
    val timestampNs: Long,
    val trackId: Int,
    val value: Double
)

internal object Tracks {
    const val CPU     = 1
    const val FPS     = 2
    const val MEMORY  = 3
    const val NETWORK = 4
    const val JANK    = 5
    const val GC      = 6
    const val THERMAL = 7

    val ALL = listOf(
        CPU     to "CPU %",
        FPS     to "FPS",
        MEMORY  to "Memory MB",
        NETWORK to "Network \u2193 MB/s",
        JANK    to "Jank dropped frames",
        GC      to "GC count delta",
        THERMAL to "Thermal state"
    )
}
