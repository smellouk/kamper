package com.smellouk.kamper.ui

import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.ApplicationInfo
import android.database.Cursor
import android.net.Uri

/**
 * Auto-initializes Kamper UI only in debuggable builds.
 *
 * This is a development convenience — it is not a security control.
 * [android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE] can be spoofed on rooted devices and
 * must not be relied upon as a security boundary.
 *
 * For production environments that require explicit initialization control, disable this
 * provider in your `AndroidManifest.xml`:
 *
 * ```xml
 * <provider
 *     android:name="com.smellouk.kamper.ui.KamperUiInitProvider"
 *     android:authorities="${applicationId}.kamper_ui_init"
 *     android:enabled="false"
 *     tools:replace="android:enabled" />
 * ```
 */
class KamperUiInitProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        val ctx = context ?: return false
        val isDebug = (ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        KamperUi.configure { isEnabled = isDebug }
        KamperUi.attach(ctx)
        return true
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
