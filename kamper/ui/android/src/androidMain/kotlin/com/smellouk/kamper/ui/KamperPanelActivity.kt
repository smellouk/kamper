package com.smellouk.kamper.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.smellouk.kamper.ui.compose.KamperPanel

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
                    onStartEngine = { repo.startEngine() },
                    onStopEngine = { repo.stopEngine() },
                    onRestartEngine = { repo.restartEngine() },
                    onDismiss = ::finish
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
}
