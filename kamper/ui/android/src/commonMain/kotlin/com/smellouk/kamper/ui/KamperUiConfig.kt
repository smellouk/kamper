package com.smellouk.kamper.ui

data class KamperUiConfig(
    var isEnabled: Boolean = true,
    var intervalInMs: Long = 1_000L,
    var position: ChipPosition = ChipPosition.CENTER_END
)

enum class ChipPosition {
    TOP_START, TOP_END,
    CENTER_START, CENTER_END,
    BOTTOM_START, BOTTOM_END
}
