package com.smellouk.konitor.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smellouk.konitor.Konitor

/**
 * Toggles Konitor monitoring on/off in response to the
 * `com.smellouk.konitor.CONFIGURE` broadcast intent.
 *
 * Send via ADB or in-process:
 * ```
 * adb shell am broadcast -a com.smellouk.konitor.CONFIGURE --ez enabled false
 * ```
 *
 * The receiver is declared with `android:exported="false"` in the library
 * manifest, so only the host app's process or the ADB shell may dispatch it.
 * No custom permission required.
 *
 * Safe default: if the `enabled` extra is missing, the receiver treats the
 * intent as enabled=true rather than silently disabling monitoring (D-07,
 * RESEARCH T-7-02 — Tampering: malformed extras).
 */
class KonitorConfigReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val enabled = intent.getBooleanExtra(EXTRA_ENABLED, true)
        if (enabled) Konitor.start() else Konitor.stop()
    }

    private companion object {
        private const val EXTRA_ENABLED = "enabled"
    }
}
