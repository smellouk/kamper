package com.smellouk.kamper.api

import android.util.Log

actual val Logger.Companion.DEFAULT: Logger
    get() = object : Logger {
        override fun log(message: String) {
            Log.i("Kamper", message)
        }
    }
