package com.smellouk.kamper.ui.compose

internal fun Float.formatDp(decimals: Int): String {
    val negative = this < 0f
    val abs = if (negative) -this else this
    val whole = abs.toLong()
    val frac = when (decimals) {
        0 -> ""
        1 -> ".${((abs - whole) * 10).toLong()}"
        2 -> ".${((abs - whole) * 100).toLong().toString().padStart(2, '0')}"
        else -> ""
    }
    return if (negative) "-$whole$frac" else "$whole$frac"
}
