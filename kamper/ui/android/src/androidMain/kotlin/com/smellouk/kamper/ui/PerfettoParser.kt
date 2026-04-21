package com.smellouk.kamper.ui

import java.io.File

internal object PerfettoParser {

    private class Reader(val data: ByteArray, var pos: Int, val limit: Int) {
        fun hasMore() = pos < limit

        fun readVarint(): Long {
            var result = 0L
            var shift = 0
            while (pos < limit) {
                val b = data[pos++].toLong() and 0xFF
                result = result or ((b and 0x7F) shl shift)
                if (b and 0x80L == 0L) break
                shift += 7
            }
            return result
        }

        fun skipField(wireType: Int) {
            when (wireType) {
                0 -> readVarint()
                1 -> pos = minOf(pos + 8, limit)
                2 -> pos = minOf(pos + readVarint().toInt().coerceAtLeast(0), limit)
                5 -> pos = minOf(pos + 4, limit)
            }
        }

        fun subReader(): Reader {
            val len = readVarint().toInt().coerceAtLeast(0)
            val end = minOf(pos + len, limit)
            val sub = Reader(data, pos, end)
            pos = end
            return sub
        }
    }

    private data class Marker(val ts: Long, val str: String)

    private fun parseTrace(r: Reader): List<Marker> {
        val markers = mutableListOf<Marker>()
        while (r.hasMore()) {
            val tag = r.readVarint()
            val field = (tag shr 3).toInt()
            val wire = (tag and 7).toInt()
            if (wire == 2 && field == 1) parsePacket(r.subReader(), markers)
            else r.skipField(wire)
        }
        return markers
    }

    private fun parsePacket(r: Reader, markers: MutableList<Marker>) {
        while (r.hasMore()) {
            val tag = r.readVarint()
            val field = (tag shr 3).toInt()
            val wire = (tag and 7).toInt()
            if (wire == 2 && field == 12) parseBundle(r.subReader(), markers)
            else r.skipField(wire)
        }
    }

    private fun parseBundle(r: Reader, markers: MutableList<Marker>) {
        while (r.hasMore()) {
            val tag = r.readVarint()
            val field = (tag shr 3).toInt()
            val wire = (tag and 7).toInt()
            if (wire == 2 && field == 1) parseEvent(r.subReader(), markers)
            else r.skipField(wire)
        }
    }

    private fun parseEvent(r: Reader, markers: MutableList<Marker>) {
        var ts = 0L
        var buf: String? = null
        while (r.hasMore()) {
            val tag = r.readVarint()
            val field = (tag shr 3).toInt()
            val wire = (tag and 7).toInt()
            when {
                wire == 0 && field == 1 -> ts = r.readVarint()
                wire == 2 && field == 5 -> buf = parsePrint(r.subReader())
                else -> r.skipField(wire)
            }
        }
        if (ts > 0 && buf != null) {
            val trimmed = buf.trimEnd('\n', '\u0000')
            if (trimmed.startsWith("B|") || trimmed.startsWith("E|")) {
                markers.add(Marker(ts, trimmed))
            }
        }
    }

    private fun parsePrint(r: Reader): String? {
        while (r.hasMore()) {
            val tag = r.readVarint()
            val field = (tag shr 3).toInt()
            val wire = (tag and 7).toInt()
            if (wire == 2 && field == 2) {
                val len = r.readVarint().toInt().coerceAtLeast(0)
                val actual = minOf(len, r.limit - r.pos)
                val s = String(r.data, r.pos, actual, Charsets.UTF_8)
                r.pos += len
                return s
            } else {
                r.skipField(wire)
            }
        }
        return null
    }

    fun parse(file: File): List<TraceSpan> {
        if (!file.exists() || file.length() == 0L) return emptyList()
        return try {
            val bytes = file.readBytes()
            val markers = parseTrace(Reader(bytes, 0, bytes.size)).sortedBy { it.ts }
            buildSpans(markers)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun buildSpans(markers: List<Marker>): List<TraceSpan> {
        val spans = mutableListOf<TraceSpan>()
        val stack = ArrayDeque<Triple<String, Long, Int>>()
        val startNs = markers.firstOrNull()?.ts ?: return emptyList()

        for (m in markers) {
            val parts = m.str.split("|", limit = 3)
            when (parts[0]) {
                "B" -> if (parts.size >= 3) {
                    stack.addLast(Triple(parts[2], m.ts, stack.size))
                }
                "E" -> if (stack.isNotEmpty()) {
                    val (name, startTs, depth) = stack.removeLast()
                    val startMs = (startTs - startNs) / 1_000_000L
                    val durMs = ((m.ts - startTs) / 1_000_000L).coerceAtLeast(1L)
                    spans.add(TraceSpan(name, startMs, durMs, depth))
                }
            }
        }
        return spans
    }
}
