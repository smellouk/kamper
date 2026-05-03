package com.smellouk.konitor.ui

private const val SEP = '' // Unit separator — field delimiter within a record

internal fun EventEntry.serialize(): String = buildString {
    append(name.pctEncode()).append(SEP)
    append(durationMs ?: "").append(SEP)
    append(receivedAtMs)
}

internal fun String.deserializeEventEntry(): EventEntry? = runCatching {
    val p = split(SEP)
    if (p.size < 3) return null
    EventEntry(
        name         = p[0].pctDecode(),
        durationMs   = p[1].toLongOrNull(),
        receivedAtMs = p[2].toLong()
    )
}.getOrNull()

private fun String.pctEncode(): String = buildString {
    for (c in this@pctEncode) {
        val code = c.code
        if (code < 0x20 || c == '%') append('%').append(code.toString(16).padStart(2, '0').uppercase())
        else append(c)
    }
}

private fun String.pctDecode(): String {
    if ('%' !in this) return this
    val sb = StringBuilder(length)
    var i = 0
    while (i < length) {
        if (this[i] == '%' && i + 2 < length) {
            val code = substring(i + 1, i + 3).toIntOrNull(16)
            if (code != null) { sb.append(code.toChar()); i += 3; continue }
        }
        sb.append(this[i++])
    }
    return sb.toString()
}
