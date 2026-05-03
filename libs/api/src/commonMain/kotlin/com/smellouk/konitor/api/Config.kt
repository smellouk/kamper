package com.smellouk.konitor.api

interface Config {
    val isEnabled: Boolean
    val intervalInMs: Long
}

const val INVALID_TIME_INTERVAL = -1111L
