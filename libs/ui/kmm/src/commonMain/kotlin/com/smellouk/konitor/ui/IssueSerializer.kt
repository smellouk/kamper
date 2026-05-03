package com.smellouk.konitor.ui

import com.smellouk.konitor.issues.Issue
import com.smellouk.konitor.issues.IssueType
import com.smellouk.konitor.issues.Severity

private const val SEP = '\u001F' // Unit separator — never appears in text

internal fun Issue.serialize(): String = buildString {
    append(id.pctEncode()).append(SEP)
    append(type.name).append(SEP)
    append(severity.name).append(SEP)
    append(message.pctEncode()).append(SEP)
    append(timestampMs).append(SEP)
    append(durationMs ?: "").append(SEP)
    append((stackTrace ?: "").pctEncode()).append(SEP)
    append((threadName ?: "").pctEncode()).append(SEP)
    append(details.entries.joinToString("\u001E") { "${it.key.pctEncode()}=${it.value.pctEncode()}" })
}

internal fun String.deserializeIssue(): Issue? = runCatching {
    val p = split(SEP)
    if (p.size < 9) return null
    Issue(
        id          = p[0].pctDecode(),
        type        = IssueType.valueOf(p[1]),
        severity    = Severity.valueOf(p[2]),
        message     = p[3].pctDecode(),
        timestampMs = p[4].toLong(),
        durationMs  = p[5].toLongOrNull(),
        stackTrace  = p[6].pctDecode().ifEmpty { null },
        threadName  = p[7].pctDecode().ifEmpty { null },
        details     = if (p[8].isEmpty()) emptyMap()
                      else p[8].split('\u001E').filter { '=' in it }
                               .associate { e -> e.substringBefore('=').pctDecode() to e.substringAfter('=').pctDecode() }
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
