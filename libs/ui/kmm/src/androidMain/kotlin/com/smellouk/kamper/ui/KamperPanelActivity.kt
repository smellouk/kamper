package com.smellouk.kamper.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.FileProvider
import com.smellouk.kamper.ui.compose.KamperPanel
import java.io.File
import java.io.FileOutputStream

class KamperPanelActivity : AppCompatActivity() {

    private val isLeanback by lazy {
        packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }

    // Owned here so dispatchKeyEvent (Activity-level) can mutate it reliably on TV.
    private var selectedTab by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)

        val repo = KamperUi.repository
        if (repo == null) {
            finish()
            return
        }

        val view = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                KamperPanel(
                    state                = repo.state,
                    settings             = repo.settings,
                    isRecording          = repo.isRecording,
                    recordingSampleCount = repo.recordingSampleCount,
                    maxRecordingSamples  = repo.maxRecordingSamples,
                    onSettingsChange     = { repo.updateSettings(it) },
                    onClearIssues        = { repo.clearIssues() },
                    onStartRecording     = { repo.startRecording() },
                    onStopRecording      = { repo.stopRecording() },
                    onExportTrace        = { sharePerfettoTrace() },
                    onStartEngine        = { repo.startEngine() },
                    onStopEngine         = { repo.stopEngine() },
                    onRestartEngine      = { repo.restartEngine() },
                    onDismiss            = ::finish,
                    onClearEvents        = { repo.clearEvents() },
                    externalTab          = selectedTab,
                    onTabChange          = { selectedTab = it },
                    isTv                 = isLeanback
                )
            }
        }
        setContentView(view)
        view.requestFocus()
    }

    // D-pad left/right: let Compose handle intra-tab navigation first (e.g. Stop→Restart buttons).
    // Only switch tabs when Compose has nothing left to focus in that direction.
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (isLeanback && event.action == KeyEvent.ACTION_DOWN &&
            (event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT || event.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)
        ) {
            val composeHandled = super.dispatchKeyEvent(event)
            if (!composeHandled) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_DPAD_LEFT  -> selectedTab = (selectedTab - 1 + TAB_COUNT) % TAB_COUNT
                    KeyEvent.KEYCODE_DPAD_RIGHT -> selectedTab = (selectedTab + 1) % TAB_COUNT
                }
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun sharePerfettoTrace() {
        val repo = KamperUi.repository ?: return
        if (repo.recordingSampleCount.value == 0) return
        val fileName = "kamper_${System.currentTimeMillis()}.perfetto-trace.gz"
        val file = File(cacheDir, fileName)
        FileOutputStream(file).use { fos -> repo.exportTraceToFile(fos) }
        val uri = FileProvider.getUriForFile(this, "$packageName.kamper.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/gzip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share Perfetto trace"))
    }

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    private companion object {
        const val TAB_COUNT = 5
    }
}
