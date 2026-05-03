package com.smellouk.kamper.ui

import com.smellouk.kamper.EventRecord
import java.io.OutputStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal const val DEFAULT_MAX_RECORDING_SAMPLES = 4_200 // ~10 min at 7 metrics/s

internal class RecordingManager(
    private val maxSamples: Int = DEFAULT_MAX_RECORDING_SAMPLES
) {
    // Guards every read or write of [recordingBuffer]. Six concurrent InfoListener
    // threads (CPU, memory, network, jank, GC, thermal) call record() while the UI
    // thread may be invoking exportTrace / exportTraceToFile. Without this lock the
    // ArrayDeque mutations race (removeFirst + addLast not atomic) and toList()
    // produces a torn snapshot. See 08-REVIEW.md CR-04.
    private val bufferLock = Any()
    private val recordingBuffer = ArrayDeque<RecordedSample>()
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    private val _recordingSampleCount = MutableStateFlow(0)
    val recordingSampleCount: StateFlow<Int> = _recordingSampleCount.asStateFlow()

    private fun nowNs(): Long = android.os.SystemClock.elapsedRealtimeNanos()

    fun record(trackId: Int, value: Double) {
        if (!_isRecording.value) return
        synchronized(bufferLock) {
            if (recordingBuffer.size >= maxSamples) recordingBuffer.removeFirst()
            recordingBuffer.addLast(RecordedSample(nowNs(), trackId, value))
            _recordingSampleCount.value = recordingBuffer.size
        }
    }

    fun startRecording() {
        synchronized(bufferLock) {
            recordingBuffer.clear()
            _recordingSampleCount.value = 0
        }
        _isRecording.value = true
    }

    fun stopRecording() {
        _isRecording.value = false
    }

    /**
     * Encodes the current recording buffer as a Perfetto trace ByteArray.
     * [events] and [issues] are folded into their respective named event tracks.
     * Both default to empty for backward compatibility.
     */
    fun exportTrace(
        events: List<EventRecord> = emptyList(),
        issues: List<IssueRecord> = emptyList()
    ): ByteArray {
        val snapshot = synchronized(bufferLock) { recordingBuffer.toList() }
        return PerfettoExporter.export(snapshot, events, issues)
    }

    /**
     * Streams the current recording buffer to [out] as a gzip-compressed Perfetto trace.
     * [events] and [issues] are folded into their respective named event tracks.
     * The caller owns [out] (typically a [java.io.FileOutputStream]) and must close it
     * after this method returns. Memory-efficient alternative to [exportTrace] for long
     * recordings — does not hold the protobuf bytes in memory.
     */
    fun exportTraceToFile(
        out: OutputStream,
        events: List<EventRecord> = emptyList(),
        issues: List<IssueRecord> = emptyList()
    ) {
        val snapshot = synchronized(bufferLock) { recordingBuffer.toList() }
        PerfettoExporter.exportToFile(snapshot, events, issues, out)
    }

    fun clearRecording() {
        synchronized(bufferLock) {
            recordingBuffer.clear()
            _recordingSampleCount.value = 0
        }
        _isRecording.value = false
    }
}
