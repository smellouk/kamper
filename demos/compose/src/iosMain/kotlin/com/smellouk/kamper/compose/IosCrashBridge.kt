package com.smellouk.kamper.compose

import com.smellouk.kamper.issues.Issue

internal object IosCrashBridge {
    var onCrash: ((Issue) -> Unit)? = null
}
