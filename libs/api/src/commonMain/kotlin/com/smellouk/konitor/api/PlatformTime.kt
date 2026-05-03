package com.smellouk.konitor.api

/**
 * Wall-clock millisecond timestamp used internally by Performance to record
 * when each sample was delivered (FEAT-03 / Engine.validate()). Internal:
 * not part of the public Konitor API.
 */
internal expect fun currentApiTimeMs(): Long
