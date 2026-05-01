package com.smellouk.kamper.rn

import java.util.UUID

internal actual fun generateId(): String = UUID.randomUUID().toString()

internal actual fun currentTimeMs(): Long = System.currentTimeMillis()
