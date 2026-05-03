package com.smellouk.konitor.ui

data class KonitorUiConfig(
    var isEnabled: Boolean = true,
    var intervalInMs: Long = 1_000L,
    var position: ChipPosition = ChipPosition.CENTER_END,
    var maxRecordingSamples: Int = 4_200
)

enum class ChipPosition {
    TOP_START, TOP_END,
    CENTER_START, CENTER_END,
    BOTTOM_START, BOTTOM_END
}
