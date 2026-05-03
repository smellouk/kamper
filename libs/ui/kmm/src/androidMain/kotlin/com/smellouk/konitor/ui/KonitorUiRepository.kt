package com.smellouk.konitor.ui

import android.app.Application
import android.content.Context
import com.smellouk.konitor.Konitor
import com.smellouk.konitor.api.InfoListener
import com.smellouk.konitor.api.UserEventInfo
import java.io.OutputStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal actual class KonitorUiRepository(
    context: Context,
    actual val maxRecordingSamples: Int = 4_200
) {
    private val appContext = context.applicationContext as Application
    private val preferencesStore = AndroidPreferencesStore(context)
    private val _state = MutableStateFlow(KonitorUiState.EMPTY)
    private val settingsRepository = SettingsRepository(preferencesStore)
    private val recordingManager = RecordingManager(maxSamples = maxRecordingSamples)
    private val lifecycleManager = ModuleLifecycleManager(
        appContext        = appContext,
        state             = _state,
        recordingManager  = recordingManager,
        preferencesStore  = preferencesStore
    )

    actual val state: StateFlow<KonitorUiState> = _state.asStateFlow()
    actual val settings: StateFlow<KonitorUiSettings> = settingsRepository.settings
    actual val isRecording: StateFlow<Boolean> = recordingManager.isRecording
    actual val recordingSampleCount: StateFlow<Int> = recordingManager.recordingSampleCount

    private val eventListener: InfoListener<UserEventInfo> = { info ->
        if (info != UserEventInfo.INVALID) {
            val entry = EventEntry(info.name, info.durationMs, System.currentTimeMillis())
            _state.update { s ->
                val updated = (listOf(entry) + s.events).take(MAX_EVENTS)
                s.copy(events = updated)
            }
            saveEvents(_state.value.events)
        }
    }

    init {
        lifecycleManager.initialise(settingsRepository.settings.value)
        val persisted = loadPersistedEvents()
        if (persisted.isNotEmpty()) _state.update { it.copy(events = persisted) }
        Konitor.addInfoListener(eventListener)
    }

    actual fun updateSettings(s: KonitorUiSettings) {
        val old = settingsRepository.settings.value
        val normalized = s.copy(
            showJank    = if (!old.jankEnabled    && s.jankEnabled)    true else s.showJank,
            showGc      = if (!old.gcEnabled      && s.gcEnabled)      true else s.showGc,
            showThermal = if (!old.thermalEnabled && s.thermalEnabled) true else s.showThermal,
            showGpu     = if (!old.gpuEnabled     && s.gpuEnabled)     true else s.showGpu
        )
        settingsRepository.updateSettings(normalized)
        lifecycleManager.applySettings(old, normalized)
    }

    actual fun clearIssues() = lifecycleManager.clearIssues()
    actual fun clearEvents() {
        _state.update { it.copy(events = emptyList()) }
        preferencesStore.putString(PREF_EVENTS, "")
    }
    actual fun startRecording() = recordingManager.startRecording()
    actual fun stopRecording() = recordingManager.stopRecording()
    actual fun exportTrace(): ByteArray {
        val events = Konitor.drainEvents()
        val issues = lifecycleManager.snapshotIssueRecords()
        return recordingManager.exportTrace(events, issues)
    }

    /**
     * Streams the current recording buffer to [out] as a gzip-compressed Perfetto trace.
     * Drains [Konitor.drainEvents()] to fold custom events into the "Events" track, and
     * snapshots issue records to fold detected issues into the "Issues" track.
     * The caller owns [out] (typically a [java.io.FileOutputStream]) and must close it
     * after this method returns. Memory-efficient alternative to [exportTrace] for long
     * recordings — does not hold the protobuf bytes in memory.
     */
    fun exportTraceToFile(out: OutputStream) {
        val events = Konitor.drainEvents()
        val issues = lifecycleManager.snapshotIssueRecords()
        recordingManager.exportTraceToFile(out, events, issues)
    }

    actual fun clearRecording() = recordingManager.clearRecording()
    actual fun startEngine() = lifecycleManager.startEngine()
    actual fun stopEngine() = lifecycleManager.stopEngine()
    actual fun restartEngine() = lifecycleManager.restartEngine()

    fun reattachKonitorListener() {
        Konitor.addInfoListener(eventListener)
    }

    actual fun clear() {
        Konitor.removeInfoListener(eventListener)
        lifecycleManager.clear()
        settingsRepository.clear()
    }

    private fun loadPersistedEvents(): List<EventEntry> {
        val raw = preferencesStore.getString(PREF_EVENTS, "")
        if (raw.isEmpty()) return emptyList()
        return raw.split('').mapNotNull { it.deserializeEventEntry() }
    }

    private fun saveEvents(events: List<EventEntry>) {
        preferencesStore.putString(PREF_EVENTS, events.joinToString("") { it.serialize() })
    }

    private companion object {
        const val MAX_EVENTS = 200
        const val PREF_EVENTS = "konitor_events_list"
    }
}
