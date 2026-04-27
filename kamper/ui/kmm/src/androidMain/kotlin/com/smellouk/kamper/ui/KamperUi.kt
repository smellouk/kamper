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
        val repo = KamperUiRepository(app, config.maxRecordingSamples.coerceAtLeast(100))
            .also { repository = it }
        overlayManager = AndroidOverlayManager(app, repo.state, repo.settings, config, repo::clearIssues).also { it.show() }
    }

    actual fun detach() {
        overlayManager?.hide()
        repository?.clear()
        overlayManager = null
        repository = null
    }

    /**
     * Public facade — attach overlay using the supplied Context.
     * External Gradle modules call this since `attach(context)` is internal.
     */
    fun show(context: Context) = attach(context)

    /**
     * Public facade — required by `expect object KamperUi.hide()`.
     */
    actual fun hide() = detach()
}
