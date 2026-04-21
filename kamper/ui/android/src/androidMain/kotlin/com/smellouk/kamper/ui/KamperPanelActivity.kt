package com.smellouk.kamper.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.FileProvider
import com.smellouk.kamper.ui.compose.KamperPanel
import java.io.File

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
                    state = repo.state,
                    settings = repo.settings,
                    onSettingsChange = { repo.updateSettings(it) },
                    onClearIssues = { repo.clearIssues() },
                    onDismiss = ::finish,
                    onStartCapture = { repo.startCapture() },
                    onStopCapture = { repo.stopCapture() },
                    onShareTrace = ::shareTrace
                )
            }
        }
        setContentView(view)
    }

    override fun finish() {
        super.finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }

    private fun shareTrace() {
        val filePath = KamperUi.repository?.state?.value?.traceFilePath ?: return
        val file = File(filePath)
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(this, "$packageName.kamper.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share Perfetto Trace"))
    }
}
