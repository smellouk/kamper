package com.smellouk.kamper.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smellouk.kamper.Kamper

/**
 * Toggles Kamper monitoring on/off in response to the
 * `com.smellouk.kamper.CONFIGURE` broadcast intent.
 *
 * Send via ADB or in-process:
 * ```
 * adb shell am broadcast -a com.smellouk.kamper.CONFIGURE --ez enabled false
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
class KamperConfigReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val enabled = intent.getBooleanExtra(EXTRA_ENABLED, true)
        if (enabled) Kamper.start() else Kamper.stop()
    }

    private companion object {
        private const val EXTRA_ENABLED = "enabled"
    }
}
