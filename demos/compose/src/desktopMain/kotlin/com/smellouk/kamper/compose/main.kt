package com.smellouk.kamper.compose

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

fun main() = application {
    val windowState = rememberWindowState(size = DpSize(820.dp, 640.dp))
    Window(
        onCloseRequest = ::exitApplication,
        title = appTitle,
        state = windowState
    ) {
        App()
    }
}
