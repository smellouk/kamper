package com.smellouk.kamper.ui

import android.app.Application
import android.content.Context

actual object KamperUi {
    internal var config: KamperUiConfig = KamperUiConfig()
    internal var repository: KamperUiRepository? = null
    private var overlayManager: AndroidOverlayManager? = null

    actual fun configure(block: KamperUiConfig.() -> Unit) {
        config = KamperUiConfig().apply(block)
    }

    actual fun attach() {
        // Intentionally empty: ContentProvider calls attach(context) automatically.
    }

    internal fun attach(context: Context) {
        if (!config.isEnabled) return
        val app = context.applicationContext as Application
        val repo = KamperUiRepository(app).also { repository = it }
        overlayManager = AndroidOverlayManager(app, repo.state, repo.settings, config).also { it.show() }
    }

    actual fun detach() {
        overlayManager?.hide()
        repository?.clear()
        overlayManager = null
        repository = null
    }
}
