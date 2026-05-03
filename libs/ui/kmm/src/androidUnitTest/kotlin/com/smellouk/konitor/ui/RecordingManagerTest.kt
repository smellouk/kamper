package com.smellouk.konitor.ui

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Suppress("IllegalIdentifier")
class RecordingManagerTest {
    private val classToTest = RecordingManager()

    @BeforeTest
    fun setUp() {
        classToTest.clearRecording()
    }

    // ── Record guard ──────────────────────────────────────────────────────────

    @Test
    fun `record should not add sample when not recording`() {
        classToTest.record(Tracks.CPU, 50.0)
        assertEquals(0, classToTest.recordingSampleCount.value)
    }

    // ── Start recording ───────────────────────────────────────────────────────

    @Test
    fun `startRecording should set isRecording to true`() {
        classToTest.startRecording()
        assertTrue(classToTest.isRecording.value)
    }

    @Test
    fun `startRecording then record should increment sample count`() {
        classToTest.startRecording()
        classToTest.record(Tracks.CPU, 50.0)
        assertEquals(1, classToTest.recordingSampleCount.value)
    }

    @Test
    fun `startRecording clears previous buffer`() {
        classToTest.startRecording()
        classToTest.record(Tracks.CPU, 1.0)
        classToTest.startRecording()
        assertEquals(0, classToTest.recordingSampleCount.value)
    }

    // ── Buffer cap ────────────────────────────────────────────────────────────

    @Test
    fun `buffer should cap at MAX_RECORDING_SAMPLES`() {
        classToTest.startRecording()
        repeat(DEFAULT_MAX_RECORDING_SAMPLES + 10) { i ->
            classToTest.record(Tracks.CPU, i.toDouble())
        }
        assertEquals(DEFAULT_MAX_RECORDING_SAMPLES, classToTest.recordingSampleCount.value)
    }

    // ── Stop recording ────────────────────────────────────────────────────────

    @Test
    fun `stopRecording should set isRecording to false`() {
        classToTest.startRecording()
        classToTest.stopRecording()
        assertFalse(classToTest.isRecording.value)
    }

    @Test
    fun `record should not add sample after stopRecording`() {
        classToTest.startRecording()
        classToTest.record(Tracks.CPU, 1.0)
        classToTest.stopRecording()
        classToTest.record(Tracks.CPU, 2.0)
        assertEquals(1, classToTest.recordingSampleCount.value)
    }

    // ── Clear recording ───────────────────────────────────────────────────────

    @Test
    fun `clearRecording should reset isRecording to false`() {
        classToTest.startRecording()
        classToTest.clearRecording()
        assertFalse(classToTest.isRecording.value)
    }

    @Test
    fun `clearRecording should reset recordingSampleCount to zero`() {
        classToTest.startRecording()
        classToTest.record(Tracks.CPU, 1.0)
        classToTest.clearRecording()
        assertEquals(0, classToTest.recordingSampleCount.value)
    }

    // ── Export ────────────────────────────────────────────────────────────────

    @Test
    fun `exportTrace should return non-null ByteArray`() {
        val result = classToTest.exportTrace()
        assertNotNull(result)
    }
}

