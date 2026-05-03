package com.smellouk.konitor.ui

import platform.Foundation.NSUserDefaults

internal class ApplePreferencesStore : PreferencesStore {
    private val defaults = NSUserDefaults.standardUserDefaults

    override fun getBoolean(key: String, default: Boolean): Boolean =
        if (defaults.objectForKey(key) != null) defaults.boolForKey(key) else default

    override fun putBoolean(key: String, value: Boolean) {
        defaults.setBool(value, key)
    }

    override fun getLong(key: String, default: Long): Long =
        if (defaults.objectForKey(key) != null) defaults.integerForKey(key).toLong() else default

    override fun putLong(key: String, value: Long) {
        defaults.setInteger(value, key)
    }

    override fun getFloat(key: String, default: Float): Float =
        if (defaults.objectForKey(key) != null) defaults.floatForKey(key) else default

    override fun putFloat(key: String, value: Float) {
        defaults.setFloat(value, key)
    }

    override fun getInt(key: String, default: Int): Int =
        if (defaults.objectForKey(key) != null) defaults.integerForKey(key).toInt() else default

    override fun putInt(key: String, value: Int) {
        defaults.setInteger(value.toLong(), key)
    }

    override fun getString(key: String, default: String): String =
        defaults.stringForKey(key) ?: default

    override fun putString(key: String, value: String) {
        defaults.setObject(value, key)
    }
}
