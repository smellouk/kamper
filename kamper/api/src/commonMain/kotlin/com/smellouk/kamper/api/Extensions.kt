package com.smellouk.kamper.api

fun Long.toMb(): Float {
    return this / KILO / KILO
}

private const val KILO = 1024F
