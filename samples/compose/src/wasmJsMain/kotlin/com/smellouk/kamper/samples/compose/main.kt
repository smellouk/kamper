package com.smellouk.kamper.samples.compose

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    CanvasBasedWindow("Kamper Performance") {
        App()
    }
}
