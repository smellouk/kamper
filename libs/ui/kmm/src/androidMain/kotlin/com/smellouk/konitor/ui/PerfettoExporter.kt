package com.smellouk.konitor.ui

import com.smellouk.konitor.EventRecord
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.zip.GZIPOutputStream

// ── Binary protobuf writer (streaming) ────────────────────────────────────────
//
// Writes proto bytes directly to the supplied OutputStream. For length-delimited
// sub-messages, the inner block writes to a ByteArrayOutputStream first so the
// length prefix can be emitted (protobuf wire-format requires the byte count
// before the message body). Inner blocks in Perfetto trace are small (< 100B);
// the memory savings come from streaming the OUTER writer (sample packets).

internal class ProtoWriter(private val out: OutputStream) {

    private fun writeByte(b: Int) { out.write(b and 0xFF) }

    fun writeVarint(value: Long) {
        var v = value
        while (v and 0x7FL.inv() != 0L) {
            writeByte(((v and 0x7F) or 0x80).toInt())
            v = v ushr 7
        }
        writeByte(v.toInt())
    }

    private fun writeFixed64(value: Long) {
        var v = value
        repeat(8) {
            writeByte((v and 0xFF).toInt())
            v = v ushr 8
        }
    }

    private fun writeTag(field: Int, wireType: Int) {
        writeVarint(((field.toLong() shl 3) or wireType.toLong()))
    }

    fun uint64(field: Int, value: Long)   { writeTag(field, 0); writeVarint(value) }
    fun uint32(field: Int, value: Int)    { writeTag(field, 0); writeVarint(value.toLong()) }
    fun double(field: Int, value: Double) { writeTag(field, 1); writeFixed64(value.toBits()) }

    fun string(field: Int, value: String) {
        val bytes = value.encodeToByteArray()
        writeTag(field, 2)
        writeVarint(bytes.size.toLong())
        out.write(bytes)
    }

    fun message(field: Int, block: ProtoWriter.() -> Unit) {
        // Length-delimited fields require the inner byte count up front, so the
        // inner block is buffered to a ByteArrayOutputStream. Inner messages in
        // Perfetto trace are small (track_descriptor ≈ name+ids ≈ tens of bytes;
        // track_event ≈ 30 bytes). The dominant allocation we eliminated was the
        // outer accumulator over ALL sample packets.
        val inner = ByteArrayOutputStream()
        ProtoWriter(inner).block()
        val bytes = inner.toByteArray()
        writeTag(field, 2)
        writeVarint(bytes.size.toLong())
        out.write(bytes)
    }
}

// ── Perfetto trace encoder ────────────────────────────────────────────────────
//
// Protobuf field references (Perfetto trace.proto / track_event.proto):
//   Trace.packet                       = 1  (repeated message)
//   TracePacket.timestamp              = 8  (uint64, nanoseconds)
//   TracePacket.trusted_packet_seq_id  = 10 (uint32)
//   TracePacket.track_event            = 11 (message)
//   TracePacket.track_descriptor       = 60 (message)
//   TracePacket.timestamp_clock_id     = 58 (uint32, 6=BOOTTIME)
//   TrackDescriptor.uuid               = 1  (uint64)
//   TrackDescriptor.name               = 2  (string)
//   TrackDescriptor.counter            = 8  (empty message → counter track; OMIT for event track)
//   TrackEvent.track_uuid              = 11 (uint64)
//   TrackEvent.type                    = 9  (int32, 4=TYPE_COUNTER)
//   TrackEvent.double_value            = 44 (double — counter_value_field oneof)
//   D-21: TrackEvent.type values: TYPE_SLICE_BEGIN=1, TYPE_SLICE_END=2, TYPE_INSTANT=3
//   D-21: TrackEvent.name             = 23 (string — event name for INSTANT + BEGIN packets only)

internal object PerfettoExporter {

    private const val SEQ_ID = 1
    private const val CLOCK_BOOTTIME = 6

    // D-21: TrackEvent.type values for named event tracks
    private const val TRACK_EVENT_TYPE_SLICE_BEGIN: Int = 1
    private const val TRACK_EVENT_TYPE_SLICE_END: Int = 2
    private const val TRACK_EVENT_TYPE_INSTANT: Int = 3

    // D-21: TrackEvent.name field number (proto field 23)
    private const val TRACK_EVENT_NAME_FIELD: Int = 23

    private const val MS_TO_NS: Long = 1_000_000L

    /**
     * Backward-compatible ByteArray export. Used by tests and any in-memory caller.
     * The [events] and [issues] parameters default to empty for backward compatibility.
     */
    fun export(
        samples: List<RecordedSample>,
        events: List<EventRecord> = emptyList(),
        issues: List<IssueRecord> = emptyList()
    ): ByteArray {
        val baos = ByteArrayOutputStream()
        writeTrace(samples, events, issues, baos)
        return baos.toByteArray()
    }

    /**
     * Streaming gzip export. Writes a complete gzip-compressed Perfetto trace to [out].
     * The caller owns [out] and is responsible for opening/closing it; this method
     * wraps it in a [GZIPOutputStream] internally and finishes the gzip stream before
     * returning (so the gzip footer/checksum is always present).
     * The [events] and [issues] parameters default to empty for backward compatibility.
     */
    fun exportToFile(
        samples: List<RecordedSample>,
        events: List<EventRecord> = emptyList(),
        issues: List<IssueRecord> = emptyList(),
        out: OutputStream
    ) {
        GZIPOutputStream(out.buffered()).use { gzip ->
            writeTrace(samples, events, issues, gzip)
        }
    }

