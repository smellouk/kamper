package com.smellouk.kamper.api

/**
 * Platform tag used to stamp [KamperEvent.platform]. One of:
 * "android", "ios", "jvm", "macos", "js", "wasmjs", "tvos".
 *
 * Mirrors the existing `expect val Logger.Companion.DEFAULT` precedent — actuals live
 * in each kamper-api source set.
 */
public expect val currentPlatform: String
