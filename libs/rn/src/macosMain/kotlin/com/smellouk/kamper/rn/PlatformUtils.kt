package com.smellouk.kamper.rn

import platform.Foundation.NSUUID
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

internal actual fun generateId(): String = NSUUID().UUIDString()

private const val MS_PER_SECOND = 1000.0

internal actual fun currentTimeMs(): Long = (NSDate().timeIntervalSince1970 * MS_PER_SECOND).toLong()
