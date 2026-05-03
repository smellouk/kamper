package com.smellouk.konitor.ui

import android.app.Application
import android.content.Context

actual object KonitorUi {
    internal var config: KonitorUiConfig = KonitorUiConfig()
    internal var repository: KonitorUiRepository? = null
    private var overlayManager: AndroidOverlayManager? = null

    actual fun configure(block: KonitorUiConfig.() -> Unit) {
        config = KonitorUiConfig().apply(block)
    }

    actual fun attach() {
        // Intentionally empty: ContentProvider calls attach(context) automatically.
    }

    internal fun attach(context: Context) {
        if (!config.isEnabled) return
        if (overlayManager != null) return
        val app = context.applicationContext as Application
        val repo = KonitorUiRepository(app, config.maxRecordingSamples.coerceAtLeast(100))
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
     * Re-registers the UI event listener on the Konitor singleton.
     * Call this after Konitor.clear() to restore chip event delivery.
     */
    fun reattachEventListener() {
        repository?.reattachKonitorListener()
    }

    /**
     * Public facade — required by `expect object KonitorUi.hide()`.
     */
    actual fun hide() = detach()
}
