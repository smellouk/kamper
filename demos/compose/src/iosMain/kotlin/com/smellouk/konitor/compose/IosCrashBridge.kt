package com.smellouk.konitor.compose

import com.smellouk.konitor.issues.Issue

internal object IosCrashBridge {
    var onCrash: ((Issue) -> Unit)? = null
}
