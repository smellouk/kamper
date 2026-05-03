package com.smellouk.konitor.ui

internal interface PreferencesStore {
    fun getBoolean(key: String, default: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    fun getLong(key: String, default: Long): Long
    fun putLong(key: String, value: Long)
    fun getFloat(key: String, default: Float): Float
    fun putFloat(key: String, value: Float)
    fun getInt(key: String, default: Int): Int
    fun putInt(key: String, value: Int)
    fun getString(key: String, default: String): String
    fun putString(key: String, value: String)
}