    private fun writeTrace(
        samples: List<RecordedSample>,
        events: List<EventRecord>,
        issues: List<IssueRecord>,
        sink: OutputStream
    ) {
        val root = ProtoWriter(sink)

        // Events track first so it appears at the top in Perfetto UI
        if (events.isNotEmpty()) writeEventPackets(root, events)

        // Issues track below Events
        if (issues.isNotEmpty()) writeIssuePackets(root, issues)

        // Emit one TrackDescriptor per metric (counter) track
        Tracks.ALL.forEach { (trackId, trackName) ->
            root.message(1) {              // Trace.packet
                uint32(10, SEQ_ID)
                message(60) {              // track_descriptor
                    uint64(1, trackId.toLong())
                    string(2, trackName)
                    message(8) {}          // counter {} marks this as a counter track
                }
            }
        }

        // Emit one counter event per recorded sample
        samples.forEach { sample ->
            root.message(1) {              // Trace.packet
                uint64(8, sample.timestampNs)
                uint32(10, SEQ_ID)
                uint32(58, CLOCK_BOOTTIME)
                message(11) {              // track_event
                    uint64(11, sample.trackId.toLong())
                    uint32(9, 4)           // TYPE_COUNTER
                    double(44, sample.value)
                }
            }
        }
    }

    /**
     * Emit the "Events" track descriptor (D-18) and one packet per [EventRecord] (D-16/D-17).
     * Extracted to keep [writeTrace] under the 60-line detekt limit.
     */
    private fun writeEventPackets(root: ProtoWriter, events: List<EventRecord>) {
        // Track descriptor — D-18: NO counter{} sub-message → named event track
        root.message(1) {                               // Trace.packet
            uint32(10, SEQ_ID)
            message(60) {                               // track_descriptor
                uint64(1, Tracks.EVENTS.toLong())       // uuid = 9
                string(2, "Events")                     // name
                // No message(8) {} — absence of counter{} makes it a named event track
            }
        }
        // Event packets
        events.forEach { event -> writeEventRecord(root, event) }
    }

    private fun writeEventRecord(root: ProtoWriter, event: EventRecord) {
        val dur = event.durationNs
        if (dur == null) {
            // D-16: TYPE_INSTANT — one packet with name (field 23)
            root.message(1) {
                uint64(8, event.timestampNs)
                uint32(10, SEQ_ID)
                uint32(58, CLOCK_BOOTTIME)
                message(11) {                               // track_event
                    uint64(11, Tracks.EVENTS.toLong())      // track_uuid
                    uint32(9, TRACK_EVENT_TYPE_INSTANT)     // type = 3
                    string(TRACK_EVENT_NAME_FIELD, event.name)
                }
            }
        } else {
            // D-17: TYPE_SLICE_BEGIN at timestampNs — with name (field 23)
            root.message(1) {
                uint64(8, event.timestampNs)
                uint32(10, SEQ_ID)
                uint32(58, CLOCK_BOOTTIME)
                message(11) {
                    uint64(11, Tracks.EVENTS.toLong())
                    uint32(9, TRACK_EVENT_TYPE_SLICE_BEGIN) // type = 1
                    string(TRACK_EVENT_NAME_FIELD, event.name)
                }
            }
            // D-17 + Pitfall 4: TYPE_SLICE_END at timestampNs + durationNs — NO name
            root.message(1) {
                uint64(8, event.timestampNs + dur)
                uint32(10, SEQ_ID)
                uint32(58, CLOCK_BOOTTIME)
                message(11) {
                    uint64(11, Tracks.EVENTS.toLong())
                    uint32(9, TRACK_EVENT_TYPE_SLICE_END)   // type = 2
                    // No name field — name only on BEGIN, per D-17 + Pitfall 4
                }
            }
        }
    }

    private fun writeIssuePackets(root: ProtoWriter, issues: List<IssueRecord>) {
        root.message(1) {
            uint32(10, SEQ_ID)
            message(60) {                               // track_descriptor
                uint64(1, Tracks.ISSUES.toLong())       // uuid = 10
                string(2, "Issues")
                // No message(8) {} — absence of counter{} makes it a named event track
            }
        }
        issues.forEach { record -> writeIssueRecord(root, record) }
    }

    private fun writeIssueRecord(root: ProtoWriter, record: IssueRecord) {
        val issue = record.issue
        val dur = issue.durationMs?.times(MS_TO_NS)
        val name = "${issue.type.name} [${issue.severity.name}]"
        if (dur == null) {
            root.message(1) {
                uint64(8, record.timestampNs)
                uint32(10, SEQ_ID)
                uint32(58, CLOCK_BOOTTIME)
                message(11) {
                    uint64(11, Tracks.ISSUES.toLong())
                    uint32(9, TRACK_EVENT_TYPE_INSTANT)
                    string(TRACK_EVENT_NAME_FIELD, name)
                }
            }
        } else {
            root.message(1) {
                uint64(8, record.timestampNs)
                uint32(10, SEQ_ID)
                uint32(58, CLOCK_BOOTTIME)
                message(11) {
                    uint64(11, Tracks.ISSUES.toLong())
                    uint32(9, TRACK_EVENT_TYPE_SLICE_BEGIN)
                    string(TRACK_EVENT_NAME_FIELD, name)
                }
            }
            root.message(1) {
                uint64(8, record.timestampNs + dur)
                uint32(10, SEQ_ID)
                uint32(58, CLOCK_BOOTTIME)
                message(11) {
                    uint64(11, Tracks.ISSUES.toLong())
                    uint32(9, TRACK_EVENT_TYPE_SLICE_END)
                }
            }
        }
    }
}
