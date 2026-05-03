/**
 * Real assertions for D-19/D-21 Perfetto event encoding.
 * Plan 08 fills the 5 stubs created in Plan 01.
 *
 * Test strategy: call PerfettoExporter.export(samples, events) and walk the resulting
 * byte array with a minimal ProtoWalker that accumulates top-level Trace.packet bytes,
 * then inspects each packet's field values. This mirrors the production ProtoWriter API.
 */
@file:Suppress("MagicNumber")
package com.smellouk.kamper.ui

import com.smellouk.kamper.EventRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@Suppress("IllegalIdentifier")
class PerfettoExporterEventTest {

    // ── Proto varint / wire-format walking helpers ────────────────────────────

    /**
     * Read a single unsigned varint from [buf] starting at [pos].
     * Returns Pair(value, nextPos).
     */
    private fun readVarint(buf: ByteArray, pos: Int): Pair<Long, Int> {
        var value = 0L
        var shift = 0
        var cursor = pos
        while (true) {
            val b = buf[cursor++].toInt() and 0xFF
            value = value or ((b and 0x7F).toLong() shl shift)
            if (b and 0x80 == 0) break
            shift += 7
        }
        return Pair(value, cursor)
    }

    /**
     * Recursively walk a proto message (length-delimited bytes from [start] to
     * [start]+[len]) and collect every (fieldNumber, value) pair encountered.
     * Value is Long for varint/fixed types; ByteArray for length-delimited (string/message).
     */
    private fun walkMessage(buf: ByteArray, start: Int, len: Int): List<Pair<Int, Any>> {
        val fields = mutableListOf<Pair<Int, Any>>()
        var pos = start
        val end = start + len
        while (pos < end) {
            val (tag, p1) = readVarint(buf, pos)
            pos = p1
            val fieldNum = (tag ushr 3).toInt()
            val wireType = (tag and 7L).toInt()
            when (wireType) {
                0 -> { // varint
                    val (v, p2) = readVarint(buf, pos)
                    pos = p2
                    fields.add(Pair(fieldNum, v))
                }
                1 -> { // 64-bit fixed
                    pos += 8
                    fields.add(Pair(fieldNum, 0L)) // not needed in tests
                }
                2 -> { // length-delimited
                    val (msgLen, p2) = readVarint(buf, pos)
                    pos = p2
                    val bytes = buf.copyOfRange(pos, pos + msgLen.toInt())
                    pos += msgLen.toInt()
                    fields.add(Pair(fieldNum, bytes))
                }
                5 -> { // 32-bit fixed
                    pos += 4
                    fields.add(Pair(fieldNum, 0L))
                }
                else -> break
            }
        }
        return fields
    }

    /** Walk top-level Trace bytes and return all Trace.packet byte arrays (field 1). */
    private fun extractPackets(trace: ByteArray): List<ByteArray> =
        walkMessage(trace, 0, trace.size)
            .filter { (field, _) -> field == 1 }
            .map { (_, v) -> v as ByteArray }

    /**
     * Parse a packet's fields and walk sub-messages recursively for a single
     * packet (Trace.packet). Returns a flat list of FieldPath -> value where
     * path is encoded as "field.subfield".
     *
     * For our tests we only need shallow inspection; walkMessage is sufficient.
     */
    private fun packetFields(packet: ByteArray): List<Pair<Int, Any>> =
        walkMessage(packet, 0, packet.size)

    private fun subFields(data: ByteArray): List<Pair<Int, Any>> =
        walkMessage(data, 0, data.size)

    // ── Test fixtures ─────────────────────────────────────────────────────────

    private val emptySamples: List<RecordedSample> = emptyList()

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    fun exportEmitsEventsTrackDescriptorWithoutCounter() {
        // covers D-18, D-19
        val events = listOf(EventRecord(timestampNs = 1_000L, name = "tap", durationNs = null))
        val bytes = PerfettoExporter.export(emptySamples, events)
        val packets = extractPackets(bytes)

        // Find the packet that has a track_descriptor (field 60) with uuid=9 (Tracks.EVENTS)
        val descriptorPackets = packets.filter { pkt ->
            packetFields(pkt).any { (f, v) ->
                f == 60 && v is ByteArray &&
                    subFields(v).any { (sf, sv) -> sf == 1 && sv == Tracks.EVENTS.toLong() }
            }
        }
        assertEquals(1, descriptorPackets.size, "Expected exactly one EVENTS track descriptor packet")

        // Verify name = "Events"
        val descriptorBytes = packetFields(descriptorPackets[0])
            .first { (f, v) -> f == 60 && v is ByteArray }.second as ByteArray
        val descriptorFields = subFields(descriptorBytes)

        // uuid == 9
        assertTrue(descriptorFields.any { (f, v) -> f == 1 && v == Tracks.EVENTS.toLong() })
        // name == "Events"
        assertTrue(
            descriptorFields.any { (f, v) ->
                f == 2 && v is ByteArray && String(v) == "Events"
            }
        )
        // NO counter{} sub-message (field 8 must NOT be present) — D-18 / Pitfall 3
        assertFalse(descriptorFields.any { (f, _) -> f == 8 })
    }

