package com.smellouk.kamper.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSinceReferenceDate

private const val DEFAULT_MAX_RECORDING_SAMPLES = 4_200 // ~10 min at 7 metrics/s

internal class RecordingManager(
    private val maxSamples: Int = DEFAULT_MAX_RECORDING_SAMPLES
) {
    private val recordingBuffer = ArrayDeque<RecordedSample>()
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    private val _recordingSampleCount = MutableStateFlow(0)
    val recordingSampleCount: StateFlow<Int> = _recordingSampleCount.asStateFlow()

    private fun nowNs(): Long =
        (NSDate.timeIntervalSinceReferenceDate * 1_000_000_000).toLong()

    fun record(trackId: Int, value: Double) {
        if (!_isRecording.value) return
        if (recordingBuffer.size >= maxSamples) recordingBuffer.removeFirst()
        recordingBuffer.addLast(RecordedSample(nowNs(), trackId, value))
        _recordingSampleCount.value = recordingBuffer.size
    }

    fun startRecording() {
        recordingBuffer.clear()
        _recordingSampleCount.value = 0
        _isRecording.value = true
    }

    fun stopRecording() {
        _isRecording.value = false
    }

    fun exportTrace(): ByteArray = ByteArray(0) // Perfetto export is Android-only

    fun clearRecording() {
        recordingBuffer.clear()
        _recordingSampleCount.value = 0
        _isRecording.value = false
    }
}
