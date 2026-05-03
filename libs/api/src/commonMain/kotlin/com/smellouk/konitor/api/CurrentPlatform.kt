package com.smellouk.konitor.api

/**
 * Platform tag used to stamp [KonitorEvent.platform]. One of:
 * "android", "ios", "jvm", "macos", "js", "wasmjs", "tvos".
 *
 * Mirrors the existing `expect val Logger.Companion.DEFAULT` precedent — actuals live
 * in each konitor-api source set.
 */
public expect val currentPlatform: String
