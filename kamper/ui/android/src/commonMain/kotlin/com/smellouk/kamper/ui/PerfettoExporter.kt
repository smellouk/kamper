package com.smellouk.kamper.ui

// ── Binary protobuf writer ────────────────────────────────────────────────────

internal class ProtoWriter {
    private val buf = mutableListOf<Byte>()

    private fun writeByte(b: Int) { buf.add((b and 0xFF).toByte()) }

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
        repeat(8) { writeByte((v and 0xFF).toInt()); v = v ushr 8 }
    }

    private fun writeTag(field: Int, wireType: Int) {
        writeVarint(((field.toLong() shl 3) or wireType.toLong()))
    }

    fun uint64(field: Int, value: Long)  { writeTag(field, 0); writeVarint(value) }
    fun uint32(field: Int, value: Int)   { writeTag(field, 0); writeVarint(value.toLong()) }
    fun double(field: Int, value: Double){ writeTag(field, 1); writeFixed64(value.toBits()) }

    fun string(field: Int, value: String) {
        val bytes = value.encodeToByteArray()
        writeTag(field, 2)
        writeVarint(bytes.size.toLong())
        bytes.forEach { buf.add(it) }
    }

    fun message(field: Int, block: ProtoWriter.() -> Unit) {
        val inner = ProtoWriter()
        inner.block()
        val bytes = inner.toByteArray()
        writeTag(field, 2)
        writeVarint(bytes.size.toLong())
        bytes.forEach { buf.add(it) }
    }

    fun toByteArray(): ByteArray = buf.toByteArray()
}

// ── Perfetto trace encoder ────────────────────────────────────────────────────
//
// Protobuf field references (Perfetto trace.proto / track_event.proto):
//   Trace.packet                       = 1  (repeated message)
//   TracePacket.timestamp              = 8  (uint64, nanoseconds)
//   TracePacket.trusted_packet_seq_id  = 10 (uint32)
//   TracePacket.track_event            = 11 (message)
//   TracePacket.track_descriptor       = 60 (message)
//   TracePacket.timestamp_clock_id     = 58 (uint32, 1=REALTIME)
//   TrackDescriptor.uuid               = 1  (uint64)
//   TrackDescriptor.name               = 2  (string)
//   TrackDescriptor.counter            = 8  (empty message → counter track)
//   TrackEvent.track_uuid              = 11 (uint64)
//   TrackEvent.type                    = 9  (int32, 4=TYPE_COUNTER)
//   TrackEvent.double_value            = 44 (double — counter_value_field oneof)

internal object PerfettoExporter {

    private const val SEQ_ID = 1
    private const val CLOCK_REALTIME = 1

    fun export(samples: List<RecordedSample>): ByteArray {
        val root = ProtoWriter()

        // Emit one TrackDescriptor per metric track
        Tracks.ALL.forEach { (trackId, trackName) ->
            root.message(1) {              // Trace.packet
                uint32(10, SEQ_ID)
                message(60) {             // track_descriptor
                    uint64(1, trackId.toLong())
                    string(2, trackName)
                    message(8) {}         // counter {} marks this as a counter track
                }
            }
        }

        // Emit one counter event per recorded sample
        samples.forEach { sample ->
            root.message(1) {             // Trace.packet
                uint64(8, sample.timestampNs)
                uint32(10, SEQ_ID)
                uint32(58, CLOCK_REALTIME)
                message(11) {             // track_event
                    uint64(11, sample.trackId.toLong())
                    uint32(9, 4)          // TYPE_COUNTER
                    double(44, sample.value)
                }
            }
        }

        return root.toByteArray()
    }
}
