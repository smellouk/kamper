package com.smellouk.konitor.ui

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
    const val GPU     = 8
    const val EVENTS  = 9   // Phase 24 D-19: named event track, NOT in ALL (different descriptor)
    const val ISSUES  = 10  // Named event track for issues, NOT in ALL (different descriptor)

    val ALL = listOf(
        CPU     to "CPU %",
        FPS     to "FPS",
        MEMORY  to "Memory MB",
        NETWORK to "Network \u2193 MB/s",
        JANK    to "Jank dropped frames",
        GC      to "GC count delta",
        THERMAL to "Thermal state",
        GPU     to "GPU %"
    )
}
