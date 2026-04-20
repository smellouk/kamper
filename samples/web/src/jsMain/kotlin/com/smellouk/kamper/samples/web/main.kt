package com.smellouk.kamper.samples.web

import com.smellouk.kamper.samples.web.ui.App
import kotlinx.browser.document

fun main() {
    document.addEventListener("DOMContentLoaded", { App.init() })
}
