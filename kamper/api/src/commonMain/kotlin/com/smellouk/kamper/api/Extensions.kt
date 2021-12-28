package com.smellouk.kamper.api

fun Long.toMb(): Float {
    return this / KILO / KILO
}

fun Long.millisToSeconds(): Double = this / MILLIS_TO_SECONDS_UNIT

private const val KILO = 1024F
private const val MILLIS_TO_SECONDS_UNIT = 1000000000.0