    @Test
    fun exportEmitsTypeInstantPacketForInstantEvent() {
        // covers D-16, D-21
        val events = listOf(EventRecord(timestampNs = 500_000L, name = "purchase", durationNs = null))
        val bytes = PerfettoExporter.export(emptySamples, events)
        val packets = extractPackets(bytes)

        // Find packet with track_event (field 11) that has type=3 (TYPE_INSTANT)
        val eventPackets = packets.filter { pkt ->
            packetFields(pkt).any { (f, v) ->
                f == 11 && v is ByteArray && subFields(v).any { (sf, sv) -> sf == 9 && sv == 3L }
            }
        }
        assertEquals(1, eventPackets.size, "Expected exactly one TYPE_INSTANT packet")

        val pktFields = packetFields(eventPackets[0])
        // timestamp matches
        assertTrue(pktFields.any { (f, v) -> f == 8 && v == 500_000L })

        val trackEventBytes = pktFields.first { (f, v) -> f == 11 && v is ByteArray }.second as ByteArray
        val teFields = subFields(trackEventBytes)
        // track_uuid = 9 (Tracks.EVENTS)
        assertTrue(teFields.any { (f, v) -> f == 11 && v == Tracks.EVENTS.toLong() })
        // type = 3
        assertTrue(teFields.any { (f, v) -> f == 9 && v == 3L })
        // name field 23 = "purchase"
        assertTrue(
            teFields.any { (f, v) -> f == 23 && v is ByteArray && String(v) == "purchase" }
        )
    }

    @Test
    fun exportEmitsTypeSliceBeginAndEndForDurationEvent() {
        // covers D-17, D-21
        val startNs = 1_000_000L
        val durationNs = 500_000L
        val events = listOf(EventRecord(timestampNs = startNs, name = "video", durationNs = durationNs))
        val bytes = PerfettoExporter.export(emptySamples, events)
        val packets = extractPackets(bytes)

        // Find TYPE_SLICE_BEGIN (type=1) packet
        val beginPackets = packets.filter { pkt ->
            packetFields(pkt).any { (f, v) ->
                f == 11 && v is ByteArray && subFields(v).any { (sf, sv) -> sf == 9 && sv == 1L }
            }
        }
        assertEquals(1, beginPackets.size, "Expected exactly one TYPE_SLICE_BEGIN packet")

        val beginPktFields = packetFields(beginPackets[0])
        // timestamp == startNs
        assertTrue(beginPktFields.any { (f, v) -> f == 8 && v == startNs })
        // name == "video" on BEGIN
        val beginTeBytes =
            beginPktFields.first { (f, v) -> f == 11 && v is ByteArray }.second as ByteArray
        val beginTeFields = subFields(beginTeBytes)
        assertTrue(beginTeFields.any { (f, v) -> f == 23 && v is ByteArray && String(v) == "video" })

        // Find TYPE_SLICE_END (type=2) packet
        val endPackets = packets.filter { pkt ->
            packetFields(pkt).any { (f, v) ->
                f == 11 && v is ByteArray && subFields(v).any { (sf, sv) -> sf == 9 && sv == 2L }
            }
        }
        assertEquals(1, endPackets.size, "Expected exactly one TYPE_SLICE_END packet")

        val endPktFields = packetFields(endPackets[0])
        // timestamp == startNs + durationNs
        assertTrue(endPktFields.any { (f, v) -> f == 8 && v == startNs + durationNs })
    }

    @Test
    fun exportEmitsTrackEventNameOnBeginAndInstantOnly() {
        // covers D-21, no name on END (Pitfall 4)
        val events = listOf(
            EventRecord(timestampNs = 100L, name = "instant_ev", durationNs = null),
            EventRecord(timestampNs = 200L, name = "slice_ev", durationNs = 300L)
        )
        val bytes = PerfettoExporter.export(emptySamples, events)
        val packets = extractPackets(bytes)

        // Collect all track_event sub-messages
        val trackEvents = packets.flatMap { pkt ->
            packetFields(pkt)
                .filter { (f, v) -> f == 11 && v is ByteArray }
                .map { (_, v) -> subFields(v as ByteArray) }
        }

        // TYPE_INSTANT (3): must have name (field 23)
        val instantTe = trackEvents.first { te -> te.any { (f, v) -> f == 9 && v == 3L } }
        assertTrue(instantTe.any { (f, v) -> f == 23 && v is ByteArray && String(v) == "instant_ev" })

        // TYPE_SLICE_BEGIN (1): must have name (field 23)
        val beginTe = trackEvents.first { te -> te.any { (f, v) -> f == 9 && v == 1L } }
        assertTrue(beginTe.any { (f, v) -> f == 23 && v is ByteArray && String(v) == "slice_ev" })

        // TYPE_SLICE_END (2): must NOT have name (field 23) — Pitfall 4
        val endTe = trackEvents.first { te -> te.any { (f, v) -> f == 9 && v == 2L } }
        assertFalse(endTe.any { (f, _) -> f == 23 })
    }

    @Test
    fun exportWithEmptyEventsListProducesNoEventsTrack() {
        // covers D-20 backward compatibility
        val bytes = PerfettoExporter.export(emptySamples, emptyList())
        val str = String(bytes)

        // "Events" string must NOT appear in the trace bytes
        assertFalse(str.contains("Events"), "No EVENTS track descriptor expected")

        // No track_descriptor with uuid=9 (Tracks.EVENTS)
        val packets = extractPackets(bytes)
        val eventsDescriptors = packets.filter { pkt ->
            packetFields(pkt).any { (f, v) ->
                f == 60 && v is ByteArray &&
                    subFields(v).any { (sf, sv) -> sf == 1 && sv == Tracks.EVENTS.toLong() }
            }
        }
        assertTrue(eventsDescriptors.isEmpty(), "No EVENTS track_descriptor expected for empty events")
    }
}
