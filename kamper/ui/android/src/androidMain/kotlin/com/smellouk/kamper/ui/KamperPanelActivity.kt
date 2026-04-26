package com.smellouk.kamper.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.FileProvider
import com.smellouk.kamper.ui.compose.KamperPanel
import java.io.File
import java.io.FileOutputStream

class KamperPanelActivity : AppCompatActivity() {
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
                    onDismiss            = ::finish
                )
            }
        }
        setContentView(view)
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
}
