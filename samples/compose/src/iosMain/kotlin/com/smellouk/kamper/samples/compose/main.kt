package com.smellouk.kamper.samples.compose

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

@Suppress("FunctionName", "unused")
fun MainViewController(): UIViewController = ComposeUIViewController { App() }
