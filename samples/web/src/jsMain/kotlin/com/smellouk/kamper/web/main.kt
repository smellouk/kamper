package com.smellouk.kamper.web

import com.smellouk.kamper.web.ui.App
import kotlinx.browser.document

fun main() {
    document.addEventListener("DOMContentLoaded", { App.init() })
}
